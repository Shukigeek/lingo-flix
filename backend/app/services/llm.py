"""LLM layer: smart quizzes, explanations, translations and the self-test harness.

Providers (in priority order):
  1. Anthropic Claude (official SDK) when LINGO_ANTHROPIC_API_KEY is set
  2. Google Gemini via REST when LINGO_GEMINI_API_KEY is set
  3. Heuristic engine (always available, offline)

Every call is cached in the ``llm_cache`` table keyed by a hash of the task + input.
"""
from __future__ import annotations

import asyncio
import hashlib
import json
import logging
import random
from typing import Any

import httpx
from pydantic import BaseModel, Field
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from ..config import get_settings
from ..models import LlmCache
from . import quiz_engine as qe

log = logging.getLogger("lingoflix.llm")

LANG_NAMES = {
    "en": "English", "he": "Hebrew", "es": "Spanish", "fr": "French", "de": "German", "it": "Italian",
    "pt": "Portuguese", "ru": "Russian", "ar": "Arabic", "ja": "Japanese", "zh": "Chinese", "ko": "Korean",
}


def lang_name(code: str) -> str:
    return LANG_NAMES.get(code, code)


# ---------- Structured output schemas ----------
class ClozePlan(BaseModel):
    hidden_words: list[str] = Field(description="Exact words from the sentence to hide, in sentence order")
    translation: str
    grammar_note: str = Field(description="One short note about grammar or usage in the learner's native language")
    hint: str = Field(description="A short hint for the first hidden word, without revealing it")
    distractors: list[list[str]] = Field(description="For each hidden word, 3 plausible wrong options")
    cefr: str = Field(description="CEFR level A1..C2")


class WordExplanationModel(BaseModel):
    word: str
    translation: str
    part_of_speech: str
    definition: str
    example: str


class Explanation(BaseModel):
    translation: str
    word_explanations: list[WordExplanationModel]
    grammar_notes: list[str]
    idioms: list[str]
    cultural_note: str | None = None
    difficulty_cefr: str


class TranslationBatch(BaseModel):
    translations: list[str]


class DifficultyBatch(BaseModel):
    levels: list[str]
    key_words: list[list[str]]


class SolverAnswer(BaseModel):
    words: list[str] = Field(description="Your guess for each blank, in order")
    confidence: float = Field(ge=0, le=1)


class QualityJudgement(BaseModel):
    score: float = Field(ge=0, le=1, description="How good is this cloze for a learner")
    notes: str


# ---------- Provider base ----------
class Provider:
    name = "heuristic"

    async def complete_json(self, system: str, user: str, schema: type[BaseModel]) -> BaseModel | None:
        return None


class AnthropicProvider(Provider):
    name = "anthropic"

    def __init__(self, api_key: str, model: str, fast_model: str):
        import anthropic

        self._client = anthropic.AsyncAnthropic(api_key=api_key)
        self._anthropic = anthropic
        self.model = model
        self.fast_model = fast_model

    async def complete_json(self, system: str, user: str, schema: type[BaseModel], fast: bool = False) -> BaseModel | None:
        try:
            response = await self._client.messages.parse(
                model=self.fast_model if fast else self.model,
                max_tokens=4000,
                system=system,
                messages=[{"role": "user", "content": user}],
                output_format=schema,
            )
        except self._anthropic.RateLimitError:
            log.warning("Anthropic rate limited")
            return None
        except self._anthropic.APIStatusError as e:
            log.warning("Anthropic API error %s: %s", e.status_code, e.message)
            return None
        except self._anthropic.APIConnectionError:
            log.warning("Anthropic connection error")
            return None
        if response.stop_reason == "refusal":
            return None
        return response.parsed_output


class GeminiProvider(Provider):
    name = "gemini"

    def __init__(self, api_key: str, model: str):
        self.api_key = api_key
        self.model = model

    async def complete_json(self, system: str, user: str, schema: type[BaseModel], fast: bool = False) -> BaseModel | None:
        url = f"https://generativelanguage.googleapis.com/v1beta/models/{self.model}:generateContent?key={self.api_key}"
        body = {
            "systemInstruction": {"parts": [{"text": system}]},
            "contents": [{"role": "user", "parts": [{"text": user}]}],
            "generationConfig": {"responseMimeType": "application/json", "temperature": 0.4},
        }
        try:
            async with httpx.AsyncClient(timeout=60) as client:
                r = await client.post(url, json=body)
                r.raise_for_status()
                data = r.json()
            text = data["candidates"][0]["content"]["parts"][0]["text"]
            return schema.model_validate_json(text)
        except Exception as e:  # noqa: BLE001
            log.warning("Gemini failed: %s", e)
            return None


_provider: Provider | None = None


def get_provider() -> Provider:
    global _provider
    if _provider is not None:
        return _provider
    s = get_settings()
    if s.anthropic_api_key:
        try:
            _provider = AnthropicProvider(s.anthropic_api_key, s.anthropic_model, s.anthropic_fast_model)
        except Exception as e:  # noqa: BLE001
            log.warning("Anthropic SDK unavailable: %s", e)
            _provider = Provider()
    elif s.gemini_api_key:
        _provider = GeminiProvider(s.gemini_api_key, s.gemini_model)
    else:
        _provider = Provider()
    return _provider


def set_provider_for_tests(p: Provider | None) -> None:
    global _provider
    _provider = p


# ---------- Cache ----------
def _key(task: str, payload: Any) -> str:
    raw = json.dumps(payload, sort_keys=True, ensure_ascii=False)
    return f"{task}:{hashlib.sha256(raw.encode()).hexdigest()[:40]}"


async def cache_get(db: AsyncSession | None, key: str) -> dict | None:
    if db is None:
        return None
    row = await db.scalar(select(LlmCache).where(LlmCache.cache_key == key))
    if row:
        return json.loads(row.payload)
    return None


async def cache_put(db: AsyncSession | None, key: str, task: str, provider: str, payload: dict) -> None:
    if db is None:
        return
    existing = await db.scalar(select(LlmCache).where(LlmCache.cache_key == key))
    if existing:
        existing.payload = json.dumps(payload, ensure_ascii=False)
        existing.provider = provider
    else:
        db.add(LlmCache(cache_key=key, task=task, provider=provider, payload=json.dumps(payload, ensure_ascii=False)))
    await db.commit()


# ---------- Tasks ----------
SYSTEM_TUTOR = (
    "You are LingoFlix, an expert language tutor who builds exercises from TV-show subtitles. "
    "You choose words that teach the most (content words, idioms, collocations), never proper nouns "
    "or filler words, and you explain clearly in the learner's native language. Respond only with JSON that matches the schema."
)


async def smart_cloze(
    text: str,
    difficulty: str,
    mode: str,
    lang: str,
    native: str,
    db: AsyncSession | None = None,
    use_llm: bool = True,
) -> dict:
    """Return quiz fields. Always succeeds; falls back to heuristics."""
    base = qe.build_cloze(text, difficulty, mode, lang)
    result = {
        "text": text,
        "tokens": base.tokens,
        "hidden_indices": base.hidden_indices,
        "hidden_words": base.hidden_words,
        "masked_text": base.masked_text,
        "choices": base.choices,
        "word_bank": base.word_bank,
        "hint": base.hint,
        "translation": None,
        "grammar_note": None,
        "cefr": base.cefr,
        "difficulty": difficulty,
        "mode": mode,
        "provider": "heuristic",
    }
    provider = get_provider()
    if not use_llm or provider.name == "heuristic" or not base.hidden_indices:
        return result

    key = _key("cloze", {"t": text, "d": difficulty, "m": mode, "l": lang, "n": native})
    cached = await cache_get(db, key)
    plan: ClozePlan | None = None
    if cached:
        plan = ClozePlan.model_validate(cached)
    else:
        n_hide = len(base.hidden_indices)
        user = (
            f"Sentence ({lang_name(lang)}): \"{text}\"\n"
            f"Learner native language: {lang_name(native)}. Difficulty: {difficulty}. Mode: {mode}.\n"
            f"Choose exactly {n_hide} word(s) to hide (they must appear verbatim in the sentence, no punctuation). "
            f"Give 3 distractors per hidden word that are the same part of speech and plausible in context. "
            f"Write translation, grammar_note and hint in {lang_name(native)}."
        )
        plan = await provider.complete_json(SYSTEM_TUTOR, user, ClozePlan)  # type: ignore[arg-type]
        if plan:
            await cache_put(db, key, "cloze", provider.name, plan.model_dump())
    if not plan:
        return result

    # Map the LLM's hidden words back onto token indices; keep heuristic choice if mapping fails.
    indices: list[int] = []
    lowered = [qe.clean_word(t) for t in base.tokens]
    for w in plan.hidden_words:
        cw = qe.clean_word(w)
        for i, t in enumerate(lowered):
            if t == cw and i not in indices:
                indices.append(i)
                break
    if indices:
        indices.sort()
        result["hidden_indices"] = indices
        result["hidden_words"] = [base.tokens[i] for i in indices]
        result["masked_text"] = qe.detokenize(["_____" if i in indices else t for i, t in enumerate(base.tokens)])
        if mode == "multiple_choice":
            rng = random.Random(hash(text) & 0xFFFF)
            choices = []
            for j, i in enumerate(indices):
                target = qe.clean_word(base.tokens[i])
                ds = [qe.clean_word(d) for d in (plan.distractors[j] if j < len(plan.distractors) else [])]
                ds = [d for d in ds if d and d != target][:3]
                while len(ds) < 3:
                    ds.append(qe._mutate(target, rng))
                opts = ds + [target]
                rng.shuffle(opts)
                choices.append(opts)
            result["choices"] = choices
    result["translation"] = plan.translation
    result["grammar_note"] = plan.grammar_note
    result["hint"] = plan.hint or result["hint"]
    if plan.cefr in {"A1", "A2", "B1", "B2", "C1", "C2"}:
        result["cefr"] = plan.cefr
    result["provider"] = provider.name
    return result


async def explain(text: str, focus_words: list[str], lang: str, native: str, db: AsyncSession | None = None) -> dict:
    provider = get_provider()
    key = _key("explain", {"t": text, "f": focus_words, "l": lang, "n": native})
    cached = await cache_get(db, key)
    if cached:
        cached["cached"] = True
        return cached
    if provider.name != "heuristic":
        user = (
            f"Explain this {lang_name(lang)} subtitle line to a {lang_name(native)} speaker.\n"
            f"Line: \"{text}\"\n"
            f"Focus words: {', '.join(focus_words) if focus_words else 'pick the 2-4 most useful words'}.\n"
            f"Write translation, definitions, grammar notes, idioms and cultural note in {lang_name(native)}; keep examples in {lang_name(lang)}."
        )
        ex = await provider.complete_json(SYSTEM_TUTOR, user, Explanation)  # type: ignore[arg-type]
        if ex:
            payload = ex.model_dump()
            payload["provider"] = provider.name
            payload["cached"] = False
            await cache_put(db, key, "explain", provider.name, payload)
            return payload
    # Heuristic fallback
    cefr, _ = qe.estimate_cefr(text, lang)
    words = focus_words or [w for w in qe.tokenize(text) if qe.word_value(w, lang, 1, 5) > 0.5][:3]
    return {
        "translation": "",
        "word_explanations": [
            {"word": w, "translation": "", "part_of_speech": "", "definition": f"'{w}' — {len(qe.clean_word(w))} letters", "example": text}
            for w in words
        ],
        "grammar_notes": [],
        "idioms": [],
        "cultural_note": None,
        "difficulty_cefr": cefr,
        "provider": "heuristic",
        "cached": False,
    }


async def translate_lines(lines: list[str], lang: str, native: str, db: AsyncSession | None = None, chunk: int = 40) -> list[str | None]:
    provider = get_provider()
    if provider.name == "heuristic" or not lines:
        return [None] * len(lines)
    out: list[str | None] = []
    for start in range(0, len(lines), chunk):
        part = lines[start : start + chunk]
        key = _key("translate", {"p": part, "l": lang, "n": native})
        cached = await cache_get(db, key)
        if cached:
            out.extend(cached["translations"])
            continue
        numbered = "\n".join(f"{i + 1}. {t}" for i, t in enumerate(part))
        user = (
            f"Translate each numbered {lang_name(lang)} subtitle line into natural {lang_name(native)}. "
            f"Return exactly {len(part)} translations in order.\n{numbered}"
        )
        res = await provider.complete_json(SYSTEM_TUTOR, user, TranslationBatch)  # type: ignore[arg-type]
        if res and len(res.translations) == len(part):
            await cache_put(db, key, "translate", provider.name, {"translations": res.translations})
            out.extend(res.translations)
        else:
            out.extend([None] * len(part))
    return out


async def rate_lines(lines: list[str], lang: str, db: AsyncSession | None = None, chunk: int = 40) -> list[tuple[str, list[str]]]:
    provider = get_provider()
    fallback = [(qe.estimate_cefr(t, lang)[0], [w for w in qe.tokenize(t) if qe.word_value(w, lang, 1, 5) > 0.55][:3]) for t in lines]
    if provider.name == "heuristic" or not lines:
        return fallback
    out: list[tuple[str, list[str]]] = []
    for start in range(0, len(lines), chunk):
        part = lines[start : start + chunk]
        key = _key("rate", {"p": part, "l": lang})
        cached = await cache_get(db, key)
        if cached:
            out.extend(zip(cached["levels"], cached["key_words"]))
            continue
        numbered = "\n".join(f"{i + 1}. {t}" for i, t in enumerate(part))
        user = (
            f"For each numbered {lang_name(lang)} line give its CEFR level (A1..C2) and the 1-3 most useful vocabulary words to learn.\n"
            f"Return exactly {len(part)} entries in order.\n{numbered}"
        )
        res = await provider.complete_json(SYSTEM_TUTOR, user, DifficultyBatch, fast=True)  # type: ignore[call-arg]
        if res and len(res.levels) == len(part):
            kws = res.key_words if len(res.key_words) == len(part) else [[] for _ in part]
            await cache_put(db, key, "rate", provider.name, {"levels": res.levels, "key_words": kws})
            out.extend(zip(res.levels, kws))
        else:
            out.extend(fallback[start : start + chunk])
    return out


async def solve_cloze(masked_text: str, n_blanks: int, lang: str, choices: list[list[str]] | None = None) -> SolverAnswer | None:
    """Ask the LLM to solve a cloze *without* seeing the answer (used by self-test)."""
    provider = get_provider()
    if provider.name == "heuristic":
        return None
    system = "You are a careful language learner taking a fill-in-the-blank test. Respond only with JSON."
    extra = ""
    if choices:
        extra = "\nOptions per blank: " + json.dumps(choices, ensure_ascii=False)
    user = f"Fill the {n_blanks} blank(s) marked _____ in this {lang_name(lang)} sentence:\n\"{masked_text}\"{extra}"
    res = await provider.complete_json(system, user, SolverAnswer)  # type: ignore[arg-type]
    return res  # type: ignore[return-value]


async def judge_quality(sentence: str, hidden_words: list[str], lang: str) -> QualityJudgement | None:
    provider = get_provider()
    if provider.name == "heuristic":
        return None
    system = "You are a strict reviewer of language-learning exercises. Respond only with JSON."
    user = (
        f"Sentence: \"{sentence}\"\nHidden words: {hidden_words}\n"
        "Score 0..1 how good this cloze is for a learner (1 = teaches useful vocabulary and is solvable from context; "
        "0 = hides a name, filler, or is unsolvable). Add a one-sentence note."
    )
    return await provider.complete_json(system, user, QualityJudgement)  # type: ignore[return-value]


def heuristic_quality(sentence: str, hidden_words: list[str], lang: str) -> float:
    toks = qe.tokenize(sentence)
    n = len(toks)
    if not hidden_words:
        return 0.0
    vals = []
    for w in hidden_words:
        try:
            i = toks.index(w)
        except ValueError:
            i = 1
        vals.append(qe.word_value(w, lang, i, n))
    ratio_hidden = len(hidden_words) / max(1, len([t for t in toks if any(c.isalpha() for c in t)]))
    context_penalty = 0.0 if ratio_hidden <= 0.5 else (ratio_hidden - 0.5)
    return round(max(0.0, min(1.0, sum(vals) / len(vals) - context_penalty)), 3)


async def self_test(sentences: list[str], difficulty: str, lang: str, native: str, use_llm: bool, db: AsyncSession | None = None) -> dict:
    """Generate quizzes then try to solve them blind, and judge quality.

    Returns accuracy (solver correct / total) and average quality.  With the
    heuristic provider the 'solver' is a naive baseline (picks the first choice),
    which still validates that generated quizzes are well-formed.
    """
    provider = get_provider()
    cases = []
    solved = 0
    quality_sum = 0.0
    for s in sentences:
        quiz = await smart_cloze(s, difficulty, "typing", lang, native, db=db, use_llm=use_llm)
        hidden = quiz["hidden_words"]
        masked = quiz["masked_text"]
        answer: list[str] = []
        notes = None
        if use_llm and provider.name != "heuristic":
            res = await solve_cloze(masked, len(hidden), lang)
            answer = res.words if res else []
            judged = await judge_quality(s, hidden, lang)
            q = judged.score if judged else heuristic_quality(s, hidden, lang)
            notes = judged.notes if judged else None
        else:
            # Baseline solver: knows only the hint & lengths -> reproduces the heuristic to check well-formedness
            answer = [("_" * len(qe.clean_word(w))) for w in hidden]
            q = heuristic_quality(s, hidden, lang)
            notes = "heuristic quality estimate"
        ok, _sim, _ = qe.grade(hidden, " ".join(answer), lenient=True)
        ok = ok and bool(hidden)
        if ok:
            solved += 1
        quality_sum += q
        cases.append(
            {
                "sentence": s,
                "hidden_words": hidden,
                "masked_text": masked,
                "solver_answer": answer,
                "solved": ok,
                "quality_score": round(q, 3),
                "notes": notes,
            }
        )
        await asyncio.sleep(0)
    total = len(sentences)
    accuracy = solved / total if total else 0.0
    avg_q = quality_sum / total if total else 0.0
    if provider.name == "heuristic" or not use_llm:
        verdict = "well-formed" if avg_q >= 0.45 else "needs-tuning"
    elif accuracy >= 0.7 and avg_q >= 0.6:
        verdict = "excellent"
    elif accuracy >= 0.5:
        verdict = "good"
    else:
        verdict = "needs-tuning"
    return {
        "provider": provider.name,
        "total": total,
        "solved": solved,
        "accuracy": round(accuracy, 3),
        "avg_quality": round(avg_q, 3),
        "verdict": verdict,
        "cases": cases,
    }

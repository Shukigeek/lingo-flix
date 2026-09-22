"""Heuristic 'smart cloze' engine.

Given a sentence, choose which words are worth hiding for a language learner,
produce multiple-choice distractors, estimate CEFR difficulty and grade answers
with fuzzy matching. This engine runs with no network and is the fallback for
the LLM provider; the LLM layer refines its output when a key is configured.
"""
from __future__ import annotations

import difflib
import math
import random
import re
import unicodedata
from dataclasses import dataclass, field

from .srt import words_of

TOKEN_RE = re.compile(r"\s+|(?<=[.,!?;:\"“”()])|(?=[.,!?;:\"“”()])")

# Very common function words; low learning value per token.
STOPWORDS = {
    "en": set(
        """a an the and or but if so of to in on at by for with from as is are was were be been being am
        do does did have has had it its this that these those i you he she we they me him her us them my your his
        our their what which who whom whose where when why how not no yes oh ok okay um uh hey there here then than
        too very just also can could will would shall should may might must about up down out over under again
        into onto off all any some each few more most other such only own same s t don ll re ve d m""".split()
    ),
    "es": set("el la los las un una unos unas de del a al y o pero si no que en con por para es son está están yo tú él ella nosotros ellos mi tu su lo le se me te nos".split()),
    "fr": set("le la les un une des de du à au aux et ou mais si ne pas que qui en dans sur avec pour est sont je tu il elle nous vous ils elles ce cette ces mon ma mes ton ta tes son sa ses".split()),
    "de": set("der die das ein eine einer eines und oder aber wenn nicht ja nein zu in auf mit für von ist sind ich du er sie es wir ihr mein dein sein".split()),
    "he": set("של את על אני אתה את הוא היא אנחנו הם הן זה זאת לא כן מה מי איך למה גם רק עם כל יש אין אבל או אז כי".split()),
}

# Words that carry high learning value (phrasal / idiomatic / sitcom vocabulary).
BOOST_WORDS = {
    "en": {
        "break", "pivot", "moist", "awkward", "gorgeous", "hilarious", "obviously", "seriously", "apparently",
        "actually", "literally", "ridiculous", "unbelievable", "definitely", "probably", "whatever", "anyway",
        "mean", "guess", "suppose", "figure", "kidding", "freak", "chill", "hang", "grab", "dump", "crush",
        "date", "break-up", "weird", "creepy", "gross", "fancy", "cheap", "broke", "wedding", "divorce",
    }
}

# Rough CEFR word lists (tiny embedded sample). Words not found are assumed B2+.
CEFR_A1 = set(
    """hello hi bye good bad yes no please thank thanks sorry name friend family mother father sister brother
    house home school work day night morning time today tomorrow yesterday week year food water coffee tea
    eat drink go come see look like love want need have make take give get know think say tell talk ask
    big small new old happy sad hot cold one two three four five six seven eight nine ten first last
    man woman boy girl people person child city country car bus train phone book money job door room""".split()
)
CEFR_A2 = set(
    """always never sometimes often usually already still yet again enough maybe because although while
    beautiful dangerous difficult easy expensive famous favorite important interesting popular quiet ready
    strange terrible wonderful decide forget remember explain arrive leave stay wait wear carry choose
    follow invite miss prefer promise return travel visit worry married single boyfriend girlfriend
    kitchen bedroom apartment restaurant office hospital airport ticket weather holiday birthday party""".split()
)
CEFR_B1 = set(
    """actually apparently basically definitely eventually exactly honestly obviously particularly probably
    seriously suddenly unfortunately admit afford apologize appreciate argue avoid complain convince deserve
    disappoint embarrass encourage ignore insist mention pretend realize recognize refuse regret relax
    suggest suppose survive threaten warn awkward confident curious embarrassed guilty jealous nervous
    proud relieved ridiculous responsible upset relationship situation opportunity experience attitude""".split()
)


@dataclass
class ClozeResult:
    tokens: list[str]
    hidden_indices: list[int]
    hidden_words: list[str]
    masked_text: str
    choices: list[list[str]] = field(default_factory=list)
    word_bank: list[str] = field(default_factory=list)
    hint: str | None = None
    cefr: str = "A2"
    difficulty_score: float = 0.0


def tokenize(text: str) -> list[str]:
    return [t for t in TOKEN_RE.split(text) if t and not t.isspace()]


def strip_accents(s: str) -> str:
    return "".join(c for c in unicodedata.normalize("NFD", s) if unicodedata.category(c) != "Mn")


def clean_word(w: str) -> str:
    return re.sub(r"^[^\w']+|[^\w']+$", "", w.lower())


def word_value(word: str, lang: str, position: int, total: int) -> float:
    """Score 0..1: how useful is this word to hide for a learner?"""
    w = clean_word(word)
    if not w or not any(ch.isalpha() for ch in w):
        return 0.0
    if w in STOPWORDS.get(lang, set()):
        return 0.08
    score = 0.35
    score += min(len(w), 10) / 25  # longer words carry more content
    if w in BOOST_WORDS.get(lang, set()):
        score += 0.3
    if w in CEFR_A1:
        score -= 0.1
    elif w in CEFR_A2:
        score += 0.05
    elif w in CEFR_B1:
        score += 0.15
    else:
        score += 0.12
    if word[:1].isupper() and position > 0:
        score -= 0.2  # likely a proper noun; poor cloze target
    if "'" in w:
        score -= 0.1  # contractions are annoying to type
    # Prefer middle-of-sentence words slightly: better context on both sides
    if total > 3:
        rel = position / max(total - 1, 1)
        score += 0.08 * (1 - abs(rel - 0.5) * 2)
    return max(0.0, min(1.0, score))


def estimate_cefr(text: str, lang: str = "en") -> tuple[str, float]:
    words = [clean_word(w) for w in words_of(text)]
    words = [w for w in words if w]
    if not words:
        return "A1", 0.0
    n = len(words)
    avg_len = sum(len(w) for w in words) / n
    unknown = 0
    hard = 0
    for w in words:
        if w in STOPWORDS.get(lang, set()) or w in CEFR_A1:
            continue
        if w in CEFR_A2:
            continue
        if w in CEFR_B1:
            hard += 1
            continue
        unknown += 1
    ratio_hard = (hard + unknown * 1.4) / n
    length_factor = min(n / 18, 1.0)
    complexity = 0.45 * ratio_hard + 0.3 * length_factor + 0.25 * min((avg_len - 3) / 5, 1.0)
    complexity = max(0.0, min(1.0, complexity))
    if complexity < 0.18:
        cefr = "A1"
    elif complexity < 0.32:
        cefr = "A2"
    elif complexity < 0.48:
        cefr = "B1"
    elif complexity < 0.65:
        cefr = "B2"
    elif complexity < 0.8:
        cefr = "C1"
    else:
        cefr = "C2"
    return cefr, round(complexity, 3)


def hidden_count(n_candidates: int, difficulty: str, mode: str) -> int:
    if mode in ("multiple_choice", "listening"):
        return 1
    if difficulty == "hard":
        return max(1, math.ceil(n_candidates * 0.7))
    if difficulty == "medium":
        return max(1, math.ceil(n_candidates * 0.4))
    return 1


def _mutate(word: str, rng: random.Random) -> str:
    """Make a plausible-looking wrong word (typo, swapped ending)."""
    w = word
    if len(w) < 3:
        return w + "s"
    ops = ["ending", "swap", "double", "drop"]
    op = rng.choice(ops)
    if op == "ending":
        for a, b in (("ing", "ed"), ("ed", "ing"), ("s", ""), ("ly", ""), ("y", "ies"), ("e", "ing")):
            if w.endswith(a) and a:
                return w[: -len(a)] + b
        return w + "s"
    if op == "swap" and len(w) > 3:
        i = rng.randrange(1, len(w) - 2)
        return w[:i] + w[i + 1] + w[i] + w[i + 2 :]
    if op == "double":
        i = rng.randrange(1, len(w) - 1)
        return w[:i] + w[i] + w[i:]
    return w[:-1]


def make_choices(word: str, pool: list[str], lang: str, rng: random.Random, k: int = 4) -> list[str]:
    """Distractors: same-length-ish real words from the pool, plus a mutated form."""
    target = clean_word(word)
    candidates = []
    for p in pool:
        c = clean_word(p)
        if not c or c == target or c in STOPWORDS.get(lang, set()):
            continue
        if abs(len(c) - len(target)) <= 2:
            candidates.append(c)
    rng.shuffle(candidates)
    distractors: list[str] = []
    for c in candidates:
        if c not in distractors:
            distractors.append(c)
        if len(distractors) >= k - 2:
            break
    while len(distractors) < k - 1:
        m = _mutate(target, rng)
        if m != target and m not in distractors:
            distractors.append(m)
    options = distractors[: k - 1] + [target]
    rng.shuffle(options)
    return options


def make_hint(word: str, lang: str) -> str:
    w = clean_word(word)
    if not w:
        return ""
    if len(w) <= 3:
        return f"{w[0]}{'_' * (len(w) - 1)}"
    return f"{w[0]}{'_' * (len(w) - 2)}{w[-1]}  ({len(w)} letters)"


def build_cloze(
    text: str,
    difficulty: str = "easy",
    mode: str = "typing",
    lang: str = "en",
    seed: int | None = None,
    extra_pool: list[str] | None = None,
) -> ClozeResult:
    rng = random.Random(seed if seed is not None else hash(text) & 0xFFFFFFFF)
    tokens = tokenize(text)
    n = len(tokens)
    scored = [(word_value(t, lang, i, n), i) for i, t in enumerate(tokens)]
    candidates = [(s, i) for s, i in scored if s > 0.1]
    if not candidates:
        candidates = [(s, i) for s, i in scored if s > 0]
    if not candidates:
        cefr, score = estimate_cefr(text, lang)
        return ClozeResult(tokens, [], [], text, cefr=cefr, difficulty_score=score)

    k = min(hidden_count(len(candidates), difficulty, mode), len(candidates))
    # Weighted sampling without replacement, favoring high-value words but keeping variety.
    chosen: list[int] = []
    pool = candidates[:]
    while pool and len(chosen) < k:
        weights = [max(s, 0.01) ** 2 for s, _ in pool]
        pick = rng.choices(range(len(pool)), weights=weights, k=1)[0]
        chosen.append(pool[pick][1])
        pool.pop(pick)
    # Avoid hiding two adjacent words on easy/medium (too little context).
    if difficulty != "hard":
        chosen.sort()
        filtered: list[int] = []
        for idx in chosen:
            if not filtered or idx - filtered[-1] > 1:
                filtered.append(idx)
        if filtered:
            chosen = filtered
    chosen.sort()
    hidden_words = [tokens[i] for i in chosen]
    masked_tokens = ["_____" if i in chosen else t for i, t in enumerate(tokens)]
    masked = detokenize(masked_tokens)
    cefr, score = estimate_cefr(text, lang)
    result = ClozeResult(tokens, chosen, hidden_words, masked, cefr=cefr, difficulty_score=score)

    all_pool = [t for t in tokens if t not in hidden_words] + (extra_pool or [])
    if mode == "multiple_choice":
        result.choices = [make_choices(w, all_pool, lang, rng) for w in hidden_words]
    if mode == "word_bank":
        bank = [clean_word(t) for t in tokens if any(c.isalpha() for c in t)]
        decoys = [clean_word(p) for p in (extra_pool or [])][:4]
        bank = [b for b in bank if b] + [d for d in decoys if d]
        rng.shuffle(bank)
        result.word_bank = bank
    if difficulty == "easy" and hidden_words:
        result.hint = make_hint(hidden_words[0], lang)
    return result


def detokenize(tokens: list[str]) -> str:
    out = ""
    for t in tokens:
        if t in {".", ",", "!", "?", ";", ":", ")", "”"}:
            out += t
        elif t in {"(", "“"}:
            out += (" " if out and not out.endswith(" ") else "") + t
        elif t == '"':
            out += (" " if out and not out.endswith(" ") else "") + t
        else:
            out += (" " if out and not out.endswith(("(", "“")) else "") + t
    return out.strip()


# ---------- Grading ----------
def similarity(a: str, b: str) -> float:
    a2, b2 = strip_accents(clean_word(a)), strip_accents(clean_word(b))
    if not a2 and not b2:
        return 1.0
    return difflib.SequenceMatcher(None, a2, b2).ratio()


@dataclass
class WordGrade:
    word: str
    correct: bool
    user_word: str | None
    similarity: float


def grade(hidden_words: list[str], user_answer: str, lenient: bool = True, mode: str = "typing") -> tuple[bool, float, list[WordGrade]]:
    """Grade an answer.

    In typing mode the user may type only the hidden words or the whole sentence;
    each hidden word is matched against the best user token. Lenient mode accepts
    small typos (similarity >= 0.8) and ignores accents/case/punctuation.
    """
    user_tokens = [clean_word(t) for t in re.split(r"\s+", user_answer.strip()) if clean_word(t)]
    results: list[WordGrade] = []
    used: set[int] = set()
    threshold = 0.8 if lenient else 0.999
    for w in hidden_words:
        target = clean_word(w)
        best_i, best_s = -1, 0.0
        for i, ut in enumerate(user_tokens):
            if i in used:
                continue
            s = similarity(ut, target)
            if s > best_s:
                best_i, best_s = i, s
        ok = best_s >= threshold
        if ok and best_i >= 0:
            used.add(best_i)
        results.append(WordGrade(w, ok, user_tokens[best_i] if best_i >= 0 else None, round(best_s, 3)))
    if not results:
        return True, 1.0, results
    avg = sum(r.similarity for r in results) / len(results)
    all_ok = all(r.correct for r in results)
    return all_ok, round(avg, 3), results


def xp_for(difficulty: str, multiplier: int, base: dict[str, int] | None = None) -> int:
    base = base or {"easy": 20, "medium": 30, "hard": 40}
    return base.get(difficulty, 20) * max(1, multiplier)


def combo_multiplier(combo: int) -> int:
    if combo >= 10:
        return 10
    if combo >= 5:
        return 5
    if combo >= 3:
        return 3
    if combo >= 2:
        return 2
    return 1

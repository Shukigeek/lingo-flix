"""Exercise the LLM code paths with a deterministic fake provider (no network)."""
import re

import pytest
from pydantic import BaseModel


def _numbered(user: str) -> int:
    return len(re.findall(r"^\d+\. ", user, flags=re.M))

from app.services import llm


class FakeProvider(llm.Provider):
    name = "fake-llm"
    model = "fake"

    def __init__(self):
        self.calls = 0

    async def complete_json(self, system: str, user: str, schema: type[BaseModel], fast: bool = False):
        self.calls += 1
        if schema is llm.ClozePlan:
            return llm.ClozePlan(
                hidden_words=["espresso"],
                translation="מכונת האספרסו התקלקלה שוב",
                grammar_note="past simple",
                hint="a strong coffee",
                distractors=[["latte", "toaster", "kettle"]],
                cefr="A2",
            )
        if schema is llm.Explanation:
            return llm.Explanation(translation="תרגום", word_explanations=[], grammar_notes=["note"], idioms=[], cultural_note=None, difficulty_cefr="B1")
        if schema is llm.TranslationBatch:
            n = _numbered(user)
            return llm.TranslationBatch(translations=[f"תרגום {i}" for i in range(n)])
        if schema is llm.DifficultyBatch:
            n = _numbered(user)
            return llm.DifficultyBatch(levels=["B1"] * n, key_words=[["word"]] * n)
        if schema is llm.SolverAnswer:
            return llm.SolverAnswer(words=["espresso"], confidence=0.9)
        if schema is llm.QualityJudgement:
            return llm.QualityJudgement(score=0.8, notes="good")
        return None


@pytest.fixture
def fake():
    p = FakeProvider()
    llm.set_provider_for_tests(p)
    yield p
    llm.set_provider_for_tests(None)


@pytest.mark.asyncio
async def test_smart_cloze_uses_llm_plan(fake):
    res = await llm.smart_cloze("The espresso machine broke again.", "easy", "multiple_choice", "en", "he", db=None)
    assert res["provider"] == "fake-llm"
    assert res["hidden_words"] == ["espresso"]
    assert res["translation"].startswith("מכונת")
    assert "espresso" in res["choices"][0] and len(res["choices"][0]) == 4


@pytest.mark.asyncio
async def test_self_test_with_llm_solver(fake):
    rep = await llm.self_test(["The espresso machine broke again."], "easy", "en", "he", use_llm=True, db=None)
    assert rep["provider"] == "fake-llm"
    assert rep["solved"] == 1
    assert rep["accuracy"] == 1.0
    assert rep["verdict"] == "excellent"


@pytest.mark.asyncio
async def test_translate_and_rate(fake):
    tr = await llm.translate_lines(["a", "b", "c"], "en", "he", db=None)
    assert tr == ["תרגום 0", "תרגום 1", "תרגום 2"]
    rated = await llm.rate_lines(["a", "b"], "en", db=None)
    assert rated == [("B1", ["word"]), ("B1", ["word"])]


@pytest.mark.asyncio
async def test_llm_cache_roundtrip(fake, client):
    """Second identical call must be served from the llm_cache table."""
    from app.database import get_session_factory

    async with get_session_factory()() as db:
        await llm.smart_cloze("The espresso machine broke again.", "easy", "typing", "en", "he", db=db)
        calls = fake.calls
        await llm.smart_cloze("The espresso machine broke again.", "easy", "typing", "en", "he", db=db)
        assert fake.calls == calls

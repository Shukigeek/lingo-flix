from app.services import quiz_engine as qe
from app.services.srt import detect_language, merge_fragments, parse_srt, to_srt, vtt_to_srt

SAMPLE_SRT = """1
00:00:01,000 --> 00:00:02,500
<i>MONICA:</i> There's nothing to tell!

2
00:00:02,600 --> 00:00:04,000
He's just some guy
I work with.

3
00:00:04,100 --> 00:00:06,000
[laughter]

4
00:00:06,100 --> 00:00:07,000
- Come on!
- You're going out with the guy.
5
00:00:07,100 --> 00:00:09,000
♪ ♪
"""


def test_parse_srt_cleans_tags_speakers_and_sounds():
    lines = parse_srt(SAMPLE_SRT)
    texts = [l.text for l in lines]
    assert texts[0] == "There's nothing to tell!"
    assert lines[0].speaker == "MONICA"
    assert texts[1] == "He's just some guy I work with."
    assert "[laughter]" not in " ".join(texts)
    assert "♪" not in " ".join(texts)
    assert lines[0].start_ms == 1000 and lines[0].end_ms == 2500
    # block 4 & 5 were not separated by a blank line: still parsed
    assert any("going out" in t for t in texts)


def test_merge_fragments_joins_unfinished_sentences():
    lines = parse_srt(
        """1
00:00:01,000 --> 00:00:02,000
I was thinking that maybe

2
00:00:02,100 --> 00:00:03,000
we could go out tonight.

3
00:00:05,000 --> 00:00:06,000
Sounds great.
"""
    )
    merged = merge_fragments(lines)
    assert len(merged) == 2
    assert merged[0].text == "I was thinking that maybe we could go out tonight."
    assert merged[0].end_ms == 3000


def test_to_srt_roundtrip():
    lines = parse_srt(SAMPLE_SRT)
    out = to_srt(lines)
    again = parse_srt(out)
    assert [l.text for l in again] == [l.text for l in lines]


def test_vtt_to_srt():
    vtt = """WEBVTT
Kind: captions
Language: en

00:00:00.500 --> 00:00:02.000 align:start position:0%
<c>Hello</c> there

00:00:02.000 --> 00:00:03.000
Hello there

00:00:03.000 --> 00:00:04.000
General Kenobi
"""
    srt = vtt_to_srt(vtt)
    lines = parse_srt(srt)
    assert [l.text for l in lines] == ["Hello there", "General Kenobi"]


def test_detect_language():
    assert detect_language("שלום, מה קורה איתך היום? הכל בסדר") == "he"
    assert detect_language("¿Qué tal? El niño está en la casa.") == "es"
    assert detect_language("Hello there, how are you doing today?") == "en"


def test_cloze_prefers_content_words():
    text = "We were on a break, and I honestly thought it was over."
    res = qe.build_cloze(text, "easy", "typing", "en", seed=1)
    assert len(res.hidden_words) == 1
    assert qe.clean_word(res.hidden_words[0]) not in qe.STOPWORDS["en"]
    assert "_____" in res.masked_text


def test_cloze_hard_hides_more():
    text = "Could you please stop talking about the espresso machine for one minute?"
    easy = qe.build_cloze(text, "easy", "typing", "en", seed=3)
    hard = qe.build_cloze(text, "hard", "typing", "en", seed=3)
    assert len(hard.hidden_words) > len(easy.hidden_words)


def test_multiple_choice_has_answer_among_options():
    text = "Apparently he also collects toothbrushes."
    res = qe.build_cloze(text, "easy", "multiple_choice", "en", seed=7)
    assert len(res.choices) == 1
    assert qe.clean_word(res.hidden_words[0]) in res.choices[0]
    assert len(res.choices[0]) == 4


def test_grade_lenient_accepts_typo_and_order():
    ok, sim, per = qe.grade(["espresso", "machine"], "machine expresso")
    assert ok
    assert sim > 0.8
    assert all(p.correct for p in per)


def test_grade_strict_rejects_typo():
    ok, _, _ = qe.grade(["espresso"], "expresso", lenient=False)
    assert not ok


def test_cefr_estimate_monotonic():
    a1 = qe.estimate_cefr("I like coffee.", "en")[1]
    c1 = qe.estimate_cefr("Apparently the ostentatious negotiations collapsed unexpectedly.", "en")[1]
    assert c1 > a1


def test_combo_multiplier():
    assert qe.combo_multiplier(1) == 1
    assert qe.combo_multiplier(2) == 2
    assert qe.combo_multiplier(5) == 5
    assert qe.combo_multiplier(12) == 10

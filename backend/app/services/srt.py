"""SRT parsing, cleaning, language detection and generation.

Pure functions, no I/O, so they can be unit-tested and reused by the LLM layer.
"""
from __future__ import annotations

import re
from dataclasses import dataclass, field

TIME_RE = re.compile(r"(\d{1,2}):(\d{2}):(\d{2})[.,](\d{1,3})")
ARROW_RE = re.compile(r"\s*-->\s*")
TAG_RE = re.compile(r"<[^>]+>|\{[^}]*\}")
SPEAKER_RE = re.compile(r"^\s*([A-Z][A-Za-z .'\-]{1,24}):\s+")
SOUND_RE = re.compile(r"^\s*[\[\(].*?[\]\)]\s*$")
MUSIC_RE = re.compile(r"^[\s♪♫#]*$")
DASH_RE = re.compile(r"^\s*[-–—]\s*")
WS_RE = re.compile(r"\s+")
WORD_RE = re.compile(r"[A-Za-zÀ-ÿ֐-׿؀-ۿЀ-ӿ']+")

LANG_HINTS = {
    "he": (0x0590, 0x05FF),
    "ar": (0x0600, 0x06FF),
    "ru": (0x0400, 0x04FF),
    "ja": (0x3040, 0x30FF),
    "zh": (0x4E00, 0x9FFF),
    "ko": (0xAC00, 0xD7AF),
    "el": (0x0370, 0x03FF),
}

SPANISH = set("ñ¡¿")
FRENCH = set("çœ")
GERMAN = set("ßäöüÄÖÜ")
PORTUGUESE = set("ãõ")


@dataclass
class SrtLine:
    index: int
    start_ms: int
    end_ms: int
    text: str
    speaker: str | None = None
    raw_lines: list[str] = field(default_factory=list)

    @property
    def duration_ms(self) -> int:
        return max(0, self.end_ms - self.start_ms)


def parse_time(ts: str) -> int:
    m = TIME_RE.search(ts)
    if not m:
        raise ValueError(f"Bad timestamp: {ts!r}")
    h, mi, s, ms = m.groups()
    ms = ms.ljust(3, "0")
    return int(h) * 3_600_000 + int(mi) * 60_000 + int(s) * 1000 + int(ms)


def format_time(ms: int) -> str:
    ms = max(0, int(ms))
    h, rem = divmod(ms, 3_600_000)
    mi, rem = divmod(rem, 60_000)
    s, ms = divmod(rem, 1000)
    return f"{h:02d}:{mi:02d}:{s:02d},{ms:03d}"


def clean_text(text: str) -> tuple[str, str | None]:
    """Strip tags/sound cues; return (clean_text, speaker)."""
    text = TAG_RE.sub("", text)
    speaker = None
    lines_out = []
    for ln in text.split("\n"):
        ln = ln.strip()
        if not ln or SOUND_RE.match(ln) or MUSIC_RE.match(ln):
            continue
        m = SPEAKER_RE.match(ln)
        if m:
            speaker = m.group(1).strip()
            ln = ln[m.end():]
        ln = DASH_RE.sub("", ln)
        lines_out.append(ln)
    joined = WS_RE.sub(" ", " ".join(lines_out)).strip()
    return joined, speaker


def parse_srt(content: str) -> list[SrtLine]:
    """Tolerant SRT parser: handles missing indices, CRLF, WebVTT headers, stray blank lines."""
    content = content.replace("﻿", "").replace("\r\n", "\n").replace("\r", "\n")
    lines = content.split("\n")
    out: list[SrtLine] = []
    i = 0
    idx = 0
    n = len(lines)
    while i < n:
        ln = lines[i].strip()
        if "-->" in ln:
            try:
                start_s, end_s = ARROW_RE.split(ln, maxsplit=1)
                start_ms = parse_time(start_s)
                end_ms = parse_time(end_s.split(" ")[0])
            except ValueError:
                i += 1
                continue
            i += 1
            raw: list[str] = []
            while i < n and lines[i].strip() != "":
                nxt = lines[i].strip()
                # A bare number followed by a timing line means the next block started without a blank line.
                if nxt.isdigit() and i + 1 < n and "-->" in lines[i + 1]:
                    break
                if "-->" in nxt:
                    break
                raw.append(lines[i])
                i += 1
            text, speaker = clean_text("\n".join(raw))
            if text:
                idx += 1
                out.append(SrtLine(idx, start_ms, end_ms, text, speaker, raw))
        else:
            i += 1
    return out


def merge_fragments(lines: list[SrtLine], max_gap_ms: int = 350, max_words: int = 22) -> list[SrtLine]:
    """Merge lines that are clearly one sentence split across cues (no terminal punctuation, tiny gap)."""
    if not lines:
        return []
    merged: list[SrtLine] = [SrtLine(1, lines[0].start_ms, lines[0].end_ms, lines[0].text, lines[0].speaker)]
    for ln in lines[1:]:
        prev = merged[-1]
        ends_sentence = prev.text.rstrip().endswith((".", "!", "?", "…", '"', "”"))
        gap = ln.start_ms - prev.end_ms
        combined_words = len(prev.text.split()) + len(ln.text.split())
        starts_lower = ln.text[:1].islower()
        same_speaker = ln.speaker is None or ln.speaker == prev.speaker
        if (not ends_sentence or starts_lower) and gap <= max_gap_ms and combined_words <= max_words and same_speaker:
            prev.text = f"{prev.text} {ln.text}".strip()
            prev.end_ms = ln.end_ms
        else:
            merged.append(SrtLine(len(merged) + 1, ln.start_ms, ln.end_ms, ln.text, ln.speaker))
    return merged


def to_srt(lines: list[SrtLine], translations: dict[int, str] | None = None) -> str:
    blocks = []
    for i, ln in enumerate(lines, start=1):
        text = ln.text
        if translations and i in translations and translations[i]:
            text = f"{text}\n{translations[i]}"
        blocks.append(f"{i}\n{format_time(ln.start_ms)} --> {format_time(ln.end_ms)}\n{text}\n")
    return "\n".join(blocks)


def detect_language(text: str) -> str:
    counts = {k: 0 for k in LANG_HINTS}
    for ch in text:
        o = ord(ch)
        for k, (lo, hi) in LANG_HINTS.items():
            if lo <= o <= hi:
                counts[k] += 1
    best = max(counts, key=counts.get)
    if counts[best] > 5:
        return best
    chars = set(text)
    if chars & SPANISH:
        return "es"
    if chars & PORTUGUESE:
        return "pt"
    if chars & GERMAN:
        return "de"
    if chars & FRENCH:
        return "fr"
    lowered = text.lower()
    fr_hits = sum(lowered.count(w) for w in (" le ", " la ", " les ", " est ", " je ", " nous "))
    es_hits = sum(lowered.count(w) for w in (" el ", " los ", " las ", " que ", " está ", " pero "))
    it_hits = sum(lowered.count(w) for w in (" il ", " che ", " non ", " sono ", " perché ", " gli "))
    de_hits = sum(lowered.count(w) for w in (" der ", " die ", " das ", " ich ", " nicht ", " und "))
    top = max((fr_hits, "fr"), (es_hits, "es"), (it_hits, "it"), (de_hits, "de"))
    if top[0] >= 8:
        return top[1]
    return "en"


def normalize(text: str) -> str:
    return WS_RE.sub(" ", re.sub(r"[^\w\s']", " ", text.lower())).strip()


def words_of(text: str) -> list[str]:
    return WORD_RE.findall(text)


def shift(lines: list[SrtLine], offset_ms: int) -> list[SrtLine]:
    return [SrtLine(l.index, max(0, l.start_ms + offset_ms), max(0, l.end_ms + offset_ms), l.text, l.speaker) for l in lines]


def vtt_to_srt(vtt: str) -> str:
    """Convert WebVTT (as produced by yt-dlp) to SRT, dropping styling and duplicate rolling captions."""
    vtt = vtt.replace("\r\n", "\n")
    body = vtt.split("\n\n")
    out: list[str] = []
    n = 0
    last_text = ""
    for block in body:
        blines = [b for b in block.split("\n") if b.strip()]
        if not blines:
            continue
        timing_idx = next((i for i, b in enumerate(blines) if "-->" in b), None)
        if timing_idx is None:
            continue
        timing = blines[timing_idx]
        try:
            start_s, end_s = ARROW_RE.split(timing, maxsplit=1)
            start = parse_time(start_s)
            end = parse_time(end_s.split(" ")[0])
        except ValueError:
            continue
        text_lines = blines[timing_idx + 1:]
        text = re.sub(r"<[^>]+>", "", "\n".join(text_lines)).strip()
        text = WS_RE.sub(" ", text)
        if not text or text == last_text:
            continue
        last_text = text
        n += 1
        out.append(f"{n}\n{format_time(start)} --> {format_time(end)}\n{text}\n")
    return "\n".join(out)

"""SM-2 spaced repetition with a mastery score."""
from __future__ import annotations

from datetime import datetime, timedelta

from ..models import SentenceProgress


def quality_from_result(is_correct: bool, similarity: float, duration_ms: int | None) -> int:
    """Map an answer to the SM-2 quality scale 0..5."""
    if not is_correct:
        return 1 if similarity >= 0.5 else 0
    q = 4
    if similarity >= 0.98:
        q = 5
    if duration_ms is not None and duration_ms > 25_000:
        q = max(3, q - 1)
    return q


def apply_review(progress: SentenceProgress, quality: int, now: datetime | None = None) -> SentenceProgress:
    now = now or datetime.utcnow()
    quality = max(0, min(5, quality))
    # A freshly constructed row has Python-side defaults unset until flush.
    progress.repetitions = progress.repetitions or 0
    progress.interval_days = progress.interval_days or 0.0
    progress.ease_factor = progress.ease_factor or 2.5
    progress.correct_count = progress.correct_count or 0
    progress.wrong_count = progress.wrong_count or 0
    if quality < 3:
        progress.repetitions = 0
        progress.interval_days = 0.0
        progress.wrong_count += 1
        due = now + timedelta(minutes=10)
    else:
        if progress.repetitions == 0:
            progress.interval_days = 1.0
        elif progress.repetitions == 1:
            progress.interval_days = 3.0
        else:
            progress.interval_days = round(progress.interval_days * progress.ease_factor, 2)
        progress.repetitions += 1
        progress.correct_count += 1
        due = now + timedelta(days=progress.interval_days)
    ef = progress.ease_factor + (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02))
    progress.ease_factor = max(1.3, round(ef, 3))
    progress.last_reviewed_at = now
    progress.due_at = due
    total = progress.correct_count + progress.wrong_count
    recent = min(progress.repetitions, 6) / 6
    accuracy = progress.correct_count / total if total else 0
    progress.mastery = round(min(1.0, 0.6 * recent + 0.4 * accuracy), 3)
    return progress

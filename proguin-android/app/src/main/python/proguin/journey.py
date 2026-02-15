import json
from datetime import datetime

# ===============================
# Storage helpers
# ===============================

def load(path):
    try:
        with open(path, "r", encoding="utf-8") as f:
            return json.load(f)
    except:
        return {
            "current_day": 1,
            "streak": 0,
            "xp": 0,
            "completed_days": {},
            "last_completed_date": ""
        }

def save(path, data):
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2)

def today_iso():
    return datetime.now().strftime("%Y-%m-%d")


# ===============================
# Day Plan Generator (Set 1 only)
# ARC 1: Awakening (Days 1-7)
# ===============================

def _arc1_day_plan(day):
    # Base visual config (one image, different "layers")
    # image_key must match Android drawable name WITHOUT extension.
    base = {
        "arc_id": 1,
        "arc_title": "ARC 1: Awakening — Three Wins",
        "image_key": "arc1_base",
    }

    # Day-specific themes/layers
    # overlay_tint: hex color string
    # overlay_alpha: 0.0 - 1.0
    # tilt_deg: rotation for “anime angle”
    # zoom: scale multiplier
    # gradient_a/b: background gradient colors (hex)
    d = {
        1: {
            "day_title": "Day 1 — Wake Up, Small Wins",
            "story": "The penguin opens its eyes. No big goals. Just one clean step.",
            "quote": "Small wins wake sleeping giants.",
            "overlay_tint": "#00E5FF",
            "overlay_alpha": 0.10,
            "tilt_deg": -2.0,
            "zoom": 1.03,
            "gradient_a": "#0B0F1A",
            "gradient_b": "#101A2B",
            "tasks": [
                {"name": "Physical Win: Walk / Stretch", "minutes": 5},
                {"name": "Mental Win: 1 Focus Task", "minutes": 10},
                {"name": "Spiritual Win: Gratitude / Silence", "minutes": 3},
            ]
        },
        2: {
            "day_title": "Day 2 — Discipline Begins",
            "story": "Snow is heavy. The penguin learns: consistency beats motivation.",
            "quote": "Discipline is freedom in disguise.",
            "overlay_tint": "#FF4FD8",
            "overlay_alpha": 0.10,
            "tilt_deg": 2.0,
            "zoom": 1.04,
            "gradient_a": "#0B0F1A",
            "gradient_b": "#1A1030",
            "tasks": [
                {"name": "Physical Win: 20 Squats / Walk", "minutes": 6},
                {"name": "Mental Win: One hard thing first", "minutes": 12},
                {"name": "Spiritual Win: 3 deep breaths + intent", "minutes": 3},
            ]
        },
        3: {
            "day_title": "Day 3 — Identity Shift",
            "story": "The penguin stops wishing. It becomes the kind of one who shows up.",
            "quote": "Act like the person you want to be.",
            "overlay_tint": "#FFFFFF",
            "overlay_alpha": 0.06,
            "tilt_deg": -3.0,
            "zoom": 1.05,
            "gradient_a": "#0B0F1A",
            "gradient_b": "#0F1C2E",
            "tasks": [
                {"name": "Physical Win: Mobility / Stretch", "minutes": 6},
                {"name": "Mental Win: 1 priority task (no multitask)", "minutes": 15},
                {"name": "Spiritual Win: Write 1 line gratitude", "minutes": 2},
            ]
        },
        4: {
            "day_title": "Day 4 — Focus Mode",
            "story": "Wind distracts. The penguin locks eyes on the next footprint.",
            "quote": "Focus is a superpower.",
            "overlay_tint": "#00E5FF",
            "overlay_alpha": 0.12,
            "tilt_deg": 3.0,
            "zoom": 1.06,
            "gradient_a": "#08101E",
            "gradient_b": "#0E2A3A",
            "tasks": [
                {"name": "Physical Win: Brisk walk / steps", "minutes": 7},
                {"name": "Mental Win: 1 Pomodoro Deep Work", "minutes": 25},
                {"name": "Spiritual Win: Silence + reset", "minutes": 3},
            ]
        },
        5: {
            "day_title": "Day 5 — Clean Up Your Space",
            "story": "The penguin clears clutter. A clean space creates a clean mind.",
            "quote": "Your environment shapes your behavior.",
            "overlay_tint": "#FF4FD8",
            "overlay_alpha": 0.12,
            "tilt_deg": -2.0,
            "zoom": 1.04,
            "gradient_a": "#0B0F1A",
            "gradient_b": "#2A1024",
            "tasks": [
                {"name": "Physical Win: Water + posture check", "minutes": 4},
                {"name": "Mental Win: Clean desk / phone (10 items)", "minutes": 10},
                {"name": "Spiritual Win: Gratitude — 3 things", "minutes": 4},
            ]
        },
        6: {
            "day_title": "Day 6 — Momentum",
            "story": "Now the penguin moves faster. Momentum makes hard things easy.",
            "quote": "Motion beats emotion.",
            "overlay_tint": "#FFFFFF",
            "overlay_alpha": 0.05,
            "tilt_deg": 1.5,
            "zoom": 1.06,
            "gradient_a": "#07111D",
            "gradient_b": "#132B2A",
            "tasks": [
                {"name": "Physical Win: 1 small workout set", "minutes": 8},
                {"name": "Mental Win: Finish one pending task", "minutes": 20},
                {"name": "Spiritual Win: 60-sec calm breathing", "minutes": 2},
            ]
        },
        7: {
            "day_title": "Day 7 — Proof Day",
            "story": "One week done. The penguin proves: it can be trusted.",
            "quote": "Consistency is the real confidence.",
            "overlay_tint": "#00E5FF",
            "overlay_alpha": 0.14,
            "tilt_deg": -1.0,
            "zoom": 1.08,
            "gradient_a": "#070D16",
            "gradient_b": "#101A2B",
            "tasks": [
                {"name": "Physical Win: Walk / Stretch (celebration)", "minutes": 10},
                {"name": "Mental Win: Weekly review (wins + lesson)", "minutes": 10},
                {"name": "Spiritual Win: Gratitude + intention", "minutes": 4},
            ]
        },
    }

    if day not in d:
        # fallback (still returns valid plan)
        day = 1

    plan = {}
    plan.update(base)
    plan.update(d[day])
    plan["day_number"] = day
    return plan


def get_day_plan(day_number: int):
    # For now: only Arc1 Days 1-7
    return _arc1_day_plan(int(day_number))


# ===============================
# Completion / Progress
# ===============================

def complete_day(data):
    # mark today's current_day as completed
    day = int(data.get("current_day", 1))
    today = today_iso()

    # streak calc
    last = data.get("last_completed_date", "")
    if last == today:
        # already completed today (safe)
        return data

    if last:
        # simple streak logic: if yesterday then +1 else reset
        # (we keep it simple to avoid timezone bugs)
        data["streak"] = int(data.get("streak", 0)) + 1
    else:
        data["streak"] = 1

    data["completed_days"][str(day)] = True
    data["last_completed_date"] = today
    data["xp"] = int(data.get("xp", 0)) + 10

    # move to next day (auto)
    data["current_day"] = day + 1
    return data

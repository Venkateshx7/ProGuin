import json
import os
from datetime import datetime

# ---------------------------
# Storage helpers
# ---------------------------

def _default_state():
    return {
        "current_day": 1,
        "xp": 0,
        "streak": 0,
        "last_done_date": "",
        "completed_days": []
    }

def load(path: str):
    try:
        if not os.path.exists(path):
            data = _default_state()
            save(path, data)
            return data

        with open(path, "r", encoding="utf-8") as f:
            data = json.load(f)

        # fill missing keys
        default = _default_state()
        for k, v in default.items():
            if k not in data:
                data[k] = v

        if not isinstance(data.get("completed_days"), list):
            data["completed_days"] = []

        return data

    except Exception:
        data = _default_state()
        try:
            save(path, data)
        except Exception:
            pass
        return data

def save(path: str, data: dict):
    parent = os.path.dirname(path)
    if parent:
        os.makedirs(parent, exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2)

def set_current_day(data: dict, day: int):
    if not isinstance(data, dict):
        data = _default_state()

    # fill missing keys
    default = _default_state()
    for k, v in default.items():
        if k not in data:
            data[k] = v

    try:
        day = int(day)
    except Exception:
        day = 1

    day = max(1, min(day, 74))
    data["current_day"] = day
    return data

def complete_day(data):
    # data is a dict loaded from journey.json
    if not isinstance(data, dict):
        data = _default_state()

    current_day = int(data.get("current_day", 1) or 1)
    xp = int(data.get("xp", 0) or 0)
    streak = int(data.get("streak", 0) or 0)

    completed_days = data.get("completed_days", [])
    if not isinstance(completed_days, list):
        completed_days = []

    # normalize list -> set[int]
    done = set()
    for x in completed_days:
        try:
            done.add(int(x))
        except Exception:
            pass

    # ✅ If current day already done, do NOT increment again
    if current_day in done:
        max_done = max(done) if done else 0
        # keep consistent: unlock next based on max done
        next_day = max_done + 1 if max_done > 0 else current_day
        data["current_day"] = min(next_day, 74)
        data["completed_days"] = sorted(done)
        data["xp"] = xp
        data["streak"] = streak
        return data

    # ✅ Mark current day done
    done.add(current_day)

    # ✅ Update XP + Streak
    xp += 10
    streak += 1

    # ✅ Unlock next day
    max_done = max(done) if done else current_day
    next_day = max_done + 1
    if next_day > 74:
        next_day = 74

    data["completed_days"] = sorted(done)
    data["current_day"] = next_day
    data["xp"] = xp
    data["streak"] = streak

    # optional: store completion date
    try:
        data["last_done_date"] = datetime.now().strftime("%Y-%m-%d")
    except Exception:
        pass

    return data

# ---------------------------
# Day / Arc mapping
# ---------------------------

def _arc_index(day: int) -> int:
    if day <= 7: return 1
    if day <= 14: return 2
    if day <= 21: return 3
    if day <= 28: return 4
    if day <= 35: return 5
    if day <= 42: return 6
    if day <= 49: return 7
    if day <= 56: return 8
    if day <= 63: return 9
    if day <= 70: return 10
    return 11

def _arc_day(day: int) -> int:
    arc = _arc_index(day)
    if arc == 1: start = 1
    elif arc == 2: start = 8
    elif arc == 3: start = 15
    elif arc == 4: start = 22
    elif arc == 5: start = 29
    elif arc == 6: start = 36
    elif arc == 7: start = 43
    elif arc == 8: start = 50
    elif arc == 9: start = 57
    elif arc == 10: start = 64
    else: start = 71
    return (day - start) + 1

# ---------------------------
# Arc titles/colors
# ---------------------------

ARC_TITLES = {
    1: "ARC 1: Awakening — Three Wins",
    2: "ARC 2: Momentum — Build the Engine",
    3: "ARC 3: Focus — Cut Distractions",
    4: "ARC 4: Strength — Level Up",
    5: "ARC 5: Consistency — No Zero Days",
    6: "ARC 6: Discipline — Hard Days",
    7: "ARC 7: Clarity — Sharpen Mind",
    8: "ARC 8: Growth — Bigger Challenges",
    9: "ARC 9: Power — Strong Identity",
    10: "ARC 10: Mastery — Elite Mode",
    11: "ARC 11: Finish — Final Push",
}

ARC_COLORS = {
    2: ("#00E676", "#00E5FF"),
    3: ("#7C4DFF", "#40C4FF"),
    4: ("#FFC107", "#FF4FD8"),
    5: ("#00E5FF", "#00E676"),
    6: ("#FF5252", "#FFC107"),
    7: ("#40C4FF", "#7C4DFF"),
    8: ("#FF4FD8", "#00E676"),
    9: ("#00E5FF", "#FFC107"),
    10: ("#7C4DFF", "#FF4FD8"),
    11: ("#00E5FF", "#FFFFFF"),
}

# ---------------------------
# NEW: Arc stacking tasks (your requirement)
# ---------------------------

ARC_STACK_TASKS = {
    1: "Workout",
    2: "Study",
    3: "Meditation",
    4: "Skill Practice",
    5: "Project Work",
    6: "Reading",
    7: "Aptitude",
    8: "Communication",
    9: "Health / Diet",
    10: "Deep Work",
    11: "Review + Planning",
}

def _ramp_15_to_45(arc_day: int) -> int:
    # arc_day: 1..7 -> 15,20,25,30,35,40,45
    try:
        arc_day = int(arc_day)
    except Exception:
        arc_day = 1
    arc_day = max(1, min(arc_day, 7))
    return min(45, 15 + (arc_day - 1) * 5)

def _build_arc_stack_tasks(arc: int, arc_day: int):
    tasks = []
    ramp = _ramp_15_to_45(arc_day)

    for a in range(1, arc + 1):
        name = ARC_STACK_TASKS.get(a, f"Task {a}")

        # older arcs stay at 45 once unlocked
        minutes = 45 if a < arc else ramp

        tasks.append({"name": name, "minutes": minutes})

    return tasks

def _auto_story(arc: int, arc_day: int, day: int) -> str:
    themes = {
        2: "Momentum is built when you repeat the basics even when you don’t feel like it.",
        3: "Focus is power. Every distraction you remove becomes energy you gain.",
        4: "Strength is earned. You don’t become stronger by wishing — you become stronger by doing.",
        5: "Consistency turns ordinary days into extraordinary results.",
        6: "Discipline is showing up on hard days — that’s where growth lives.",
        7: "Clarity comes when you stop overthinking and start executing.",
        8: "Growth means new pressure. You’re not breaking — you’re upgrading.",
        9: "Power is identity. You act like the person you want to become.",
        10: "Mastery is boring repetition done like a pro.",
        11: "Final push. You’ve come too far to go soft now."
    }
    base = themes.get(arc, "The penguin keeps moving. Small wins compound into power.")
    return f"Day {day}: {base} (Arc Day {arc_day}/7)"

def _auto_quote(arc: int, arc_day: int) -> str:
    quotes = {
        2: ["Start before you’re ready.", "Momentum beats motivation.", "Tiny actions create big change."],
        3: ["What you focus on grows.", "Distraction is the real enemy.", "One task. Full energy."],
        4: ["Hard days build strong people.", "Earn it today.", "Level up or stay same."],
        5: ["No zero days.", "Consistency is a superpower.", "Repeat. Repeat. Repeat."],
        6: ["Discipline is freedom.", "Do it even tired.", "Your future self is watching."],
        7: ["Clarity comes from action.", "Simplify and execute.", "Less noise. More results."],
        8: ["Pressure makes diamonds.", "Upgrade your standards.", "New level, new you."],
        9: ["Identity creates habits.", "Act like the winner.", "You become what you do daily."],
        10: ["Mastery is practice.", "Boring done daily becomes greatness.", "Pro mindset only."],
        11: ["Finish strong.", "One last push.", "You’re closer than you think."]
    }
    pool = quotes.get(arc, ["Consistency beats intensity."])
    return pool[(arc_day - 1) % len(pool)]

# ---------------------------
# Arc 1 detailed (Days 1-7)
# ---------------------------

ARC1_TITLE = "ARC 1: Awakening — Three Wins"

ARC1_DAYS = {
    1: {
        "day_title": "Day 1 — Wake Up, Small Wins",
        "story": "The penguin opens its eyes. No big goals. Just one clean step.",
        "quote": "Small wins wake sleeping giants.",
        "overlay_tint": "#00E5FF",
        "overlay_alpha": 0.10,
        "tilt_deg": -1.5,
        "zoom": 1.03,
        "gradient_a": "#0B0F1A",
        "gradient_b": "#101A2B",
    },
    2: {
        "day_title": "Day 2 — The Breath of Control",
        "story": "The storm outside is loud. The penguin learns the storm inside can be quiet.",
        "quote": "Control your breath, control your day.",
        "overlay_tint": "#7C4DFF",
        "overlay_alpha": 0.12,
        "tilt_deg": 1.0,
        "zoom": 1.02,
        "gradient_a": "#090A12",
        "gradient_b": "#121A34",
    },
    3: {
        "day_title": "Day 3 — Discipline > Motivation",
        "story": "Motivation disappears. The penguin moves anyway. That’s the power.",
        "quote": "Discipline is a promise you keep.",
        "overlay_tint": "#FF1744",
        "overlay_alpha": 0.10,
        "tilt_deg": -0.8,
        "zoom": 1.04,
        "gradient_a": "#0B0A10",
        "gradient_b": "#1A1020",
    },
    4: {
        "day_title": "Day 4 — The Clean Screen Oath",
        "story": "The penguin clears noise. The mind becomes sharp like a blade.",
        "quote": "A clean screen is a clean mind.",
        "overlay_tint": "#00C853",
        "overlay_alpha": 0.10,
        "tilt_deg": 0.5,
        "zoom": 1.02,
        "gradient_a": "#07110B",
        "gradient_b": "#0F2318",
    },
    5: {
        "day_title": "Day 5 — Focus Blade",
        "story": "One target. One strike. The penguin learns ‘less, but perfect’.",
        "quote": "Do less. Do it legendary.",
        "overlay_tint": "#FF9100",
        "overlay_alpha": 0.12,
        "tilt_deg": -1.2,
        "zoom": 1.03,
        "gradient_a": "#130C06",
        "gradient_b": "#2A1607",
    },
    6: {
        "day_title": "Day 6 — Recovery Is Training",
        "story": "The penguin rests like a warrior. Recovery is not weakness.",
        "quote": "Rest is part of the grind.",
        "overlay_tint": "#18FFFF",
        "overlay_alpha": 0.09,
        "tilt_deg": 1.3,
        "zoom": 1.01,
        "gradient_a": "#071018",
        "gradient_b": "#0B1F2B",
    },
    7: {
        "day_title": "Day 7 — The First Evolution",
        "story": "Seven days. The penguin is not the same. The mountain notices.",
        "quote": "Consistency is the ultimate transformation.",
        "overlay_tint": "#D500F9",
        "overlay_alpha": 0.11,
        "tilt_deg": 0.0,
        "zoom": 1.04,
        "gradient_a": "#0B0616",
        "gradient_b": "#1A0B2B",
    },
}

def _make_arc_plan(arc: int, arc_day: int, day: int):
    a, b = ARC_COLORS.get(arc, ("#00E5FF", "#FF4FD8"))
    return {
        "arc_index": arc,
        "arc_day": arc_day,
        "arc_title": ARC_TITLES.get(arc, f"ARC {arc}"),
        "day_number": day,
        "day_title": f"Day {day} — {ARC_TITLES.get(arc, f'ARC {arc}')}",
        "story": _auto_story(arc, arc_day, day),
        "quote": _auto_quote(arc, arc_day),
        "image_key": f"bg_arc{arc}",
        "overlay_tint": a,
        "overlay_alpha": 0.08,
        "tilt_deg": 0.0,
        "zoom": 1.02,
        "gradient_a": a,
        "gradient_b": b,
        "tasks": _build_arc_stack_tasks(arc, arc_day),
    }

# ---------------------------
# Public API
# ---------------------------

def get_day_plan(day: int):
    try:
        day = int(day)
    except Exception:
        day = 1

    day = max(1, min(day, 74))

    arc = _arc_index(day)
    arc_day = _arc_day(day)

    # Arc1 detailed (but tasks are now your arc-stack model too)
    if arc == 1:
        d = ARC1_DAYS.get(arc_day, ARC1_DAYS[1])
        return {
            "arc_index": arc,
            "arc_day": arc_day,
            "arc_title": ARC1_TITLE,
            "day_number": day,
            "day_title": d["day_title"],
            "story": d["story"],
            "quote": d["quote"],
            "image_key": f"bg_arc{arc}",
            "overlay_tint": d["overlay_tint"],
            "overlay_alpha": d["overlay_alpha"],
            "tilt_deg": d["tilt_deg"],
            "zoom": d["zoom"],
            "gradient_a": d["gradient_a"],
            "gradient_b": d["gradient_b"],
            "tasks": _build_arc_stack_tasks(arc, arc_day),
        }

    # other arcs auto generated
    return _make_arc_plan(arc, arc_day, day)

import json
import os
import uuid
from datetime import datetime, timedelta

# -------------------------
# Helpers
# -------------------------

def _now_str():
    return datetime.now().strftime("%Y-%m-%d %H:%M")

def _default_pages():
    return {
        "current_page": "default",
        "pages": {
            "default": {"title": "default", "tasks": []}
        }
    }

def _ensure_parent_dir(path: str):
    parent = os.path.dirname(path)
    if parent and (not os.path.exists(parent)):
        os.makedirs(parent, exist_ok=True)

def _atomic_write_json(path: str, data):
    _ensure_parent_dir(path)
    tmp_path = path + ".tmp"
    with open(tmp_path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
    os.replace(tmp_path, path)

def _parse_iso(s: str):
    if not s:
        return None
    try:
        return datetime.strptime(s, "%Y-%m-%d %H:%M")
    except Exception:
        return None

def _format_iso(dt: datetime):
    return dt.strftime("%Y-%m-%d %H:%M")

def _normalize_task(t: dict):
    if "id" not in t or not t["id"]:
        t["id"] = str(uuid.uuid4())
    if "name" not in t:
        t["name"] = "Task"
    if "timer_minutes" not in t:
        t["timer_minutes"] = None
    if "reward" not in t:
        t["reward"] = None
    if "scheduled_start" not in t:
        t["scheduled_start"] = None
    if "started_at" not in t:
        t["started_at"] = None
    if "completed" not in t:
        t["completed"] = False

    # ✅ Tier-1 + Tier-2 fields
    if "tags" not in t or not isinstance(t["tags"], list):
        t["tags"] = []
    if "recurrence" not in t or not isinstance(t["recurrence"], (dict, type(None))):
        t["recurrence"] = None
    if "subtasks" not in t or not isinstance(t["subtasks"], list):
        t["subtasks"] = []
    if "note" not in t or not isinstance(t["note"], str):
        t["note"] = ""

    return t

def _normalize_pages(pages):
    if not isinstance(pages, dict):
        pages = _default_pages()

    if "pages" not in pages or not isinstance(pages["pages"], dict):
        pages["pages"] = {}

    if "default" not in pages["pages"] or not isinstance(pages["pages"]["default"], dict):
        pages["pages"]["default"] = {"title": "default", "tasks": []}

    for pid, page in list(pages["pages"].items()):
        if not isinstance(page, dict):
            pages["pages"][pid] = {"title": str(pid), "tasks": []}
            page = pages["pages"][pid]
        if "title" not in page:
            page["title"] = str(pid)
        if "tasks" not in page or not isinstance(page["tasks"], list):
            page["tasks"] = []

        new_tasks = []
        for t in page["tasks"]:
            if not isinstance(t, dict):
                continue
            new_tasks.append(_normalize_task(t))
        page["tasks"] = new_tasks

    cp = pages.get("current_page", "default")
    if cp not in pages["pages"]:
        pages["current_page"] = "default"

    return pages

# -------------------------
# Storage
# -------------------------

def load_pages(path: str):
    try:
        if not os.path.exists(path):
            pages = _default_pages()
            save_pages(path, pages)
            return pages

        with open(path, "r", encoding="utf-8") as f:
            raw = f.read().strip()

        if not raw:
            pages = _default_pages()
            save_pages(path, pages)
            return pages

        pages = json.loads(raw)
        pages = _normalize_pages(pages)
        save_pages(path, pages)
        return pages

    except Exception:
        pages = _default_pages()
        try:
            save_pages(path, pages)
        except Exception:
            pass
        return pages

def save_pages(path: str, pages):
    pages = _normalize_pages(pages)
    _atomic_write_json(path, pages)

# -------------------------
# Task builder
# -------------------------

def build_task(name: str, timer_minutes=None, reward=None, scheduled_start=None, tags=None, recurrence=None, subtasks=None):
    # Backward compatible: older callers pass only first 4 args.
    if tags is None:
        tags = []
    if subtasks is None:
        subtasks = []
    if isinstance(tags, str):
        tags = [x.strip() for x in tags.split(",") if x.strip()]
    if not isinstance(tags, list):
        tags = []

    # recurrence example:
    # {"type":"daily","interval":1}
    # {"type":"weekly","interval":1,"days":[0,2,4]}   # Mon=0 ... Sun=6
    if recurrence is not None and not isinstance(recurrence, dict):
        recurrence = None

    sub_list = []
    if isinstance(subtasks, str):
        for line in subtasks.splitlines():
            tx = line.strip()
            if tx:
                sub_list.append({"text": tx, "done": False})
    elif isinstance(subtasks, list):
        for s in subtasks:
            if isinstance(s, dict) and "text" in s:
                sub_list.append({"text": str(s.get("text","")).strip(), "done": bool(s.get("done", False))})
            elif isinstance(s, str) and s.strip():
                sub_list.append({"text": s.strip(), "done": False})

    return _normalize_task({
        "id": str(uuid.uuid4()),
        "name": name,
        "timer_minutes": timer_minutes,
        "reward": reward,
        "scheduled_start": scheduled_start,
        "started_at": None,
        "completed": False,
        "tags": tags,
        "recurrence": recurrence,
        "subtasks": sub_list,
        "note": ""
    })

# -------------------------
# Current page helpers
# -------------------------

def _get_current_page(pages):
    pages = _normalize_pages(pages)

    cp = pages.get("current_page", "default")
    container = pages.get("pages", {})

    if cp not in container:
        cp = "default"
        pages["current_page"] = "default"

    page = container.get(cp)
    if not isinstance(page, dict):
        container[cp] = {"title": str(cp), "tasks": []}
        page = container[cp]

    page.setdefault("tasks", [])
    return page

def add_task_to_current_page(pages, task):
    page = _get_current_page(pages)
    page.setdefault("tasks", []).append(_normalize_task(task))

def start_task_current_page(pages, index: int):
    page = _get_current_page(pages)
    tasks = page.get("tasks", [])
    if 0 <= index < len(tasks):
        tasks[index]["started_at"] = _now_str()
        tasks[index]["completed"] = False
        tasks[index]["scheduled_start"] = None
        return True
    return False

def mark_task_done_current_page(pages, index: int):
    page = _get_current_page(pages)
    tasks = page.get("tasks", [])
    if 0 <= index < len(tasks):
        tasks[index]["completed"] = True
        tasks[index]["started_at"] = None
        tasks[index]["scheduled_start"] = None
        return True
    return False

def delete_task_current_page(pages, index: int):
    page = _get_current_page(pages)
    tasks = page.get("tasks", [])
    if 0 <= index < len(tasks):
        tasks.pop(index)
        return True
    return False

# -------------------------
# Multi-page helpers
# -------------------------

def add_page(pages, page_id: str, title: str):
    pages = _normalize_pages(pages)
    pages.setdefault("pages", {})
    if page_id in pages["pages"]:
        return False
    pages["pages"][page_id] = {"title": title, "tasks": []}
    return True

def rename_page(pages, old_id: str, new_id: str):
    pages = _normalize_pages(pages)
    container = pages.setdefault("pages", {})

    if old_id not in container:
        return False
    if new_id in container:
        return False
    if old_id == "default":
        return False

    container[new_id] = container.pop(old_id)
    if pages.get("current_page") == old_id:
        pages["current_page"] = new_id
    return True

def delete_page(pages, page_id: str):
    pages = _normalize_pages(pages)
    container = pages.setdefault("pages", {})

    if page_id == "default":
        container["default"] = container.get("default", {"title": "default", "tasks": []})
        return False

    if page_id in container:
        del container[page_id]

    if pages.get("current_page") == page_id:
        pages["current_page"] = "default"
        if "default" not in container:
            container["default"] = {"title": "default", "tasks": []}

    return True

def set_current_page(pages, page_id: str):
    pages = _normalize_pages(pages)
    if page_id in pages.get("pages", {}):
        pages["current_page"] = page_id
        return True
    return False

# -------------------------
# By-ID helpers (Alarm + Timer finish)
# -------------------------

def _find_task_ref(pages, task_id: str):
    pages = _normalize_pages(pages)
    pages_container = pages.get("pages", {})
    for page_id, page in pages_container.items():
        tasks = page.get("tasks", [])
        for idx, t in enumerate(tasks):
            if t.get("id") == task_id:
                return page_id, page, idx, t
    return None, None, None, None

def start_task_by_id(pages, task_id: str):
    _, _, _, t = _find_task_ref(pages, task_id)
    if t:
        t["started_at"] = _now_str()
        t["completed"] = False
        t["scheduled_start"] = None
        return True
    return False

def stop_task_by_id(pages, task_id: str):
    _, _, _, t = _find_task_ref(pages, task_id)
    if t:
        t["started_at"] = None
        return True
    return False

def complete_task_by_id(pages, task_id: str):
    _, _, _, t = _find_task_ref(pages, task_id)
    if t:
        t["completed"] = True
        t["started_at"] = None
        t["scheduled_start"] = None
        return True
    return False

def mark_task_done_by_id(pages, task_id: str):
    return complete_task_by_id(pages, task_id)

def set_task_running(pages, task_id: str, running: bool):
    _, _, _, t = _find_task_ref(pages, task_id)
    if t:
        if running:
            t["started_at"] = _now_str()
            t["completed"] = False
        else:
            t["started_at"] = None
        return True
    return False

def delete_task_by_id(pages, task_id: str):
    page_id, page, idx, _ = _find_task_ref(pages, task_id)
    if page and idx is not None:
        page["tasks"].pop(idx)
        return True
    return False

def set_task_schedule_by_id(pages, task_id: str, scheduled_iso: str):
    _, _, _, t = _find_task_ref(pages, task_id)
    if t:
        t["scheduled_start"] = scheduled_iso
        return True
    return False

# -------------------------
# Tier 1: Recurrence (Daily / Weekly / Every N days)
# -------------------------

def _compute_next_from_recurrence(task: dict, base_dt: datetime):
    rec = task.get("recurrence")
    if not rec or not isinstance(rec, dict):
        return None

    rtype = str(rec.get("type", "")).lower().strip()
    interval = rec.get("interval", 1)
    try:
        interval = int(interval)
        if interval < 1:
            interval = 1
    except Exception:
        interval = 1

    if rtype == "daily":
        return base_dt + timedelta(days=interval)

    if rtype == "every_ndays":
        return base_dt + timedelta(days=interval)

    if rtype == "weekly":
        days = rec.get("days", [])
        if not isinstance(days, list) or not days:
            # fallback: same weekday
            days = [base_dt.weekday()]
        days = sorted([int(d) for d in days if isinstance(d, (int, float, str))])

        # Find next occurrence after base_dt
        cur_wd = base_dt.weekday()
        # try next days in the coming 14 days max
        for add in range(1, 15):
            cand = base_dt + timedelta(days=add)
            if cand.weekday() in days:
                # apply interval (weeks) by skipping in chunks
                weeks_between = (cand.date() - base_dt.date()).days // 7
                if weeks_between % interval == 0:
                    return cand
        return base_dt + timedelta(days=7 * interval)

    return None

def complete_task_and_generate_next(pages, task_id: str):
    """
    Marks the task completed AND if it has recurrence, creates the next instance and returns it.
    Returns: dict(next_task) or None
    """
    page_id, page, idx, t = _find_task_ref(pages, task_id)
    if not t:
        return None

    # Determine base time: if task had scheduled_start, use that time-of-day; else now
    base_dt = _parse_iso(t.get("scheduled_start") or "") or datetime.now()

    # complete current
    t["completed"] = True
    t["started_at"] = None
    t["scheduled_start"] = None

    next_dt = _compute_next_from_recurrence(t, base_dt)
    if not next_dt:
        return None

    # clone task (keep name, timer, reward, tags, recurrence, subtasks)
    new_task = build_task(
        name=t.get("name", "Task"),
        timer_minutes=t.get("timer_minutes", None),
        reward=t.get("reward", None),
        scheduled_start=_format_iso(next_dt),
        tags=t.get("tags", []),
        recurrence=t.get("recurrence", None),
        subtasks=t.get("subtasks", []),
    )
    # keep note empty for new instance
    new_task["note"] = ""

    # append to same page
    if page and isinstance(page.get("tasks"), list):
        page["tasks"].append(new_task)

    return new_task

import json
import os
import uuid
from datetime import datetime

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
    # atomic replace on most OS
    os.replace(tmp_path, path)

def _normalize_pages(pages):
    """Always keep required keys/shape so Kotlin UI never breaks."""
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

        # normalize each task
        new_tasks = []
        for t in page["tasks"]:
            if not isinstance(t, dict):
                continue
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
            new_tasks.append(t)
        page["tasks"] = new_tasks

    cp = pages.get("current_page", "default")
    if cp not in pages["pages"]:
        pages["current_page"] = "default"

    return pages

# -------------------------
# Storage
# -------------------------

def load_pages(path: str):
    """
    Loads pages.json safely.
    If missing/corrupt -> creates default and saves it.
    """
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
        # optional: write back normalized version
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

def build_task(name: str, timer_minutes=None, reward=None, scheduled_start=None):
    return {
        "id": str(uuid.uuid4()),
        "name": name,
        "timer_minutes": timer_minutes,
        "reward": reward,
        "scheduled_start": scheduled_start,
        "started_at": None,
        "completed": False
    }

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
    page.setdefault("tasks", []).append(task)

def start_task_current_page(pages, index: int):
    page = _get_current_page(pages)
    tasks = page.get("tasks", [])
    if 0 <= index < len(tasks):
        tasks[index]["started_at"] = _now_str()
        tasks[index]["completed"] = False
        return True
    return False

def mark_task_done_current_page(pages, index: int):
    page = _get_current_page(pages)
    tasks = page.get("tasks", [])
    if 0 <= index < len(tasks):
        tasks[index]["completed"] = True
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
        # don't rename default
        return False

    container[new_id] = container.pop(old_id)
    if pages.get("current_page") == old_id:
        pages["current_page"] = new_id
    return True

def delete_page(pages, page_id: str):
    pages = _normalize_pages(pages)
    container = pages.setdefault("pages", {})

    if page_id == "default":
        # never delete default
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

def start_task_by_id(pages, task_id: str):
    pages = _normalize_pages(pages)
    pages_container = pages.get("pages", {})
    for _, page in pages_container.items():
        for t in page.get("tasks", []):
            if t.get("id") == task_id:
                t["started_at"] = _now_str()
                t["completed"] = False
                return True
    return False

def mark_task_done_by_id(pages, task_id: str):
    pages = _normalize_pages(pages)
    pages_container = pages.get("pages", {})
    for _, page in pages_container.items():
        for t in page.get("tasks", []):
            if t.get("id") == task_id:
                t["completed"] = True
                return True
    return False

def delete_task_by_id(pages, task_id: str):
    pages = _normalize_pages(pages)
    pages_container = pages.get("pages", {})
    for _, page in pages_container.items():
        tasks = page.get("tasks", [])
        for i, t in enumerate(tasks):
            if t.get("id") == task_id:
                tasks.pop(i)
                return True
    return False

# -------------------------
# NEW: Schedule helper (Journey Schedule button)
# -------------------------

def set_task_schedule_by_id(pages, task_id: str, scheduled_iso: str):
    pages = _normalize_pages(pages)
    pages_container = pages.get("pages", {})
    for _, page in pages_container.items():
        for t in page.get("tasks", []):
            if t.get("id") == task_id:
                t["scheduled_start"] = scheduled_iso
                return True
    return False

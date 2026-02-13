import json
import os
import uuid
from datetime import datetime

def _now_str():
    return datetime.now().strftime("%Y-%m-%d %H:%M")

def _default_pages():
    return {
        "current_page": "default",
        "pages": {
            "default": {"title": "default", "tasks": []}
        }
    }

def load_pages(path: str):
    if not os.path.exists(path):
        pages = _default_pages()
        save_pages(path, pages)
        return pages
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)

def save_pages(path: str, pages):
    with open(path, "w", encoding="utf-8") as f:
        json.dump(pages, f, indent=2)

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

def _get_current_page(pages):
    cp = pages.get("current_page", "default")
    container = pages.get("pages", {})
    if cp not in container:
        cp = "default"
        pages["current_page"] = "default"
        if "default" not in container:
            container["default"] = {"title": "default", "tasks": []}
            pages["pages"] = container
    return container[cp]

def add_task_to_current_page(pages, task):
    page = _get_current_page(pages)
    page.setdefault("tasks", []).append(task)

def start_task_current_page(pages, index: int):
    page = _get_current_page(pages)
    tasks = page.get("tasks", [])
    if 0 <= index < len(tasks):
        tasks[index]["started_at"] = _now_str()
        tasks[index]["completed"] = False

def mark_task_done_current_page(pages, index: int):
    page = _get_current_page(pages)
    tasks = page.get("tasks", [])
    if 0 <= index < len(tasks):
        tasks[index]["completed"] = True

def delete_task_current_page(pages, index: int):
    page = _get_current_page(pages)
    tasks = page.get("tasks", [])
    if 0 <= index < len(tasks):
        tasks.pop(index)

# ===== multipage =====
def add_page(pages, page_id: str, title: str):
    pages.setdefault("pages", {})
    if page_id in pages["pages"]:
        return
    pages["pages"][page_id] = {"title": title, "tasks": []}

def rename_page(pages, old_id: str, new_id: str):
    container = pages.setdefault("pages", {})
    if old_id not in container:
        return
    if new_id in container:
        return
    container[new_id] = container.pop(old_id)
    if pages.get("current_page") == old_id:
        pages["current_page"] = new_id

def delete_page(pages, page_id: str):
    container = pages.setdefault("pages", {})
    if page_id == "default":
        container["default"] = container.get("default", {"title": "default", "tasks": []})
        return
    if page_id in container:
        del container[page_id]
    if pages.get("current_page") == page_id:
        pages["current_page"] = "default"
        if "default" not in container:
            container["default"] = {"title": "default", "tasks": []}

# ===== by-id helpers (Alarm + Timer finish) =====
def start_task_by_id(pages, task_id: str):
    pages_container = pages.get("pages", {})
    for _, page in pages_container.items():
        for t in page.get("tasks", []):
            if t.get("id") == task_id:
                t["started_at"] = _now_str()
                t["completed"] = False
                return True
    return False

def mark_task_done_by_id(pages, task_id: str):
    pages_container = pages.get("pages", {})
    for page_id, page in pages_container.items():
        for t in page.get("tasks", []):
            if t.get("id") == task_id:
                t["completed"] = True
                return True
    return False


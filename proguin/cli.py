import os
import json
from datetime import datetime, timedelta


def create_page():
    """Ask user for page title and create a page."""
    title = input("Enter page title: ")
    page = {"title": title, "tasks": []}
    return page


def create_task():
    """Ask user inputs and return a task dictionary."""
    name = input("Enter task name: ")

    timer_minutes = None
    wants_timer = input("Do you wants a timer in minutes? (y/n): ").strip().lower()
    if wants_timer == "y":
        while True:
            timer_minutes = input("Enter timer_minutes: ")
            if timer_minutes.isdigit():
                timer_minutes = int(timer_minutes)
                break
            else:
                print("Please enter a number")

    reward = None
    wants_reward = input("Do you wants a reward? (y/n): ").strip().lower()
    if wants_reward == "y":
        reward = input("Enter reward: ")

    task = {
        "name": name,
        "timer_minutes": timer_minutes,
        "reward": reward,
        "completed": False,
        "started_at": None,
        "ends_at": None
    }
    return task


def add_task_to_page(page, task):
    """Add a task into the page."""
    page["tasks"].append(task)


def start_task(page, task_index):
    """Start a task and set started_at / ends_at values."""
    task = page["tasks"][task_index]

    start_dt = datetime.now()
    task["started_at"] = start_dt.isoformat(timespec="seconds")

    ends_at = calculate_ends_at(start_dt, task["timer_minutes"])
    if ends_at is not None:
        task["ends_at"] = ends_at.isoformat(timespec="seconds")
    else:
        task["ends_at"] = None


def calculate_ends_at(started_at, timer_minutes):
    """Return end time if timer exists, otherwise None."""
    if timer_minutes is None:
        return None
    else:
        return started_at + timedelta(minutes=timer_minutes)


def mark_task_done(page, task_index):
    """Mark a task as completed."""
    if task_index < 0 or task_index >= len(page["tasks"]):
        print("Task is invalid")
        return
    page["tasks"][task_index]["completed"] = True


def save_page(page):
    """Save page to data/page.json."""
    os.makedirs("data", exist_ok=True)
    with open("data/page.json", "w") as file:
        json.dump(page, file, indent=4)


def load_page():
    """Load page from JSON (and fill missing keys for old data)."""
    with open("data/page.json", "r") as file:
        page = json.load(file)

    for task in page.get("tasks", []):  # migration defaults
        task.setdefault("started_at", None)
        task.setdefault("ends_at", None)

    return page


def status_text(completed):
    """Return task status text."""
    return "-> Done" if completed else "-> Not Done"


def list_tasks(page):
    """Print all tasks in the page."""
    print(page["title"])
    if len(page["tasks"]) == 0:
        print("No tasks yet")
        return

    for number, task in enumerate(page["tasks"], start=1):
        if task["timer_minutes"] is None:
            timer_display = "-"
        else:
            timer_display = f"({task['timer_minutes']}m)"

        if task["reward"] is None:
            reward_display = ""
        else:
            reward_display = f" -> {task['reward']}"

        status = status_text(task["completed"])

        if task["completed"]:
            print(number, ".", "[x]", task["name"], timer_display, reward_display, status)
        else:
            print(number, ".", "[ ]", task["name"], timer_display, reward_display, status)


def main():
    """Temporary flow until menu is added."""
    if os.path.exists("data/page.json"):
        page = load_page()
    else:
        page = create_page()
        save_page(page)

    task = create_task()
    add_task_to_page(page, task)

    task_index = len(page["tasks"]) - 1
    start_task(page, task_index)
    save_page(page)

    list_tasks(page)


if __name__ == "__main__":
    main()

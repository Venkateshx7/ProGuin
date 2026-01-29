import json
import os
from datetime import datetime, timedelta

SCHEDULE_FMT = "%Y-%m-%d %H:%M"


def ask_scheduled_start():
    """Ask user to choose choice in scheduling a task"""
    while True:
        print("When should this task start?")
        print("1. Start now")
        print("2. Schedule for later")
        choice = input("Enter choice: ").strip()
        if choice == "1":
            return None
        elif choice == "2":
            while True:
                raw = input(
                    "Enter scheduled date & time (YYYY-MM-DD HH:MM) e.g., 2026-02-05 21:00: "
                ).strip()
                try:
                    datetime.strptime(raw, SCHEDULE_FMT)
                    return raw
                except ValueError:
                    print("Invalid Formate.  Example: 2026-02-05 21:00")

        else:
            print("Invalid choice")


def create_page():
    """Create a new page with user-provided title."""
    title = input("Enter page title: ")
    page = {"title": title, "tasks": []}
    return page


def create_task():
    """Create a new task with name, timer, and and reward."""
    name = input("Enter task name: ")
    scheduled_start = ask_scheduled_start()
    timer_minutes = None
    wants_timer = input("Do you want to set a timer? (y/n): ").strip().lower()
    if wants_timer == "y":
        while True:
            timer_minutes_str = input("Enter timer minutes: ")
            if timer_minutes_str.isdigit():
                timer_minutes = int(timer_minutes_str)
                break
            else:
                print("Please enter a number")

    reward = None
    wants_reward = input("Do you want to add a reward? (y/n): ").strip().lower()
    if wants_reward == "y":
        reward = input("Enter reward: ")

    task = {
        "name": name,
        "timer_minutes": timer_minutes,
        "reward": reward,
        "scheduled_start": scheduled_start,
        "completed": False,
        "started_at": None,
        "ends_at": None,
    }
    return task


def delete_task(path, id):
    with open(path, "r", encoding="utf-8") as f:
        data = json.load(f)

    tasks = data["tasks"]

    task_to_delete = id - 1

    if 0 <= task_to_delete < len(tasks):
        tasks.pop(task_to_delete)
    else:
        print(f" {id} is out of range, please enter valid ID.")
        return

    with open(path, "r", encoding="utf-8") as f:
        data = json.load(f)

    data["tasks"] = tasks

    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=4, ensure_ascii=False)

    print(f"Deleted task with ID {id}.")

    page = load_page()

    return page


def add_task_to_page(page, task):
    """Add a task to the page's task list."""
    page["tasks"].append(task)


def start_task(page, task_index):
    """Start a task by setting started_at and ends_at (as ISO string or None)."""
    if task_index < 0 or task_index >= len(page["tasks"]):
        print("Invalid task index")
        return
    task = page["tasks"][task_index]
    if task["started_at"] is not None:
        print("Task already started")
        return

    start_dt = datetime.now()
    task["started_at"] = start_dt.isoformat(timespec="seconds")

    if task["timer_minutes"] is not None:
        ends_at = start_dt + timedelta(minutes=task["timer_minutes"])
        task["ends_at"] = ends_at.isoformat(timespec="seconds")

    else:
        task["ends_at"] = None


def mark_task_done(page, task_index):
    """Mark a task as completed."""
    if task_index < 0 or task_index >= len(page["tasks"]):
        print("Invalid task index")
        return
    page["tasks"][task_index]["completed"] = True


def save_page(page):
    """Save the page data to JSON file."""
    os.makedirs("data", exist_ok=True)
    with open("data/page.json", "w") as file:
        json.dump(page, file, indent=4)


def load_page():
    """Load page from JSON or create new if missing."""
    if not os.path.exists("data/page.json"):
        print("No existing page found. Creating a new one.")
        return create_page()
    with open("data/page.json", "r") as file:
        page = json.load(file)
    for task in page.get("tasks", []):
        task.setdefault("started_at", None)
        task.setdefault("ends_at", None)
        task.setdefault("scheduled_start", None)
    return page


def status_text(completed):
    """Return status string for display."""
    return "Done" if completed else "Not Done"


def list_tasks(page):
    """List all tasks with formatted output."""
    print("\n" + page["title"])
    if len(page["tasks"]) == 0:
        print("No tasks yet")
        return
    for id, task in enumerate(page["tasks"], start=1):
        timer_display = (
            f"{task['timer_minutes']}m" if task["timer_minutes"] is not None else "-"
        )
        reward_display = f"{task['reward']}" if task["reward"] is not None else ""
        scheduled_display = (
            f"{task['scheduled_start']}" if task["scheduled_start"] is not None else ""
        )
        status = status_text(task["completed"])
        checkbox = "(🟢)" if task["completed"] else "[🟡]"

        started = f" (Started: {task['started_at']})" if task["started_at"] else ""
        ends = f" (Ends: {task['ends_at']})" if task["ends_at"] else ""

        print(
            f"\n {checkbox} {task['name']} \n   Time: {timer_display} \n   Reward: {reward_display} \n   Scheduled to: {scheduled_display} \n   Started: {started} \n   Ends: {ends} \n   Status: {status} \n   ID: {id}"
        )


def get_task_index(page, prompt):
    """Safely get a valid 1-based task index from user input."""
    while True:
        try:
            index = int(input(prompt)) - 1
            if 0 <= index < len(page["tasks"]):
                return index
            else:
                print(f"Invalid task ID. Must be between 1 and {len(page['tasks'])}.")
        except ValueError:
            print("Please enter a valid ID.")


def main():
    try:  # for keyboard interrupt
        page = load_page()

        loaded_first_time = True

        while True:
            if loaded_first_time == True:
                print("\n=== ProGuin - Your Personal Task Manager ===")
                print("1 - View tasks")
                print("2 - Add new task")
                print("3 - Mark task done")
                print("4 - Remove task")
                print("5 - Exit Proguin")
            else:
                print(
                    "\n| 1 - View tasks | 2 - Add new task | 3 - Mark task done | 4 - Remove task | 5 - Exit ProGuin |"
                )

            loaded_first_time = False

            choice = input("Enter your choice (1-5): ").strip()

            if choice == "1":
                list_tasks(page)
            elif choice == "2":
                task = create_task()
                add_task_to_page(page, task)
                # Auto-start if timer was set
                if (
                    task["scheduled_start"] is None
                    and task["timer_minutes"] is not None
                ):
                    task_index = len(page["tasks"]) - 1
                    start_task(page, task_index)
                    print("▶ Task started with timer")
                else:
                    print("✅ Task added successfully")
                save_page(page)
            elif choice == "3":
                list_tasks(page)
                if len(page["tasks"]) > 0:
                    index = get_task_index(page, "Enter task ID to mark done: ")
                    mark_task_done(page, index)
                    save_page(page)
                    print("✔ Task marked as done")
            elif choice == "4":
                task_to_remove = input("Enter the ID of a task you want to remove: ")
                page = delete_task("data/page.json", int(task_to_remove))
            elif choice == "5":
                save_page(page)
                print("👋 See you soon. Keep moving forward.")
                break
            else:
                print("⚠ Please enter a valid option.")
    except KeyboardInterrupt:
        print("\n\nExited ProGuin (Ctrl+C)\n")


if __name__ == "__main__":
    main()

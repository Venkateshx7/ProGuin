import os
import json
from datetime import datetime, timedelta

SCHEDULE_FMT = "%Y-%m-%d %H:%M"
def ask_scheduled_start():
    """Ask user to choose choice in scheduling a task"""
    while True:
        choice = input("start_now or schedule_later ? (1=start_now ,2=schedule_later): ")
        if choice == "1":
            return None
        elif choice == "2":
            while True:
                raw = input(f"Enter scheduled date & time ({SCHEDULE_FMT}): ").strip()
                try:
                    datetime.strptime(raw,SCHEDULE_FMT)
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
    wants_timer = input("Do you want a timer in minutes? (y/n): ").strip().lower()
    if wants_timer == "y":
        while True:
            timer_minutes_str = input("Enter timer_minutes: ")
            if timer_minutes_str.isdigit():
                timer_minutes = int(timer_minutes_str)
                break
            else:
                print("Please enter a number")

    reward = None
    wants_reward = input("Do you want a reward? (y/n): ").strip().lower()
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
    return "-> Done" if completed else "-> Not Done"

def list_tasks(page):
    """List all tasks with formatted output."""
    print("\n" + page["title"])
    if len(page["tasks"]) == 0:
        print("No tasks yet")
        return
    for number, task in enumerate(page["tasks"], start=1):
        timer_display = f"({task['timer_minutes']}m)" if task["timer_minutes"] is not None else "-"
        reward_display = f" -> {task['reward']}" if task["reward"] is not None else ""
        scheduled_display = f" (Scheduled: {task['scheduled_start']})" if task["scheduled_start"] is not None else ""
        status = status_text(task["completed"])
        checkbox = "[x]" if task["completed"] else "[ ]"
        
        started = f" (Started: {task['started_at']})" if task["started_at"] else ""
        ends = f" (Ends: {task['ends_at']})" if task["ends_at"] else ""
        
        print(f"{number}. {checkbox} {task['name']} {timer_display}{reward_display}{scheduled_display}{started}{ends} {status}")

def get_task_index(page, prompt):
    """Safely get a valid 1-based task index from user input."""
    while True:
        try:
            index = int(input(prompt)) - 1
            if 0 <= index < len(page["tasks"]):
                return index
            else:
                print(f"Invalid task number. Must be between 1 and {len(page['tasks'])}.")
        except ValueError:
            print("Please enter a valid number.")

def main():
    page = load_page()
    
    while True:
        print("\n=== ProGuin ===")
        print("1. View tasks")
        print("2. Add a task")
        print("3. Mark a task as completed")
        print("4. Exit")
        
        choice = input("Enter your choice (1-4): ").strip()
        
        if choice == '1':
            list_tasks(page)
        elif choice == '2':
            task = create_task()
            add_task_to_page(page, task)
            # Auto-start if timer was set
            if task["scheduled_start"] is None and task["timer_minutes"] is not None:
                task_index = len(page["tasks"]) - 1
                start_task(page, task_index)
                print("Task added and started (timer active)!")
            else:
                print("Task added!")
            save_page(page)
        elif choice == '3':
            list_tasks(page)
            if len(page["tasks"]) > 0:
                index = get_task_index(page, "Enter task number to mark done: ")
                mark_task_done(page, index)
                save_page(page)
                print("Task marked as done!")
        elif choice == '4':
            save_page(page)
            print("Goodbye! Keep building slowly.")
            break
        else:
            print("Invalid choice. Try again.")

if __name__ == "__main__":
    main()
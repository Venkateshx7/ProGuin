import os
import json
from datetime import datetime, timedelta

def create_page():
    title = input("Enter page title: ")
    page = {"title": title, "tasks": []}
    return page

def create_task():
    name = input("Enter task name: ")
    timer_minutes = None
    wants_timer = input("Do you want a timer in minutes? (y/n): ").strip().lower()
    if wants_timer == "y":
        while True:
            timer_minutes = input("Enter timer_minutes: ")
            if timer_minutes.isdigit():
                timer_minutes = int(timer_minutes)
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
        "completed": False,
        "started_at": None,
        "ends_at": None
    }
    return task

def add_task_to_page(page, task):
    page["tasks"].append(task)

def start_task(page, task_index):
    if task_index < 0 or task_index >= len(page["tasks"]):
        print("Invalid task index")
        return
    task = page["tasks"][task_index]
    if task["started_at"] is not None:
        print("Task already started")
        return

    start_dt = datetime.now()
    task["started_at"] = start_dt.isoformat(timespec="seconds")
    task["ends_at"] = calculate_ends_at(start_dt, task["timer_minutes"])

def calculate_ends_at(started_at, timer_minutes):
    if timer_minutes is None:
        return None
    else:
        return started_at + timedelta(minutes=timer_minutes)

def mark_task_done(page, task_index):
    if task_index < 0 or task_index >= len(page["tasks"]):
        print("Invalid task index")
        return
    page["tasks"][task_index]["completed"] = True

def save_page(page):
    os.makedirs("data", exist_ok=True)
    with open("data/page.json", "w") as file:
        json.dump(page, file, indent=4)

def load_page():
    if not os.path.exists("data/page.json"):
        print("No existing page found. Creating a new one.")
        return create_page()
    with open("data/page.json", "r") as file:
        page = json.load(file)
    for task in page.get("tasks", []):
        task.setdefault("started_at", None)
        task.setdefault("ends_at", None)
    return page

def status_text(completed):
    return "-> Done" if completed else "-> Not Done"

def list_tasks(page):
    print("\n" + page["title"])
    if len(page["tasks"]) == 0:
        print("No tasks yet")
    for number, task in enumerate(page["tasks"], start=1):
        timer_display = f"({task['timer_minutes']}m)" if task["timer_minutes"] is not None else "-"
        reward_display = f" -> {task['reward']}" if task["reward"] is not None else ""
        status = status_text(task["completed"])
        checkbox = "[x]" if task["completed"] else "[ ]"
        
        started = f" (Started: {task['started_at']})" if task["started_at"] else ""
        ends = f" (Ends: {task['ends_at']})" if task["ends_at"] else ""
        
        print(f"{number}. {checkbox} {task['name']} {timer_display}{reward_display}{started}{ends} {status}")

def get_task_index(prompt):
    while True:
        try:
            index = int(input(prompt)) - 1
            if index >= 0:
                return index
            else:
                print("Index must be positive.")
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
            save_page(page)
            print("Task added!")
        elif choice == '3':
            list_tasks(page)
            if len(page["tasks"]) > 0:
                try:
                    index = int(input("Enter task number to mark done: ")) - 1
                    mark_task_done(page, index)
                    save_page(page)
                    print("Task marked as done!")
                except ValueError:
                    print("Please enter a valid number.")
        elif choice == '4':
            save_page(page)
            print("Goodbye! Keep building slowly. 🐧")
            break
        else:
            print("Invalid choice. Try again.")

if __name__ == "__main__":
    main()
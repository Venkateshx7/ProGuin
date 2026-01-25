import os
import json

from datetime import datetime, timedelta

def create_page():
    title = input("Enter page title: ")# ask user for page title
    page = {"title": title , "tasks": []} # create a page dictionary
    return page # return it

def create_task():
    name = input("Enter task name: ")# 1) ask task name
    timer_minutes = None
    wants_timer = input("Do you wants a timer in minutes? (y/n): ").strip().lower()# 2) ask if user wants timer (y/n)
    if wants_timer == "y":
        timer_minutes = int(input("Enter timer minutes: "))# 3) if yes -> ask minutes (number)

    reward = None
    wants_reward = input("Do you wants a reward? (y/n): ").strip().lower()# 4) ask if user wants reward (y/n)
    if wants_reward == "y":
        reward = input("Enter reward: ") # 5) if yes -> ask reward text

    task = {
        "name" : name ,
        "timer_minutes" : timer_minutes,
        "reward" : reward,
        "completed" : False,
        "started_at": None,
        "ends_at": None
    }
    return task# 6) return task dictionary
def status_text(completed):
    return "Done" if completed else "Not Done"

def add_task_to_page(page, task):
    page["tasks"].append(task)

def start_task(page, task_index):
    task = page["tasks"][task_index]

    start_dt = datetime.now()
    task["started_at"] = start_dt.isoformat(timespec="seconds")

    task["ends_at"] = calculate_ends_at(start_dt, task["timer_minutes"])
    if task["ends_at"] is not None:
        task["ends_at"] = task["ends_at"].isoformat(timespec="seconds")

def calculate_ends_at(started_at, timer_minutes):
    if timer_minutes is None:
        return None
    else:
        return started_at+ timedelta(minutes=timer_minutes)

def mark_task_done(page, task_index):
    page["tasks"][task_index]["completed"] = True

def save_page(page):
    os.makedirs("data", exist_ok=True)
    with open("data/page.json", "w") as file:
        json.dump(page, file, indent=4 )

def load_page():
    with open("data/page.json", "r") as file:
        page = json.load(file)
    for task in page.get("tasks", []):#it is just a migration code
        task.setdefault("started_at", None)
        task.setdefault("ends_at", None)
    return page

def list_task(page):
    print(page["title"])
    if len(page["tasks"]) == 0:
        print("No tasks yet")
    for number,task in enumerate(page["tasks"], start=1):

        timer_display = None
        if task["timer_minutes"] is None:
           timer_display = ("-")
        else:
            timer_display = f"({task['timer_minutes']}m)"

        reward_display = None
        if task["reward"] is None:
            reward_display = ""
        else:
            reward_display =  f" -> {task['reward']}"

        if task["completed"]:
            print(number,".", "[x]", task["name"],timer_display,reward_display)
        else:
            print(number,".", "[ ]" , task["name"],timer_display,reward_display)

if __name__ == "__main__":
    if os.path.exists("data/page.json"):
        p = load_page()
    else:
        p = create_page()
        save_page(p)

    t = create_task()
    add_task_to_page(p, t)

    task_index = len(p["tasks"]) - 1

    start_task(p, task_index)
    save_page(p)

    # Reload to confirm persistence
    p2 = load_page()
    task2 = p2["tasks"][task_index]

    print("started_at:", task2["started_at"])
    print("ends_at:", task2["ends_at"])
    print("timer_minutes:", task2["timer_minutes"])

    list_task(p2)

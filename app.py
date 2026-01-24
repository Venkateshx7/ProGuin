import os
import json

from IPython.core.page import page_file
from mistune.plugins.task_lists import task_lists


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
        "completed" : False
    }
    return task# 6) return task dictionary
def status_text(completed):
    return "Done" if completed else "Not Done"

def add_task_to_page(page, task):
    page["tasks"].append(task)

def mark_task_done(page, task_index):
    page["tasks"][task_index]["completed"] = True

def save_page(page):
    with open("data/page.json", "w") as file:
        json.dump(page, file, indent=4 )

def load_page():
    with open("data/page.json", "r") as file:
        page = json.load(file)
    return page

def list_task(page):
    print(page["title"])
    if len(page["tasks"]) == 0:
        print("No tasks yet")
    for number,task in enumerate(page["tasks"], start=1):
        print(number,".", task["name"])

if __name__ == "__main__" :
    if os.path.exists("data/page.json"):
        p = load_page()
    else:
        p = create_page()
        save_page(p)
    t = create_task()
    add_task_to_page(p,t)
    print(p)
    mark_task_done(p,0)
    print(p)
    save_page(p)
    l = load_page()
    print(l)
    list_task(p)
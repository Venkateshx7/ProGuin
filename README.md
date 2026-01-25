ProGuin
👤 Author : Venkatesh D — Creator & Maintainer of ProGuin

ProGuin is a productivity system.

This is not just a todo-list app.

ProGuin is a growing experiment that combines ideas from multiple productivity philosophies and books,
starting from a simple CLI-based foundation and evolving step by step.

The goal is to build a system that:

Encourages consistency over motivation

Rewards completion, not planning

Works across CLI, desktop, and mobile in the future

This repository represents the beginning.

Philosophy

Discipline over motivation

Do hard work first, reward later

Time-bound focus

Simple daily structure

What ProGuin is NOT

Not a fancy todo app

Not a motivational quote generator

Not overwhelming

Current Features

Task-based daily planning

Optional time-bound tasks

Optional rewards

Persistent data storage (JSON)

Interactive CLI menu (v0.2.0)

Roadmap

Packaging improvements (v0.3.0)

Basic tests

Edit / delete tasks

Focus timers

Desktop / mobile versions (future)

Requirements

Python 3.8+

ProGuin uses only Python standard library modules.
No external dependencies are required.

How to Run
Option 1 — Install locally (recommended)
pip install -e .
proguin

Option 2 — Run directly
python -m proguin.cli

Menu (v0.2.0)
=== ProGuin ===
1. View tasks
2. Add a task
3. Mark a task as completed
4. Exit

Usage Example
Enter page title: My Day

=== ProGuin ===
1. View tasks
2. Add a task
3. Mark a task as completed
4. Exit

Enter your choice (1-4): 2
Enter task name: Learn Python
Do you want a timer in minutes? (y/n): y
Enter timer_minutes: 30
Do you want a reward? (y/n): n

My Day
1. [ ] Learn Python (30m) -> Not Done

Data Storage

Tasks are stored locally in:

data/page.json





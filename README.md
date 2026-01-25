# ProGuin 
## 👤 Author : **Venkatesh D**  - Creator & maintainer of ProGuin  

ProGuin is a productivity system 

This is not just a todo-list app.

ProGuin is a growing experiment that combines ideas from multiple productivity philosophies and books, 
starting from a simple CLI-based foundation and evolving step by step.

The goal is to build a system that:
- encourages consistency over motivation
- rewards completion, not planning
- works across CLI, desktop, and mobile in the future

This repository represents the beginning.

## Philosophy
- Discipline over motivation
- Do hard work first, reward later
- Time-bound focus
- Simple daily structure

## What ProGuin is NOT
- Not a fancy todo app
- Not a motivational quote generator
- Not overwhelming

## Current Features
- Task-based daily planning
- Time-bound tasks
- Rewards for completion
- Persistent data (JSON)

## Roadmap
- Interactive CLI menu (v0.2.0)
- Packaging improvements (v0.3.0)
- Focus timers
- Auto-run daily schedule
- Desktop/mobile versions (future)

## How to Run

- Requires Python 3.8+

### Dependencies

ProGuin currently uses only Python standard library modules.
No external packages are required.

- Clone the repository
- Run:

How to Run
Option 1 — Install locally (recommended)

pip install -e .
proguin


Option 2 — Run directly
python -m proguin.cli


### Demo
```text
Enter page title: My Day
Enter task name: Learn Python
Do you wants a timer in minutes? (y/n): y
Enter timer_minutes: 30
Do you wants a reward? (y/n): n

started_at: 2026-01-25T15:21:51
ends_at: 2026-01-25T15:51:51
timer_minutes: 30
My Day
1 . [ ] Learn Python (30m)  -> Not Done

```
Menu (v0.2.0)- Currently in progress....
1. View tasks
2. Add a task
3. Mark a task as completed
4. Exit
   
Data Storage
Tasks are stored locally in:

data/page.json

⭐ If you find ProGuin useful, consider giving it a star.

License

MIT License





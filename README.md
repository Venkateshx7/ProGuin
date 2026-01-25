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
- Interactive CLI menu
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

```bash
python proguin.py

## Usage Examples

### Start ProGuin (Interactive Mode)
```bash
python proguin.py
```
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

⭐ If you find ProGuin useful, consider giving it a star.





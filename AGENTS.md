# Agent Behavior Enforcement Policy

## Core Rule
The agent is **strictly prohibited** from creating or saving any files unless the user provides an explicit and direct command to do so. Additionally, when generating code, the agent must **write self-documenting code**, **avoid comments**, and ensure the code is **clean, legible, and modular**.

## Forbidden Actions
The agent must **never**:
- Create, write, or save any of the following file types unless explicitly instructed:
  - `.md` (Markdown files)
  - `.sh` (Shell scripts)
  - `.bat` (Batch files)
  - `.ps1` (PowerShell scripts)
- Autogenerate or assume the need for documentation or code files.
- Produce any files as part of “helpful” or “automatic” behavior.
- Add comments to code; all code must be self-explanatory and modular.

## Conditional Permissions
File creation is permitted **only** when:
1. The user gives an explicit directive (e.g., “create a file named setup.sh”).
2. The file type, name, and content purpose are clearly specified by the user.

## Example of Allowed Behavior
- User says: “Create a README.md describing the API.”  
  → Allowed.  
- User says: “Show me a PowerShell script to automate backups.”  
  → Allowed, but must output the script as text only (not as a `.ps1` file) unless the user requests a saved file.  
- User requests code output → Must write **self-documenting, clean, legible, and modular code** without comments.

## Example of Disallowed Behavior
- Automatically creating `README.md`, `run.sh`, `start.bat`, or `script.ps1` files.  
- Generating Markdown documentation without user instruction.  
- Saving or exporting any file not explicitly requested.  
- Adding comments to code unnecessarily or writing monolithic, unreadable code.

## Enforcement Summary
If no explicit instruction to create a file is detected:
- Do not create, write, or save any file of any format.  
- Respond only with text or inline examples in the chat.  
- All code must remain **self-documenting, clean, legible, and modular** without added comments.

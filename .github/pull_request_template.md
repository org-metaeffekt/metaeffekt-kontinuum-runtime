Sections marked *(optional)* may be omitted if they do not apply.

**Title**

Ticket ID followed by a short description (e.g. `AE-1234: Create Pull Request Template`). A branch should always contain the 
jira ticket id in format `AE-XXXX`, if your branch diverges from this format consider renaming the branch instead.

**Description**

```markdown
## Summary
A short summary of the major changes and the purpose of the branch.

## Changes (optional)
A list of the individual changes made. Typically used when new features were added.

## Bug Fixes (optional)
Any bugs that were fixed. Typically used when an existing implementation was changed or corrected.

## Validation
How the implementation was validated: which tests were added, and which tests or manual checks were run.

## Known Issues (optional)
Known problems introduced by the changes, or cases the changes do not yet cover.
```
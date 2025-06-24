# Git Hooks

This directory contains custom git hooks for the json5-kotlin project to automate development tasks and ensure code quality.

## Pre-commit Hook

### What it does

The `pre-commit` hook automatically formats Kotlin code using ktlint before each commit. It:

1. **Runs code formatting**: Executes `./gradlew formatKotlin` to format all Kotlin code according to project standards
2. **Handles formatting changes**: If the formatter makes changes to your code, those changes are automatically added to your commit
3. **Prevents bad commits**: If formatting fails (due to syntax errors or other issues), the commit is aborted

### Setup Instructions

To enable the pre-commit hook, you need to copy or symlink it to your local `.git/hooks/` directory:

#### Option 1: Copy the hook (recommended)
```bash
cp .githooks/pre-commit .git/hooks/pre-commit
```

#### Option 2: Symlink the hook (for automatic updates)
```bash
ln -s ../../.githooks/pre-commit .git/hooks/pre-commit
```

#### Option 3: Configure git to use .githooks directory globally
```bash
git config core.hooksPath .githooks
```

### Verification

To verify the hook is working correctly:

1. Make a small change to a Kotlin file with intentional formatting issues (e.g., extra spaces)
2. Try to commit the change: `git commit -m "test formatting"`
3. You should see the hook run, format the code, and include the formatted changes in your commit

### Manual Formatting

You can also run the formatting command manually at any time:

```bash
./gradlew formatKotlin
```

### Troubleshooting

**Hook not running?**
- Check that the hook file has execute permissions: `ls -la .git/hooks/pre-commit`
- If using symlink, ensure the path is correct
- Verify the hook is in the correct location: `.git/hooks/pre-commit`

**Formatting failing?**
- Run `./gradlew formatKotlin` manually to see detailed error messages
- Check for syntax errors in your Kotlin code
- Ensure you have Java 21 installed as required by the project

**Want to bypass the hook temporarily?**
- Use `git commit --no-verify` to skip the pre-commit hook for a single commit
- Only use this in exceptional cases as it bypasses code quality checks

## Benefits

- **Consistent code style**: Automatically enforces project coding standards
- **Reduces review overhead**: No more formatting-related review comments
- **Developer convenience**: No need to remember to run formatting manually
- **Clean commit history**: All commits maintain consistent formatting

## Customization

To modify the hook behavior, edit `.githooks/pre-commit` and then update your local hook:

```bash
cp .githooks/pre-commit .git/hooks/pre-commit
```

The hook is designed to be minimal and focused on formatting. For additional checks (linting, tests), consider extending the hook or adding separate hooks.
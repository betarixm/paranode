# Convention Guidelines

## Code Style

### General

- **Style**: Adhere to typical Java style conventions.
- **Examples**:
  - Classes and similar entities should use `PascalCase`.
  - All other entities should use `camelCase`.
  - For files associated with Scala: name them in `camelCase.scala`.
  - Other generic files: name them in `kebab-case.md`. Special cases like Python or Rust files should use `snake_case.py`.

### Formatting and Linting

- **scalafmt**:
  - Version: `3.7.14`
  - Configuration:

    ```properties
    version = 3.7.14
    runner.dialect=scala213source3
    ```

- **scalafix**:
  - Adhere to the default configurations of `scalafmt` and `scalafix`.
  - Rules:

    ```properties
    rules = [
      DisableSyntax,
      ExplicitResultTypes,
      LeakingImplicitClassVal,
      NoAutoTupling,
      NoValInForComprehension,
      OrganizeImports,
      ProcedureSyntax,
      RedundantSyntax,
      RemoveUnused,
    ]
    ```

## Git Strategy

### Branching

- **main**:
  - **WARNING**: DO NOT PUSH DIRECTLY TO THIS BRANCH.
  - Only merge through PRs with at least one review.
  - Only merges from `develop`.
- **develop**:
  - You can commit freely but it's recommended to branch off for work as described below.
  - Branch naming conventions:
    - Features: `feat/kebab-case`
    - Fixes: `fix/kebab-case`
    - Chores: `chore/kebab-case`

### Commit Messages

Follow the guidelines provided by [Conventional Commits](https://github.com/conventional-changelog/commitlint/tree/master/%40commitlint/config-conventional). Commit messages should have the following structure:

```plaintext
<type>[optional scope]: <description>

[optional body]

[optional footer]
```

#### Commit Types

- `fix`: Patching a bug in the codebase. Related to PATCH in semantic versioning.
- `feat`: Adding a new feature. Related to MINOR in semantic versioning.
- `BREAKING CHANGE`: Indicates a breaking API change, relating to MAJOR in semantic versioning. This can be a part of any type of commit.
- Additional types like `build:`, `chore:`, `ci:`, `docs:`, `style:`, `refactor:`, `perf:`, and `test:` are also allowed and are recommended by conventions such as @commitlint/config-conventional.

## Documentation

- All documentation should be written in Markdown.
- Ensure documentation passes `markdownlint`.

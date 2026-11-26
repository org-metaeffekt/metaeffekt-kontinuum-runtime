# Test Cases

Each test case `tc-XXXX` is checked by a test in `org.metaeffekt.kontinuum.runtime.testcases`. The configuration is
loaded and the pipeline is generated. Depending on the test case, the result is checked in one of two ways:

| Test case | Check                                                                                                       |
|:----------|:------------------------------------------------------------------------------------------------------------|
| `tc-0001` | Roundtrip: the local pipeline script is executed and the result is compared with the expected output.        |
| `tc-0002` | Generation only: the parameters of the processors in the local and the GitLab pipeline are checked.          |
| `tc-0004` | Roundtrip: the inventories of all stages are named after the asset-level `inventory` or the default.         |

Roundtrip tests require a Kontinuum checkout at `../../metaeffekt-kontinuum` and are skipped otherwise. Tests that
only generate pipelines run without it, and the files referenced by their configuration do not need to exist.

## Layout

```
test-cases/
  shared/workbench/        resources shared by all test cases (e.g. the reference inventory)
  tc-XXXX/workbench/       workbench of the test case: configuration template, scripts, inventories
  tc-XXXX/workspace/       prepared inputs of the test case (e.g. an already extracted inventory)
```

## Configuration Templates

The configuration in `tc-XXXX/workbench/` is a template. All paths in it use placeholders, which the test replaces
with absolute paths before the configuration is used:

| Placeholder               | Replaced with                    |
|:--------------------------|:---------------------------------|
| `${workbench.dir}`        | `test-cases/tc-XXXX/workbench`   |
| `${workspace.dir}`        | `test-cases/tc-XXXX/workspace`   |
| `${shared.workbench.dir}` | `test-cases/shared/workbench`    |

The test writes the resolved configuration to `target/test-cases/tc-XXXX/` and only uses that copy for loading the
configuration, generating the pipeline and executing it. A placeholder left after the replacement fails the test.
The runtime does not replace placeholders. The replacement is implemented once in `TestCaseConfigTemplates` and
shared by all test cases.

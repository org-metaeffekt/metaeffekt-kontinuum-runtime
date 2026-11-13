# Test Cases

Each test case `tc-XXXX` is executed as a roundtrip by a test in `org.metaeffekt.kontinuum.runtime.testcases`: the
configuration is loaded, the local pipeline script is generated and executed, and the result is compared with the
expected output. The tests require a Kontinuum checkout at `../../metaeffekt-kontinuum` and are skipped otherwise.

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
The runtime does not replace placeholders.

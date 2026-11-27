# Test Cases

A test case `tc-NNNN` provides a workbench and a workspace for one or more configurations `tc-NNNN-NN`. Each
configuration is checked by a test class in `org.metaeffekt.kontinuum.runtime.testcases`. The prefix of a test method
names what it checks:

| Prefix            | Check                                                                                           |
|:------------------|:------------------------------------------------------------------------------------------------|
| `localPipeline_`  | The local pipeline is generated, and single steps of it are checked (processor and parameters). |
| `gitlabPipeline_` | The GitLab pipeline is generated, and the parameters of the processors are checked.             |
| `execution_`      | Roundtrip: the local pipeline is executed, and the produced files are checked.                  |

| Configuration | Scenario                                                                                         |
|:--------------|:-------------------------------------------------------------------------------------------------|
| `tc-0001-01`  | An extraction script selects the inventory of an artifact from a prepared inventory.             |
| `tc-0002-01`  | The period of cert reports is passed to the report processor.                                    |
| `tc-0003-01`  | A Windows appliance is extracted, split into parts and an inventory is selected by a script.     |
| `tc-0004-01`  | The inventories of all stages are named after the asset-level `inventory` or the default.        |

Roundtrip tests require a Kontinuum checkout and are skipped otherwise. The checkout is configured by
`ae.kontinuum.dir` in the `.local.properties` of the runtime root. Without the property, a checkout next to the
runtime at `../../metaeffekt-kontinuum` is used. Generation tests run without it, and the files referenced by their
configuration do not need to exist.

Roundtrip tests are tagged with `@Tag(ROUNDTRIP)` (tag `roundtrip`). They run by default and can be excluded or run on
their own:

```
mvn -pl pipeline-generator test -DexcludedGroups=roundtrip
mvn -pl pipeline-generator test -Dgroups=roundtrip
```

## Naming

The test case and the configuration are derived from the name of the test class, so no further mapping is needed:

| Element                | Pattern                                          | Example                                    |
|:-----------------------|:-------------------------------------------------|:-------------------------------------------|
| Test class             | `TestCase<NNNN>Config<NN>Test`                   | `TestCase0001Config02Test`                 |
| Configuration template | `tc-<NNNN>/workbench/tc-<NNNN>-<NN>-config.yaml` | `tc-0001/workbench/tc-0001-02-config.yaml` |
| Result directory       | `target/test-cases/tc-<NNNN>-<NN>/`              | `target/test-cases/tc-0001-02/`            |

To add a configuration, add the template to the workbench of the test case and a test class named after it. A test
class whose name does not match the pattern, or whose template does not exist, fails.

## Layout

```
test-cases/
  shared/workbench/        resources shared by all test cases (e.g. the reference inventory)
  tc-NNNN/workbench/       workbench of the test case: configuration templates, scripts, inventories
  tc-NNNN/workspace/       prepared inputs of the test case (e.g. an already extracted inventory)
```

The workbench and the workspace are shared by all configurations of a test case.

## Configuration Templates

The configurations in `tc-NNNN/workbench/` are templates. All paths in them use placeholders, which the test replaces
with absolute paths before the configuration is used:

| Placeholder               | Replaced with                    |
|:--------------------------|:---------------------------------|
| `${workbench.dir}`        | `test-cases/tc-NNNN/workbench`   |
| `${workspace.dir}`        | `test-cases/tc-NNNN/workspace`   |
| `${shared.workbench.dir}` | `test-cases/shared/workbench`    |

The test writes the resolved configuration to its result directory and only uses that copy for loading the
configuration, generating the pipeline and executing it. A placeholder left after the replacement fails the test.
The runtime does not replace placeholders. The replacement is implemented once in `AbstractTestCase` and shared by
all test classes.

## Results

The result directory of a test class contains the resolved configuration, the generated pipelines in `working/`, the
log of the executed pipeline in `pipeline.log` and the workspace of the pipeline in `workspace/<project id>/`.

The result directory is cleaned once before the tests of a class. The local pipeline is executed at most once per
class, so all roundtrip tests of a class check the same result, and the result is kept after the run.

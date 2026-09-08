# metaeffekt-kontinuum-runtime

`metaeffekt-kontinuum-runtime` houses both the dynamic CI/CD pipeline generator tooling and the runtime container environment used by `metaeffekt-kontinuum`.

---

## 1. Dynamic Pipeline Generator (Maven Modules)

The repository contains Maven modules responsible for generating CI/CD pipelines dynamically from project configurations:

- **`pipeline-generator`**: Core engine and models for parsing configurations, building execution graphs, and generating local shell scripts or GitLab CI YAML pipelines.
- **`execution`**: `kontinuum-maven-plugin` providing Maven goal integration (`generate-local-pipeline`, `generate-gitlab-pipeline`) for executing pipeline generation.
- **`container`**: Packaging module for building the runtime Docker container image.

### Building the Java Tooling

To compile, test, and install the Java modules into your local Maven repository:

```bash
mvn clean install
```

---

## 2. Pipeline Orchestration & DAG Execution Architecture

The pipeline generation architecture separates static processor catalog models from dynamic execution graph orchestration, enabling Directed Acyclic Graph (DAG) generation and parallel job execution.

### Execution Graph in `AssetExecutionContext`

Rather than embedding mutable dependency references within static processor definitions, DAG dependencies are tracked within [`AssetExecutionContext`](pipeline-generator/src/main/java/org/metaeffekt/kontinuum/runtime/models/shared/AssetExecutionContext.java).

- **`addSequential(Processor... processors)`**: Convenience helper for linear stages. Registers each processor and automatically creates predecessor dependencies between them.
- **`addDependency(Processor target, Processor... dependsOn)`**: Registers explicit directed dependencies between tasks for branching (fan-out) or merging (fan-in).
- **`getDependencies(Processor processor)`**: Queries the upstream prerequisites for any processor.

```java
// Example: Sequential chain in ExtractStageHandler
context.addSequential(
    createInventoryExtraction(context),
    createMetadataAttachment(context),
    createInventoryReferenceEnrichment(context)
);

// Example: Parallel fan-out in PrepareStageHandler
Processor copy = context.addProcessor(createInventoryCopy(context));
Processor spdx = context.addProcessor(createSpdxConversion(context));
Processor cdx  = context.addProcessor(createCycloneDxConversion(context));

context.addDependency(spdx, copy);
context.addDependency(cdx, copy);
```

### Context Map Pipeline Generation

[`Pipeline.generatePipeline()`](pipeline-generator/src/main/java/org/metaeffekt/kontinuum/runtime/generator/shared/Pipeline.java) returns `Map<Asset, AssetExecutionContext>`. This preserves both the ordered list of processors and the entire topological dependency graph for downstream generators.

### GitLab CI DAG (`needs: [ ... ]`) Parallelism

[`GitlabPipeline`](pipeline-generator/src/main/java/org/metaeffekt/kontinuum/runtime/generator/gitlab/GitlabPipeline.java) maps context dependencies into GitLab CI `needs: [ ... ]` entries:
- Jobs with explicit dependencies emit `needs: [ <dep_job_1>, <dep_job_2> ]`, allowing GitLab CI to execute independent tasks concurrently without waiting for whole stages to complete.
- Falls back to stage-sequential ordering when no explicit dependencies are configured.

### Collision-Free Job Name Generation

To prevent duplicate job name collisions in generated GitLab CI YAML files:
- **Discriminator Keys (`DISCRIMINATOR_KEYS`)**: Automatically appends parameter qualifiers (such as document type, target language, mode, format) to base job names without processor-specific `if` checks.
- **Occurrence Registry (`assignJobNames()`)**: Pre-computes job names across the pipeline and automatically appends index suffixes (`-2`, `-3`, etc.) to duplicate base names, ensuring 100% uniqueness while keeping `needs:` references precisely aligned.

---

## 3. Configuration & Mojo Parameters

### Optional Local Maven Repository (`localMavenRepo` / `LOCAL_MAVEN_REPO`)

Allows specifying a custom local Maven repository directory for pipeline steps (injected as `-Dmaven.repo.local=<path>` into generated Maven executions).

- **Mojo Configuration**: Configured via the `localMavenRepo` (or `local.maven.repo`) property in `AbstractGeneratePipelineMojo`, `GenerateGitlabPipelineMojo`, and `GenerateLocalPipelineMojo`.
- **GitLab Container Default**: Works seamlessly with pre-cached dependencies in the runtime container located at `/root/.m2/repository`.
- **Optional**: When omitted or blank, the `-Dmaven.repo.local` flag is omitted, allowing standard repository resolution.

### Supported Locales

[`SupportedLocale`](pipeline-generator/src/main/java/org/metaeffekt/kontinuum/runtime/models/shared/SupportedLocale.java) supports standard locale identifiers (`en_US`, `de_DE`, `en`, `de`) directly in YAML pipeline configuration files through Jackson `@JsonCreator` and `@JsonValue` annotations.
Configurations strictly expect `locales:` under the report configuration.

### TMD_SOURCE Property

The `TMD_SOURCE` property on `EnvironmentConfiguration` defaults to `ae-kosmos` with Lombok `@Builder.Default` support, properly propagated by pipeline generation Mojos.

---

## 4. Processor Catalog & Models (`ProcessorDefinitions`)

- **Domain Separation**: `Processor`, `MavenProcessor`, and `StandaloneProcessor` are pure task catalog descriptors defining CLI/Maven parameters, script locations, and lifecycle phases.
- **Polymorphic Deep Copy**: `Processor.copy()` provides concrete implementations on subclasses (`MavenProcessor`, `StandaloneProcessor`, `ProcessorParameter`) to safely clone processor instances from the catalog without mutating defaults.
- **Standalone Parameter Sanitization**: Standalone shell script generation filters out `null` and blank parameter values to avoid injecting literal `null` strings into command lines.

---

## 5. Kontinuum Runtime Container

The configuration of the container built via the GitHub workflow in this repository is described in [container/Dockerfile](container/Dockerfile).

### Container Contents

- **Base Image:** `maven:3.9.11-amazoncorretto-17-debian`
- **Working Directory:** `/usr/src/metaeffekt-kontinuum`
- **Cached Maven Artifacts:**
  - `/root/.m2/repository/com/metaeffekt`
  - `/root/.m2/repository/org/metaeffekt` (including `org.metaeffekt.kontinuum.runtime` artifacts)
  - Pre-cached third-party dependencies required for offline pipeline execution.

### Building the Container

You can build the container using Maven or directly via the container build script. Both methods ensure that the Java generator modules are built first.

#### Option A: Building via Maven Profile
```bash
mvn clean install -Pbuild-container
```

#### Option B: Building via Shell Script
```bash
./container/build.sh
```

Run `./container/build.sh --help` to view interactive and non-interactive build flags (e.g. core versions, image tags, Docker Hub credentials).

---

## 6. Pipeline Configuration Specification

For the complete schema, detailed explanations of all configuration parameters, and YAML usage examples, refer to the [Pipeline Configuration Guide](pipeline-configuration.md).


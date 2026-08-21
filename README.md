# metaeffekt-kontinuum-runtime

`metaeffekt-kontinuum-runtime` houses both the dynamic CI/CD pipeline generator tooling and the runtime container environment used by `metaeffekt-kontinuum`.

---

## 1. Dynamic Pipeline Generator (Maven Modules)

The repository contains Maven modules responsible for generating CI/CD pipelines dynamically from project configurations:

- **`pipeline-generator`**: Core engine and models for parsing configurations and generating CI/CD pipelines.
- **`execution`**: `kontinuum-maven-plugin` providing Maven goal integration for executing pipeline generation.
- **`container`**: Packaging module for building the runtime Docker container image.

### Building the Java Tooling

To compile, test, and install the Java modules into your local Maven repository:

```bash
mvn clean install
```

---

## 2. Kontinuum Runtime Container

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

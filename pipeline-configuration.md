# Pipeline Configuration Guide (`pipeline-configuration.yaml`)

This document provides a comprehensive reference for the pipeline configuration file (`pipeline-configuration.yaml` or
`sample-product-pipeline.yaml`) consumed by `metaeffekt-kontinuum-runtime`.

The pipeline generator compiles this YAML configuration into either a dynamic **GitLab CI Downstream Pipeline** (
`.gitlab-ci.yml`) or a local **bash pipeline script** (`sample-product-pipeline.sh`).

---

## Table of Contents

1. [High-Level Structure](#1-high-level-structure)
2. [Project Properties (`projectProperties`)](#2-project-properties-projectproperties)
    - [Project Metadata (`project`)](#21-project-metadata-project)
    - [Asset Definitions (`assets`)](#22-asset-definitions-assets)
    - [Asset Resolvers (`urlResolver`, `mavenResolver`, `containerResolver`)](#23-asset-resolvers)
3. [Reports Configuration (`reports`)](#3-reports-configuration-reports)
    - [Supported Report Types](#31-supported-report-types)
    - [Single-Asset vs. Multi-Asset Consolidation](#32-single-asset-vs-multi-asset-consolidation)
    - [Stage Chains Driven by Report Types](#33-stage-chains-driven-by-report-types)
4. [Dashboards Configuration (`dashboards`)](#4-dashboards-configuration-dashboards)
5. [Portfolio Manager Integration (`portfolioManager`)](#5-portfolio-manager-integration-portfoliomanager)
6. [Pipeline Options (`options`)](#6-pipeline-options-options)
    - [Global Options (`options.global`)](#61-global-options-optionsglobal)
    - [Enrichment Options (`options.enrichment`)](#62-enrichment-options-optionsenrichment)
7. [Complete Reference Example](#7-complete-reference-example)

---

## 1. High-Level Structure

The pipeline configuration is structured into five main top-level sections:

```yaml
projectProperties:
  project: { ... }
  assets: [ ... ]

reports: [ ... ]

dashboards: [ ... ]

portfolioManager: { ... }

options:
  global: { ... }
  enrichment: { ... }
```

| Section             | Required | Description                                                                                          |
|:--------------------|:---------|:-----------------------------------------------------------------------------------------------------|
| `projectProperties` | **Yes**  | Defines projecet metadata and the list of software assets to analyze.                                |
| `reports`           | Optional | Specifies the documents (PDFs, annexes, archives) to produce and which assets to include.            |
| `dashboards`        | Optional | Specifies assets for which vulnerability dashboards are generated.                                   |
| `portfolioManager`  | Optional | Configures access to an external portfolio manager service, to offload / execute long-running tasks. |
| `options`           | Optional | Feature toggles controlling license scanning, SBOM export, vulnerability data sources and so on.      |

---

## 2. Project Properties (`projectProperties`)

### 2.1 Project Metadata (`project`)

Defines organizational and project identity information used across generated reports, metadata attachments, and
dashboards.

```yaml
projectProperties:
  project:
    id: "metaeffekt-workbench"
    name: "Metaeffekt Workbench"
    version: "1.0.0"
```

| Parameter | Type   | Required | Description                                                 | What It Produces / Affects                                         |
|:----------|:-------|:---------|:------------------------------------------------------------|:-------------------------------------------------------------------|
| `id`      | String | **Yes**  | Machine-readable unique identifier for the project/product. | Used to construct the workspace and as an artifact identifier.     |
| `name`    | String | **Yes**  | Human-readable project/product title.                       | Displayed on report cover pages, document headers, and dashboards. |
| `version` | String | **Yes**  | Product version string.                                     | Displayed on report covers and attached to inventory metadata.     |

---

### 2.2 Asset Definitions (`assets`)

The `assets` list specifies each software component, archive, container image, or repository to be extracted and
analyzed.

```yaml
projectProperties:
  assets:
    - id: "sample-asset"
      name: "sample-asset"
      version: "1.0.0"
      reference: "inventories/example-reference-inventory/inventory"
      assessmentId: "sample-product-1"
      context: "local"
      urlResolver:
        url: "https://example.com/downloads/sample-asset-1.0.0.zip"
```

| Parameter      | Type   | Required | Description                                                                            | What It Produces / Affects                                                                                         |
|:---------------|:-------|:---------|:---------------------------------------------------------------------------------------|:-------------------------------------------------------------------------------------------------------------------|
| `id`           | String | **Yes**  | Unique asset key within the pipeline.                                                  | Forms workspace stage directories (`<stage>/<asset-id>/...`) and links report/dashboard definitions to this asset. |
| `name`         | String | **Yes**  | Component or asset name.                                                               | Asset name utilized throughout the pipeline run.                                                                   |
| `version`      | String | **Yes**  | Asset version.                                                                         | Asset version utilized throughout the pipeline run.                                                                |
| `reference`    | String | **Yes**  | Path to the reference inventory directory or file relative to the workbench directory. | Used as an information repository from which information missing in the extracted asset is pulled.                 |
| `assessmentId` | String | Optional | Vulnerability assessment identifier.                                                   | Locates manual assessments in the workbench directory: `assessments/<project-name>/<assessmentId>/<context>/context`. |
| `context`      | String | Optional | Assessment context (e.g., `local`, `remote`).                                          | Sub-directory qualifier to dynamically resolve context information.                                                |
| `assets`       | List   | Optional | Nested list of child assets.                                                           | Supports hierarchical asset decomposition (e.g., an asset containing multiple other assets).                       |

---

### 2.3 Asset Resolvers

Each asset should define **at most one** resolver indicating how the asset is downloaded or retrieved in the `FETCH`
stage.

#### `urlResolver`

Downloads an asset from an HTTP(S) URL or a local/remote file URL.

```yaml
urlResolver:
  url: "https://github.com/example/example/archive/refs/tags/v0.17.0.zip"
  # Optional authentication options:
  username: "user"
  password: "secret-password"
  token: "secret-token"
  headerName: "Authorization"
  headerValue: "Bearer secret-token"
```

Parameterized URLs can also be expressed with `urlPattern` using `${version}` or `${name}` placeholders:

```yaml
urlResolver:
  urlPattern: "https://github.com/example/${name/archive/refs/tags/v${version}.zip"
```

#### `mavenResolver`

Downloads an artifact from a standard Maven repository using group ID, artifact ID, and version conventions.

```yaml
mavenResolver:
  groupId: "org.metaeffekt.core"
  artifactId: "ae-inventory-processor"
  artifactVersion: "0.159.0"
  repoUrl: "https://repo1.maven.org/maven2"  # Optional; defaults to Maven Central
```

#### `containerResolver`

Pulls and saves a container image tarball from Docker Hub or a custom container registry.

```yaml
containerResolver:
  image: "alpine"
  tag: "3.20"
  repoUrl: "docker.io"  # Optional; defaults to docker.io
```

---

## 3. Reports Configuration (`reports`)

The `reports` section defines which types of documents are generated for which assets.

```yaml
reports:
  - id: "report-group-1" 
    assetIds: [ "sample-asset" ]     # Assets included in this report
    types: [ "VR", "SDA" ]           # Report types to generate
    locales: [ "en_US", "de_DE" ]    # Locales to render
    organization: "metaeffekt GmbH"
    classificationRating: "DEFAULT"
    controlRating: "DEFAULT"
    productName: "product-name"
    productVersion: "1.0.0"
    productWatermark: "CONFIDENTIAL"
    overviewAdvisors: [ "CERT_FR", "NVD" ]
    preReportFilterFile: "scripts/prepare.kts"
```

| Parameter              | Type   | Required | Description                                                                                             | What It Produces / Affects                                                        |
|:-----------------------|:-------|:---------|:--------------------------------------------------------------------------------------------------------|:----------------------------------------------------------------------------------|
| `id`                   | String | Optional | Group identifier for multi-asset reports.                                                               | Required for internal workspace management.                                       |
| `assetIds`             | List   | **Yes**  | List of asset IDs whose inventories feed into this report.                                              | Influences which assets make up a given report.                                   |
| `types`                | List   | **Yes**  | List of report type keys to generate (e.g. `VR`, `SDA`, `LD`, `ILD`, `CR`, `VSR`, `CA`).                | Dictates which document generation processors and dependency chains run.          |
| `locales`              | List   | **Yes**  | Target locales for document generation (e.g., `["en_US"]`, `["en_US", "de_DE"]`).                       | Renders documents in the specified languages.                                     |
| `organization`         | String | Optional | Organization name.                                                                                      | Rendered in document headers, footers, and title blocks.                          |
| `classificationRating` | String | Optional | Document security classification (e.g. `RESTRICTED`, `DEFAULT`).                                        | Sets the security classification in the report.                                   |
| `controlRating`        | String | Optional | Control rating (e.g. `CONFIDENTIAL`, `OPEN`).                                                           | Sets the control rating in the report.                                            |
| `productName`          | String | Optional | Encomapssing product name listed in the report.                                                         | Encomapssing product name listed in the report.                                   |
| `productVersion`       | String | Optional | Encomapssing product version listed in the report.                                                      | Encomapssing product version listed in the report.                                |
| `productWatermark`     | String | Optional | Watermark text printed across all report pages.                                                         | Displays a customer specific watermark in the report.                             |
| `overviewAdvisors`     | List   | Optional | List of vulnerability advisory sources to enable for vulnerability reports (e.g. `["CERT_FR", "NVD"]`). | Enables the set overview advisors for vulnerability centric reports.              |
| `preReportFilterFile`  | String | Optional | A path to a kotlin script file filtering the inventory used to generate the report.                     | Enables customer specific filtering and adjustment of the report-input inventory. |

---the set overview advisors for vulnerability centric reports.

### 3.1 Supported Report Types

| Key   | Name                          |
|:------|:------------------------------|
| `VR`  | Vulnerability Report          |
| `VSR` | Vulnerability Summary Report  |
| `CR`  | Cert Report                   |
| `SDA` | Software Distribution Annex   |
| `LD`  | License Documentation         |
| `ILD` | Initial License Documentation |
| `CA`  | Custom Annex                  |

---

## 4. Dashboards Configuration (`dashboards`)

The `dashboards` section instructs the pipeline to generate vulnerability dashboards.

```yaml
dashboards:
  - assetIds: [ "sample-asset", "sample-asset-2" ]
    tenant: "metaeffekt"
```

| Parameter  | Type   | Required | Description                                                 | What It Produces / Affects                                                                  |
|:-----------|:-------|:---------|:------------------------------------------------------------|:--------------------------------------------------------------------------------------------|
| `assetIds` | List   | **Yes**  | List of asset IDs for which dashboards should be generated. | Automatically triggers vulnerability enrichment and generates a dashboard per listed asset. |
| `tenant`   | String | **Yes**  | Multi-tenant organization identifier for the dashboard.     | Passed to dashboard generation (`param.tenant.id`).                                          |

---

## 5. Portfolio Manager Integration (`portfolioManager`)

Configures an external portfolio manager service capable of resolving, scanning and enriching asset inventories.

```yaml
portfolioManager:
  project: "metaeffekt-examples"
```

| Parameter | Type   | Required | Description                                       | What It Produces / Affects                              |
|:----------|:-------|:---------|:--------------------------------------------------|:--------------------------------------------------------|
| `project` | String | **Yes**  | The project name in the Portfolio Manager server. | Identifies the project scope for uploads and downloads. |

### Lifecycle Impact When Enabled:

1. **`PREPARE` Stage**:
   Uploads the extracted asset inventory to Portfolio Manager using configured credentials (`portfolio.manager.url`,
   `portfolio.manager.token`).
2. **`PREPARE` Stage**:
   Downloads the latest collective reference inventory from Portfolio Manager.
3. **`AGGREGATE` Stage**:
   Enriches the asset inventory with the downloaded Portfolio Manager reference inventory.

> [!NOTE]
> In short, the portfolio manager is currently meant to replace local resolve and scan workflows, 
> as these are long-running processes requiring external data to produce sufficient results.
> For this process to work, the relevant configuration files have to be availabel in the pipeline and the portfolio manager service
> has to be configured correctly.

---

## 6. Pipeline Options (`options`)

### 6.1 Global Options (`options.global`)

Controls activation of optional pipeline stages and standalone SBOM generation.

```yaml
options:
  global:
    enableResolve: true
    enableScan: true
    enableCycloneDxBom: true
    enableSpdxBom: true
```

| Parameter            | Type    | Default | Description                                                              | What It Produces / Affects                                                                          |
|:---------------------|:--------|:--------|:-------------------------------------------------------------------------|:----------------------------------------------------------------------------------------------------|
| `enableResolve`      | Boolean | `false` | Enables the resolving of inventories. Usually done by portfolio manager. | Resolves component source URLs, repository links, and missing metadata using external repositories. |
| `enableScan`         | Boolean | `false` | Enables the scanning of inventories. Usually done by portfolio manager.  | Scans component sources and binaries for licenses, copyrights, and notices.                         |
| `enableCycloneDxBom` | Boolean | `false` | Enables CycloneDX SBOM generation.                                       | Creates a SPDX Bom.                                                                                 |
| `enableSpdxBom`      | Boolean | `false` | Enables SPDX SBOM generation.                                            | Creates a CycloneDX Bom.                                                                            |

---

### 6.2 Enrichment Options (`options.enrichment`)

Configures security policy rules and activates or disables specific vulnerability advisory databases.

```yaml
options:
  enrichment:
    securityPolicyFile: "policies/security-policy/security-policy.json"
    securityPolicyActiveIds: [ "base_configuration" ]
    activateNvd: true
    activateCertFr: true
    activateCertEu: true
    activateCertSei: true
    activateMsrc: true
    activateOsv: true
    activateKev: true
    activateEpss: true
    activateEol: true
    activateCsaf: false
```

| Parameter                 | Type    | Default | Description                                                                                                                                 |
|:--------------------------|:--------|:--------|:--------------------------------------------------------------------------------------------------------------------------------------------|
| `securityPolicyFile`      | String  | None    | Path to security policy JSON file relative to the workbench directory. Sets security thresholds, CVSS score mappings, and assessment rules. |
| `securityPolicyActiveIds` | List    | `[]`    | List of active rule configuration profile IDs defined inside the security policy.                                                           |
| `activateNvd`             | Boolean | `true`  | Enables vulnerability enrichment from the National Vulnerability Database (CVE/NVD).                                                        |
| `activateCertFr`          | Boolean | `true`  | Enables security advisories from CERT-FR.                                                                                                   |
| `activateCertEu`          | Boolean | `true`  | Enables security advisories from CERT-EU.                                                                                                   |
| `activateCertSei`         | Boolean | `true`  | Enables security advisories from SEI CERT.                                                                                                  |
| `activateMsrc`            | Boolean | `true`  | Enables Microsoft Security Response Center (MSRC) advisory data.                                                                            |
| `activateOsv`             | Boolean | `true`  | Enables Open Source Vulnerabilities (OSV) database enrichment.                                                                              |
| `activateKev`             | Boolean | `true`  | Enables CISA Known Exploited Vulnerabilities (KEV) catalog matching.                                                                        |
| `activateEpss`            | Boolean | `true`  | Enriches CVEs with Exploit Prediction Scoring System (EPSS) probabilities.                                                                  |
| `activateEol`             | Boolean | `true`  | Enables End-of-Life (EOL) component lifecycle detection.                                                                                    |
| `activateCsaf`            | Boolean | `true`  | Enables Common Security Advisory Framework (CSAF) vendor advisory enrichment.                                                               |

---

## 7. Complete Reference Example

Below is a complete, production-ready `pipeline-configuration.yaml` showing all sections:

```yaml
projectProperties:
  project:
    id: "sample-product"
    name: "Sample Product Suite"
    version: "2.5.0"

  assets:
    # Asset 1: Downloaded from remote URL archive
    - id: "backend-service"
      name: "backend-service"
      version: "2.5.0"
      reference: "inventories/backend-service/inventory"
      assessmentId: "sample-product-assessments"
      context: "production"
      urlResolver:
        url: "https://github.com/my-org/backend-service/archive/refs/tags/v2.5.0.zip"

    # Asset 2: Downloaded from Maven Central
    - id: "common-utils"
      name: "common-utils"
      version: "1.2.0"
      reference: "inventories/common-utils/inventory"
      assessmentId: "sample-product-assessments"
      context: "production"
      mavenResolver:
        groupId: "org.myorg.utils"
        artifactId: "common-utils"
        artifactVersion: "1.2.0"

    # Asset 3: Pulled from Docker container registry
    - id: "gateway-proxy"
      name: "gateway-proxy"
      version: "1.25-alpine"
      reference: "inventories/gateway-proxy/inventory"
      assessmentId: "sample-product-assessments"
      context: "production"
      containerResolver:
        image: "nginx"
        tag: "1.25-alpine"

reports:
  # Multi-asset Vulnerability Report
  - id: "full-vulnerability-report"
    assetIds: [ "backend-service", "common-utils", "gateway-proxy" ]
    types: [ "VR", "VSR" ]
    locales: [ "en_US", "de_DE" ]
    organization: "My Organization Inc."
    classificationRating: "INTERNAL"
    controlRating: "DEFAULT"
    productWatermark: "CONFIDENTIAL"
    overviewAdvisors: [ "CERT_FR", "NVD" ]

  # Software Distribution Annex with full source aggregation
  - assetIds: [ "backend-service" ]
    types: [ "SDA" ]
    locales: [ "en_US" ]
    organization: "My Organization Inc."
    classificationRating: "PUBLIC"
    controlRating: "DEFAULT"

dashboards:
  - assetIds: [ "backend-service", "gateway-proxy" ]
    tenant: "metaeffekt"

portfolioManager:
  project: "sample-product-suite"

options:
  global:
    enableResolve: true
    enableScan: true
    enableCycloneDxBom: true
    enableSpdxBom: true

  enrichment:
    securityPolicyFile: "policies/security-policy/security-policy.json"
    securityPolicyActiveIds: [ "base_configuration" ]
    activateNvd: true
    activateCertFr: true
    activateCertEu: true
    activateOsv: true
    activateKev: true
    activateEpss: true
    activateEol: true
    activateCsaf: false
```

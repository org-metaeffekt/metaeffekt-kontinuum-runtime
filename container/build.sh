#!/bin/bash

set -eo pipefail

readonly SELF_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly TARGET_DIR="$SELF_DIR/target"
readonly TEMP_MAVEN_REPO="$TARGET_DIR/.m2/repository"
readonly LOCAL_MAVEN_REPO="$SELF_DIR/local-maven-repo"

# Helper function to strip surrounding quotes if present
strip_quotes() {
    local val="$1"
    val="${val#\"}"
    val="${val%\"}"
    val="${val#\'}"
    val="${val%\'}"
    echo "$val"
}

readonly AE_CORE_VERSION="$(strip_quotes "$1")"
readonly AE_ARTIFACT_ANALYSIS_VERSION="$(strip_quotes "$2")"
readonly AE_PORTFOLIO_MANAGER_VERSION="$(strip_quotes "$3")"
readonly AE_KONTINUUM_VERSION="$(strip_quotes "$4")"
readonly DOCKER_TAG="$(strip_quotes "$5")"
readonly DOCKER_USERNAME="$(strip_quotes "$6")"
readonly DOCKER_ACCESS_TOKEN="$(strip_quotes "$7")"
readonly DOCKER_REGISTRY="$(strip_quotes "$8")"

# Function to check all required input argument variables
check_args() {
    local missing=0

    if [[ -z "$AE_CORE_VERSION" ]]; then
        echo "Error: AE_CORE_VERSION (argument 1) is missing or empty." >&2
        missing=1
    fi
    if [[ -z "$AE_ARTIFACT_ANALYSIS_VERSION" ]]; then
        echo "Error: AE_ARTIFACT_ANALYSIS_VERSION (argument 2) is missing or empty." >&2
        missing=1
    fi
    if [[ -z "$AE_PORTFOLIO_MANAGER_VERSION" ]]; then
        echo "Error: AE_PORTFOLIO_MANAGER_VERSION (argument 3) is missing or empty." >&2
        missing=1
    fi
    if [[ -z "$AE_KONTINUUM_VERSION" ]]; then
        echo "Error: AE_KONTINUUM_VERSION (argument 8) is missing or empty." >&2
        missing=1
    fi
    if [[ -z "$DOCKER_TAG" ]]; then
        echo "Error: DOCKER_TAG (argument 4) is missing or empty." >&2
        missing=1
    fi
    if [[ -z "$DOCKER_USERNAME" ]]; then
        echo "Error: DOCKER_USERNAME (argument 5) is missing or empty." >&2
        missing=1
    fi
    if [[ -z "$DOCKER_ACCESS_TOKEN" ]]; then
        echo "Error: DOCKER_ACCESS_TOKEN (argument 6) is missing or empty." >&2
        missing=1
    fi
    if [[ -z "$DOCKER_REGISTRY" ]]; then
        echo "Error: DOCKER_REGISTRY (argument 7) is missing or empty." >&2
        missing=1
    fi

    if [[ $missing -ne 0 ]]; then
        echo "" >&2
        echo "Usage: $0 <AE_CORE_VERSION> <AE_ARTIFACT_ANALYSIS_VERSION> <AE_PORTFOLIO_MANAGER_VERSION> <AE_KONTINUUM_VERSION> <DOCKER_TAG> <DOCKER_USERNAME> <DOCKER_ACCESS_TOKEN> <DOCKER_REGISTRY>" >&2
        exit 1
    fi
}


# Function to check if git repository exists
check_git_repo() {
    local url="$1"
    if git ls-remote --exit-code "$url" &>/dev/null; then
        return 0
    else
        return 1
    fi
}

# Function to clone repository
clone_repo() {
    local url="$1"
    local dest="$2"
    local branch="$3"

    echo "Cloning $url to $dest (branch: $branch)"

    if ! check_git_repo "$url"; then
        echo "Git repository not found or inaccessible: $url"
        return 1
    fi

    # If set to HEAD-SNAPSHOT always use main or master
    if [[ "$branch" == "HEAD-SNAPSHOT" ]]; then
          git clone --depth 1 "$url" "$dest" || {
              echo "Failed to clone repository: $url"
              return 1
          }
    else
          git clone --depth 1 --branch "$branch" "$url" "$dest" || {
              echo "Failed to clone repository: $url"
              return 1
          }
    fi
}

# Function to build Maven project
build_maven() {
    local project_dir="$1"

    echo "Building Maven project in $project_dir"

    cd "$project_dir" || {
        echo "Failed to change to directory: $project_dir"
        return 1
    }

    mvn install -DskipTests -Dmaven.repo.local="$TEMP_MAVEN_REPO" || {
        echo "Maven build failed in $project_dir"
        return 1
    }

    cd "$SELF_DIR" || return 1
}

build_docker() {
    echo "Building Docker image with tag: $DOCKER_TAG"

    # Create BuildKit config file for insecure/HTTP registry support
    cat << EOF > "$SELF_DIR/buildkitd.toml"
[registry."$DOCKER_REGISTRY"]
  http = true
  insecure = true
EOF

    # Re-create builder node if config changed or builder missing
    if docker buildx inspect multiarch-builder &>/dev/null; then
        echo "Updating multi-arch builder with insecure HTTP registry configuration"
        docker buildx rm multiarch-builder &>/dev/null || true
    fi

    echo "Creating multi-arch builder for registry $DOCKER_REGISTRY"
    docker buildx create --use --name multiarch-builder --config "$SELF_DIR/buildkitd.toml"

    if [[ -n "$DOCKER_USERNAME" && -n "$DOCKER_ACCESS_TOKEN" ]]; then
        echo "Logging in to Docker Hub as $DOCKER_USERNAME"
        echo "$DOCKER_ACCESS_TOKEN" | docker login -u "$DOCKER_USERNAME" --password-stdin || {
            echo "Docker login failed"
            return 1
        }
    fi

    docker buildx build \
        --platform linux/amd64,linux/arm64 \
        --push \
        -f "$SELF_DIR/Dockerfile" \
        --tag "$DOCKER_REGISTRY/metaeffekt/metaeffekt-kontinuum-runtime:$DOCKER_TAG" \
        "$SELF_DIR" || {
        echo "Docker build and push failed"
        return 1
    }

    echo "Docker image pushed successfully: $DOCKER_REGISTRY/metaeffekt/metaeffekt-kontinuum-runtime:$DOCKER_TAG"
}

build_core() {
      local core_dir="$TARGET_DIR/metaeffekt-core"
      if ! clone_repo "git@github.com:org-metaeffekt/metaeffekt-core.git" "$core_dir" "$AE_CORE_VERSION"; then
          echo "Failed to clone metaeffekt-core"
          exit 1
      fi

      if ! build_maven "$core_dir"; then
          echo "Failed to build metaeffekt-core"
          exit 1
      fi
}

build_artifact_analysis() {
      local artifact_dir="$TARGET_DIR/metaeffekt-artifact-analysis"
      if ! clone_repo "git@github.com:org-metaeffekt/metaeffekt-artifact-analysis.git" "$artifact_dir" "$AE_ARTIFACT_ANALYSIS_VERSION"; then
          echo "Failed to clone metaeffekt-artifact-analysis"
          exit 1
      fi

      if ! build_maven "$artifact_dir"; then
          echo "Failed to build metaeffekt-artifact-analysis"
          exit 1
      fi
}

build_portfolio_manager() {
      local portfolio_dir="$TARGET_DIR/metaeffekt-portfolio-manager"
      if ! clone_repo "http://ae-server:7990/scm/ae/metaeffekt-portfolio-manager.git" "$portfolio_dir" "$AE_PORTFOLIO_MANAGER_VERSION"; then
          echo "Failed to clone metaeffekt-portfolio-manager"
          exit 1
      fi

      if ! build_maven "$portfolio_dir"; then
          echo "Failed to build metaeffekt-portfolio-manager"
          exit 1
      fi
}

build_kontinuum() {
        local kontinuum_dir="$TARGET_DIR/metaeffekt-kontinuum"
        if ! clone_repo "git@github.com:org-metaeffekt/metaeffekt-kontinuum.git" "$kontinuum_dir" "$AE_KONTINUUM_VERSION"; then
            echo "Failed to clone metaeffekt-kontinuum"
            exit 1
        fi

      local items_to_remove=(
          ".git"
          ".github"
          ".jenkins"
          "docs"
          "tests"
          ".gitignore"
      )

      for item in "${items_to_remove[@]}"; do
          rm -rf "$kontinuum_dir/$item"
      done
}

main() {
  check_args

  build_core
  build_artifact_analysis
  build_portfolio_manager
  build_kontinuum

  build_docker
}

main "$@"
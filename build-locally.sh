#!/bin/bash

set -eo pipefail

readonly SELF_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly TMP_DIR="$SELF_DIR/tmp"
readonly LOCAL_MAVEN_REPO="$SELF_DIR/local-maven-repo"
readonly TEMP_MAVEN_REPO="$TMP_DIR/local-maven-repo"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Default version values (can be overridden by environment variables)
DEFAULT_AE_CORE_VERSION="main"
DEFAULT_AE_ARTIFACT_ANALYSIS_VERSION="main"
DEFAULT_AE_KONTINUUM_VERSION="main"
DEFAULT_CONTAINER_VERSION="latest"

# Function to print colored output
print_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to display help/usage
show_help() {
    cat << EOF
Usage: $0 [OPTIONS]

Build the metaeffekt-kontinuum-runtime container locally.

Options:
    -h, --help              Show this help message
    -c, --core-version      Version of metaeffekt-core to use (default: $DEFAULT_AE_CORE_VERSION)
    -a, --artifact-version  Version of metaeffekt-artifact-analysis to use (default: $DEFAULT_AE_ARTIFACT_ANALYSIS_VERSION)
    -k, --kontinuum-version Version of metaeffekt-kontinuum to use (default: $DEFAULT_AE_KONTINUUM_VERSION)
    -d, --docker-tag        Docker image tag (default: $DEFAULT_CONTAINER_VERSION)
    -y, --yes               Skip confirmation prompts

Environment variables:
    AE_CORE_VERSION         Version of metaeffekt-core
    AE_ARTIFACT_ANALYSIS_VERSION Version of metaeffekt-artifact-analysis
    AE_KONTINUUM_VERSION    Version of metaeffekt-kontinuum
    CONTAINER_VERSION       Docker image tag

Examples:
    $0                          # Interactive mode
    $0 --core-version v1.0.0    # Use specific core version
    $0 -c v1.0.0 -k v2.0.0      # Use specific versions
    AE_CORE_VERSION=v1.0.0 $0   # Use environment variables
EOF
}

# Function to prompt for version with default
prompt_version() {
    local prompt="$1"
    local default="$2"
    local value=""
    
    read -p "$prompt [default: $default]: " value
    echo "${value:-$default}"
}

# Function to ensure directory exists
ensure_dir() {
    local dir="$1"
    if [[ ! -d "$dir" ]]; then
        print_info "Creating directory: $dir"
        mkdir -p "$dir"
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
    
    print_info "Cloning $url to $dest (branch: $branch)"
    
    if ! check_git_repo "$url"; then
        print_error "Git repository not found or inaccessible: $url"
        return 1
    fi
    
    git clone --depth 1 --branch "$branch" "$url" "$dest" || {
        print_error "Failed to clone repository: $url"
        return 1
    }
}

# Function to build Maven project
build_maven() {
    local project_dir="$1"
    local repo_dir="$2"
    
    print_info "Building Maven project in $project_dir"
    
    cd "$project_dir" || {
        print_error "Failed to change to directory: $project_dir"
        return 1
    }
    
    mvn clean install -DskipTests -Dmaven.repo.local="$repo_dir" || {
        print_error "Maven build failed in $project_dir"
        return 1
    }
    
    cd "$SELF_DIR" || return 1
}

# Function to copy artifacts to local Maven repo
copy_artifacts() {
    local src_repo="$1"
    local dest_repo="$2"
    
    print_info "Copying Maven artifacts to local repository"
    
    ensure_dir "$dest_repo"
    
    # Copy required directories
    for dir in "com/metaeffekt" "org/metaeffekt" "org/jetbrains/kotlin"; do
        local src_dir="$src_repo/$dir"
        local dest_dir="$dest_repo/$dir"
        
        if [[ -d "$src_dir" ]]; then
            print_info "Copying $dir"
            cp -r "$src_dir" "$(dirname "$dest_dir")/"
        else
            print_warn "Source directory not found: $src_dir"
        fi
    done
}

# Function to setup kontinuum directory
setup_kontinuum() {
    local repo_url="$1"
    local dest_dir="$2"
    local branch="$3"
    
    print_info "Setting up metaeffekt-kontinuum"
    
    # Clone the repository
    clone_repo "$repo_url" "$dest_dir" "$branch" || return 1
    
    # Remove unnecessary directories and files
    print_info "Cleaning up repository"
    
    local items_to_remove=(
        ".git"
        ".github"
        ".jenkins"
        "docs"
        "tests"
        ".gitignore"
    )
    
    for item in "${items_to_remove[@]}"; do
        local path="$dest_dir/$item"
        if [[ -e "$path" ]]; then
            rm -rf "$path"
        fi
    done
}

# Function to build Docker image
build_docker() {
    local docker_tag="$1"
    
    print_info "Building Docker image with tag: $docker_tag"
    
    # Create multi-arch builder if it doesn't exist
    if ! docker buildx inspect multiarch-builder &>/dev/null; then
        print_info "Creating multi-arch builder"
        docker buildx create --use --name multiarch-builder
    else
        print_info "Using existing multi-arch builder"
        docker buildx use multiarch-builder
    fi
    
    # Build the image
    docker buildx build \
        --platform linux/amd64,linux/arm64 \
        -f "$SELF_DIR/container/Dockerfile" \
        --tag "metaeffekt/metaeffekt-kontinuum-runtime:$docker_tag" \
        "$SELF_DIR" || {
        print_error "Docker build failed"
        return 1
    }
    
    print_info "Docker image built successfully: metaeffekt/metaeffekt-kontinuum-runtime:$docker_tag"
}

# Function to cleanup temporary files
cleanup() {
    print_info "Cleaning up temporary files"
    
    if [[ -d "$TMP_DIR" ]]; then
        rm -rf "$TMP_DIR"
        print_info "Removed temporary directory: $TMP_DIR"
    fi

    if [[ -d "$SELF_DIR/metaeffekt-kontinuum" ]]; then
        rm -rf "$SELF_DIR/metaeffekt-kontinuum"
        print_info "Removed kontinuum directory: $TMP_DIR"
    fi
}

# Function to handle script exit
cleanup_on_exit() {
    local exit_code=$?
    cleanup
    exit $exit_code
}

# Main script execution
main() {
    # Set trap to cleanup on exit
    trap cleanup_on_exit EXIT
    
    # Parse command line arguments
    local interactive=true
    local core_version=""
    local artifact_version=""
    local kontinuum_version=""
    local docker_tag=""
    
    while [[ $# -gt 0 ]]; do
        case $1 in
            -h|--help)
                show_help
                exit 0
                ;;
            -c|--core-version)
                core_version="$2"
                interactive=false
                shift 2
                ;;
            -a|--artifact-version)
                artifact_version="$2"
                interactive=false
                shift 2
                ;;
            -k|--kontinuum-version)
                kontinuum_version="$2"
                interactive=false
                shift 2
                ;;
            -d|--docker-tag)
                docker_tag="$2"
                interactive=false
                shift 2
                ;;
            -y|--yes)
                interactive=false
                shift
                ;;
            *)
                print_error "Unknown option: $1"
                show_help
                exit 1
                ;;
        esac
    done
    
    # Set versions from environment variables or defaults
    core_version="${core_version:-${AE_CORE_VERSION:-$DEFAULT_AE_CORE_VERSION}}"
    artifact_version="${artifact_version:-${AE_ARTIFACT_ANALYSIS_VERSION:-$DEFAULT_AE_ARTIFACT_ANALYSIS_VERSION}}"
    kontinuum_version="${kontinuum_version:-${AE_KONTINUUM_VERSION:-$DEFAULT_AE_KONTINUUM_VERSION}}"
    docker_tag="${docker_tag:-${CONTAINER_VERSION:-$DEFAULT_CONTAINER_VERSION}}"
    
    # Interactive prompts
    if [[ "$interactive" == true ]]; then
        print_info "Interactive mode - press Enter to accept defaults"
        core_version=$(prompt_version "Enter metaeffekt-core version" "$core_version")
        artifact_version=$(prompt_version "Enter metaeffekt-artifact-analysis version" "$artifact_version")
        kontinuum_version=$(prompt_version "Enter metaeffekt-kontinuum version" "$kontinuum_version")
        docker_tag=$(prompt_version "Enter Docker image tag" "$docker_tag")
    fi
    
    # Display configuration
    print_info "Configuration:"
    echo "  metaeffekt-core version: $core_version"
    echo "  metaeffekt-artifact-analysis version: $artifact_version"
    echo "  metaeffekt-kontinuum version: $kontinuum_version"
    echo "  Docker tag: $docker_tag"
    
    # Confirm if not auto-skipped
    if [[ "$interactive" == true ]]; then
        read -p "Continue with these settings? [y/N] " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            print_info "Build cancelled by user"
            exit 0
        fi
    fi
    
    # Ensure required directories exist
    ensure_dir "$TMP_DIR"
    ensure_dir "$LOCAL_MAVEN_REPO"
    
    # Build metaeffekt-core
    local core_dir="$TMP_DIR/metaeffekt-core"
    if ! clone_repo "https://github.com/org-metaeffekt/metaeffekt-core.git" "$core_dir" "$core_version"; then
        print_error "Failed to clone metaeffekt-core"
        exit 1
    fi
    
    if ! build_maven "$core_dir" "$TEMP_MAVEN_REPO"; then
        print_error "Failed to build metaeffekt-core"
        exit 1
    fi
    
    # Build metaeffekt-artifact-analysis
    local artifact_dir="$TMP_DIR/metaeffekt-artifact-analysis"
    if ! clone_repo "https://github.com/org-metaeffekt/metaeffekt-artifact-analysis.git" "$artifact_dir" "$artifact_version"; then
        print_error "Failed to clone metaeffekt-artifact-analysis"
        exit 1
    fi
    
    if ! build_maven "$artifact_dir" "$TEMP_MAVEN_REPO"; then
        print_error "Failed to build metaeffekt-artifact-analysis"
        exit 1
    fi
    
    # Copy artifacts to local Maven repo
    copy_artifacts "$TEMP_MAVEN_REPO" "$LOCAL_MAVEN_REPO"
    
    # Setup metaeffekt-kontinuum
    local kontinuum_dir="$SELF_DIR/metaeffekt-kontinuum"
    if ! setup_kontinuum "https://github.com/org-metaeffekt/metaeffekt-kontinuum.git" "$kontinuum_dir" "$kontinuum_version"; then
        print_error "Failed to setup metaeffekt-kontinuum"
        exit 1
    fi
    
    # Build Docker image
    if ! build_docker "$docker_tag"; then
        print_error "Failed to build Docker image"
        exit 1
    fi
    
    print_info "Build completed successfully!"
    print_info "Docker image: metaeffekt/metaeffekt-kontinuum-runtime:$docker_tag"
}

# Run main function with all arguments
main "$@"

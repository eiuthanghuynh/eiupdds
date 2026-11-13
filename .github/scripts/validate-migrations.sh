#!/usr/bin/env bash
# ==============================================================================
# Flyway Migration Quality Gate Script for EIU PDDS
# Validates file naming convention, version uniqueness, and PR immutability.
# ==============================================================================
set -euo pipefail

MIGRATION_DIR="server/src/main/resources/db/migration"

echo "=== [1/3] Checking Flyway Migration File Naming Conventions ==="
if [ ! -d "$MIGRATION_DIR" ]; then
    echo "ERROR: Migration directory not found: $MIGRATION_DIR"
    exit 1
fi

INVALID_NAMING=0
for file in "$MIGRATION_DIR"/*.sql; do
    [ -e "$file" ] || continue
    filename=$(basename "$file")
    if [[ ! "$filename" =~ ^V[0-9]+__[a-zA-Z0-9_]+\.sql$ ]]; then
        echo "::error file=$file::Invalid migration filename '$filename'. Must match pattern: V<version>__<snake_case_description>.sql"
        INVALID_NAMING=1
    fi
done

if [ "$INVALID_NAMING" -ne 0 ]; then
    echo "ERROR: One or more migration files violate the naming convention."
    exit 1
fi
echo "All migration filenames conform to Flyway standards."

echo "=== [2/3] Checking for Duplicate Version Numbers ==="
DUPLICATE_VERSIONS=$(ls -1 "$MIGRATION_DIR"/V*.sql 2>/dev/null | sed -E 's/.*\/V([0-9]+)__.*/\1/' | sort -n | uniq -d || true)

if [ -n "$DUPLICATE_VERSIONS" ]; then
    echo "ERROR: Duplicate Flyway migration versions detected:"
    for ver in $DUPLICATE_VERSIONS; do
        echo "::error::Version $ver is used by multiple migration files:"
        ls -1 "$MIGRATION_DIR"/V"${ver}"__*.sql
    done
    exit 1
fi
echo "No duplicate migration versions found."

echo "=== [3/3] Checking Immutability of Existing Migrations ==="
BASE_BRANCH="${GITHUB_BASE_REF:-main}"

# Determine comparison target ref if in Git repository
if git rev-parse --verify "origin/$BASE_BRANCH" >/dev/null 2>&1; then
    COMPARE_REF="origin/$BASE_BRANCH"
elif git rev-parse --verify "$BASE_BRANCH" >/dev/null 2>&1; then
    COMPARE_REF="$BASE_BRANCH"
else
    COMPARE_REF=""
fi

if [ -n "$COMPARE_REF" ]; then
    # Detect modifications (M) or deletions (D) to migration files that exist on the base branch
    MODIFIED_FILES=$(git diff --name-only --diff-filter=MD "$COMPARE_REF"...HEAD -- "$MIGRATION_DIR" || true)
    if [ -n "$MODIFIED_FILES" ]; then
        echo "ERROR: Migrations merged to $BASE_BRANCH are immutable!"
        echo "The following existing migration files were modified or deleted in this branch:"
        for f in $MODIFIED_FILES; do
            echo "::error file=$f::Migration file $f was modified or deleted. Merged migrations are immutable."
        done
        echo "Resolution: Revert changes to existing files and create a new versioned migration instead."
        exit 1
    fi
    echo "Migration immutability check passed against $COMPARE_REF."
else
    echo "Skipping base branch comparison (base ref not available in local git context)."
fi

echo "=== All Flyway migration checks passed successfully! ==="

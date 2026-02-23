#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="${1:-$HOME}"

echo "Scanning for git repositories under: $ROOT_DIR"
echo

find "$ROOT_DIR" -type d -name ".git" | while read -r gitdir; do
    repo_dir="$(dirname "$gitdir")"
    repo_name="$(basename "$repo_dir")"

    # Skip backed up repos
    if [[ "$repo_name" == *.corrupt.* ]]; then
        echo "Skipping backup repo: $repo_dir"
        continue
    fi

    echo "Checking: $repo_dir"

    if git -C "$repo_dir" fsck --no-dangling >/dev/null 2>&1; then
        echo "Repository is healthy"
        continue
    fi

    echo "Repository is corrupt"

    remote_url="$(git -C "$repo_dir" remote get-url origin 2>/dev/null || true)"

    if [[ -z "$remote_url" ]]; then
        echo "No origin remote found. Skipping."
        continue
    fi

    backup_dir="${repo_dir}.corrupt.$(date +%Y%m%d%H%M%S)"
    echo "Moving corrupt repo to: $backup_dir"
    mv "$repo_dir" "$backup_dir"

    echo "Re-cloning from: $remote_url"
    git clone "$remote_url" "$repo_dir"

    echo "Recovered: $repo_dir"
    echo
done

echo "Done."
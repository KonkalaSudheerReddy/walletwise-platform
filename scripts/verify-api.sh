#!/usr/bin/env bash
set -Eeuo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

if ! command -v node >/dev/null 2>&1; then
  echo "Node.js 20 or newer is required for API verification." >&2
  exit 1
fi

node "$repo_root/scripts/verify-api.mjs"

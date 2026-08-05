#!/usr/bin/env bash
set -Eeuo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker is required. Install Docker Desktop or Docker Engine with Compose v2." >&2
  exit 1
fi

docker compose version >/dev/null
docker compose up --build "$@"

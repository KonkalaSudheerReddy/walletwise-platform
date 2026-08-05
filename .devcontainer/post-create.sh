#!/usr/bin/env bash
set -Eeuo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker was not installed by the dev-container feature." >&2
  exit 1
fi

docker compose up --build --detach

echo "WalletWise is starting. Open the forwarded 'WalletWise application' port when prompted."

#!/usr/bin/env bash
set -Eeuo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"

./backend/mvnw -B -ntp -f backend/pom.xml dependency:go-offline
npm ci --prefix frontend

echo "Dependencies are ready. Run: docker compose up --build"


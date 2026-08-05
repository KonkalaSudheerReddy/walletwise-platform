#!/usr/bin/env bash
set -Eeuo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
skip_docker=false
run_e2e=false

for argument in "$@"; do
  case "$argument" in
    --skip-docker) skip_docker=true ;;
    --e2e) run_e2e=true ;;
    *) echo "Unknown argument: $argument" >&2; exit 2 ;;
  esac
done

cd "$repo_root"

echo "[1/3] Verifying backend"
./backend/mvnw -B -ntp -f backend/pom.xml clean verify

echo "[2/3] Verifying frontend"
npm ci --prefix frontend
npm run format:check --prefix frontend
npm run lint --prefix frontend
npm run test --prefix frontend
npm run build --prefix frontend

if [[ "$run_e2e" == true ]]; then
  echo "Running Playwright against PLAYWRIGHT_BASE_URL (default: http://localhost:8080)"
  npm run test:e2e --prefix frontend
fi

if [[ "$skip_docker" == false ]]; then
  echo "[3/3] Validating Compose and production image"
  docker compose config --quiet
  docker build --tag walletwise-platform:verify .
else
  echo "[3/3] Docker verification skipped by request"
fi

echo "WalletWise verification completed successfully."

#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
echo "For the native preview, press Ctrl+C in the terminal running preview.sh."
echo "For Docker, run: docker compose stop"
echo "Quit Docker Desktop as well to release its VM memory."

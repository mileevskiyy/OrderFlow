#!/bin/sh
set -eu
root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$root"
npm --prefix frontend ci
npm --prefix frontend run build
mkdir -p backend/src/main/resources/static
cp -R frontend/dist/. backend/src/main/resources/static/
exec "$root/mvnw" package "$@"

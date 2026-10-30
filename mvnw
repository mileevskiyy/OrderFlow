#!/bin/sh
set -eu
root=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
version=3.9.16
base="$root/.tools"
binary="$base/apache-maven-$version/bin/mvn"
if [ ! -x "$binary" ]; then
  mkdir -p "$base"
  url="https://dlcdn.apache.org/maven/maven-3/$version/binaries/apache-maven-$version-bin.tar.gz"
  curl --fail --location --show-error "$url" -o "$base/maven.tar.gz"
  curl --fail --location --show-error "$url.sha512" -o "$base/maven.sha512"
  expected=$(awk '{print $1}' "$base/maven.sha512")
  actual=$(shasum -a 512 "$base/maven.tar.gz" | awk '{print $1}')
  [ "$expected" = "$actual" ] || { echo 'Maven checksum verification failed' >&2; exit 1; }
  tar -xzf "$base/maven.tar.gz" -C "$base"
fi
exec "$binary" -Dmaven.repo.local="$base/repository" -f "$root/backend/pom.xml" "$@"

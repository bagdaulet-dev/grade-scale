#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
./mvnw -B -ntp package
mkdir -p .local
runtime_jar=$(mktemp "$PWD/.local/preview.XXXXXX")
cp target/grade-scale.jar "$runtime_jar"
# Keep the running archive stable while Maven rebuilds target/grade-scale.jar.
trap 'rm -f -- "$runtime_jar"' EXIT
java -Xms32m -Xmx192m -jar "$runtime_jar" --spring.profiles.active=preview \
  "--spring.web.resources.static-locations=file:$PWD/src/main/resources/static/,classpath:/static/"

#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."
if [[ ! -f .env ]]; then
    printf 'Missing .env. Follow docs/LOCAL_SETUP.md to configure MySQL.\n' >&2
    exit 1
fi

JAVA_HOME="$(/usr/libexec/java_home -F -v 21)"
export JAVA_HOME
startup_port="${SERVER_PORT:-8081}"
set -a
source .env
set +a
export SERVER_PORT="$startup_port"

exec ./mvnw spring-boot:run

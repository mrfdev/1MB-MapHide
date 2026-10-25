#!/bin/sh
# Use the installed Gradle after selecting the project's validated Oracle JDKs.
set -eu
project_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
exec python3 "$project_dir/scripts/java_toolchains.py" "$@"

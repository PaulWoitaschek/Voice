#!/bin/bash
set -e
./gradlew :scripts:installDist --quiet --console=plain
./scripts/build/install/scripts/bin/scripts "$@"

#!/usr/bin/env bash
# Runs the plain-Java self test. Needs only a JDK (17 or newer), no Maven and no database.
set -euo pipefail
cd "$(dirname "$0")/.."
out="$(mktemp -d)"
javac -d "$out" -sourcepath java tests/com/kanakani/SelfTest.java
java -cp "$out" com.kanakani.SelfTest

#!/usr/bin/env bash
# Offline preflight checks that can run without Maven, Tomcat, or PostgreSQL.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
printf '%s\\n' '== Java self-test =='
bash scripts/selftest.sh
printf '%s\\n' '== Required deployment assets =='
for f in pom.xml config/web.xml config/context.xml db/01_schema.sql jsp/_header.jsp static/images/kanakani-logo.png; do
  test -s "$f" || { echo "Missing or empty: $f" >&2; exit 1; }
done
printf '%s\\n' 'Required files present.'
printf '%s\\n' '== Configuration safety checks =='
if grep -RInE 'password[[:space:]]*=[[:space:]]*"[^$]' java config --include='*.java' --include='*.xml'; then
  echo 'Review possible hard-coded password above.' >&2
  exit 1
fi
echo 'No obvious hard-coded password assignment found.'

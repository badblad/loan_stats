#!/usr/bin/env bash
set -euo pipefail

csv_file="${1:-LoanStats_securev1_2017Q4.csv}"
if [[ ! -f "$csv_file" ]]; then
  echo "CSV file not found: $csv_file" >&2
  exit 1
fi

awk 'NR == 1 { next } /^[[:space:]]*$/ { exit } { print }' "$csv_file" | docker compose exec -T db psql \
  -v ON_ERROR_STOP=1 \
  -U loan_app \
  -d loan_stats \
  -c '\copy loan_stats FROM STDIN WITH (FORMAT csv, HEADER true)'

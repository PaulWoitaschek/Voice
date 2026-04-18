#!/usr/bin/env bash
set -euo pipefail

destination="build/compose_compiler_reports"
composables_destination="$destination/composables.txt"
classes_destination="$destination/classes.txt"

./gradlew :app:assemblePlayRelease -Pvoice.composeCompilerReports=true

mkdir -p "$destination"
rm -rf "${destination:?}"/*

found=0
composables_found=0
classes_found=0

: > "$composables_destination"
: > "$classes_destination"

while IFS= read -r compose_dir; do
  found=1
  module_dir="${compose_dir%/build/compose_compiler}"
  module_rel="${module_dir#./}"
  module_name="${module_rel//\//__}"
  if [[ -z "$module_name" || "$module_name" == "." ]]; then
    module_name="root"
  fi

  while IFS= read -r report_file; do
    composables_found=1
    {
      printf '## %s\n' "$module_name"
      cat "$report_file"
      printf '\n'
    } >> "$composables_destination"
  done < <(find "$compose_dir" -name '*-composables.txt' -type f | sort)

  while IFS= read -r report_file; do
    classes_found=1
    {
      printf '## %s\n' "$module_name"
      cat "$report_file"
      printf '\n'
    } >> "$classes_destination"
  done < <(find "$compose_dir" -name '*-classes.txt' -type f | sort)
done < <(find . -path '*/build/compose_compiler' -type d | sort)

if [[ "$found" -eq 0 ]]; then
  echo "No Compose compiler reports found after build."
  exit 1
fi

if [[ "$composables_found" -eq 0 || "$classes_found" -eq 0 ]]; then
  echo "Compose compiler report files were not found under module reports directories."
  exit 1
fi

echo "Collected Compose compiler reports in $destination"
echo " - $composables_destination"
echo " - $classes_destination"

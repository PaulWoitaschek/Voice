#!/usr/bin/env bash
set -euo pipefail

destination="build/compose_compiler_reports"

./gradlew :app:assemblePlayRelease -Pvoice.composeCompilerReports=true

mkdir -p "$destination"
rm -rf "${destination:?}"/*

found=0

while IFS= read -r compose_dir; do
  found=1
  module_dir="${compose_dir%/build/compose_compiler}"
  module_rel="${module_dir#./}"
  module_name="${module_rel//\//__}"
  if [[ -z "$module_name" || "$module_name" == "." ]]; then
    module_name="root"
  fi

  mkdir -p "$destination/$module_name"

  while IFS= read -r report_entry; do
    entry_name="${report_entry#"$compose_dir"/}"
    if [[ -d "$report_entry" ]]; then
      mkdir -p "$destination/$module_name/$entry_name"
      continue
    fi

    parent_dir="$(dirname "$entry_name")"
    mkdir -p "$destination/$module_name/$parent_dir"
    cp "$report_entry" "$destination/$module_name/$entry_name"
  done < <(find "$compose_dir" -mindepth 1)
done < <(find . -path '*/build/compose_compiler' -type d | sort)

if [[ "$found" -eq 0 ]]; then
  echo "No Compose compiler reports found after build."
  exit 1
fi

echo "Collected Compose compiler reports in $destination"

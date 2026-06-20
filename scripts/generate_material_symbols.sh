#!/usr/bin/env bash
set -euo pipefail

readonly PACKAGE="voice.core.ui.icons"
readonly OUT_DIR="core/ui/src/main/kotlin/voice/core/ui/icons"
readonly BASE_URL="https://fonts.gstatic.com/render/v1/Material+Symbols+Outlined/24dp"
readonly VARIANT="opsz,wght,FILL,GRAD,ROND@24,400,0,0,50"
readonly GENERATED_AT="$(date -u +"%Y-%m-%dT%H:%M:%SZ")"

mkdir -p "${OUT_DIR}"
find "${OUT_DIR}" -maxdepth 1 -type f -name '*.kt' -delete

cat > "${OUT_DIR}/VoiceIcons.kt" <<EOF
package ${PACKAGE}

public object VoiceIcons
EOF

generate_icon() {
  local icon_name="$1"
  local property_name="$2"
  local source_url="${BASE_URL}/${icon_name}.kt?var=${VARIANT}"
  local output_file="${OUT_DIR}/${property_name}.kt"

  {
    printf 'package %s\n\n' "${PACKAGE}"
    printf '/*\n'
    printf ' * Source: %s\n' "${source_url}"
    printf ' * Generated: %s\n' "${GENERATED_AT}"
    printf ' */\n'
    curl --compressed --fail --silent --show-error --location "${source_url}" |
      perl -0pe '
        s/^package com\.example\.test\n\n//;
        s/\@Suppress\("CheckReturnValue"\)\npublic val '"${icon_name}"': ImageVector\n  get\(\) \{\n    if \(_'"${icon_name}"' != null\) \{\n      return _'"${icon_name}"'!!\n    \}\n    _'"${icon_name}"' =/\@Suppress("UnusedReceiverParameter")\npublic val VoiceIcons.'"${property_name}"': ImageVector\n  get() =/;
        s/name = "'"${icon_name}"'"/name = "'"${property_name}"'"/g;
        s/\n    return _'"${icon_name}"'!!\n  \}\n\nprivate var _'"${icon_name}"': ImageVector\? = null\n?/\n/s;
      '
  } > "${output_file}"
}

generate_icon add Add
generate_icon analytics Analytics
generate_icon arrow_back ArrowBack
generate_icon arrow_forward ArrowForward
generate_icon audio_file AudioFile
generate_icon auto_awesome AutoAwesome
generate_icon bedtime Bedtime
generate_icon bedtime_off BedtimeOff
generate_icon book Book
generate_icon bug_report BugReport
generate_icon check Check
generate_icon chevron_left ChevronLeft
generate_icon chevron_right ChevronRight
generate_icon close Close
generate_icon coffee Coffee
generate_icon collections_bookmark CollectionsBookmark
generate_icon construction Construction
generate_icon delete Delete
generate_icon done Done
generate_icon download Download
generate_icon expand_more ExpandMore
generate_icon fast_rewind FastRewind
generate_icon favorite Favorite
generate_icon folder Folder
generate_icon grid_view GridView
generate_icon help Help
generate_icon history History
generate_icon hourglass_empty HourglassEmpty
generate_icon image Image
generate_icon language Language
generate_icon laptop_mac Laptop
generate_icon library_books LibraryBooks
generate_icon lightbulb Lightbulb
generate_icon lock_open LockOpen
generate_icon more_vert MoreVert
generate_icon not_started NotStarted
generate_icon person Person
generate_icon remove Remove
generate_icon search Search
generate_icon sentiment_satisfied SentimentSatisfied
generate_icon settings Settings
generate_icon speed Speed
generate_icon tag Tag
generate_icon timelapse Timelapse
generate_icon timer Timer
generate_icon title Title
generate_icon undo Undo
generate_icon view_list ViewList

#!/usr/bin/env bash
set -euo pipefail

readonly OUT_DIR="core/ui/src/main/res/drawable"
readonly BASE_URL="https://fonts.gstatic.com/render/v1/Material+Symbols+Outlined/24dp"

# Usage: icon <name> [filled|mirrored|play]
icon() {
  local name="$1"
  local option="${2:-}"
  local fill=0
  local file="ic_${name}"
  local attributes=""
  case "${option}" in
    filled)
      fill=1
      file="ic_${name}_filled"
      ;;
    mirrored)
      # For the icons that Google marks as auto mirrored, so they point the right way in
      # right-to-left layouts.
      attributes='
    android:autoMirrored="true"'
      ;;
    play)
      # Lint only checks the free build, which doesn't show these icons.
      attributes='
    xmlns:tools="http://schemas.android.com/tools"
    tools:ignore="UnusedResources"'
      ;;
  esac
  local xml
  xml="$(curl --compressed --fail --silent --show-error --location \
    "${BASE_URL}/${name}.xml?var=opsz,wght,FILL,GRAD,ROND@24,400,${fill},0,50")"
  printf '%s\n' "${xml/android:viewportHeight=\"24\">/android:viewportHeight=\"24\"${attributes}>}" \
    > "${OUT_DIR}/${file}.xml"
}

icon add
icon analytics
icon arrow_back mirrored
icon arrow_drop_down
icon arrow_forward mirrored
icon audio_file
icon auto_awesome
icon bedtime
icon bedtime filled
icon bedtime_off
icon book
icon bookmark
icon bookmark_add
icon bug_report
icon check
icon chevron_right mirrored
icon close
icon cloud
icon cloud_done
icon cloud_off
icon coffee
icon collections_bookmark
icon construction
icon content_cut
icon contrast mirrored
icon crop
icon delete
icon dns
icon done
icon download
icon download_done
icon edit
icon emoji_food_beverage
icon error
icon expand_more
icon fast_forward
icon fast_rewind
icon favorite
icon folder
icon format_quote
icon forum
icon forward_media
icon headphones
icon help mirrored
icon history
icon hive
icon hourglass_empty
icon image
icon image_search
icon keyboard_arrow_down
icon laptop_mac
icon library_books mirrored
icon light_mode
icon lightbulb
icon lock_open
icon logout mirrored
icon mail play
icon mic
icon more_vert
icon network_cell
icon not_started
icon palette
icon pause
icon person
icon play_arrow
icon remove
icon replay
icon schedule
icon search
icon search_off
icon settings
icon skip_next
icon skip_previous
icon smartphone
icon sort mirrored
icon speed
icon star play
icon swap_horiz
icon sync
icon touch_app
icon undo mirrored
icon vibration
icon view_list mirrored
icon visibility
icon visibility_off
icon volume_up mirrored
icon volunteer_activism

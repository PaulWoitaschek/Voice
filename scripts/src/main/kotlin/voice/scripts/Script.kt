package voice.scripts

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands

class Script : CliktCommand() {
  override fun run() {}
}

fun main(args: Array<String>) {
  Script()
    .subcommands(
      UpdateScreenshots(),
      NewFeatureModule(),
      PopulateTestFiles(),
    )
    .main(args)
}

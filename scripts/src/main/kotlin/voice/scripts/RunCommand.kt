package voice.scripts

fun runCommand(vararg command: String) {
  val process = ProcessBuilder(command.toList())
    .redirectOutput(ProcessBuilder.Redirect.INHERIT)
    .start()
  process.waitFor().also {
    check(it == 0) {
      "output=${process.errorStream.bufferedReader().readText()}"
    }
  }
}

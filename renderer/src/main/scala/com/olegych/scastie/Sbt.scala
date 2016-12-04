package com.olegych.scastie

import java.io.{File, InputStreamReader}

import org.apache.commons.lang3.SystemUtils
import org.apache.commons.collections15.buffer.CircularFifoBuffer
import org.slf4j.LoggerFactory

import scalax.file.Path

case class Sbt(dir: File,
               clearOnExit: Boolean,
               uniqueId: String = Sbt.defaultUniqueId) {
  private val log = LoggerFactory.getLogger(getClass)
  private val (process, fin, input, fout) = {
    def absolutePath(command: String) = new File(command).getAbsolutePath
    val builder = new ProcessBuilder(
      absolutePath(if (SystemUtils.IS_OS_WINDOWS) "xsbt.cmd" else "xsbt.sh"))
      .directory(dir)
    val currentOpts = Option(System.getenv("SBT_OPTS"))
      .getOrElse("")
      .replaceAll(
        "-agentlib:jdwp=transport=dt_shmem,server=n,address=.*,suspend=y",
        "")
    builder
      .environment()
      .put(
        "SBT_OPTS",
        currentOpts + " -Djline.terminal=jline.UnsupportedTerminal -Dsbt.log.noformat=true")
    log.info("Starting sbt with {} {} {}",
             builder.command(),
             builder.environment(),
             builder.directory())
    val process = builder.start()
    import scalax.io.JavaConverters._
    //can't use .lines since it eats all input
    (process,
     process.getOutputStream,
     process.getOutputStream.asUnmanagedOutput,
     new InputStreamReader(process.getInputStream))
  }

  private def collect(lineCallback: (String, Boolean) => Unit): Unit = {
    import collection.JavaConversions._
    val chars  = new CircularFifoBuffer[Character](1000)
    var read   = 0
    var prompt = false
    while (read != -1 && !prompt) {
      read = fout.read()
      if (read == 10) {
        val line = chars.mkString
        prompt = line == uniqueId
        lineCallback(line, false)
        log.info(" sbt: " + line)
        chars.clear()
      } else {
        chars.add(read.toChar)
      }
    }
  }

  collect((line, _) => ())

  def process(command: String,
              lineCallback: (String, Boolean) => Unit,
              waitForPrompt: Boolean = true): Unit = {
    input.write(command + System.lineSeparator)
    fin.flush()
    log.info("sbt: " + command)
    if (waitForPrompt) {
      collect(lineCallback)
    }
  }

  def close(): Unit = {
    try process("exit", (_, _) => (), waitForPrompt = false)
    catch {
      case e: Exception => log.error("Error while soft exit", e)
    }
    ProcessKiller.instance.kill(process)
    if (clearOnExit) {
      try Path(dir).deleteRecursively(force = true, continueOnFailure = true)
      catch {
        case e: Exception => log.error("Error while cleaning up", e)
      }
    }
  }
  def resultAsString(result: Seq[String]) =
    result.mkString(System.lineSeparator).replaceAll(uniqueId, "")
}

object Sbt {
  def defaultUniqueId: String = ">"
}

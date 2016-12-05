package actors

import java.nio.file._
import java.util.concurrent.atomic.AtomicLong
import System.{lineSeparator => nl}

class PastesContainer(root: Path) {

  def write(paste: AddPaste): Long = {
    val id = lastId.incrementAndGet()

    if(Files.exists(pasteDir(id)))
      throw new Exception(s"trying to write paste to existing folder: ${newPasteDir.toString}")

    codeFile(id).write(paste.code)
    sbtConfigFile(id).write(paste.sbtConfig)

    id
  }

  def read(id: Long): Option[Paste] = {
    if(Files.exists(pasteDir(id))) {
      (codeFile(id).read, sbtConfigFile(id).read) match {
        case (Some(code), Some(sbtConfig)) => Some(Paste(id, code, sbtConfig))
        case _ => None
      }
    } else None
  }

  private val lastId = {
      val last =
        if(Files.exists(root)) {
            import scala.collection.JavaConverters._
            val PasteFormat = "paste(\\d+)".r
            val ds = Files.newDirectoryStream(root)
            val pasteNumbers =
              ss.asScala
                .map(_.getName)
                .collect {
                  case PasteFormat(id) => id.toLong
                }
                .max
            ds.close()
        } else 0L

      new AtomicLong(last)
  }

  private def pasteDir(id: Long): Path  = root.resolve("paste" + id)
  private def codeFile(id: Long)        = new ExtendedFile(pasteDir(id).resolve(Path.get("main.scala")))
  private def sbtConfigFile(id: Long)   = new ExtendedFile(pasteDir(id).resolve(Path.get("config.sbt")))

  private class ExtendedFile(path: Path) {
    def read: Option[String] = {
      if(Files.exists(path)) {
        Some(Files.readAllLines(path).toArray.mkString(nl)).filter(!_.isEmpty)
      } else None
    }

    def write(content: String): Unit = {
      path.getParent.toFile.mkdirs()
      Files.write(absPath, c.getBytes, CREATE_NEW)
    }

    // def write(content: Option[String], truncate: Boolean = true): Unit = {
    //   path.getParent.toFile.mkdirs()
    //
    //   val option =
    //     if(truncate) StandardOpenOption.TRUNCATE_EXISTING
    //     else StandardOpenOption.APPEND
    //
    //   content match {
    //     case Some(c) => Files.write(absPath, c.getBytes, CREATE_NEW, option)
    //     case None => ()
    //   }
    // }
    // def exists = File.exists(absPath)
  }
}

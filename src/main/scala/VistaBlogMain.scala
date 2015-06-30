import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

import scalaj.http.Http

// The main application
object VistaBlogMain extends App {
  val GHMDRendererUrl = "https://api.github.com/markdown"
  val defaultSource = "source"
  val defaultTarget = "target"

  val config = args match {
    case Array(aPath) =>
      Some((aPath, defaultSource, defaultTarget))
    case Array(aPath, aSource) =>
      Some((aPath, aSource, defaultTarget))
    case Array(aPath, aSource, aTarget, _*) =>
      Some((aPath, aSource, aTarget))
    case _ =>
      println(
        s"Usage: java -jar vistablog-x.x.x <blogPath> "+
        s"[<source> default 'source'] [<target> default 'target']")
      None
  }

  config.foreach { case (path, source, target) =>
    val header = stringFromFile(new File(s"$path/$source/header.html"))
    val footer = stringFromFile(new File(s"$path/$source/footer.html"))

    val d = new File(s"$path/new")
    if (d.exists && d.isDirectory) {
      for (file <- d.listFiles() if file.isFile) {
        val markdown = stringFromFile(file)
        val html = Http(GHMDRendererUrl).postData(markdown).asString.body
        val htmlFull = s"$header\n$html\n$footer"
        val fileNamePieces = file.getName.split('.')
        val fileName = fileNamePieces.dropRight(1).mkString("")
        Files.write(
          Paths.get(s"$path/$target/$fileName.html"),
          htmlFull.getBytes(StandardCharsets.UTF_8))
        Files.move(
          Paths.get(file.getCanonicalPath),
          Paths.get(s"$path/$source/posts/${file.getName}"))
      }
    }
  }

  private def stringFromFile(file: File): String = {
    val source = io.Source.fromFile(file)
    try source.getLines mkString "\n" finally source.close()
  }
}


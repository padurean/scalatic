import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.{StandardCopyOption, Files, Paths}

import scalaj.http.Http

object VistaBlog extends App {
  val GHMDRendererUrl = "https://api.github.com/markdown"
  val defaultSource = "source"
  val defaultTarget = "target"

  val options = args match {
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

  options.foreach { case (path, source, target) =>
    renderNewPosts(path, source, target)
    copyFiles(
      s"$path/$source",
      s"$path/$target",
      excludeFiles = Set("header.html", "footer.html"))
  }

  private def copyFiles(
    srcFolderPath: String,
    destFolderPath: String,
    excludeFiles: Set[String]) = {
    val srcFolder = new File(srcFolderPath)
    if (!srcFolder.exists || !srcFolder.isDirectory) {
      println(s"ERROR: $srcFolder does not exist or is not a folder.")
    } else { for (file <- srcFolder.listFiles()
      if !excludeFiles(file.getName) && file.isFile) {
        Files.copy(
          Paths.get(file.getCanonicalPath),
          Paths.get(s"$destFolderPath/${file.getName}"),
          StandardCopyOption.REPLACE_EXISTING)
      }
    }
  }

  private def renderNewPosts(
    path: String,
    source: String,
    target: String)
  : Unit = {
    val header = stringFromFile(new File(s"$path/$source/header.html"))
    val footer = stringFromFile(new File(s"$path/$source/footer.html"))

    val newPostsFolder = new File(s"$path/new")
    if (!newPostsFolder.exists || !newPostsFolder.isDirectory) {
      println(s"ERROR: $newPostsFolder does not exist or is not a folder.")
    } else {
      for (file <- newPostsFolder.listFiles() if file.isFile) {
        val srcFileName = file.getName
        val srcFileNamePieces = srcFileName.split('.')
        val destFileName =
          srcFileNamePieces.dropRight(1).mkString("", "", ".html")

        println(s"\nRendering $srcFileName => $destFileName ...")
        val markdown = "{\"text\": \"" + stringFromFile(file) + "\"}"
        val html = Http(GHMDRendererUrl).postData(markdown).asString.body
        val htmlFull = s"$header\n$html\n$footer"

        val destFilePath = s"$path/$target/$destFileName"
        println(s"Writing $destFilePath ...")
        Files.write(
          Paths.get(destFilePath),
          htmlFull.getBytes(StandardCharsets.UTF_8))

        val processedSrcFilePath = s"$path/$source/posts/$srcFileName"
        println(s"Moving ${file.getCanonicalPath} to $processedSrcFilePath ...")
        Files.move(
          Paths.get(file.getCanonicalPath),
          Paths.get(processedSrcFilePath),
          StandardCopyOption.REPLACE_EXISTING)
      }
    }
  }

  private def stringFromFile(file: File): String = {
    val source = io.Source.fromFile(file)
    try source.getLines mkString "\n" finally source.close()
  }
}


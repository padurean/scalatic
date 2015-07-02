import java.nio.charset.StandardCharsets
import java.nio.file.{Path, Files, Paths, StandardCopyOption}

import scala.collection.JavaConverters._

import scalaj.http.Http

object Scalatic extends App {
  val GHMDRendererUrl = "https://api.github.com/markdown"
  val defaultSource = "source"
  val defaultTarget = "target"

  val options = validateArgs(args).orElse {
    println(
      s"Usage: java -jar scalatic-x.x.x <blogPath> " +
      s"[<source> default 'source'] [<target> default 'target']")
    None
  }

  options.foreach { case (path, source, target) =>
    val newPath = s"$path/new"
    requireFolder(newPath)

    val sourcePath = s"$path/$source"
    requireFolder(sourcePath)

    val sourcePostsPath = s"$sourcePath/posts"
    createFolderIfNotExists(sourcePostsPath)

    val targetPath = s"$path/$target"
    createFolderIfNotExists(targetPath)

    renderNewPosts(newPath, sourcePath, sourcePostsPath, targetPath)

    copyFiles(
      sourcePath,
      targetPath,
      excludeFiles = Set("header.html", "footer.html"))
  }

  private def renderNewPosts(
    newPath: String,
    sourcePath: String,
    sourcePostsPath: String,
    targetPath: String)
  : Unit = {
    val header = stringFromFile(s"$sourcePath/header.html")
    val footer = stringFromFile(s"$sourcePath/footer.html")

    val newPostsFolder = Paths.get(newPath)
    for (
      newSrcFile <- Files.newDirectoryStream(newPostsFolder).asScala
      if !Files.isDirectory(newSrcFile)
    )
    {
      val html = render(newSrcFile, header, footer)

      val srcFileName = newSrcFile.getFileName.toString

      val destFileName =
        srcFileName.split('.').dropRight(1).mkString("", "", ".html")
      writeFile(html, s"$targetPath/$destFileName")

      val processedSrcFilePath = s"$sourcePostsPath/$srcFileName"
      moveFile(newSrcFile, Paths.get(processedSrcFilePath))
    }
  }

  private def moveFile(srcFile: Path, destFile: Path) = {
    println(s"Moving ${srcFile.toString } to ${destFile.toString} ...")
    Files.move(srcFile, destFile, StandardCopyOption.REPLACE_EXISTING)
  }

  private def writeFile(fileContent: String, filePath: String): Path = {
    println(s"Writing $filePath ...")
    Files.write(
      Paths.get(filePath),
      fileContent.getBytes(StandardCharsets.UTF_8))
  }

  private def render(file: Path, header: String, footer: String): String = {
    val srcFilePath: String = file.toString
    println(s"\nRendering $srcFilePath ...")
    val markdown = "{\"text\": \"" + stringFromFile(srcFilePath) + "\"}"
    val html = Http(GHMDRendererUrl).postData(markdown).asString.body
    val htmlFull = s"$header\n$html\n$footer"
    htmlFull
  }

  private def requireFolder(pathToFolder: String) = {
    val folder = Paths.get(pathToFolder)
    require(
      Files.exists(folder) && Files.isDirectory(folder),
      s"$pathToFolder does not exist or is not a folder")
  }

  private def createFolderIfNotExists(pathToFolder: String) = {
    val folder = Paths.get(pathToFolder)
    if (Files.notExists(folder) || !Files.isDirectory(folder))
      Files.createDirectory(folder)
  }

  private def validateArgs(scalaticArgs: Array[String])
  : Option[(String,String,String)] =
    scalaticArgs match {
      case Array(aPath) =>
        Some((aPath, defaultSource, defaultTarget))
      case Array(aPath, aSource) =>
        Some((aPath, aSource, defaultTarget))
      case Array(aPath, aSource, aTarget, _*) =>
        Some((aPath, aSource, aTarget))
      case _ =>
        None
    }

  private def copyFiles(
    srcFolderPath: String,
    destFolderPath: String,
    excludeFiles: Set[String]) = {
    val srcFolder = Paths.get(srcFolderPath)
    for (
      file <- Files.newDirectoryStream(srcFolder).asScala
      if !excludeFiles(file.getFileName.toString) && !Files.isDirectory(file)
    ) {
      Files.copy(
        file,
        Paths.get(s"$destFolderPath/${file.getFileName.toString}"),
        StandardCopyOption.REPLACE_EXISTING)
    }
  }

  private def stringFromFile(filePath: String): String = {
    val source = io.Source.fromFile(filePath)
    try source.getLines mkString "\n" finally source.close()
  }
}

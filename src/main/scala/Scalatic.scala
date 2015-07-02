import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.{Path, Files, Paths, StandardCopyOption}

import org.joda.time.DateTime

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

    // TODO OGG: add requireFile method and call it here to check that header and footer are there
    val header = stringFromFile(s"$sourcePath/header.html")
    val footer = stringFromFile(s"$sourcePath/footer.html")
    renderNewPosts(
      newPath, sourcePath, sourcePostsPath, targetPath, header, footer)

    generateIndex(sourcePostsPath, targetPath, header, footer)

    copyFiles(
      sourcePath,
      targetPath,
      excludeFiles = Set("header.html", "footer.html"))
  }

  private def generateIndex(
      sourcePostsPath: String,
      targetPath: String,
      header: String,
      footer: String) = {

    val linksToPosts = for (
      srcFile <- Files.newDirectoryStream(Paths.get(sourcePostsPath)).asScala
      if !Files.isDirectory(srcFile)
    ) yield {
      val srcFileName = srcFile.getFileName.toString
      val srcFileNamePiecesNoExt = srcFileName.split('.').dropRight(1)
      val url = srcFileNamePiecesNoExt.mkString("", ".", ".html")
      val title = srcFileNamePiecesNoExt.mkString(" ").split('-').mkString(" ")
      s"<a href='$url'>$title</a>"
    }

    val html = linksToPosts.mkString("<br/>\n")
    writeFile(s"$header\n$html\n$footer", s"$targetPath/index.html")
  }

  private def renderNewPosts(
    newPath: String,
    sourcePath: String,
    sourcePostsPath: String,
    targetPath: String,
    header: String,
    footer: String)
  : Unit = {
    val newPostsFolder = Paths.get(newPath)
    for (
      newSrcFile <- Files.newDirectoryStream(newPostsFolder).asScala
      if !Files.isDirectory(newSrcFile)
    ) {
      val html = render(newSrcFile, header, footer)

      val srcFileName = newSrcFile.getFileName.toString

      val destFileName =
        srcFileName.split('.').dropRight(1).mkString("", ".", ".html")
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
      val destFile = Paths.get(s"$destFolderPath/${file.getFileName.toString}")
      println(s"Copying ${file.toString} to ${destFile.toString} ...")
      Files.copy(
        file,
        destFile,
        StandardCopyOption.REPLACE_EXISTING)
    }
  }

  private def stringFromFile(filePath: String): String = {
    val source = io.Source.fromFile(filePath)
    try source.getLines mkString "\n" finally source.close()
  }
}

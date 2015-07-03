import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.{Path, Files, Paths, StandardCopyOption}

import org.joda.time.DateTime

import scala.collection.JavaConverters._

import scalaj.http.{HttpOptions, Http}

object Scalatic extends App {
  val GHMDRendererUrl = "https://api.github.com/markdown/raw"
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
    requireFile(newPath, mustBeFolder = true)

    val sourcePath = s"$path/$source"
    requireFile(sourcePath, mustBeFolder = true)

    val headerPath = s"$sourcePath/header.html"
    requireFile(headerPath, mustBeFolder = false)

    val footerPath = s"$sourcePath/footer.html"
    requireFile(footerPath, mustBeFolder = false)

    val sourcePostsPath = s"$sourcePath/posts"
    createFolderIfNotExists(sourcePostsPath)

    val targetPath = s"$path/$target"
    createFolderIfNotExists(targetPath)

    val header = stringFromFile(headerPath)
    val footer = stringFromFile(footerPath)
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
      srcFile <- Files.newDirectoryStream(Paths.get(sourcePostsPath))
        .asScala.toSeq.sortWith { (l, r) =>
          Files.getLastModifiedTime(l).compareTo(
            Files.getLastModifiedTime(r)) > 0
        }
      if !Files.isDirectory(srcFile)
    ) yield {
      val srcFileName = srcFile.getFileName.toString
      val srcFileNamePiecesNoExt = srcFileName.split('.').dropRight(1)
      val url = srcFileNamePiecesNoExt.mkString("", ".", ".html")
      val title = srcFileNamePiecesNoExt.mkString(" ").split('-').mkString(" ")
      s"<a href='$url' class='blog-index-link'>$title</a>"
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
    val markdown = stringFromFile(srcFilePath)
    val html = Http(GHMDRendererUrl)
      .postData(markdown)
      .header("Content-Type", "text/plain")
      .header("Charset", "UTF-8")
      .option(HttpOptions.readTimeout(10000))
      .asString.body
    val htmlFull = s"$header\n$html\n$footer"
    htmlFull
  }

  private def requireFile(pathToFile: String, mustBeFolder: Boolean) = {
    val requiredFile = Paths.get(pathToFile)
    val isFolder = Files.isDirectory(requiredFile)
    val folderOrFile = if (mustBeFolder) "folder" else "file"
    require(
      Files.exists(requiredFile) && (if (mustBeFolder) isFolder else !isFolder),
      s"$pathToFile does not exist or is not a $folderOrFile")
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
        Some((noEndSlash(aPath), defaultSource, defaultTarget))
      case Array(aPath, aSource) =>
        Some((noEndSlash(aPath), noEndSlash(aSource), defaultTarget))
      case Array(aPath, aSource, aTarget, _*) =>
        Some((noEndSlash(aPath), noEndSlash(aSource), noEndSlash(aTarget)))
      case _ =>
        None
    }

  private def noEndSlash(str: String) = {
    if(str.endsWith("/") || str.endsWith("\\")) {
      str.dropRight(1)
    } else {
      str
    }
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

  def stringFromFile(filePath: String): String = {
    val source = io.Source.fromFile(filePath)
    try source.getLines mkString "\n" finally source.close()
  }
}

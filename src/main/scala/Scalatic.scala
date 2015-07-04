import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, Paths, StandardCopyOption}

import org.joda.time.format.{ISODateTimeFormat, DateTimeFormat}

import scala.collection.JavaConverters._

import org.joda.time.DateTime
import scala.language.implicitConversions
import scalaj.http.{Http, HttpOptions}

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
    // TODO OGG: put all these require in create in a method
    // called validBlogFileSystemAnatomy which returns a BlogFileSystemAnatomy object
    val newPath = s"$path/new"
    requireFile(newPath, mustBeFolder = true)

    validateFileNames(newPath)

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

  class Tabs(n: Int) extends {
    val tab="  "
    def tabs =
      (for (i <- 1 to n) yield tab).mkString
  }
  implicit def tabs(value: Int): Tabs = new Tabs(value)

  case class PostSummary(url: String, title: String, date: DateTime)
    extends Ordered[PostSummary] {
    override def compare(that: PostSummary): Int = that.date.compareTo(this.date)
  }
  object PostSummary {
    val df = DateTimeFormat.shortDateTime()
    val dfIso = ISODateTimeFormat.dateTimeNoMillis()

    def toLink(ps: PostSummary): String = {
      s"${2.tabs}<article>\n${3.tabs}<header>\n" +
        s"${4.tabs}<a href='${ps.url}' class='blog-index-link'>${ps.title}</a>\n" +
        s"${4.tabs}<time pubdate datetime='${dfIso.print(ps.date)}' class='blog-index-date'>" +
        s"${df.print(ps.date)}</time>\n${3.tabs}</header>\n${2.tabs}</article>"
    }
  }

  def generateIndex(
      sourcePostsPath: String,
      targetPath: String,
      header: String,
      footer: String) = {
    import Scalatic.PostSummary._

    val linksToPosts = for (
      srcFile <- Files.newDirectoryStream(Paths.get(sourcePostsPath)).asScala
      if !Files.isDirectory(srcFile)
    ) yield fileNameToPostSummary(srcFile.getFileName.toString)

    val html = linksToPosts.toSeq.sorted.map(toLink).mkString("<br/>\n")
    writeFile(s"$header\n$html\n$footer", s"$targetPath/index.html")
  }

  private def fileNameToPostSummary(fileName: String): PostSummary = {
    val fileNameNoExt = fileName.split('.')(0)
    val url = s"$fileNameNoExt.html"

    val fileNamePiecesNoExt = fileNameNoExt.split("-")
    val fileNameNoDate = fileNamePiecesNoExt.dropRight(5)
    val title = fileNameNoDate.mkString(" ")

    val maxi = fileNamePiecesNoExt.length - 1
    val date = new DateTime(
      fileNamePiecesNoExt(maxi - 4).toInt,
      fileNamePiecesNoExt(maxi - 3).toInt,
      fileNamePiecesNoExt(maxi - 2).toInt,
      fileNamePiecesNoExt(maxi - 1).toInt,
      fileNamePiecesNoExt(maxi).toInt)
    PostSummary(url, title, date)
  }

  def validateFileNames(folderPath: String) = {
    val expected = "Some-blog-post-name-<yyyy-MM-dd-HH-mm>.md"
    val example = "Your-wise-blog-post-name-2015-07-15-00-45.md"
    for (
      file <- Files.newDirectoryStream(Paths.get(folderPath)).asScala if !Files.isDirectory(file);
      fileName <- Option(file.getFileName.toString) if !fileName.startsWith(".")
    ) {
      val fileName = file.getFileName.toString
      val fileNamePieces = fileName.split('.')
      val errMsg =
        s"File name '$fileName' does not match the expected format " +
        s"'$expected' - e.g. '$example'"

      require(fileNamePieces.length == 2, errMsg)
      require(fileNamePieces(0).split("-").length > 5, errMsg)
    }
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

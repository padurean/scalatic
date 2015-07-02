import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths, StandardCopyOption}

import scala.collection.JavaConverters._

import scalaj.http.Http

object Scalatic extends App {
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
        s"Usage: java -jar scalatic-x.x.x <blogPath> "+
        s"[<source> default 'source'] [<target> default 'target']")
      None
  }

  options.foreach { case (path, source, target) =>
    // require that new folder exists
    val newPath = s"$path/new"
    val newFolder = Paths.get(newPath)
    require(
      Files.exists(newFolder)  && Files.isDirectory(newFolder),
      s"$newPath does not exist or is not a folder")

    // require that source folder exists
    val sourcePath = s"$path/$source"
    val sourceFolder = Paths.get(sourcePath)
    require(
      Files.exists(sourceFolder) && Files.isDirectory(sourceFolder),
      s"$sourcePath does not exist or is not a folder")

    // if the source posts folder does not exist, create it
    val sourcePostsPath = s"$sourcePath/posts"
    val srcPostsFolder = Paths.get(sourcePostsPath)
    if (Files.notExists(srcPostsFolder) || !Files.isDirectory(srcPostsFolder))
      Files.createDirectory(srcPostsFolder)

    // if the target folder does not exist, create it
    val targetPath = s"$path/$target"
    val targetFolder = Paths.get(targetPath)
    if (Files.notExists(targetFolder) || !Files.isDirectory(targetFolder))
      Files.createDirectory(targetFolder)

    renderNewPosts(newPath, sourcePath, sourcePostsPath, targetPath)

    copyFiles(
      sourcePath,
      targetPath,
      excludeFiles = Set("header.html", "footer.html"))
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
      file <- Files.newDirectoryStream(newPostsFolder).asScala
      if !Files.isDirectory(file)
    )
    {
      val srcFileName = file.getFileName.toString
      val srcFileNamePieces = srcFileName.split('.')
      val destFileName =
        srcFileNamePieces.dropRight(1).mkString("", "", ".html")

      // render
      println(s"\nRendering $srcFileName => $destFileName ...")
      val markdown =
        "{\"text\": \"" + stringFromFile(file.toString) + "\"}"
      val html = Http(GHMDRendererUrl).postData(markdown).asString.body
      val htmlFull = s"$header\n$html\n$footer"

      // write
      val destFilePath = s"$targetPath/$destFileName"
      println(s"Writing $destFilePath ...")
      Files.write(
        Paths.get(destFilePath),
        htmlFull.getBytes(StandardCharsets.UTF_8))

      // move source post file to processed folder
      val processedSrcFilePath = s"$sourcePostsPath/$srcFileName"
      println(s"Moving ${file.toString} to $processedSrcFilePath ...")
      Files.move(
        file,
        Paths.get(processedSrcFilePath),
        StandardCopyOption.REPLACE_EXISTING)
    }
  }

  private def stringFromFile(filePath: String): String = {
    val source = io.Source.fromFile(filePath)
    try source.getLines mkString "\n" finally source.close()
  }
}


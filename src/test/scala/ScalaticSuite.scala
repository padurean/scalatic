import java.io.IOException
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes

import Scalatic.stringFromFile
import ScalaticSuite._
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}

class ScalaticSuite extends FunSuite with BeforeAndAfter with ScalaFutures {
  implicit override val patienceConfig =
    PatienceConfig(timeout = Span(5, Seconds))

  val basePath = "src/test/scala/scalatictest"
  val newPath = s"$basePath/new"
  val srcPath = s"$basePath/source"
  val srcPostsPath = s"$srcPath/posts"
  val targetPath = s"$basePath/target"
  val expectedPath = s"$basePath/expected"


  val firstPostName = "Blog-post-Sample-One-2015-06-15-03-45"
  val secondPostName = "Blog-post-Sample-Two-2015-07-03-23-45"

  val htmlExt = ".html"
  val mdExt = ".md"

  val firstPostMdName = s"$firstPostName$mdExt"
  val secondPostMdName = s"$secondPostName$mdExt"
  val firstPostHtmlName = s"$firstPostName$htmlExt"
  val secondPostHtmlName = s"$secondPostName$htmlExt"

  before {  }
  
  test("Scalatic - Test static blog generation") {

    Scalatic.main(Array(basePath))

    assert(Files.exists(Paths.get(s"$targetPath/normalize.css")))
    assert(Files.exists(Paths.get(s"$targetPath/github-markdown.css")))
    assert(Files.exists(Paths.get(s"$targetPath/github-light.css")))
    assert(Files.exists(Paths.get(s"$targetPath/cayman.css")))
    assert(Files.exists(Paths.get(s"$targetPath/index.html")))
    assert(Files.exists(Paths.get(s"$targetPath/$firstPostHtmlName")))
    assert(Files.exists(Paths.get(s"$targetPath/$secondPostHtmlName")))

    assert(Files.exists(Paths.get(s"$srcPostsPath/$firstPostMdName")))
    assert(Files.exists(Paths.get(s"$srcPostsPath/$secondPostMdName")))

    assert(!Files.exists(Paths.get(s"$newPath/$firstPostMdName")))
    assert(!Files.exists(Paths.get(s"$newPath/$secondPostMdName")))

    assertResult(
      expected = stringFromFile(s"$expectedPath/index.html"))(
        actual = stringFromFile(s"$targetPath/index.html"))
    assertResult(
      expected = stringFromFile(s"$expectedPath/$firstPostHtmlName"))(
      actual = stringFromFile(s"$targetPath/$firstPostHtmlName"))
    assertResult(
      expected = stringFromFile(s"$expectedPath/$secondPostHtmlName"))(
        actual = stringFromFile(s"$targetPath/$secondPostHtmlName"))

    // put'em back
    Files.move(
      Paths.get(s"$srcPostsPath/$firstPostMdName"),
      Paths.get(s"$newPath/$firstPostMdName"))
    Files.move(
      Paths.get(s"$srcPostsPath/$secondPostMdName"),
      Paths.get(s"$newPath/$secondPostMdName"))

    // delete the source/posts and target folders
    deleteFolder(s"$basePath/target")
    deleteFolder(s"$basePath/source/posts")
  }
  
  after {  }
}

object ScalaticSuite {
  def deleteFolder(folderPath: String) = {
    val folder = Paths.get(folderPath)
    Files.walkFileTree(folder, new SimpleFileVisitor[Path]() {
      override def visitFile(
        file: Path,
        attrs: BasicFileAttributes)
      : FileVisitResult = {
        Files.delete(file)
        FileVisitResult.CONTINUE
      }

      override def postVisitDirectory(
        dir: Path,
        exc: IOException)
      : FileVisitResult = {
        Files.delete(dir)
        FileVisitResult.CONTINUE
      }

    })
  }
}


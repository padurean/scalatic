import java.nio.file.{Files, Paths}

import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}

class ScalaticSuite extends FunSuite with BeforeAndAfter with ScalaFutures {
  implicit override val patienceConfig =
    PatienceConfig(timeout = Span(5, Seconds))

  val basePath = "src/test/scala/scalatictest"

  before {  }
  
  test("Scalatic - Test static blog generation") {

    Scalatic.main(Array(basePath))
    assert(Files.exists(Paths.get(s"$basePath/target/github-markdown.css")))
    assert(Files.exists(Paths.get(s"$basePath/target/index.html")))
    assert(Files.exists(Paths.get(s"$basePath/target/my-first-post.html")))
    assert(Files.exists(Paths.get(s"$basePath/target/my-second-post.html")))
    assert(Files.exists(Paths.get(s"$basePath/source/posts/my-first-post.md")))
    assert(Files.exists(Paths.get(s"$basePath/source/posts/my-second-post.md")))
    assert(!Files.exists(Paths.get(s"$basePath/new/my-first-post.md")))
    assert(!Files.exists(Paths.get(s"$basePath/new/my-second-post.md")))

    // put'em back and clean the target
    Files.move(
      Paths.get(s"$basePath/source/posts/my-first-post.md"),
      Paths.get(s"$basePath/new/my-first-post.md"))
    Files.move(
      Paths.get(s"$basePath/source/posts/my-second-post.md"),
      Paths.get(s"$basePath/new/my-second-post.md"))
    Files.delete(Paths.get(s"$basePath/target/my-second-post.html"))
    Files.delete(Paths.get(s"$basePath/target/my-first-post.html"))
    Files.delete(Paths.get(s"$basePath/target/index.html"))
    Files.delete(Paths.get(s"$basePath/target/github-markdown.css"))

    // delete the source/posts and target folders
    Files.delete(Paths.get(s"$basePath/source/posts"))
    Files.delete(Paths.get(s"$basePath/target"))
  }
  
  after {  }
}


import java.nio.file.{Files, Paths}

import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}

class ScalaticSuite extends FunSuite with BeforeAndAfter with ScalaFutures {
  implicit override val patienceConfig = PatienceConfig(timeout = Span(5, Seconds))

  val basePath = "src/test/scala/scalatictest"

  before {  }
  
  test("Scalatic - Test static blog generation") {

    Scalatic.main(Array(basePath))
    assert(Files.exists(Paths.get(s"$basePath/target/github-markdown.css")))
    assert(Files.exists(Paths.get(s"$basePath/target/myFirstPost.html")))
    assert(Files.exists(Paths.get(s"$basePath/target/mySecondPost.html")))
    assert(Files.exists(Paths.get(s"$basePath/source/posts/myFirstPost.md")))
    assert(Files.exists(Paths.get(s"$basePath/source/posts/mySecondPost.md")))
    assert(!Files.exists(Paths.get(s"$basePath/new/myFirstPost.md")))
    assert(!Files.exists(Paths.get(s"$basePath/new/mySecondPost.md")))

    // put'em back and clean the target
    Files.move(
      Paths.get(s"$basePath/source/posts/myFirstPost.md"),
      Paths.get(s"$basePath/new/myFirstPost.md"))
    Files.move(
      Paths.get(s"$basePath/source/posts/mySecondPost.md"),
      Paths.get(s"$basePath/new/mySecondPost.md"))
    Files.delete(Paths.get(s"$basePath/target/myFirstPost.html"))
    Files.delete(Paths.get(s"$basePath/target/mySecondPost.html"))
    Files.delete(Paths.get(s"$basePath/target/github-markdown.css"))

    // delete the source/posts and target folders
    Files.delete(Paths.get(s"$basePath/source/posts"))
    Files.delete(Paths.get(s"$basePath/target"))
  }
  
  after {  }
}


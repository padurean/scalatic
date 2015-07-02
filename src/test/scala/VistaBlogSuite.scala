import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import java.io.File
import java.nio.file.{Files, Paths}

class VistaBlogSuite extends FunSuite with BeforeAndAfter with ScalaFutures {
  implicit override val patienceConfig = PatienceConfig(timeout = Span(5, Seconds))

  val basePath = "src/test/scala/vistablogtest"

  before {  }
  
  test("Vista Blog - Test static blog generation") {

    VistaBlog.main(Array(basePath))
    assert(new File(s"$basePath/target/github-markdown.css").exists())
    assert(new File(s"$basePath/target/myFirstPost.html").exists())
    assert(new File(s"$basePath/target/mySecondPost.html").exists())
    assert(new File(s"$basePath/source/posts/myFirstPost.md").exists())
    assert(new File(s"$basePath/source/posts/mySecondPost.md").exists())
    assert(!new File(s"$basePath/new/myFirstPost.md").exists())
    assert(!new File(s"$basePath/new/mySecondPost.md").exists())

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
  }
  
  after {  }
}


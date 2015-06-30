import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}

class VistaBlogSuite extends FunSuite with BeforeAndAfter with ScalaFutures {
  implicit override val patienceConfig = PatienceConfig(timeout = Span(5, Seconds))

  val pref = "Vista Blog"

  before {  }
  
  test(s"$pref - Test 1") {
    VistaBlogMain.main(Array("Ionutzzz"))
    assert(true)
  }

  test(s"$pref - Test 2") {
    VistaBlogMain.main(Array("Cokartzzzz"))
    assert(3-2 == 1)
  }
  
  after {  }
}


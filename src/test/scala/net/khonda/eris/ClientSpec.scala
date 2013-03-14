import org.specs2.mutable.Specification

class ClientSpec extends Specification {
  "abc" should {
    "start with a" in {
      "abc".startsWith("a") mustEqual (true)
    }
  }
}

// See LICENSE for license details.

package chiselTests

class ModuleExplicitResetSpec extends ChiselFlatSpec  {

  "A Module with an explicit reset in compatibility mode" should "elaborate" in {
    import Chisel._
    val myReset = Bool(true)
    class ModuleExplicitReset(reset: Bool) extends Module(_reset = reset) {
      val io = new Bundle {
        val done = Bool(OUTPUT)
      }

      io.done := Bool(false)
    }

    elaborate {
      new ModuleExplicitReset(myReset)
    }
  }

  "A Module with an explicit reset in non-compatibility mode" should "elaborate" in {
    import chisel3._
    val myReset = Bool(true)
    class ModuleExplicitReset(reset: Bool) extends Module(_reset = reset) {
      val io = IO(new Bundle {
        val done = Bool(OUTPUT)
      })

      io.done := Bool(false)
    }

    elaborate {
      new ModuleExplicitReset(myReset)
    }
  }
}

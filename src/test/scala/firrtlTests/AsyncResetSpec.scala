// See LICENSE for license details.

package firrtlTests

import firrtl._
import firrtl.ir._
import FirrtlCheckers._

class AsyncResetSpec extends FirrtlFlatSpec {
  def compile(input: String): CircuitState =
    (new VerilogCompiler).compileAndEmit(CircuitState(parse(input), ChirrtlForm), List.empty)
  def compileBody(body: String) = {
    val str = """
      |circuit Test :
      |  module Test :
      |""".stripMargin + body.split("\n").mkString("    ", "\n    ", "")
    compile(str)
  }

  "AsyncReset" should "generate async-reset always blocks" in {
    val result = compileBody(s"""
      |input clock : Clock
      |input reset : AsyncReset
      |input x : UInt<8>
      |output z : UInt<8>
      |reg r : UInt<8>, clock with : (reset => (reset, UInt(123)))
      |r <= x
      |z <= r""".stripMargin
    )
    result should containLine ("always @(posedge clock or posedge reset) begin")
  }

  it should "support casting to other types" in {
    val result = compileBody(s"""
      |input a : AsyncReset
      |output v : UInt<1>
      |output w : SInt<1>
      |output x : Clock
      |output y : Fixed<1><<0>>
      |output z : AsyncReset
      |v <= asUInt(a)
      |w <= asSInt(a)
      |x <= asClock(a)
      |y <= asFixedPoint(a, 0)
      |z <= asAsyncReset(a)""".stripMargin
    )
    result should containLine ("assign v = $unsigned(a);")
    result should containLine ("assign w = $signed(a);")
    result should containLine ("assign x = a;")
    result should containLine ("assign y = $signed(a);")
    result should containLine ("assign z = a;")
  }

  "Other types" should "support casting to AsyncReset" in {
    val result = compileBody(s"""
      |input a : UInt<1>
      |input b : SInt<1>
      |input c : Clock
      |input d : Fixed<1><<0>>
      |input e : AsyncReset
      |output v : AsyncReset
      |output w : AsyncReset
      |output x : AsyncReset
      |output y : AsyncReset
      |output z : AsyncReset
      |v <= asAsyncReset(a)
      |w <= asAsyncReset(a)
      |x <= asAsyncReset(a)
      |y <= asAsyncReset(a)
      |z <= asAsyncReset(a)""".stripMargin
    )
    result should containLine ("assign v = a;")
    result should containLine ("assign w = a;")
    result should containLine ("assign x = a;")
    result should containLine ("assign y = a;")
    result should containLine ("assign z = a;")
  }

  "Non-literals" should "NOT be allowed as reset values for AsyncReset" in {
    an [passes.CheckHighForm.NonLiteralAsyncResetValueException] shouldBe thrownBy {
      compileBody(s"""
        |input clock : Clock
        |input reset : AsyncReset
        |input x : UInt<8>
        |input y : UInt<8>
        |output z : UInt<8>
        |reg r : UInt<8>, clock with : (reset => (reset, y))
        |r <= x
        |z <= r""".stripMargin
      )
    }
  }


  "Every combination of clock and async reset" should "generate their own always block" in {
    val result = compileBody(s"""
      |input clock0 : Clock
      |input clock1 : Clock
      |input syncReset : UInt<1>
      |input reset0 : AsyncReset
      |input reset1 : AsyncReset
      |input x : UInt<8>[6]
      |output z : UInt<8>[6]
      |reg r0 : UInt<8>, clock0 with : (reset => (syncReset, UInt(123)))
      |reg r1 : UInt<8>, clock1 with : (reset => (syncReset, UInt(123)))
      |reg r2 : UInt<8>, clock0 with : (reset => (reset0, UInt(123)))
      |reg r3 : UInt<8>, clock0 with : (reset => (reset1, UInt(123)))
      |reg r4 : UInt<8>, clock1 with : (reset => (reset0, UInt(123)))
      |reg r5 : UInt<8>, clock1 with : (reset => (reset1, UInt(123)))
      |r0 <= x[0]
      |r1 <= x[1]
      |r2 <= x[2]
      |r3 <= x[3]
      |r4 <= x[4]
      |r5 <= x[5]
      |z[0] <= r0
      |z[1] <= r1
      |z[2] <= r2
      |z[3] <= r3
      |z[4] <= r4
      |z[5] <= r5""".stripMargin
    )
    result should containLine ("always @(posedge clock0) begin")
    result should containLine ("always @(posedge clock1) begin")
    result should containLine ("always @(posedge clock0 or posedge reset0) begin")
    result should containLine ("always @(posedge clock1 or posedge reset0) begin")
    result should containLine ("always @(posedge clock0 or posedge reset1) begin")
    result should containLine ("always @(posedge clock1 or posedge reset1) begin")
  }

}

class AsyncResetExecutionTest extends ExecutionTest("AsyncResetTester", "/features")


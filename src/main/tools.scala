package loongarch32i
import chisel3._
import chisel3.util._
import Myconsts._

class decoderBundle(inWidth : Int, outWidth : Int) extends Bundle {
    val in = Input(UInt(inWidth.W))
    val out = Output(UInt(outWidth.W))
}
class decoder2_4 extends Module {
  val io = IO(new Bundle {
    val in = Input(UInt(2.W))
    val out = Output(UInt(4.W))
  })

  val decoderOut = Wire(Vec(4, UInt(1.W)))
  decoderOut := VecInit(Seq.tabulate(4)(i => (io.in === i.U)))

  io.out := Cat(decoderOut.reverse)
}

class decoder4_16 extends Module {
  val io = IO(new Bundle {
    val in = Input(UInt(4.W))
    val out = Output(UInt(16.W))
  })

  val decoderOut = Wire(Vec(16, UInt(1.W)))
  decoderOut := VecInit(Seq.tabulate(16)(i => (io.in === i.U)))

  io.out := Cat(decoderOut.reverse)
}

class decoder5_32 extends Module {
  val io = IO(new Bundle {
    val in = Input(UInt(5.W))
    val out = Output(UInt(32.W))
  })

  val decoderOut = Wire(Vec(32, UInt(1.W)))
  decoderOut := VecInit(Seq.tabulate(32)(i => (io.in === i.U)))

  io.out := Cat(decoderOut.reverse)
}

class decoder6_64 extends Module {
  val io = IO(new Bundle {
    val in = Input(UInt(6.W))
    val out = Output(UInt(64.W))
  })

  val decoderOut = Wire(Vec(64, UInt(1.W)))
  decoderOut := VecInit(Seq.tabulate(64)(i => (io.in === i.U)))

  io.out := Cat(decoderOut.reverse)
}
/*
class SignExtender(originalWidth: Int, extendedWidth: Int) extends Module {
  require(originalWidth > 0 && extendedWidth > originalWidth, "Widths must be positive and extended width must be greater than original width")
  val io = IO(new Bundle {
    val original = Input(UInt(originalWidth.W))
    val extended = Output(UInt(extendedWidth.W))
  })
  val paddingWidth = extendedWidth - originalWidth
  val signExtended = Cat(Fill(paddingWidth, io.original(originalWidth - 1)), io.original)
  io.extended := signExtended
}*/
package loongarch32i

import chisel3._ 
import chisel3.util._ 
import Myconsts._ 

class SignedDiv extends BlackBox with HasBlackBoxResource {
  val io = IO(new Bundle {
    val aclk = Input(Clock())

    val s_axis_divisor_tvalid = Input(Bool())
    val s_axis_divisor_tready = Output(Bool())
    val s_axis_divisor_tdata  = Input(UInt(dataBitWidth.W))
    
    val s_axis_dividend_tvalid = Input(Bool())
    val s_axis_dividend_tready = Output(Bool())
    val s_axis_dividend_tdata  = Input(UInt(dataBitWidth.W))
    
    val m_axis_dout_tvalid = Output(Bool())
    val m_axis_dout_tdata  = Output(UInt(hiLoBitWidth.W))
  })
}

class UnsignedDiv extends BlackBox with HasBlackBoxResource {
  val io = IO(new Bundle {
    val aclk = Input(Clock())
    
    val s_axis_divisor_tvalid = Input(Bool())
    val s_axis_divisor_tready = Output(Bool())
    val s_axis_divisor_tdata  = Input(UInt(dataBitWidth.W))
    
    val s_axis_dividend_tvalid = Input(Bool())
    val s_axis_dividend_tready = Output(Bool())
    val s_axis_dividend_tdata  = Input(UInt(dataBitWidth.W))
    
    val m_axis_dout_tvalid = Output(Bool())
    val m_axis_dout_tdata  = Output(UInt(hiLoBitWidth.W))
  })
}

class Div extends Module {
  val io = IO(new Bundle {
    val src1        = Input(UInt(dataBitWidth.W))
    val src2        = Input(UInt(dataBitWidth.W))
    val signed      = Input(Bool())
    val start       = Input(Bool())
    val allow_to_go = Input(Bool())

    val ready  = Output(Bool())
    val result = Output(UInt(hiLoBitWidth.W))
  })

  if (BUILD) {
    val signedDiv   = Module(new SignedDiv()).io
    val unsignedDiv = Module(new UnsignedDiv()).io

    signedDiv.aclk   := clock
    unsignedDiv.aclk := clock

    val unsignedDiv_sent = Seq.fill(2)(RegInit(false.B))
    val unsignedDiv_done = RegInit(false.B)
    val signedDiv_sent   = Seq.fill(2)(RegInit(false.B))
    val signedDiv_done   = RegInit(false.B)

    when(unsignedDiv.s_axis_dividend_tready && unsignedDiv.s_axis_dividend_tvalid) {
      unsignedDiv_sent(0) := true.B
    }.elsewhen(io.ready && io.allow_to_go) {
      unsignedDiv_sent(0) := false.B
    }
    when(unsignedDiv.s_axis_divisor_tready && unsignedDiv.s_axis_divisor_tvalid) {
      unsignedDiv_sent(1) := true.B
    }.elsewhen(io.ready && io.allow_to_go) {
      unsignedDiv_sent(1) := false.B
    }

    when(signedDiv.s_axis_dividend_tready && signedDiv.s_axis_dividend_tvalid) {
      signedDiv_sent(0) := true.B
    }.elsewhen(io.ready && io.allow_to_go) {
      signedDiv_sent(0) := false.B
    }
    when(signedDiv.s_axis_divisor_tready && signedDiv.s_axis_divisor_tvalid) {
      signedDiv_sent(1) := true.B
    }.elsewhen(io.ready && io.allow_to_go) {
      signedDiv_sent(1) := false.B
    }

    when(signedDiv.m_axis_dout_tvalid && !io.allow_to_go) {
      signedDiv_done := true.B
    }.elsewhen(io.allow_to_go) {
      signedDiv_done := false.B
    }

    when(unsignedDiv.m_axis_dout_tvalid && !io.allow_to_go) {
      unsignedDiv_done := true.B
    }.elsewhen(io.allow_to_go) {
      unsignedDiv_done := false.B
    }
    signedDiv.s_axis_dividend_tvalid := io.start && !signedDiv_sent(0) && io.signed
    signedDiv.s_axis_divisor_tvalid  := io.start && !signedDiv_sent(1) && io.signed

    unsignedDiv.s_axis_dividend_tvalid := io.start && !unsignedDiv_sent(0) && !io.signed
    unsignedDiv.s_axis_divisor_tvalid  := io.start && !unsignedDiv_sent(1) && !io.signed

    signedDiv.s_axis_dividend_tdata := io.src1
    signedDiv.s_axis_divisor_tdata  := io.src2

    unsignedDiv.s_axis_dividend_tdata := io.src1
    unsignedDiv.s_axis_divisor_tdata  := io.src2

    io.ready := Mux(
      io.signed,
      signedDiv.m_axis_dout_tvalid || signedDiv_done,
      unsignedDiv.m_axis_dout_tvalid || unsignedDiv_done,
    )
    val signedRes =
      Cat(signedDiv.m_axis_dout_tdata(dataBitWidth - 1, 0), signedDiv.m_axis_dout_tdata(hiLoBitWidth - 1, dataBitWidth))
    val unsignedRes =
      Cat(unsignedDiv.m_axis_dout_tdata(dataBitWidth - 1, 0), unsignedDiv.m_axis_dout_tdata(hiLoBitWidth - 1, dataBitWidth))
    io.result := Mux(io.signed, signedRes, unsignedRes)
  } else {
    val cnt = RegInit(0.U(log2Ceil(divClkNum + 1).W))
    cnt := MuxCase(
      cnt,
      Seq(
        (io.start && !io.ready) -> (cnt + 1.U),
        io.allow_to_go          -> 0.U,
      ),
    )

    val div_signed = io.signed

    val dividend_signed = io.src1(31) & div_signed
    val divisor_signed  = io.src2(31) & div_signed

    val dividend_abs = Mux(dividend_signed, (-io.src1).asUInt, io.src1.asUInt)
    val divisor_abs  = Mux(divisor_signed, (-io.src2).asUInt, io.src2.asUInt)

    val quotient_signed  = (io.src1(31) ^ io.src2(31)) & div_signed
    val remainder_signed = io.src1(31) & div_signed

    val quotient_abs  = dividend_abs / divisor_abs
    val remainder_abs = dividend_abs - quotient_abs * divisor_abs

    val quotient  = RegInit(0.S(32.W))
    val remainder = RegInit(0.S(32.W))

    when(io.start) {
      quotient  := Mux(quotient_signed, (-quotient_abs).asSInt, quotient_abs.asSInt)
      remainder := Mux(remainder_signed, (-remainder_abs).asSInt, remainder_abs.asSInt)
    }

    io.ready  := cnt >= divClkNum.U
    io.result := Cat(remainder, quotient)
  }
}
package loongarch32i

import chisel3._
import chisel3.util._
import Myconsts._

class SignedMul extends BlackBox with HasBlackBoxResource {
    val io = IO(new Bundle{
        val CLK = Input(Clock())
        val CE  = Input(Bool())
        val A   = Input(UInt((dataBitWidth + 1).W))
        val B   = Input(UInt((dataBitWidth + 1).W))

        val P   =Output(UInt((hiLoBitWidth + 2).W))
    })
}
class Mul extends Module {
    val io = IO(new Bundle{
        val src1 = Input(UInt(dataBitWidth.W))
        val src2 = Input(UInt(dataBitWidth.W))
        val signed  = Input(Bool())
        val start   = Input(Bool())
        val allow_to_go = Input(Bool())
        val ready   = Output(Bool())
        val result  = Output(UInt(hiLoBitWidth.W))
    })

    if (BUILD){
        val signedMul = Module(new SignedMul()).io
        val cnt = RegInit(0.U(log2Ceil(mulClkNum + 1).W))
        cnt := MuxCase(
            cnt,
            Seq(
                (io.start && !io.ready) -> (cnt + 1.U),
                io.allow_to_go          -> 0.U
            )
        )

        signedMul.CLK := clock 
        signedMul.CE  := io.start
        when(io.signed) {
            signedMul.A := Cat(io.src1(dataBitWidth - 1), io.src1)
            signedMul.B := Cat(io.src2(dataBitWidth - 1), io.src2)
        }.otherwise {
            signedMul.A := Cat(0.U, io.src1)
            signedMul.B := Cat(0.U, io.src2)
        } 
        io.ready := cnt >= mulClkNum.U 
        io.result := signedMul.P(hiLoBitWidth - 1, 0)
    }else{
        val cnt = RegInit(0.U(log2Ceil(mulClkNum + 1).W))
        cnt := MuxCase(
            cnt,
            Seq(
                (io.start && !io.ready) -> (cnt + 1.U),
                io.allow_to_go          -> 0.U
            )
        )

        val signed   = RegInit(0.U(hiLoBitWidth.W))
        val unsigned = RegInit(0.U(hiLoBitWidth.W))

        when(io.start){
            signed   := (io.src1.asSInt * io.src2.asSInt).asUInt
            unsigned := io.src1 * io.src2
        }
        io.result := Mux(io.signed, signed, unsigned)
        io.ready := cnt >= mulClkNum.U
    }
}
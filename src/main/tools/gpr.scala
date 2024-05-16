package loongarch32i
import chisel3._
import chisel3.util._
import Myconsts._

class regFileIo extends Bundle {
    val raddr1 = Input(UInt(5.W))
    val rdata1 = Output(UInt(dataBitWidth.W))
    
    val raddr2 = Input(UInt(5.W))
    val rdata2 = Output(UInt(dataBitWidth.W))

    val we = Input(UInt(1.W))
    val waddr = Input(UInt(5.W))
    val wdata = Input(UInt(dataBitWidth.W))
}
class regFile extends Module {
    val io = IO(new regFileIo())
    val rf = RegInit(VecInit(Seq.fill(32)(0.U(dataBitWidth.W))))
    when (io.we === 1.U){
        rf(io.waddr) := io.wdata
    }.otherwise{
        //
    }
    io.rdata1 := Mux((io.raddr1 === 0.U), 0.U(dataBitWidth.W), rf(io.raddr1))
    io.rdata2 := Mux((io.raddr2 === 0.U), 0.U(dataBitWidth.W), rf(io.raddr2))
}
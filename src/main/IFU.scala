package loongarch32i

import chisel3._
import chisel3.util._
import Myconsts._

class IfuIn extends Bundle {
    val inst_sram_rdata = Input(UInt(dataBitWidth.W))
}
class IfuMessage extends Bundle {
    val inst = Output(UInt(instBitWidth.W))
    val pc = Output(UInt(addrBitWidth.W))
}
class IFU extends Module {
    val io = IO(new Bundle{
        val in = new IfuIn
        val brTaken = Input(UInt(1.W))
        val brTarget = Input(UInt(32.W))
        val inst_sram_en = Output(UInt(1.W))
        val inst_sram_we = Output(UInt(4.W))
        val inst_sram_addr = Output(UInt(addrBitWidth.W))
        val inst_sram_wdata = Output(UInt(dataBitWidth.W))
        val out = Decoupled(new IfuMessage)
    })

    val snPc = Wire(UInt(addrBitWidth.W))
    val dnPc = Wire(UInt(addrBitWidth.W))
    val pc = RegInit(PCStart.U(addrBitWidth.W))

    
    snPc := pc + "h4".U;
    dnPc := Mux(io.brTaken === 1.U, io.brTarget, snPc)
    
    when(reset === true.B){
        io.out.valid := false.B
        pc := "h1bfffffc".U(addrBitWidth.W)
    }.otherwise{
        when (io.out.ready){
            if_id_inst := io.in.inst_sram_rdata
            if_id_pc := dnPc
            io.out.valid := true.B
        }.otherwise{
            io.out.valid := false.B
        }
    }

    val if_id_inst = RegInit(UInt(instBitWidth.W))
    val if_id_pc = RegInit(UInt(addrBitWidth.W))
    
    //io.out.ready := !io.out.valid || (io.out.valid & io.in.ready)

    io.inst_sram_en := io.out.ready
    io.inst_sram_we := Fill(4, 0.U)
    io.inst_sram_addr := dnPc
    io.inst_sram_wdata := Fill(32, 0.U)

    io.out.bits.inst := if_id_inst
    io.out.bits.pc := if_id_pc
}
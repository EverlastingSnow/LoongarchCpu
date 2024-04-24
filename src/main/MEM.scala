package loongarch32i

import chisel3._
import chisel3.util._
import Myconsts._

class MemMessage extends Bundle {
    val finalRes = Output(UInt(dataBitWidth.W))
    val grWe = Output(UInt(1.W))
    val dest = Output(UInt(5.W))
    val pc = Output(UInt(addrBitWidth.W))
}
class MEM extends Module {
    val io = IO(new Bundle{
        val in = Flipped(Decoupled(new ExuMessage))
        val data_sram_rdata = Input(UInt(dataBitWidth.W))
        val out = Decoupled(new MemMessage)
    })

    val mem_wb_pc = RegInit(UInt(addrBitWidth.W))
    val mem_wb_grWe = RegInit(UInt(1.W))
    val mem_wb_dest = RegInit(UInt(5.W))
    val mem_wb_finalRes = RegInit(UInt(dataBitWidth.W))    
    val valid = RegInit(false.B)
    
    io.out.bits.finalRes := mem_wb_finalRes
    io.out.bits.grWe := mem_wb_grWe
    io.out.bits.dest := mem_wb_dest
    io.out.bits.pc := mem_wb_pc
    val dataReceived = RegInit(false.B)
    when (io.in.valid && ~dataReceived && reset === false.B){
        mem_wb_finalRes := Mux(io.in.bits.resFromMem === 1.U, io.data_sram_rdata, io.in.bits.aluRes)
        mem_wb_grWe := io.in.bits.grWe
        mem_wb_dest := io.in.bits.dest
        mem_wb_pc := io.in.bits.pc
        dataReceived := true.B
    }.elsewhen(io.out.ready && reset === false.B){        
        dataReceived := false.B
    }

    io.in.ready := !dataReceived && io.out.ready
    io.out.valid := dataReceived && io.in.valid
}
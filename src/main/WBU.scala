package loongarch32i

import chisel3._
import chisel3.util._
import Myconsts._

class WBU extends Module {
    val io = IO(new Bundle{
        val in = Flipped(Decoupled(new MemMessage))
        val debug_wb_pc = Output(UInt(addrBitWidth.W))
        val debug_wb_rf_we = Output(UInt(4.W))
        val debug_wb_rf_wnum = Output(UInt(5.W))
        val debug_wb_rf_wdata = Output(UInt(dataBitWidth.W))
        val rfWe = Output(UInt(1.W))
        val rfWaddr = Output(UInt(5.W))
        val rfWdata = Output(UInt(dataBitWidth.W))
    })
    
    val rfWe = Wire(UInt(1.W))
    val rfWaddr = Wire(UInt(5.W))
    val rfWdata = Wire(UInt(dataBitWidth.W))
    rfWe := Mux(io.in.bits.grWe === 1.U && io.in.valid, 1.U, 0.U)
    rfWaddr := io.in.bits.dest
    rfWdata := io.in.bits.finalRes

    when(io.in.valid){
        io.rfWe := rfWe
        io.rfWaddr := rfWaddr
        io.rfWdata := rfWdata

        io.debug_wb_pc := io.in.bits.pc
        io.debug_wb_rf_we := Fill(4, rfWe)
        io.debug_wb_rf_wnum := io.in.bits.dest
        io.debug_wb_rf_wdata := io.in.bits.finalRes
    }
    io.in.ready := true.B

    // io.in.ready := true.B
    // io.out.valid := true.B 
    // io.out.ready := !io.out.valid || (io.out.valid & io.in.ready)
    // when (reset === true.B){
    //     io.out.valid := false.B
    // }.elsewhen(io.in.valid === true.B){
    //     io.out.valid := io.out.ready
    // }.otherwise{
        
    // }
}
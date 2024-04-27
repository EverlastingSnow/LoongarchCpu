package loongarch32i

import chisel3._
import chisel3.util._
import Myconsts._

class WBU extends Module {
    val io = IO(new Bundle{
        val in = Flipped(new MemMessage())
        val debug = new debug_info()
        val rfWe = Output(UInt(1.W))
        val rfWaddr = Output(UInt(5.W))
        val rfWdata = Output(UInt(dataBitWidth.W))
        val wbu_allowin = Output(Bool())
        val data_sram_rdata = Input(UInt(dataBitWidth.W))
    })
    
    val wbu_ready_go = true.B 
    val wbu_valid = RegInit(false.B)
    val wbu_allowin = (~wbu_valid) || (wbu_ready_go)
    when(wbu_allowin) {wbu_valid := io.in.valid}
    io.wbu_allowin := wbu_allowin

    val mem_wb_pc = Reg(UInt(addrBitWidth.W))
    val mem_wb_grWe = Reg(UInt(1.W))
    val mem_wb_dest = Reg(UInt(5.W))
    val mem_wb_finalRes = Reg(UInt(dataBitWidth.W))   
    when (wbu_allowin && io.in.valid){
        mem_wb_finalRes := Mux(io.in.resFromMem === 1.U, io.data_sram_rdata, io.in.aluRes)
        mem_wb_grWe := io.in.grWe
        mem_wb_dest := io.in.dest
        mem_wb_pc := io.in.pc

        // io.rfWe := rfWe
        // io.rfWaddr := rfWaddr
        // io.rfWdata := rfWdata

        // io.debug_wb_pc := io.in.pc
        // io.debug_wb_rf_we := Fill(4, rfWe)
        // io.debug_wb_rf_wnum := io.in.dest
        // io.debug_wb_rf_wdata := io.in.finalRes
    }
    val rfWe = Wire(UInt(1.W))
    val rfWaddr = Wire(UInt(5.W))
    val rfWdata = Wire(UInt(dataBitWidth.W))
    rfWe := Mux(mem_wb_grWe === 1.U, 1.U, 0.U)
    rfWaddr := mem_wb_dest
    rfWdata := mem_wb_finalRes

    io.rfWe := rfWe
    io.rfWaddr := rfWaddr
    io.rfWdata := rfWdata
    
    io.debug.debug_wb_pc := mem_wb_pc
    io.debug.debug_wb_rf_we := Fill(4, rfWe)
    io.debug.debug_wb_rf_wnum := mem_wb_dest
    io.debug.debug_wb_rf_wdata := mem_wb_finalRes
    //when(io.in.ready) {io.out.valid := io.in.valid}

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
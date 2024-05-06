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

    val mem_wb_pc = RegInit(0.U(addrBitWidth.W))
    val mem_wb_grWe = RegInit(0.U(1.W))
    val mem_wb_dest = RegInit(0.U(5.W))
    val mem_wb_resFromMem = RegInit(0.U(1.W))   
    val mem_wb_aluRes = RegInit(0.U(dataBitWidth.W))
    when (wbu_allowin && io.in.valid){
        mem_wb_resFromMem := io.in.resFromMem
        mem_wb_aluRes := io.in.aluRes
        mem_wb_grWe := io.in.grWe
        mem_wb_dest := io.in.dest
        mem_wb_pc := io.in.pc
    }
    val finalRes = Wire(UInt(dataBitWidth.W))
    finalRes := Mux(mem_wb_resFromMem === 1.U, 
    io.data_sram_rdata, mem_wb_aluRes)
    val rfWe = Wire(UInt(1.W))
    val rfWaddr = Wire(UInt(5.W))
    val rfWdata = Wire(UInt(dataBitWidth.W))
    rfWe := Mux(mem_wb_grWe === 1.U, 1.U, 0.U)
    rfWaddr := mem_wb_dest
    rfWdata := finalRes

    io.rfWe := rfWe
    io.rfWaddr := rfWaddr
    io.rfWdata := rfWdata
    
    io.debug.debug_wb_pc := mem_wb_pc
    io.debug.debug_wb_rf_we := Fill(4, rfWe)
    io.debug.debug_wb_rf_wnum := mem_wb_dest
    io.debug.debug_wb_rf_wdata := finalRes
}
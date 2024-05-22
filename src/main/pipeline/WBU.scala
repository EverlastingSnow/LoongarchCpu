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
        val wbu_stop = Output(Bool())
        val foward = new foward_info()

        val in_csr = Flipped(new csr_op_info())
        val out_csr = new csr_info()    
    })
    
    val wbu_ready_go = true.B 
    val wbu_valid = RegInit(false.B)
    val wbu_allowin = (~wbu_valid) || (wbu_ready_go)
    when(wbu_allowin) {wbu_valid := io.in.valid}
    io.wbu_allowin := wbu_allowin

    val wr_wbu_reg_enable = wbu_allowin && io.in.valid
    val mem_wb = RegEnable(io.in, wr_wbu_reg_enable)
    val mem_wb_csr = RegEnable(io.in_csr, wr_wbu_reg_enable)

    val finalRes = Wire(UInt(dataBitWidth.W))
    val rfWe = Wire(UInt(1.W))
    val rfWaddr = Wire(UInt(5.W))
    val rfWdata = Wire(UInt(dataBitWidth.W))
    finalRes := mem_wb.aluRes
    
    rfWe     := Mux(mem_wb.grWe === 1.U, 1.U, 0.U)
    rfWaddr  := mem_wb.dest
    rfWdata  := Mux(mem_wb.resFrom === 2.U, io.out_csr.rdata, finalRes)

    io.rfWe    := rfWe & wbu_valid & (mem_wb.dest =/= 0.U).asUInt & (mem_wb_csr.excp === 0.U).asUInt
    io.rfWaddr := rfWaddr
    io.rfWdata := rfWdata

    io.foward.csr_choke :=  mem_wb.grWe & wbu_valid & (mem_wb_csr.waddr === 0x40.U).asUInt & (mem_wb_csr.excp === 0.U).asUInt
    io.foward.w_valid   := mem_wb.grWe & wbu_valid & (mem_wb.dest =/= 0.U).asUInt & (mem_wb_csr.excp === 0.U).asUInt
    io.foward.w_choke   := mem_wb.grWe & wbu_valid & (mem_wb.dest =/= 0.U).asUInt & (mem_wb.resFrom === 2.U)
    io.foward.waddr     := mem_wb.dest
    io.foward.wdata     := rfWdata

    io.debug.debug_wb_pc       := mem_wb.pc
    io.debug.debug_wb_rf_we    := Fill(4, rfWe & wbu_valid & (mem_wb.dest =/= 0.U).asUInt & (mem_wb_csr.excp === 0.U).asUInt)
    io.debug.debug_wb_rf_wnum  := mem_wb.dest
    io.debug.debug_wb_rf_wdata := rfWdata

    io.out_csr.badv     := mem_wb_csr.badv
    io.out_csr.badaddr  := mem_wb_csr.badaddr
    io.out_csr.excp     := mem_wb_csr.excp & Fill(2, wbu_valid.asUInt)
    io.out_csr.ecode    := mem_wb_csr.ecode
    io.out_csr.esubcode := mem_wb_csr.esubcode 
    io.out_csr.mask_we  := mem_wb_csr.mask_we
    io.out_csr.mask     := mem_wb_csr.mask
    io.out_csr.wen      := ((mem_wb_csr.wen === 1.U) && wbu_valid).asUInt
    io.out_csr.waddr    := mem_wb_csr.waddr 
    io.out_csr.wdata    := mem_wb_csr.wdata
    io.out_csr.raddr    := mem_wb_csr.raddr
    io.out_csr.pc       := mem_wb_csr.pc

    io.wbu_stop := wbu_valid && mem_wb_csr.excp =/= 0.U
}
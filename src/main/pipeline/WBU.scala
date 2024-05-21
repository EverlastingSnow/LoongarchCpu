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

    val mem_wb_pc = RegInit(0.U(addrBitWidth.W))
    val mem_wb_grWe = RegInit(0.U(1.W))
    val mem_wb_dest = RegInit(0.U(5.W))
    val mem_wb_aluRes = RegInit(0.U(dataBitWidth.W))

    val mem_wb_csrBadv     = RegInit(0.U(1.W))
    val mem_wb_csrBadaddr  = RegInit(0.U(addrBitWidth.W))
    val mem_wb_csrExcp     = RegInit(0.U(2.W))
    val mem_wb_csrEcode    = RegInit(0.U(6.W))
    val mem_wb_csrEsubcode = RegInit(0.U(9.W))
    val mem_wb_csrPc       = RegInit(0.U(addrBitWidth.W))
    val mem_wb_csrMaskWe   = RegInit(0.U(1.W))
    val mem_wb_csrMask     = RegInit(0.U(dataBitWidth.W))
    val mem_wb_csrWen      = RegInit(0.U(1.W))
    val mem_wb_csrWaddr    = RegInit(0.U(ctrlRegLen.W))
    val mem_wb_csrWdata    = RegInit(0.U(dataBitWidth.W))
    val mem_wb_csrRaddr    = RegInit(0.U(ctrlRegLen.W))
    val mem_wb_resFrom     = RegInit(0.U(resTypeLen.W))    
    when (wbu_allowin && io.in.valid){
        mem_wb_aluRes := io.in.aluRes
        mem_wb_grWe := io.in.grWe
        mem_wb_dest := io.in.dest
        mem_wb_pc := io.in.pc
        mem_wb_resFrom := io.in.resFrom

        mem_wb_csrBadv := io.in_csr.badv
        mem_wb_csrBadaddr:= io.in_csr.badaddr
        mem_wb_csrExcp := io.in_csr.excp
        mem_wb_csrEcode := io.in_csr.ecode
        mem_wb_csrEsubcode := io.in_csr.esubcode
        mem_wb_csrMaskWe := io.in_csr.mask_we
        mem_wb_csrMask := io.in_csr.mask
        mem_wb_csrWen := io.in_csr.wen
        mem_wb_csrWaddr := io.in_csr.waddr
        mem_wb_csrWdata := io.in_csr.wdata
        mem_wb_csrRaddr := io.in_csr.raddr
        mem_wb_csrPc    := io.in_csr.pc
    }
    val finalRes = Wire(UInt(dataBitWidth.W))
    finalRes := mem_wb_aluRes
    val rfWe = Wire(UInt(1.W))
    val rfWaddr = Wire(UInt(5.W))
    val rfWdata = Wire(UInt(dataBitWidth.W))
    rfWe := Mux(mem_wb_grWe === 1.U, 1.U, 0.U)
    rfWaddr := mem_wb_dest
    rfWdata := Mux(mem_wb_resFrom === 2.U, io.out_csr.rdata, finalRes)

    io.rfWe := rfWe & wbu_valid & (mem_wb_dest =/= 0.U).asUInt & (mem_wb_csrExcp === 0.U).asUInt
    io.rfWaddr := rfWaddr
    io.rfWdata := rfWdata

    io.foward.csr_choke :=  mem_wb_grWe & wbu_valid & (mem_wb_csrWaddr === 0x40.U).asUInt & (mem_wb_csrExcp === 0.U).asUInt
    io.foward.w_valid := mem_wb_grWe & wbu_valid & (mem_wb_dest =/= 0.U).asUInt & (mem_wb_csrExcp === 0.U).asUInt
    io.foward.w_choke := mem_wb_grWe & wbu_valid & (mem_wb_dest =/= 0.U).asUInt & (mem_wb_resFrom === 2.U)
    io.foward.waddr := mem_wb_dest
    io.foward.wdata := rfWdata

    io.debug.debug_wb_pc := mem_wb_pc
    io.debug.debug_wb_rf_we := Fill(4, rfWe & wbu_valid & (mem_wb_dest =/= 0.U).asUInt & (mem_wb_csrExcp === 0.U).asUInt)
    io.debug.debug_wb_rf_wnum := mem_wb_dest
    io.debug.debug_wb_rf_wdata := rfWdata

    io.out_csr.badv := mem_wb_csrBadv
    io.out_csr.badaddr := mem_wb_csrBadaddr
    io.out_csr.excp := mem_wb_csrExcp & Fill(2, wbu_valid.asUInt)
    io.out_csr.ecode := mem_wb_csrEcode
    io.out_csr.esubcode := mem_wb_csrEsubcode 
    io.out_csr.mask_we := mem_wb_csrMaskWe
    io.out_csr.mask := mem_wb_csrMask
    io.out_csr.wen := ((mem_wb_csrWen === 1.U) && wbu_valid).asUInt
    io.out_csr.waddr := mem_wb_csrWaddr 
    io.out_csr.wdata := mem_wb_csrWdata
    io.out_csr.raddr := mem_wb_csrRaddr
    io.out_csr.pc    := mem_wb_csrPc

    io.wbu_stop := wbu_valid && mem_wb_csrExcp =/= 0.U
}
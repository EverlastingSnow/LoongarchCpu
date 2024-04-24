package loongarch32i

import chisel3._
import chisel3.util._
import Myconsts._

class mycpuIO extends Bundle {
    //inst sram
    val inst_sram_en = Output(Bool())
    val inst_sram_we = Output(UInt(4.W))
    val inst_sram_addr = Output(UInt(addrBitWidth.W))
    val inst_sram_wdata = Output(UInt(instBitWidth.W))
    val inst_sram_rdata = Input(UInt(instBitWidth.W))
    //data sram
    val data_sram_en = Output(Bool())
    val data_sram_we = Output(UInt(4.W))
    val data_sram_addr = Output(UInt(addrBitWidth.W))
    val data_sram_wdata = Output(UInt(dataBitWidth.W))
    val data_sram_rdata = Input(UInt(dataBitWidth.W))
    //trace debug
    val debug_wb_pc = Output(UInt(addrBitWidth.W))
    val debug_wb_rf_we = Output(UInt(4.W))
    val debug_wb_rf_wnum = Output(UInt(5.W))
    val debug_wb_rf_wdata = Output(UInt(dataBitWidth.W))
}

class mycpu extends Module {
    val io = IO(new mycpuIO())
    
    //val valid = RegInit(true.B)

    val ifu = Module(new IFU)
    val idu = Module(new IDU)
    val exu = Module(new EXU)
    val mem = Module(new MEM)
    val wbu = Module(new WBU)
    idu.io.in <> ifu.io.out
    exu.io.in <> idu.io.out
    mem.io.in <> exu.io.out
    wbu.io.in <> mem.io.out

    ifu.io.in.inst_sram_rdata := io.inst_sram_rdata
    io.inst_sram_en := ifu.io.inst_sram_en
    io.inst_sram_we := ifu.io.inst_sram_we
    io.inst_sram_addr := ifu.io.inst_sram_addr
    io.inst_sram_wdata := ifu.io.inst_sram_wdata

    ifu.io.brTaken := idu.io.brTaken
    ifu.io.brTarget := idu.io.brTarget

    idu.io.rfWe := wbu.rfWe
    idu.io.rfWaddr := wbu.rfWaddr
    idu.io.rfWdata := wbu.rfWdata
   
    exu.io.data_sram_rdata := io.data_sram_rdata
    io.data_sram_en := exu.io.data_sram_en
    io.data_sram_we := exu.io.data_sram_we
    io.data_sram_addr := exu.io.data_sram_addr
    io.data_sram_wdata := exu.io.data_sram_wdata


    io.debug_wb_pc := wbu.io.debug_wb_pc
    io.debug_wb_rf_we := wbu.io.debug_wb_rf_we
    io.debug_wb_rf_wnum := wbu.io.debug_wb_rf_wnum
    io.debug_wb_rf_wdata := wbu.io.debug_wb_rf_wdata
}
package loongarch32i

import chisel3._
import chisel3.util._
import Myconsts._

class mycpuIO extends Bundle {
    //inst sram
    val inst = new inst_info()
    //data sram
    val data = new data_info()
    //trace debug
    val debug = new debug_info()
}

class mycpu extends Module {
    val io = IO(new mycpuIO())
    
    val valid = RegInit(true.B)

    val ifu = Module(new IFU)
    val idu = Module(new IDU)
    val exu = Module(new EXU)
    val mem = Module(new MEM)
    val wbu = Module(new WBU)
    idu.io.in <> ifu.io.out
    exu.io.in <> idu.io.out
    mem.io.in <> exu.io.out
    wbu.io.in <> mem.io.out

    ifu.io.idu_allowin <> idu.io.idu_allowin
    idu.io.exu_allowin <> exu.io.exu_allowin
    exu.io.mem_allowin <> mem.io.mem_allowin
    mem.io.wbu_allowin <> wbu.io.wbu_allowin

    ifu.io.inst <> io.inst
    // io.inst_sram_en := ifu.io.inst_sram_en
    // io.inst_sram_we := ifu.io.inst_sram_we
    // io.inst_sram_addr := ifu.io.inst_sram_addr
    // io.inst_sram_wdata := ifu.io.inst_sram_wdata
    ifu.io.br <> idu.io.br
    // ifu.io.brTaken := idu.io.brTaken
    // ifu.io.brTarget := idu.io.brTarget

    idu.io.rfWe := wbu.io.rfWe
    idu.io.rfWaddr := wbu.io.rfWaddr
    idu.io.rfWdata := wbu.io.rfWdata

    mem.io.data <> io.data
    // io.data_sram_en := exu.io.data_sram_en
    // io.data_sram_we := exu.io.data_sram_we
    // io.data_sram_addr := exu.io.data_sram_addr
    // io.data_sram_wdata := exu.io.data_sram_wdata

    //mem.io.data_sram_rdata := io.data_sram_rdata

    wbu.io.debug <> io.debug
    wbu.io.data_sram_rdata <> io.data.data_sram_rdata
    // io.debug_wb_pc := wbu.io.debug_wb_pc
    // io.debug_wb_rf_we := wbu.io.debug_wb_rf_we
    // io.debug_wb_rf_wnum := wbu.io.debug_wb_rf_wnum
    // io.debug_wb_rf_wdata := wbu.io.debug_wb_rf_wdata
}
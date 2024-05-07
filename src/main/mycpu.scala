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

    idu.io.exu_choke.w_valid <> exu.io.choke.w_valid
    idu.io.exu_choke.waddr <> exu.io.choke.waddr
    // idu.io.mem_w_valid <> mem.io.mem_w_valid
    // idu.io.mem_waddr <> mem.io.mem_waddr
    // idu.io.wbu_w_valid <> wbu.io.wbu_w_valid
    // idu.io.wbu_waddr <> wbu.io.wbu_waddr

    idu.io.exu_foward <> exu.io.foward
    idu.io.mem_foward <> mem.io.foward
    idu.io.wbu_foward <> wbu.io.foward

    ifu.io.inst <> io.inst
    ifu.io.br <> idu.io.br

    idu.io.rfWe := wbu.io.rfWe
    idu.io.rfWaddr := wbu.io.rfWaddr
    idu.io.rfWdata := wbu.io.rfWdata

    exu.io.data <> io.data

    wbu.io.debug <> io.debug
    
    mem.io.data_sram_rdata <> io.data.data_sram_rdata
}
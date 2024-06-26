package loongarch32i

import chisel3._
import chisel3.util._
import Myconsts._

class mycpuIO extends Bundle {
    val axi = new AXI_info()
    //trace debug
    val debug = new debug_info()
}

class mycpu extends Module {
    val io = IO(new mycpuIO())
    
    val data = Module(new DCACHE).io
    val inst = Module(new ICACHE).io
    val axi  = Module(new AXI).io

    val valid = RegInit(true.B)

    val ifu = Module(new IFU)
    val idu = Module(new IDU)
    val exu = Module(new EXU)
    val mem = Module(new MEM)
    val wbu = Module(new WBU)
    val csr = Module(new CSR)
    idu.io.in <> ifu.io.out
    exu.io.in <> idu.io.out
    mem.io.in <> exu.io.out
    wbu.io.in <> mem.io.out

    ifu.io.idu_allowin <> idu.io.idu_allowin
    idu.io.exu_allowin <> exu.io.exu_allowin
    exu.io.mem_allowin <> mem.io.mem_allowin
    mem.io.wbu_allowin <> wbu.io.wbu_allowin

    ifu.io.idu_stop <> idu.io.idu_stop
    idu.io.exu_stop <> exu.io.exu_stop
    exu.io.mem_stop <> mem.io.mem_stop
    mem.io.wbu_stop <> wbu.io.wbu_stop

    idu.io.out_csr <> exu.io.in_csr
    exu.io.out_csr <> mem.io.in_csr
    mem.io.out_csr <> wbu.io.in_csr

    idu.io.exu_choke.w_valid <> exu.io.choke.w_valid
    idu.io.exu_choke.waddr <> exu.io.choke.waddr

    idu.io.exu_foward <> exu.io.foward
    idu.io.mem_foward <> mem.io.foward
    idu.io.wbu_foward <> wbu.io.foward

    idu.io.Int_en <> csr.io.Int_en
    idu.io.ct     <> csr.io.ct

    ifu.io.inst <> inst.in 
    inst.icache <> axi.icache

    ifu.io.br <> idu.io.br
    ifu.io.pc_stop <> csr.io.pc_stop
    ifu.io.dnpc <> csr.io.dnpc

    idu.io.rfWe := wbu.io.rfWe
    idu.io.rfWaddr := wbu.io.rfWaddr
    idu.io.rfWdata := wbu.io.rfWdata

    exu.io.data <> data.in
    data.dcache <> axi.dcache

    wbu.io.debug <> io.debug
    wbu.io.out_csr <> csr.io.csr
    
    mem.io.data_sram_rdata <> data.in.rdata
    mem.io.data_sram_data_ok <> data.in.data_ok

    axi.axi <> io.axi
}
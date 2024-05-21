package loongarch32i

import chisel3._
import chisel3.util._
import Myconsts._

class inst_info extends Bundle{
    val inst_sram_en = Output(UInt(1.W))
    val inst_sram_we = Output(UInt(4.W))
    val inst_sram_addr = Output(UInt(addrBitWidth.W))
    val inst_sram_wdata = Output(UInt(dataBitWidth.W))
    val inst_sram_rdata = Input(UInt(dataBitWidth.W))
}
class data_info extends Bundle{
    val data_sram_en = Output(Bool())
    val data_sram_we = Output(UInt(4.W))
    val data_sram_addr = Output(UInt(addrBitWidth.W))
    val data_sram_wdata = Output(UInt(dataBitWidth.W))
    val data_sram_rdata = Input(UInt(dataBitWidth.W))
}
class debug_info extends Bundle{
    val debug_wb_pc = Output(UInt(addrBitWidth.W))
    val debug_wb_rf_we = Output(UInt(4.W))
    val debug_wb_rf_wnum = Output(UInt(5.W))
    val debug_wb_rf_wdata = Output(UInt(dataBitWidth.W))
}
class br_info extends Bundle{
    val brTaken = Input(UInt(1.W))
    val brTarget = Input(UInt(32.W))
}
class choke_info extends Bundle{
    val w_valid = Output(UInt(1.W))
    val waddr   = Output(UInt(5.W))
}
class foward_info extends Bundle{
    val csr_choke = Output(UInt(1.W))
    val w_valid = Output(UInt(1.W))
    val w_choke = Output(UInt(1.W))
    val waddr   = Output(UInt(5.W))
    val wdata   = Output(UInt(dataBitWidth.W))
}

class csr_op_info extends Bundle {
    val pc = Output(UInt(addrBitWidth.W))
    val ecode = Output(UInt(6.W))
    val esubcode = Output(UInt(9.W))
    val excp = Output(UInt(2.W))
    //val badv = Output(UInt(1.W))
    //val badaddr = Output(UInt(dataBitWidth.W))
    val mask_we = Output(UInt(1.W))
    val mask    = Output(UInt(dataBitWidth.W))
    val wen     = Output(UInt(1.W))
    val waddr   = Output(UInt(ctrlRegLen.W))
    val wdata   = Output(UInt(dataBitWidth.W))
    val raddr   = Output(UInt(ctrlRegLen.W))
    val badv    = Output(UInt(1.W))
    val badaddr = Output(UInt(addrBitWidth.W))
}

class csr_info extends csr_op_info{
    val rdata   = Input(UInt(dataBitWidth.W))
}

class counter_info extends Bundle {
    val counterH = Output(UInt(dataBitWidth.W))
    val counterL = Output(UInt(dataBitWidth.W))
    val counterID= Output(UInt(dataBitWidth.W))
}
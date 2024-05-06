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
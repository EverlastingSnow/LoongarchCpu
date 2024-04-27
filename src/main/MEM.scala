package loongarch32i

import chisel3._
import chisel3.util._
import Myconsts._

class MemMessage extends Bundle {
    val aluRes = Output(UInt(dataBitWidth.W))
    val grWe = Output(UInt(1.W))
    val dest = Output(UInt(5.W))
    val pc = Output(UInt(addrBitWidth.W))
    val resFromMem = Output(UInt(1.W))
    val valid = Output(Bool())
}
class MEM extends Module {
    val io = IO(new Bundle{
        val in = Flipped(new ExuMessage())
        val data = new data_info()
        val out = new MemMessage()
        val mem_allowin = Output(Bool())
        val wbu_allowin = Input(Bool())
    })

    // val mem_wb_pc = Reg(UInt(addrBitWidth.W))
    // val mem_wb_grWe = Reg(UInt(1.W))
    // val mem_wb_dest = Reg(UInt(5.W))
    // val mem_wb_finalRes = Reg(UInt(dataBitWidth.W))   
    val ex_me_pc = Reg(UInt(addrBitWidth.W))
    val ex_me_aluRes = Reg(UInt(dataBitWidth.W))
    val ex_me_resFromMem = Reg(UInt(1.W))
    val ex_me_grWe = Reg(UInt(1.W))
    val ex_me_dest = Reg(UInt(5.W)) 
    val ex_me_rfdata = Reg(UInt(dataBitWidth.W))
    val ex_me_memWe = Reg(UInt(1.W))
    //val valid = RegInit(false.B)

    val mem_ready_go = true.B 
    val mem_valid = RegInit(false.B)
    val mem_allowin = (~mem_valid) || (io.wbu_allowin && mem_ready_go)
    when(mem_allowin) {mem_valid := io.in.valid}
    io.out.valid := mem_ready_go && mem_valid
    io.mem_allowin := mem_allowin
    when (mem_allowin && io.in.valid){
        // mem_wb_finalRes := Mux(io.in.resFromMem === 1.U, io.data_sram_rdata, io.in.aluRes)
        // mem_wb_grWe := io.in.grWe
        // mem_wb_dest := io.in.dest
        // mem_wb_pc := io.in.pc
        ex_me_pc := io.in.pc
        ex_me_aluRes := io.in.aluRes
        ex_me_resFromMem := io.in.resFromMem
        ex_me_grWe := io.in.grWe
        ex_me_dest := io.in.dest
        ex_me_rfdata := io.in.rfdata
        ex_me_memWe := io.in.memWe
    }

    io.data.data_sram_en := 1.U 
    io.data.data_sram_we := Mux(ex_me_memWe === 1.U, Fill(4, 1.U), Fill(4, 0.U))
    io.data.data_sram_addr := ex_me_aluRes
    io.data.data_sram_wdata := ex_me_rfdata

    io.out.aluRes := ex_me_aluRes
    io.out.grWe := ex_me_grWe
    io.out.dest := ex_me_dest
    io.out.pc := ex_me_pc
    io.out.resFromMem := ex_me_resFromMem
}
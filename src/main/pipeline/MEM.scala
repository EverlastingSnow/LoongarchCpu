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
        val mem_w_valid = Output(UInt(1.W))
        val mem_waddr = Output(UInt(5.W))
    })
    
    val ex_me_pc = RegInit(0.U(addrBitWidth.W))
    val ex_me_aluRes = RegInit(0.U(dataBitWidth.W))
    val ex_me_resFromMem = RegInit(0.U(1.W))
    val ex_me_grWe = RegInit(0.U(1.W))
    val ex_me_dest = RegInit(0.U(5.W)) 
    val ex_me_rfdata = RegInit(0.U(dataBitWidth.W))
    val ex_me_memWe = RegInit(0.U(1.W))

    val mem_ready_go = true.B 
    val mem_valid = RegInit(false.B)
    val mem_allowin = (~mem_valid) || (io.wbu_allowin && mem_ready_go)
    when(mem_allowin) {mem_valid := io.in.valid}
    io.out.valid := mem_ready_go && mem_valid
    io.mem_allowin := mem_allowin
    when (mem_allowin && io.in.valid){
        ex_me_pc := io.in.pc
        ex_me_aluRes := io.in.aluRes
        ex_me_resFromMem := io.in.resFromMem
        ex_me_grWe := io.in.grWe
        ex_me_dest := io.in.dest
        ex_me_rfdata := io.in.rfdata
        ex_me_memWe := io.in.memWe
    }

    io.data.data_sram_en := 1.U 
    io.data.data_sram_we := Mux(ex_me_memWe === 1.U && mem_valid, Fill(4, 1.U), Fill(4, 0.U))
    io.data.data_sram_addr := ex_me_aluRes
    io.data.data_sram_wdata := ex_me_rfdata

    io.mem_w_valid := ex_me_grWe & mem_valid & (ex_me_dest =/= 0.U).asUInt    
    io.mem_waddr := ex_me_dest

    io.out.aluRes := ex_me_aluRes
    io.out.grWe := ex_me_grWe
    io.out.dest := ex_me_dest
    io.out.pc := ex_me_pc
    io.out.resFromMem := ex_me_resFromMem
}
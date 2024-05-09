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
        val out = new MemMessage()
        val mem_allowin = Output(Bool())
        val wbu_allowin = Input(Bool())
        val data_sram_rdata = Input(UInt(dataBitWidth.W))
        val foward = new foward_info()
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

    val finalRes = Wire(UInt(dataBitWidth.W))
    finalRes := Mux(ex_me_resFromMem === 1.U, io.data_sram_rdata, ex_me_aluRes)

    io.foward.w_valid := ex_me_grWe & mem_valid & (ex_me_dest =/= 0.U).asUInt
    io.foward.waddr := ex_me_dest
    io.foward.wdata := finalRes

    io.out.aluRes := finalRes
    io.out.grWe := ex_me_grWe
    io.out.dest := ex_me_dest
    io.out.pc := ex_me_pc
    io.out.resFromMem := ex_me_resFromMem
}
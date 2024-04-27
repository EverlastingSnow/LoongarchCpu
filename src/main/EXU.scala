package loongarch32i

import chisel3._
import chisel3.util._
import Myconsts._

class ExuMessage extends Bundle {
    val pc = Output(UInt(addrBitWidth.W))
    val aluRes = Output(UInt(dataBitWidth.W))
    val resFromMem = Output(UInt(1.W))
    val grWe = Output(UInt(1.W))    
    val dest = Output(UInt(5.W))
    val valid = Output(Bool())
    val rfdata = Output(UInt(dataBitWidth.W))
    val memWe = Output(UInt(1.W))

}
class EXU extends Module{
    val io = IO(new Bundle{
        val in = Flipped(new IduMessage())    
        //val data = new data_info()
        val out = new ExuMessage()
        val exu_allowin = Output(Bool())
        val mem_allowin = Input(Bool())
    })

    // val ex_me_pc = Reg(UInt(addrBitWidth.W))
    // val ex_me_aluRes = Reg(UInt(dataBitWidth.W))
    // val ex_me_resFromMem = Reg(UInt(1.W))
    // val ex_me_grWe = Reg(UInt(1.W))
    // val ex_me_dest = Reg(UInt(5.W))
    val exu_ready_go = true.B 
    val exu_valid = RegInit(false.B)
    val exu_allowin = (~exu_valid) || (io.mem_allowin && exu_ready_go)
    when(exu_allowin) {exu_valid := io.in.valid}
    io.out.valid := exu_ready_go && exu_valid
    io.exu_allowin := exu_allowin

    val id_ex_pc = Reg(UInt(addrBitWidth.W))
    val id_ex_aluSrc1 = Reg(UInt(dataBitWidth.W))
    val id_ex_aluSrc2 = Reg(UInt(dataBitWidth.W))
    val id_ex_memWe = Reg(UInt(1.W))
    val id_ex_aluOp = Reg(UInt(12.W))
    val id_ex_rfdata = Reg(UInt(dataBitWidth.W))
    val id_ex_resFromMem = Reg(UInt(1.W))
    val id_ex_grWe = Reg(UInt(1.W))
    val id_ex_dest = Reg(UInt(5.W))
    when(exu_allowin && io.in.valid){
        // ex_me_pc := io.in.pc
        // ex_me_aluRes := u_alu.io.aluRes
        // ex_me_resFromMem := io.in.resFromMem
        // ex_me_grWe := io.in.grWe
        // ex_me_dest := io.in.dest
        id_ex_pc := io.in.pc
        id_ex_aluSrc1 := io.in.aluSrc1
        id_ex_aluSrc2 := io.in.aluSrc2
        id_ex_memWe := io.in.memWe
        id_ex_aluOp := io.in.aluOp
        id_ex_rfdata := io.in.rfdata
        id_ex_resFromMem := io.in.resFromMem
        id_ex_grWe := io.in.grWe
        id_ex_dest := io.in.dest 
    }

    val u_alu = Module(new alu)

    u_alu.io.aluOp := id_ex_aluOp
    u_alu.io.aluSrc1 := id_ex_aluSrc1
    u_alu.io.aluSrc2 := id_ex_aluSrc2

    io.out.aluRes := u_alu.io.aluRes
    io.out.resFromMem := id_ex_resFromMem
    io.out.grWe := id_ex_grWe
    io.out.dest := id_ex_dest
    io.out.pc := id_ex_pc
    io.out.memWe := id_ex_memWe
    io.out.rfdata := id_ex_rfdata

    // io.data.data_sram_en := 1.U 
    // io.data.data_sram_we := Mux((io.in.memWe === 1.U && io.out.valid), Fill(4, 1.U), Fill(4, 0.U))
    // io.data.data_sram_addr := u_alu.io.aluRes
    // io.data.data_sram_wdata := io.in.rfdata
}
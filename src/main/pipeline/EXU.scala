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
    //val memWe = Output(UInt(1.W))
    val wordType = Output(UInt(wordTypeLen.W))
    val ldaddr   = Output(UInt(2.W))
}
class EXU extends Module{
    val io = IO(new Bundle{
        val in = Flipped(new IduMessage())    
        val data = new data_info()
        val out = new ExuMessage()
        val exu_allowin = Output(Bool())
        val mem_allowin = Input(Bool())
        val choke = new choke_info()
        val foward = new foward_info()
    })
    val exu_ready_go = Wire(Bool()) 
    val exu_valid = RegInit(false.B)
    val exu_allowin = (~exu_valid) || (io.mem_allowin && exu_ready_go)
    when(exu_allowin) {exu_valid := io.in.valid}
    io.out.valid := exu_ready_go && exu_valid
    io.exu_allowin := exu_allowin

    val id_ex_pc = RegInit(0.U(addrBitWidth.W))
    val id_ex_aluSrc1 = RegInit(0.U(dataBitWidth.W))
    val id_ex_aluSrc2 = RegInit(0.U(dataBitWidth.W))
    val id_ex_memWe = RegInit(0.U(1.W))
    val id_ex_aluOp = RegInit(0.U(aluOpLen.W))
    val id_ex_rfdata = RegInit(0.U(dataBitWidth.W))
    val id_ex_resFromMem = RegInit(0.U(1.W))
    val id_ex_grWe = RegInit(0.U(1.W))
    val id_ex_dest = RegInit(0.U(5.W))
    val id_ex_wordType = RegInit(0.U(wordTypeLen.W))
    when(exu_allowin && io.in.valid){
        id_ex_pc := io.in.pc
        id_ex_aluSrc1 := io.in.aluSrc1
        id_ex_aluSrc2 := io.in.aluSrc2
        id_ex_memWe := io.in.memWe
        id_ex_aluOp := io.in.aluOp
        id_ex_rfdata := io.in.rfdata
        id_ex_resFromMem := io.in.resFromMem
        id_ex_grWe := io.in.grWe
        id_ex_dest := io.in.dest 
        id_ex_wordType := io.in.wordType
    }

    val u_alu = Module(new alu)

    u_alu.io.aluOp := id_ex_aluOp
    u_alu.io.aluSrc1 := id_ex_aluSrc1
    u_alu.io.aluSrc2 := id_ex_aluSrc2

    exu_ready_go := u_alu.io.aluReady

    io.choke.w_valid := id_ex_grWe & exu_valid & (id_ex_dest =/= 0.U).asUInt & id_ex_resFromMem
    io.choke.waddr := id_ex_dest

    io.foward.w_valid := id_ex_grWe & exu_valid & (id_ex_dest =/= 0.U).asUInt & !(id_ex_resFromMem)
    io.foward.waddr := id_ex_dest
    io.foward.wdata := u_alu.io.aluRes

    io.data.data_sram_ce := 1.U
    io.data.data_sram_oe := id_ex_memWe === 0.U 
    io.data.data_sram_be := MuxCase(Fill(4, 0.U), Seq(
        (id_ex_memWe === 1.U && exu_valid && id_ex_wordType === W) -> Fill(4, 1.U),
        (id_ex_memWe === 1.U && exu_valid && id_ex_wordType === H) -> Mux(u_alu.io.aluRes(1, 0) === 0.U, "b0011".U, "b1100".U),
        (id_ex_memWe === 1.U && exu_valid && id_ex_wordType === B) -> (1.U << u_alu.io.aluRes(1, 0))(3, 0)
    ))
    io.data.data_sram_we := io.data.data_sram_be.orR
    //io.data.data_sram_addr := Cat(u_alu.io.aluRes(31, 2), Fill(2, 0.U))
    io.data.data_sram_addr := u_alu.io.aluRes

    io.data.data_sram_wdata := MuxCase(id_ex_rfdata, Seq(
        (id_ex_wordType === W) -> id_ex_rfdata,
        (id_ex_wordType === B) -> Fill(4, id_ex_rfdata(7, 0)),
        (id_ex_wordType === H) -> Fill(2, id_ex_rfdata(15, 0))
    ))

    io.out.aluRes := u_alu.io.aluRes
    io.out.resFromMem := id_ex_resFromMem
    io.out.grWe := id_ex_grWe
    io.out.dest := id_ex_dest
    io.out.pc := id_ex_pc
    //io.out.memWe := id_ex_memWe
    io.out.rfdata := id_ex_rfdata
    io.out.wordType := id_ex_wordType
    io.out.ldaddr := u_alu.io.aluRes(1, 0)
}
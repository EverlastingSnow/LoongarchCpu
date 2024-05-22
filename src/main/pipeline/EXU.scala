package loongarch32i

import chisel3._
import chisel3.util._
import Myconsts._

class ExuMessage extends Bundle {
    val pc = Output(UInt(addrBitWidth.W))
    val aluRes = Output(UInt(dataBitWidth.W))
    val resFrom = Output(UInt(resTypeLen.W))
    val grWe = Output(UInt(1.W))    
    val dest = Output(UInt(5.W))
    val valid = Output(Bool())
    val rfdata = Output(UInt(dataBitWidth.W))
    val wordType = Output(UInt(wordTypeLen.W))
    val ldaddr   = Output(UInt(2.W))
}
class EXU extends Module{
    val io = IO(new Bundle{
        val in = Flipped(new IduMessage())    
        val data = new data_info()
        val out = new ExuMessage()
        val exu_allowin = Output(Bool())
        val exu_stop = Output(Bool())
        val mem_allowin = Input(Bool())
        val mem_stop = Input(Bool())
        val choke = new choke_info()
        val foward = new foward_info()

        val in_csr = Flipped(new csr_op_info())
        val out_csr = new csr_op_info()        
    })
    val exu_ready_go = Wire(Bool()) 
    val exu_valid = RegInit(false.B)
    val exu_allowin = (~exu_valid) || (io.mem_allowin && exu_ready_go)
    when(exu_allowin || io.mem_stop) {exu_valid := io.in.valid && !io.mem_stop}
    io.out.valid := exu_ready_go && exu_valid && !io.mem_stop
    io.exu_allowin := exu_allowin

    val wr_exu_reg_enable = exu_allowin && io.in.valid
    val id_ex = RegEnable(io.in, wr_exu_reg_enable)
    val id_ex_csr = RegEnable(io.in_csr, wr_exu_reg_enable)

    val u_alu = Module(new alu)

    u_alu.io.aluOp := id_ex.aluOp
    u_alu.io.aluSrc1 := id_ex.aluSrc1
    u_alu.io.aluSrc2 := id_ex.aluSrc2

    exu_ready_go := u_alu.io.aluReady

    io.choke.w_valid := id_ex.grWe & exu_valid & (id_ex.dest =/= 0.U).asUInt & (id_ex.resFrom === 1.U).asUInt
    io.choke.waddr := id_ex.dest

    io.foward.csr_choke := id_ex.grWe & exu_valid & (id_ex_csr.waddr === 0x40.U).asUInt & !io.mem_stop
    io.foward.w_valid := id_ex.grWe & exu_valid & (id_ex.dest =/= 0.U).asUInt & !(id_ex.resFrom === 1.U).asUInt & !io.mem_stop
    io.foward.w_choke := id_ex.grWe & exu_valid & (id_ex.dest =/= 0.U).asUInt & (id_ex.resFrom === 2.U) & !io.mem_stop
    io.foward.waddr := id_ex.dest
    io.foward.wdata := u_alu.io.aluRes
//ALE处理
    val ALE_check = MuxCase("b00".U(2.W), Seq(
        (id_ex_csr.excp === 0.U && id_ex.resFrom === resFromMem && id_ex.wordType === W && u_alu.io.aluRes(1, 0) =/= 0.U) -> "b01".U(2.W),
        (id_ex_csr.excp === 0.U && id_ex.resFrom === resFromMem && (id_ex.wordType === H || id_ex.wordType === HU) && u_alu.io.aluRes(0) =/= 0.U) -> "b01".U(2.W),
        (id_ex_csr.excp === 0.U && id_ex.memWe === 1.U && id_ex.wordType === W && u_alu.io.aluRes(1, 0) =/= 0.U) -> "b01".U(2.W),
        (id_ex_csr.excp === 0.U && id_ex.memWe === 1.U && id_ex.wordType === H && u_alu.io.aluRes(0) =/= 0.U) -> "b01".U(2.W)
    ))

    val ALE_Res = ListLookup(ALE_check, List(id_ex_csr.excp, id_ex_csr.ecode, id_ex_csr.esubcode, id_ex_csr.badv, id_ex_csr.badaddr), Array(
        ALE_CHECK_1 -> List(1.U(2.W), Ecode_ALE, EsubCode_Normal, 1.U, u_alu.io.aluRes)
    ))
    val final_csrExcp :: final_csrEcode :: final_csrEsubcode :: final_csrBadv :: final_csrBadaddr :: Nil = ALE_Res

//st指令
    io.data.data_sram_en := 1.U 
    io.data.data_sram_we := MuxCase(Fill(4, 0.U), Seq(
        (id_ex.memWe === 1.U && exu_valid && final_csrBadv === 0.U && id_ex_csr.excp === 0.U && !io.mem_stop && id_ex.wordType === W) -> Fill(4, 1.U),
        (id_ex.memWe === 1.U && exu_valid && final_csrBadv === 0.U && id_ex_csr.excp === 0.U && !io.mem_stop && id_ex.wordType === H) -> Mux(u_alu.io.aluRes(1, 0) === 0.U, "b0011".U, "b1100".U),
        (id_ex.memWe === 1.U && exu_valid && final_csrBadv === 0.U && id_ex_csr.excp === 0.U && !io.mem_stop && id_ex.wordType === B) -> (1.U << u_alu.io.aluRes(1, 0))(3, 0)
    ))
    io.data.data_sram_addr := u_alu.io.aluRes

    io.data.data_sram_wdata := MuxCase(id_ex.rfdata, Seq(
        (id_ex.wordType === W) -> id_ex.rfdata,
        (id_ex.wordType === B) -> Fill(4, id_ex.rfdata(7, 0)),
        (id_ex.wordType === H) -> Fill(2, id_ex.rfdata(15, 0))
    ))

    io.out.aluRes   := u_alu.io.aluRes
    io.out.resFrom  := id_ex.resFrom
    io.out.grWe     := id_ex.grWe
    io.out.dest     := id_ex.dest
    io.out.pc       := id_ex.pc
    io.out.rfdata   := id_ex.rfdata
    io.out.wordType := id_ex.wordType
    io.out.ldaddr   := u_alu.io.aluRes(1, 0)

    io.exu_stop := io.mem_stop || (exu_valid && id_ex_csr.excp =/= 0.U)

    io.out_csr.badv     := final_csrBadv
    io.out_csr.badaddr  := final_csrBadaddr
    io.out_csr.excp     := final_csrExcp
    io.out_csr.ecode    := final_csrEcode
    io.out_csr.esubcode := final_csrEsubcode 
    io.out_csr.mask_we  := id_ex_csr.mask_we
    io.out_csr.mask     := id_ex_csr.mask
    io.out_csr.wen      := id_ex_csr.wen
    io.out_csr.waddr    := id_ex_csr.waddr 
    io.out_csr.wdata    := id_ex_csr.wdata
    io.out_csr.raddr    := id_ex_csr.raddr
    io.out_csr.pc       := id_ex_csr.pc
}
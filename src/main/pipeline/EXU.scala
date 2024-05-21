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

    val id_ex_pc = RegInit(0.U(addrBitWidth.W))
    val id_ex_aluSrc1 = RegInit(0.U(dataBitWidth.W))
    val id_ex_aluSrc2 = RegInit(0.U(dataBitWidth.W))
    val id_ex_memWe = RegInit(0.U(1.W))
    val id_ex_aluOp = RegInit(0.U(aluOpLen.W))
    val id_ex_rfdata = RegInit(0.U(dataBitWidth.W))
    val id_ex_resFrom = RegInit(0.U(resTypeLen.W))
    val id_ex_grWe = RegInit(0.U(1.W))
    val id_ex_dest = RegInit(0.U(5.W))
    val id_ex_wordType = RegInit(0.U(wordTypeLen.W))

    val id_ex_csrBadv     = RegInit(0.U(1.W))
    val id_ex_csrBadaddr  = RegInit(0.U(addrBitWidth.W))
    val id_ex_csrExcp     = RegInit(0.U(2.W))
    val id_ex_csrEcode    = RegInit(0.U(6.W))
    val id_ex_csrEsubcode = RegInit(0.U(9.W))
    val id_ex_csrPc       = RegInit(0.U(addrBitWidth.W))
    val id_ex_csrMaskWe   = RegInit(0.U(1.W))
    val id_ex_csrMask     = RegInit(0.U(dataBitWidth.W))
    val id_ex_csrWen      = RegInit(0.U(1.W))
    val id_ex_csrWaddr    = RegInit(0.U(ctrlRegLen.W))
    val id_ex_csrWdata    = RegInit(0.U(dataBitWidth.W))
    val id_ex_csrRaddr    = RegInit(0.U(ctrlRegLen.W))
    when(exu_allowin && io.in.valid){
        id_ex_pc      := io.in.pc
        id_ex_aluSrc1 := io.in.aluSrc1
        id_ex_aluSrc2 := io.in.aluSrc2
        id_ex_memWe   := io.in.memWe
        id_ex_aluOp   := io.in.aluOp
        id_ex_rfdata  := io.in.rfdata
        id_ex_resFrom := io.in.resFrom
        id_ex_grWe    := io.in.grWe
        id_ex_dest    := io.in.dest 
        id_ex_wordType:= io.in.wordType
        
        id_ex_csrBadaddr:= io.in_csr.badaddr
        id_ex_csrBadv := io.in_csr.badv
        id_ex_csrExcp := io.in_csr.excp
        id_ex_csrEcode := io.in_csr.ecode
        id_ex_csrEsubcode := io.in_csr.esubcode
        id_ex_csrMaskWe := io.in_csr.mask_we
        id_ex_csrMask := io.in_csr.mask
        id_ex_csrWen := io.in_csr.wen
        id_ex_csrWaddr := io.in_csr.waddr
        id_ex_csrWdata := io.in_csr.wdata
        id_ex_csrRaddr := io.in_csr.raddr
        id_ex_csrPc    := io.in_csr.pc
    }

    val u_alu = Module(new alu)

    u_alu.io.aluOp := id_ex_aluOp
    u_alu.io.aluSrc1 := id_ex_aluSrc1
    u_alu.io.aluSrc2 := id_ex_aluSrc2

    exu_ready_go := u_alu.io.aluReady

    io.choke.w_valid := id_ex_grWe & exu_valid & (id_ex_dest =/= 0.U).asUInt & (id_ex_resFrom === 1.U).asUInt
    io.choke.waddr := id_ex_dest

    io.foward.csr_choke := id_ex_grWe & exu_valid & (id_ex_csrWaddr === 0x40.U).asUInt & !io.mem_stop
    io.foward.w_valid := id_ex_grWe & exu_valid & (id_ex_dest =/= 0.U).asUInt & !(id_ex_resFrom === 1.U).asUInt & !io.mem_stop
    io.foward.w_choke := id_ex_grWe & exu_valid & (id_ex_dest =/= 0.U).asUInt & (id_ex_resFrom === 2.U) & !io.mem_stop
    io.foward.waddr := id_ex_dest
    io.foward.wdata := u_alu.io.aluRes

    val ALE_check = MuxCase("b00".U(2.W), Seq(
        (id_ex_csrExcp === 0.U && id_ex_resFrom === resFromMem && id_ex_wordType === W && u_alu.io.aluRes(1, 0) =/= 0.U) -> "b01".U(2.W),
        (id_ex_csrExcp === 0.U && id_ex_resFrom === resFromMem && (id_ex_wordType === H || id_ex_wordType === HU) && u_alu.io.aluRes(0) =/= 0.U) -> "b01".U(2.W),
        (id_ex_csrExcp === 0.U && id_ex_memWe === 1.U && id_ex_wordType === W && u_alu.io.aluRes(1, 0) =/= 0.U) -> "b01".U(2.W),
        (id_ex_csrExcp === 0.U && id_ex_memWe === 1.U && id_ex_wordType === H && u_alu.io.aluRes(0) =/= 0.U) -> "b01".U(2.W)
    ))

    val ALE_Res = ListLookup(ALE_check, List(id_ex_csrExcp, id_ex_csrEcode, id_ex_csrEsubcode, id_ex_csrBadv, id_ex_csrBadaddr), Array(
        ALE_CHECK_1 -> List(1.U(2.W), Ecode_ALE, EsubCode_Normal, 1.U, u_alu.io.aluRes)
    ))
    val final_csrExcp :: final_csrEcode :: final_csrEsubcode :: final_csrBadv :: final_csrBadaddr :: Nil = ALE_Res


    io.data.data_sram_en := 1.U 
    io.data.data_sram_we := MuxCase(Fill(4, 0.U), Seq(
        (id_ex_memWe === 1.U && exu_valid && final_csrBadv === 0.U && id_ex_csrExcp === 0.U && !io.mem_stop && id_ex_wordType === W) -> Fill(4, 1.U),
        (id_ex_memWe === 1.U && exu_valid && final_csrBadv === 0.U && id_ex_csrExcp === 0.U && !io.mem_stop && id_ex_wordType === H) -> Mux(u_alu.io.aluRes(1, 0) === 0.U, "b0011".U, "b1100".U),
        (id_ex_memWe === 1.U && exu_valid && final_csrBadv === 0.U && id_ex_csrExcp === 0.U && !io.mem_stop && id_ex_wordType === B) -> (1.U << u_alu.io.aluRes(1, 0))(3, 0)
    ))
    io.data.data_sram_addr := u_alu.io.aluRes

    io.data.data_sram_wdata := MuxCase(id_ex_rfdata, Seq(
        (id_ex_wordType === W) -> id_ex_rfdata,
        (id_ex_wordType === B) -> Fill(4, id_ex_rfdata(7, 0)),
        (id_ex_wordType === H) -> Fill(2, id_ex_rfdata(15, 0))
    ))

    io.out.aluRes := u_alu.io.aluRes
    io.out.resFrom := id_ex_resFrom
    io.out.grWe := id_ex_grWe
    io.out.dest := id_ex_dest
    io.out.pc := id_ex_pc
    io.out.rfdata := id_ex_rfdata
    io.out.wordType := id_ex_wordType
    io.out.ldaddr := u_alu.io.aluRes(1, 0)

    io.exu_stop := io.mem_stop || (exu_valid && id_ex_csrExcp =/= 0.U)

    io.out_csr.badv := final_csrBadv
    io.out_csr.badaddr := final_csrBadaddr
    io.out_csr.excp := final_csrExcp
    io.out_csr.ecode := final_csrEcode
    io.out_csr.esubcode := final_csrEsubcode 
    io.out_csr.mask_we := id_ex_csrMaskWe
    io.out_csr.mask := id_ex_csrMask
    io.out_csr.wen := id_ex_csrWen
    io.out_csr.waddr := id_ex_csrWaddr 
    io.out_csr.wdata := id_ex_csrWdata
    io.out_csr.raddr := id_ex_csrRaddr
    io.out_csr.pc    := id_ex_csrPc
}
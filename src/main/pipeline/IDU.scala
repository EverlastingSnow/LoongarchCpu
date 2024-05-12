package loongarch32i

import chisel3._
import chisel3.util._
import Myconsts._
import Instructions._

class IduMessage extends Bundle{
    val pc = Output(UInt(addrBitWidth.W))
    val aluOp = Output(UInt(aluOpLen.W))
    val aluSrc1 = Output(UInt(dataBitWidth.W))
    val aluSrc2 = Output(UInt(dataBitWidth.W))
    val memWe = Output(UInt(1.W))
    val rfdata = Output(UInt(dataBitWidth.W))
    val resFromMem = Output(UInt(1.W))
    val grWe = Output(UInt(1.W))    
    val dest = Output(UInt(5.W))
    val valid = Output(Bool())
    val wordType = Output(UInt(wordTypeLen.W))
}
class IDU extends Module {
    val io = IO(new Bundle{
        val in = Flipped(new IfuMessage())
        val br = Flipped(new br_info())
        val out = new IduMessage()
        val rfWe = Input(UInt(1.W))
        val rfWaddr = Input(UInt(5.W))
        val rfWdata = Input(UInt(dataBitWidth.W))
        val idu_allowin = Output(Bool())
        val exu_allowin = Input(Bool())
        val exu_choke = Flipped(new choke_info())
        val exu_foward = Flipped(new foward_info())
        val mem_foward = Flipped(new foward_info())
        val wbu_foward = Flipped(new foward_info())
    })
    val if_id_inst = RegInit(0.U(instBitWidth.W))
    val if_id_pc = RegInit(0.U(addrBitWidth.W))
    
    val idu_ready_go = Wire(Bool()) 
    val idu_valid = RegInit(false.B)
    val idu_allowin = (~idu_valid) || (io.exu_allowin && idu_ready_go)
    when(idu_allowin) {idu_valid := io.in.valid}
    io.out.valid := idu_ready_go && idu_valid
    io.idu_allowin := idu_allowin

    when (idu_allowin && io.in.valid){
        if_id_inst := io.in.inst
        if_id_pc := io.in.pc
    }
    
    val inst = Wire(UInt(addrBitWidth.W))
    inst := if_id_inst
    

    val op31_26 = Wire(UInt(6.W))
    val op25_22 = Wire(UInt(4.W))
    val op21_20 = Wire(UInt(2.W))
    val op19_15 = Wire(UInt(5.W))
    val rd = Wire(UInt(5.W))
    val rj = Wire(UInt(5.W))
    val rk = Wire(UInt(5.W))
    val i12 = Wire(UInt(12.W))
    val i20 = Wire(UInt(20.W))
    val i16 = Wire(UInt(16.W))
    val i26 = Wire(UInt(26.W))
    
    op31_26 := inst(31, 26)
    op25_22 := inst(25, 22)
    op21_20 := inst(21, 20)
    op19_15 := inst(19, 15)
    rd := inst(4, 0)    
    rj := inst(9, 5)
    rk := inst(14, 10)
    i12 := inst(21, 10)    
    i20 := inst(24, 5)
    i16 := inst(25, 10)
    i26 := Cat(inst(9, 0), inst(25, 10))
    val imm_4 = 4.U(dataBitWidth.W)
    val imm_si12 = Cat(Fill(20, i12(11)), i12(11, 0))
    val imm_ui12 = Cat(Fill(20, 0.U), i12(11, 0))
    val imm_si16 = Cat(Fill(14, i16(15)), i16(15, 0), 0.U(2.W))
    val imm_si20 = Cat(i20(19, 0), 0.U(12.W))
    val imm_si26 = Cat(Fill(4, i26(25)), i26(25, 0), 0.U(2.W))
    val imm_ui5  = Wire(UInt(dataBitWidth.W))
    imm_ui5  := rk 

    val pc = if_id_pc(addrBitWidth - 1, 0)

    val rfRaddr1 = Wire(UInt(5.W))
    val rfRaddr2 = Wire(UInt(5.W))
    val rfRdata1 = Wire(UInt(dataBitWidth.W))
    val rfRdata2 = Wire(UInt(dataBitWidth.W))
    
    val u_regfile = Module(new regFile)
    u_regfile.io.raddr1 := rfRaddr1
    u_regfile.io.raddr2 := rfRaddr2
    rfRdata1 := u_regfile.io.rdata1
    rfRdata2 := u_regfile.io.rdata2
    u_regfile.io.we     := io.rfWe 
    u_regfile.io.waddr  := io.rfWaddr 
    u_regfile.io.wdata  := io.rfWdata
    
    val rjValue = Wire(UInt(dataBitWidth.W))
    val rkValue = Wire(UInt(dataBitWidth.W))

    val rk_5 = Wire(UInt(dataBitWidth.W))
    rk_5 := rkValue(4, 0)

    val idu_signals: List[UInt] = ListLookup(
        inst,
        List(aluNop, rjValue, rkValue, rNOP, rNOP, imm_NOP, READ_NOP, WR_NOP, WR_NOP, rd, JUMP_NOP, W),
        Array(
            instAdd_w    -> List(aluAdd   , rjValue, rkValue,  rj,   rk,   imm_NOP,  READ_NOP,  WR_RG,   WR_NOP, rd, JUMP_NOP,    W),
            instSub_w    -> List(aluSub   , rjValue, rkValue,  rj,   rk,   imm_NOP,  READ_NOP,  WR_RG,   WR_NOP, rd, JUMP_NOP,    W),
            instMul_w    -> List(aluMul   , rjValue, rkValue,  rj,   rk,   imm_NOP,  READ_NOP,  WR_RG,   WR_NOP, rd, JUMP_NOP,    W),
            instMul_h_w  -> List(aluMulh  , rjValue, rkValue,  rj,   rk,   imm_NOP,  READ_NOP,  WR_RG,   WR_NOP, rd, JUMP_NOP,    W),
            instMul_h_w_u-> List(aluMulhu , rjValue, rkValue,  rj,   rk,   imm_NOP,  READ_NOP,  WR_RG,   WR_NOP, rd, JUMP_NOP,    W),
            instDiv_w    -> List(aluDiv   , rjValue, rkValue,  rj,   rk,   imm_NOP,  READ_NOP,  WR_RG,   WR_NOP, rd, JUMP_NOP,    W),
            instMod_w    -> List(aluMod   , rjValue, rkValue,  rj,   rk,   imm_NOP,  READ_NOP,  WR_RG,   WR_NOP, rd, JUMP_NOP,    W),
            instDiv_w_u  -> List(aluDivu  , rjValue, rkValue,  rj,   rk,   imm_NOP,  READ_NOP,  WR_RG,   WR_NOP, rd, JUMP_NOP,    W),
            instMod_w_u  -> List(aluModu  , rjValue, rkValue,  rj,   rk,   imm_NOP,  READ_NOP,  WR_RG,   WR_NOP, rd, JUMP_NOP,    W),
            instAddi_w   -> List(aluAdd   , rjValue, imm_si12, rj,   rNOP, imm_NOP,  READ_NOP,  WR_RG,   WR_NOP, rd, JUMP_NOP,    W),
            instLd_w     -> List(aluAdd   , rjValue, imm_si12, rj,   rNOP, imm_NOP,  READ_ME,   WR_RG,   WR_NOP, rd, JUMP_NOP,    W),
            instSt_w     -> List(aluAdd   , rjValue, imm_si12, rj,   rd,   imm_NOP,  READ_NOP,  WR_NOP,  WR_ME,  rd, JUMP_NOP,    W),
            instJirl     -> List(aluAdd   , pc     , imm_4,    rj,   rNOP, imm_NOP,  READ_NOP,  WR_RG,   WR_NOP, rd, JUMP_NORMAL, W),
            instBl       -> List(aluAdd   , pc     , imm_4,    rNOP, rNOP, imm_si26, READ_NOP,  WR_RG,   WR_NOP, R1, JUMP_NORMAL, W),
            instSlt      -> List(aluSlt   , rjValue, rkValue,  rj,   rk,   imm_NOP,  READ_NOP,  WR_RG,   WR_NOP, rd, JUMP_NOP,    W),
            instSltu     -> List(aluSltu  , rjValue, rkValue,  rj,   rk,   imm_NOP,  READ_NOP,  WR_RG,   WR_NOP, rd, JUMP_NOP,    W),
            instSlti     -> List(aluSlt   , rjValue, imm_si12, rj,   rNOP, imm_NOP,  READ_NOP,  WR_RG,   WR_NOP, rd, JUMP_NOP,    W),
            instSltui    -> List(aluSltu  , rjValue, imm_si12, rj,   rNOP, imm_NOP,  READ_NOP,  WR_RG,   WR_NOP, rd, JUMP_NOP,    W), 
            instNor      -> List(aluNor   , rjValue, rkValue,  rj,   rk,   imm_NOP,  READ_NOP,  WR_RG,   WR_NOP, rd, JUMP_NOP,    W),
            instAnd      -> List(aluAnd   , rjValue, rkValue,  rj,   rk,   imm_NOP,  READ_NOP,  WR_RG,   WR_NOP, rd, JUMP_NOP,    W),
            instOr       -> List(aluOr    , rjValue, rkValue,  rj,   rk,   imm_NOP,  READ_NOP,  WR_RG,   WR_NOP, rd, JUMP_NOP,    W),
            instXor      -> List(aluXor   , rjValue, rkValue,  rj,   rk,   imm_NOP,  READ_NOP,  WR_RG,   WR_NOP, rd, JUMP_NOP,    W),
            instAndi     -> List(aluAnd   , rjValue, imm_ui12, rj,   rNOP, imm_NOP,  READ_NOP,  WR_RG,   WR_NOP, rd, JUMP_NOP,    W),            
            instOri      -> List(aluOr    , rjValue, imm_ui12, rj,   rNOP, imm_NOP,  READ_NOP,  WR_RG,   WR_NOP, rd, JUMP_NOP,    W),
            instXori     -> List(aluXor   , rjValue, imm_ui12, rj,   rNOP, imm_NOP,  READ_NOP,  WR_RG,   WR_NOP, rd, JUMP_NOP,    W),
            instSll_w    -> List(aluSlliw , rjValue, rk_5,     rj,   rk,   imm_NOP,  READ_NOP,  WR_RG,   WR_NOP, rd, JUMP_NOP,    W),
            instSrl_w    -> List(aluSrliw , rjValue, rk_5,     rj,   rk,   imm_NOP,  READ_NOP,  WR_RG,   WR_NOP, rd, JUMP_NOP,    W),
            instSra_w    -> List(aluSraiw , rjValue, rk_5,     rj,   rk,   imm_NOP,  READ_NOP,  WR_RG,   WR_NOP, rd, JUMP_NOP,    W),
            instSlli_w   -> List(aluSlliw , rjValue, imm_ui5,  rj,   rNOP, imm_NOP,  READ_NOP,  WR_RG,   WR_NOP, rd, JUMP_NOP,    W),
            instSrli_w   -> List(aluSrliw , rjValue, imm_ui5,  rj,   rNOP, imm_NOP,  READ_NOP,  WR_RG,   WR_NOP, rd, JUMP_NOP,    W),
            instSrai_w   -> List(aluSraiw , rjValue, imm_ui5,  rj,   rNOP, imm_NOP,  READ_NOP,  WR_RG,   WR_NOP, rd, JUMP_NOP,    W),
            instLu12i_w  -> List(aluLu12iw, rjValue, imm_si20, rNOP, rNOP, imm_NOP,  READ_NOP,  WR_RG,   WR_NOP, rd, JUMP_NOP,    W),
            instB        -> List(aluNop   , rjValue, rkValue,  rNOP, rNOP, imm_si26, READ_NOP,  WR_NOP,  WR_NOP, rd, JUMP_NORMAL, W),
            instBeq      -> List(aluNop   , rjValue, rkValue,  rj,   rd,   imm_si16, READ_NOP,  WR_NOP,  WR_NOP, rd, JUMP_EQ,     W),
            instBne      -> List(aluNop   , rjValue, rkValue,  rj,   rd,   imm_si16, READ_NOP,  WR_NOP,  WR_NOP, rd, JUMP_NEQ,    W),
            instBlt      -> List(aluNop   , rjValue, rkValue,  rj,   rd,   imm_si16, READ_NOP,  WR_NOP,  WR_NOP, rd, JUMP_LT,     W),
            instBge      -> List(aluNop   , rjValue, rkValue,  rj,   rd,   imm_si16, READ_NOP,  WR_NOP,  WR_NOP, rd, JUMP_GE,     W),
            instBltu     -> List(aluNop   , rjValue, rkValue,  rj,   rd,   imm_si16, READ_NOP,  WR_NOP,  WR_NOP, rd, JUMP_LTU,    W),
            instBgeu     -> List(aluNop   , rjValue, rkValue,  rj,   rd,   imm_si16, READ_NOP,  WR_NOP,  WR_NOP, rd, JUMP_GEU,    W),
            instLd_B     -> List(aluAdd   , rjValue, imm_si12, rj,   rNOP, imm_NOP,  READ_ME,   WR_RG,   WR_NOP, rd, JUMP_NOP,    B),
            instLd_H     -> List(aluAdd   , rjValue, imm_si12, rj,   rNOP, imm_NOP,  READ_ME,   WR_RG,   WR_NOP, rd, JUMP_NOP,    H),
            instLd_BU    -> List(aluAdd   , rjValue, imm_si12, rj,   rNOP, imm_NOP,  READ_ME,   WR_RG,   WR_NOP, rd, JUMP_NOP,    BU),
            instLd_HU    -> List(aluAdd   , rjValue, imm_si12, rj,   rNOP, imm_NOP,  READ_ME,   WR_RG,   WR_NOP, rd, JUMP_NOP,    HU),
            instSt_B     -> List(aluAdd   , rjValue, imm_si12, rj,   rd,   imm_NOP,  READ_NOP,  WR_NOP,  WR_ME,  rd, JUMP_NOP,    B),
            instSt_H     -> List(aluAdd   , rjValue, imm_si12, rj,   rd,   imm_NOP,  READ_NOP,  WR_NOP,  WR_ME,  rd, JUMP_NOP,    H),
            instPcaddu12i-> List(aluAdd   , pc,      imm_si20, rNOP, rNOP, imm_NOP,  READ_NOP,  WR_RG,   WR_NOP, rd, JUMP_NOP,    W)
        )
    )
    val aluOp :: aluSrc1 :: aluSrc2 :: raddr1 :: raddr2 :: brOffs :: resFromMem :: grWe :: memWe :: dest :: jump :: wordType :: Nil = idu_signals

    rfRaddr1 := raddr1
    rfRaddr2 := raddr2

/*  idu_ready_go := Mux((rfRaddr1 === io.exu_waddr && rfRaddr1 =/= rNOP && io.exu_w_valid === 1.U) || 
    (rfRaddr1 === io.mem_waddr && rfRaddr1 =/= rNOP && io.mem_w_valid === 1.U) || 
    (rfRaddr1 === io.wbu_waddr && rfRaddr1 =/= rNOP && io.wbu_w_valid === 1.U) || 
    (rfRaddr2 === io.exu_waddr && rfRaddr2 =/= rNOP && io.exu_w_valid === 1.U) || 
    (rfRaddr2 === io.mem_waddr && rfRaddr2 =/= rNOP && io.mem_w_valid === 1.U) || 
    (rfRaddr2 === io.wbu_waddr && rfRaddr2 =/= rNOP && io.wbu_w_valid === 1.U), false.B, true.B)
*/
    idu_ready_go := Mux(
    (raddr1 === io.exu_choke.waddr && rfRaddr1 =/= rNOP && io.exu_choke.w_valid === 1.U) || 
    (raddr2 === io.exu_choke.waddr && rfRaddr2 =/= rNOP && io.exu_choke.w_valid === 1.U), false.B, true.B)

    val selector1 = Wire(UInt(4.W))
    selector1:= Cat(1.U, 
    io.wbu_foward.w_valid & (io.wbu_foward.waddr === raddr1).asUInt, 
    io.mem_foward.w_valid & (io.mem_foward.waddr === raddr1).asUInt, 
    io.exu_foward.w_valid & (io.exu_foward.waddr === raddr1).asUInt)

    val selector2 = Wire(UInt(4.W))
    selector2:= Cat(1.U, 
    io.wbu_foward.w_valid & (io.wbu_foward.waddr === raddr2).asUInt, 
    io.mem_foward.w_valid & (io.mem_foward.waddr === raddr2).asUInt, 
    io.exu_foward.w_valid & (io.exu_foward.waddr === raddr2).asUInt)


    rjValue := PriorityMux(Seq(
        selector1(0) -> io.exu_foward.wdata,
        selector1(1) -> io.mem_foward.wdata,
        selector1(2) -> io.wbu_foward.wdata,
        selector1(3) -> rfRdata1
    ))
    rkValue := PriorityMux(Seq(
        selector2(0) -> io.exu_foward.wdata,
        selector2(1) -> io.mem_foward.wdata,
        selector2(2) -> io.wbu_foward.wdata,
        selector2(3) -> rfRdata2
    ))

    val rjEqRd = Wire(UInt(1.W))
    val rjLtRd = Wire(UInt(1.W))
    val rjLtRdU= Wire(UInt(1.W))
    rjEqRd := (rjValue === rkValue)
    rjLtRd := (rjValue.asSInt < rkValue.asSInt)
    rjLtRdU:= (rjValue.asUInt < rkValue.asUInt)

    val brTaken = Wire(UInt(1.W))
    brTaken := Mux(
    (jump === JUMP_EQ  &&   rjEqRd === 1.U) || 
    (jump === JUMP_NEQ &&  !rjEqRd === 1.U) ||
    (jump === JUMP_LT  &&   rjLtRd === 1.U) ||
    (jump === JUMP_LTU &&  rjLtRdU === 1.U) ||
    (jump === JUMP_GE  &&  !rjLtRd === 1.U) ||
    (jump === JUMP_GEU && !rjLtRdU === 1.U) || 
    (jump === JUMP_NORMAL) , 1.U, 0.U)
    io.br.brTaken := Mux(idu_valid === false.B, 0.U, brTaken)

    val brTarget = Wire(UInt(dataBitWidth.W))
    brTarget := Mux(brOffs === imm_NOP, rjValue + imm_si16, pc + brOffs)
    io.br.brTarget := brTarget

    io.out.aluSrc1 := aluSrc1
    io.out.aluSrc2 := aluSrc2
    io.out.rfdata := rkValue
    io.out.aluOp := aluOp
    io.out.pc := pc
    io.out.memWe := memWe
    io.out.resFromMem := resFromMem
    io.out.grWe := grWe
    io.out.dest := dest
    io.out.wordType := wordType
}
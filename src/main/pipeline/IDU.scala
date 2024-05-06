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
        val exu_w_valid = Input(UInt(1.W))
        val exu_waddr = Input(UInt(5.W))
        val mem_w_valid = Input(UInt(1.W))
        val mem_waddr = Input(UInt(5.W))
        val wbu_w_valid = Input(UInt(1.W))
        val wbu_waddr = Input(UInt(5.W))
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

    rjValue := rfRdata1
    rkValue := rfRdata2

    val idu_signals: List[UInt] = ListLookup(
        inst,
        List(aluNop, 0.U, 0.U, rNOP, rNOP, imm_NOP, 0.U, 0.U, 0.U, 0.U, 0.U),
        Array(
            instAdd_w    -> List(aluAdd   , rjValue, rkValue,  rj,   rk,   imm_NOP,  0.U,  1.U,  0.U, rd,       0.U),
            instAddi_w   -> List(aluAdd   , rjValue, imm_si12, rj,   rNOP, imm_NOP,  0.U,  1.U,  0.U, rd,       0.U),
            instLd_w     -> List(aluAdd   , rjValue, imm_si12, rj,   rNOP, imm_NOP,  1.U,  1.U,  0.U, rd,       0.U),
            instSt_w     -> List(aluAdd   , rjValue, imm_si12, rj,   rd,   imm_NOP,  0.U,  0.U,  1.U, rd,       0.U),
            instJirl     -> List(aluAdd   , pc     , imm_4,    rj,   rNOP, imm_NOP,  0.U,  1.U,  0.U, rd,       1.U),
            instBl       -> List(aluAdd   , pc     , imm_4,    rNOP, rNOP, imm_si26, 0.U,  1.U,  0.U, 1.U(5.W), 1.U),
            instSub_w    -> List(aluSub   , rjValue, rkValue,  rj,   rk,   imm_NOP,  0.U,  1.U,  0.U, rd,       0.U),
            instSlt      -> List(aluSlt   , rjValue, rkValue,  rj,   rk,   imm_NOP,  0.U,  1.U,  0.U, rd,       0.U),
            instSltu     -> List(aluSltu  , rjValue, rkValue,  rj,   rk,   imm_NOP,  0.U,  1.U,  0.U, rd,       0.U),
            instNor      -> List(aluNor   , rjValue, rkValue,  rj,   rk,   imm_NOP,  0.U,  1.U,  0.U, rd,       0.U),
            instAnd      -> List(aluAnd   , rjValue, rkValue,  rj,   rk,   imm_NOP,  0.U,  1.U,  0.U, rd,       0.U),
            instOr       -> List(aluOr    , rjValue, rkValue,  rj,   rk,   imm_NOP,  0.U,  1.U,  0.U, rd,       0.U),
            instXor      -> List(aluXor   , rjValue, rkValue,  rj,   rk,   imm_NOP,  0.U,  1.U,  0.U, rd,       0.U),
            instSlli_w   -> List(aluSlliw , rjValue, imm_ui5,  rj,   rNOP, imm_NOP,  0.U,  1.U,  0.U, rd,       0.U),
            instSrli_w   -> List(aluSrliw , rjValue, imm_ui5,  rj,   rNOP, imm_NOP,  0.U,  1.U,  0.U, rd,       0.U),
            instSrai_w   -> List(aluSraiw , rjValue, imm_ui5,  rj,   rNOP, imm_NOP,  0.U,  1.U,  0.U, rd,       0.U),
            instLu12i_w  -> List(aluLu12iw, rjValue, imm_si20, rNOP, rNOP, imm_NOP,  0.U,  1.U,  0.U, rd,       0.U),
            instB        -> List(aluNop   , rjValue, rkValue,  rNOP, rNOP, imm_si26, 0.U,  0.U,  0.U, rd,       1.U),
            instBeq      -> List(aluNop   , rjValue, rkValue,  rj,   rd,   imm_si16, 0.U,  0.U,  0.U, rd,       2.U),
            instBne      -> List(aluNop   , rjValue, rkValue,  rj,   rd,   imm_si16, 0.U,  0.U,  0.U, rd,       3.U)
        )
    )
    val aluOp :: aluSrc1 :: aluSrc2 :: raddr1 :: raddr2 :: brOffs :: resFromMem :: grWe :: memWe :: dest :: jump :: Nil = idu_signals

    rfRaddr1 := raddr1
    rfRaddr2 := raddr2

    idu_ready_go := Mux((rfRaddr1 === io.exu_waddr && rfRaddr1 =/= rNOP && io.exu_w_valid === 1.U) || 
    (rfRaddr1 === io.mem_waddr && rfRaddr1 =/= rNOP && io.mem_w_valid === 1.U) || 
    (rfRaddr1 === io.wbu_waddr && rfRaddr1 =/= rNOP && io.wbu_w_valid === 1.U) || 
    (rfRaddr2 === io.exu_waddr && rfRaddr2 =/= rNOP && io.exu_w_valid === 1.U) || 
    (rfRaddr2 === io.mem_waddr && rfRaddr2 =/= rNOP && io.mem_w_valid === 1.U) || 
    (rfRaddr2 === io.wbu_waddr && rfRaddr2 =/= rNOP && io.wbu_w_valid === 1.U), false.B, true.B)
    //idu_ready_go := true.B

    val rjEqRd = Wire(UInt(1.W))
    rjEqRd := (rjValue === rkValue)

    val brTaken = Wire(UInt(1.W))
    brTaken := Mux((jump === 2.U && rjEqRd === 1.U) || 
    (jump === 3.U && (!rjEqRd) === 1.U) || 
    (jump === 1.U), 1.U, 0.U)
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
}
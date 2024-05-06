package loongarch32i
import chisel3._

object Myconsts{
    val PCStart = "h1bfffffc"
    val addrBitWidth = 32
    val instBitWidth = 32
    val dataBitWidth = 32
    
    val aluOpLen = 4
    val aluNop    = 0.U(aluOpLen.W)
    val aluAdd    = 1.U(aluOpLen.W) 
    val aluSub    = 2.U(aluOpLen.W) 
    val aluSlt    = 3.U(aluOpLen.W)
    val aluSltu   = 4.U(aluOpLen.W)
    val aluAnd    = 5.U(aluOpLen.W)
    val aluNor    = 6.U(aluOpLen.W)
    val aluOr     = 7.U(aluOpLen.W)
    val aluXor    = 8.U(aluOpLen.W)
    val aluSlliw  = 9.U(aluOpLen.W)
    val aluSrliw  = 10.U(aluOpLen.W)
    val aluSraiw  = 11.U(aluOpLen.W)
    val aluLu12iw = 12.U(aluOpLen.W)
    val imm_NOP = 0.U
/*
    val immLen = 3
    val immNop   = 0.U(immLen)
    val imm_4    = 1.U(immLen)
    val imm_ui5  = 2.U(immLen)
    val imm_si12 = 3.U(immLen)
    val imm_si16 = 4.U(immLen)
    val imm_si20 = 5.U(immLen)
    val imm_si26 = 6.U(immLen)*/
}
/*
    aluOp := Cat(instAddW | instAddiW | instLdW | instStW | instJirl | instBl, 
    instSubW, instSlt, instSltu, instAnd, instNor, instOr, instXor, instSlliW, instSrliW, instSraiW, instLu12iW)

    need_ui5 := instSlliW | instSrliW | instSraiW
    need_si12 := instAddiW | instLdW | instStW
    need_si16 := instJirl | instBeq | instBne
    need_si20 := instLu12iW
    need_si26 := instB | instBl
    src2_is_4 := instJirl | instBl
*/
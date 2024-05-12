package loongarch32i
import chisel3._

object Myconsts{
    val PCStart = "h1bfffffc"
    val addrBitWidth = 32
    val instBitWidth = 32
    val dataBitWidth = 32
    val hiLoBitWidth = 64
    val mulClkNum    = 2 //若修改记得修改vivado中IP核的周期数
    val divClkNum    = 8 //若修改记得修改vivado中IP核的周期数
    
    val aluOpLen = 5
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
    val aluMul    = 13.U(aluOpLen.W)
    val aluMulh   = 14.U(aluOpLen.W)
    val aluMulhu  = 15.U(aluOpLen.W)
    val aluDiv    = 16.U(aluOpLen.W)
    val aluDivu   = 17.U(aluOpLen.W)
    val aluMod    = 18.U(aluOpLen.W)
    val aluModu   = 19.U(aluOpLen.W)


    val imm_NOP = 0.U
    val rNOP = 0.U

    val READ_NOP = 0.U
    val READ_ME  = 1.U

    val WR_NOP = 0.U

    val WR_ME  = 1.U
    val WR_RG  = 1.U

    val R1 = 1.U(5.W)

    val jumpLen = 4
    val JUMP_NOP = 0.U(jumpLen.W)
    val JUMP_NORMAL = 1.U(jumpLen.W)
    val JUMP_EQ = 2.U(jumpLen.W)
    val JUMP_NEQ = 3.U(jumpLen.W)
    val JUMP_LT  = 4.U(jumpLen.W)
    val JUMP_GE  = 5.U(jumpLen.W)
    val JUMP_LTU = 6.U(jumpLen.W) 
    val JUMP_GEU = 7.U(jumpLen.W)

    val BUILD = false

    val wordTypeLen = 3
    val B = 0.U(wordTypeLen.W)
    val H = 1.U(wordTypeLen.W)
    val W = 2.U(wordTypeLen.W)
    val BU = 3.U(wordTypeLen.W)
    val HU = 4.U(wordTypeLen.W)
}

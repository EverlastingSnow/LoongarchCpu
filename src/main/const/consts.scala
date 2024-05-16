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
    
    val resTypeLen = 2
    val resFromMem = 1.U(resTypeLen.W)
    val resFromCsr = 2.U(resTypeLen.W)

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
    val CSRRD     = 20.U(aluOpLen.W)
    val CSRWR     = 21.U(aluOpLen.W)
    val CSRXCHG   = 22.U(aluOpLen.W)
    val SYSCALL   = 23.U(aluOpLen.W)
    val ERTN      = 24.U(aluOpLen.W)

    val NOP = 0.U

    val imm_NOP = 0.U
    val rNOP = 0.U

    val READ_NOP = 0.U
    val READ_ME  = 1.U
    val READ_CSR = 2.U

    val WR_NOP = 0.U

    val WR_ME  = 1.U
    val WR_RG  = 1.U
    val WR_CSR = 1.U

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

    val ctrlRegLen = 14
    val CRMDID = 0x0.U(ctrlRegLen.W)
    val PRMDID = 0x1.U(ctrlRegLen.W)
    // val EUENID = 0x2.U(ctrlRegLen.W)
    // val ECFGID = 0x4.U(ctrlRegLen.W)
    val ESTATID = 0x5.U(ctrlRegLen.W)
    val ERAID = 0x6.U(ctrlRegLen.W)
    //val BADVID = 0x7.U(ctrlRegLen.W)
    val EENTRYID = 0xc.U(ctrlRegLen.W) 
    //val CPUIDID = 0x20.U(ctrlRegLen.W)
    val SAVE0ID = 0x30.U(ctrlRegLen.W)
    val SAVE1ID = 0x31.U(ctrlRegLen.W)
    val SAVE2ID = 0x32.U(ctrlRegLen.W)
    val SAVE3ID = 0x33.U(ctrlRegLen.W)

    val Ecode_SYS = 0x0b.U(6.W)
}
/*
        0.U(instBitWidth.W), //CRMD (31,9)0 (8,7)DATM (6,5)DATF (4)PG (3)DA (2)IE (1,0)PLV
        0.U(instBitWidth.W), //PRMD (31,3)0 (2)PIE (1,0)PPLV
        //0.U(instBitWidth.W), EUEN (31,1)0 (0)FPE
        //0.U(instBitWidth.W), ECFG (31,13)0 (12,11)LIE[12:11] (10)0 (9,0)LIE[9:0]
        0.U(instBitWidth.W), //ESTAT (31)0 (30,22)EsubCode (21:16)Ecode (15,13)0 (12)IS[12] (11)IS[11] (10)0 (9,2)IS[9:2]
        0.U(instBitWidth.W), //ERA  (31,0) PC
        //0.U(instBitWidth.W), BADV (31,0) vaddr
        0.U(instBitWidth.W), //EENTRY (31,6)va (5, 0)0
        //0.U(instBitWidth.W), CPUID (31,9)0 (8,0)CoreID
        0.U(instBitWidth.W), //SAVE0 
        0.U(instBitWidth.W), //SAVE1
        0.U(instBitWidth.W), //SAVE2
        0.U(instBitWidth.W), //SAVE3
*/
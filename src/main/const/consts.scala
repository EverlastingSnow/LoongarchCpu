package loongarch32i
import chisel3._
import chisel3.util._

object Myconsts{
    val PCStart = "h1bfffffc"
    val addrBitWidth = 32
    val instBitWidth = 32
    val dataBitWidth = 32
    val hiLoBitWidth = 64
    val mulClkNum    = 2 //若修改记得修改vivado中IP核的周期数
    val divClkNum    = 8 //若修改记得修改vivado中IP核的周期数
    val TIMEN        = 25 //自带计时器位宽
    val BUILD = false //是否使用IP核
    
    val resTypeLen = 2
    val resFromMem = 1.U(resTypeLen.W)
    val resFromCsr = 2.U(resTypeLen.W)
//对应上面的resFrom    
    val READ_NOP = 0.U
    val READ_ME  = 1.U
    val READ_CSR = 2.U

//alu类型,附带某些特殊异常指令编号
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
    val BREAK     = 25.U(aluOpLen.W)
    val instNop   = 31.U(aluOpLen.W) //INE错误

    val NOP = 0.U
//要用到的立即数类型
    val imm_NOP = 0.U(3.W)
    val imm_si12id = 1.U(3.W)
    val imm_si16id = 2.U(3.W)
    val imm_si20id = 3.U(3.W) 
    val imm_si26id = 4.U(3.W)

    val rNOP = 0.U
//要写的类型
    val WR_NOP = 0.U
    val WR_ME  = 1.U
    val WR_RG  = 1.U
    val WR_CSR = 1.U

    val R1 = 1.U(5.W)
//跳转类型
    val jumpLen     = 4
    val JUMP_NOP    = 0.U(jumpLen.W)
    val JUMP_NORMAL = 1.U(jumpLen.W)
    val JUMP_EQ     = 2.U(jumpLen.W)
    val JUMP_NEQ    = 3.U(jumpLen.W)
    val JUMP_LT     = 4.U(jumpLen.W)
    val JUMP_GE     = 5.U(jumpLen.W)
    val JUMP_LTU    = 6.U(jumpLen.W) 
    val JUMP_GEU    = 7.U(jumpLen.W)

//ld或者st的位宽和是否为无符号数
    val wordTypeLen = 3
    val B = 0.U(wordTypeLen.W)
    val H = 1.U(wordTypeLen.W)
    val W = 2.U(wordTypeLen.W)
    val BU = 3.U(wordTypeLen.W)
    val HU = 4.U(wordTypeLen.W)

//csr寄存器ID
    val ctrlRegLen = 14
    val CRMDID = 0x0.U(ctrlRegLen.W)
    val PRMDID = 0x1.U(ctrlRegLen.W)
    // val EUENID = 0x2.U(ctrlRegLen.W)
    val ECFGID = 0x4.U(ctrlRegLen.W)
    val ESTATID = 0x5.U(ctrlRegLen.W)
    val ERAID = 0x6.U(ctrlRegLen.W)
    val BADVID = 0x7.U(ctrlRegLen.W)
    val EENTRYID = 0xc.U(ctrlRegLen.W)
    //val TLBIDXID = 0x10.U(ctrlRegLen.W)
    //val TLBEHIID = 0x11.U(ctrlRegLen.W)
    //val TLBELO0ID = 0x12.U(ctrlRegLen.W)
    //val TLBELO1ID = 0x13.U(ctrlRegLen.W)
    //val ASIDID    = 0x18.U(ctrlRegLen.W)
    //val PGDLID    = 0x19.U(ctrlRegLen.W)
    //val PGDHID    = 0x1a.U(ctrlRegLen.W)
    //val PGDID     = 0x1b.U(ctrlRegLen.W) 
    //val CPUIDID = 0x20.U(ctrlRegLen.W)
    val SAVE0ID = 0x30.U(ctrlRegLen.W)
    val SAVE1ID = 0x31.U(ctrlRegLen.W)
    val SAVE2ID = 0x32.U(ctrlRegLen.W)
    val SAVE3ID = 0x33.U(ctrlRegLen.W)
    val TIDID   = 0x40.U(ctrlRegLen.W)
    val TCFGID  = 0x41.U(ctrlRegLen.W)
    val TVALID  = 0x42.U(ctrlRegLen.W)
    val TICLRID = 0x44.U(ctrlRegLen.W)
    //val LLBCTLID= 0x60.U(ctrlRegLen.W)
    //val TLBRENTRYID=0x88.U(ctrlRegLen.W)
    //val CTAGID  = 0x98.U(ctrlRegLen.W)
    //val DMW0ID  = 0x180.U(ctrlRegLen.W)
    //val DMW1ID  = 0x181.U(ctrlRegLen.W)
//Esubcode类型
    val EsubCode_Normal = 0.U(1.W)
    val EsubCode_ADEF = 0.U(1.W)
    val EsubCode_ADEM = 1.U(1.W)
//Ecode类型
    val Ecode_SYS  = 0x0b.U(6.W)
    val Ecode_ADEF = 0x08.U(6.W)
    val Ecode_ADEM = 0x08.U(6.W)
    val Ecode_ALE  = 0x09.U(6.W)
    val Ecode_BRK  = 0x0c.U(6.W)
    val Ecode_INE  = 0x0d.U(6.W)
    val Ecode_INT  = 0x0.U(6.W)
//ALE检查,用于EXU检查是否ALE
    val ALE_CHECK_NOP = BitPat("b00")
    val ALE_CHECK_1   = BitPat("b01")
}
/*
ADEF ALE BRK INE
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
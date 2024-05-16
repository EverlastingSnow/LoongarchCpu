package loongarch32i

import chisel3._ 
import chisel3.util._ 
import Myconsts._ 

trait BaseCsr{
    val id : UInt
    val info : Data 
    val rw_we : UInt
    def write(value : UInt) = {
        info := ((~rw_we & info.asUInt) | (rw_we & value)).asTypeOf(info)
    }
}

class CRMD_info extends Bundle {
    // val zero = 0.U(23.W)
    // val datm = 0.U(2.W)
    // val datf = 0.U(2.W)
    // val pg   = 0.U(1.W)
    // val da   = 0.U(1.W)
    // val ie   = 0.U(1.W)
    // val plv  = 0.U(2.W)
    val zero = UInt(23.W)
    val datm = UInt(2.W) 
    val datf = UInt(2.W) 
    val pg   = UInt(1.W)    
    val da   = UInt(1.W)    
    val ie   = UInt(1.W)    
    val plv  = UInt(2.W)
}
class CRMD extends BaseCsr {
    override val id = CRMDID
    override val rw_we = "b00000000000000000000000_11_11_1_1_1_11".U
    override val info = RegInit({
        val init = WireDefault(0.U.asTypeOf(new CRMD_info))
        init.da := 1.U
        init
    })
    //override val info = new CRMD_info()
}

class PRMD_info extends Bundle {
    // val zero = 0.U(29.W)
    // val pie  = 0.U(1.W)
    // val pplv = 0.U(2.W)
    val zero = UInt(29.W)
    val pie  = UInt(1.W)
    val pplv = UInt(2.W)
}
class PRMD extends BaseCsr {
    override val id = PRMDID
    override val rw_we = "b00000000000000000000000000000_1_11".U
    override val info = RegInit(0.U.asTypeOf(new PRMD_info))
}

class ESTAT_info extends Bundle {
    // val zero3 = 0.U(1.W)
    // val esubcode  = 0.U(9.W)
    // val ecode = 0.U(6.W)
    // val zero2 = 0.U(3.W)
    // val is_12 = 0.U(1.W)
    // val is_11 = 0.U(1.W)
    // val zero1 = 0.U(1.W)
    // val is_9_2 = 0.U(8.W)
    // val is_1_0 = 0.U(2.W)
    val zero3 = UInt(1.W)
    val esubcode  = UInt(9.W)
    val ecode = UInt(6.W)
    val zero2 = UInt(3.W)
    val is_12 = UInt(1.W)
    val is_11 = UInt(1.W)
    val zero1 = UInt(1.W)
    val is_9_2 = UInt(8.W)
    val is_1_0 = UInt(2.W)
}
class ESTAT extends BaseCsr {
    override val id = ESTATID
    override val rw_we = "b0_111111111_111111_000_1_1_0_11111111_11".U
    override val info = RegInit(0.U.asTypeOf(new ESTAT_info))
}

class ERA_info extends Bundle {
    // val pc = 0.U(32.W)
    val pc = UInt(addrBitWidth.W)
}
class ERA extends BaseCsr {
    override val id = ERAID
    override val rw_we = "b11111111111111111111111111111111".U
    override val info = RegInit(0.U.asTypeOf(new ERA_info))
}

class EENTRY_info extends Bundle {
    // val va = 0.U(26.W)
    // val zero = 0.U(6.W)
    val va = UInt(26.W)
    val zero = UInt(6.W)
}
class EENTRY extends BaseCsr {
    override val id = EENTRYID
    override val rw_we = "b11111111111111111111111111000000".U
    override val info = RegInit(0.U.asTypeOf(new EENTRY_info))
}

class SAVE extends Bundle {
    val data = UInt(dataBitWidth.W)
}
class SAVE0 extends BaseCsr {
    override val id = SAVE0ID
    override val rw_we = "b11111111111111111111111111".U
    override val info = RegInit(0.U.asTypeOf(new SAVE))
}
class SAVE1 extends BaseCsr {
    override val id = SAVE1ID
    override val rw_we = "b11111111111111111111111111".U
    override val info = RegInit(0.U.asTypeOf(new SAVE))
}
class SAVE2 extends BaseCsr {
    override val id = SAVE2ID
    override val rw_we = "b11111111111111111111111111".U
    override val info = RegInit(0.U.asTypeOf(new SAVE))
}
class SAVE3 extends BaseCsr {
    override val id = SAVE3ID
    override val rw_we = "b11111111111111111111111111".U
    override val info = RegInit(0.U.asTypeOf(new SAVE))
}
        //CRMD (31,9)0 (8,7)DATM (6,5)DATF (4)PG (3)DA (2)IE (1,0)PLV
        //PRMD (31,3)0 (2)PIE (1,0)PPLV
        //0.U(instBitWidth.W), EUEN (31,1)0 (0)FPE
        //0.U(instBitWidth.W), ECFG (31,13)0 (12,11)LIE[12:11] (10)0 (9,0)LIE[9:0]
        //ESTAT (31)0 (30,22)EsubCode (21:16)Ecode (15,13)0 (12)IS[12] (11)IS[11] (10)0 (9,2)IS[9:2] (1,0)IS[1:0]
        //ERA  (31,0) PC
        //0.U(instBitWidth.W), BADV (31,0) vaddr
        //EENTRY (31,6)va (5, 0)0
        //0.U(instBitWidth.W), CPUID (31,9)0 (8,0)CoreID
        //SAVE0 
        //SAVE1
        //SAVE2
        //SAVE3


class CSR extends Module {
    val io = IO(new Bundle{
        val csr = Flipped(new csr_info())
        val pc_stop = Output(Bool())
        val dnpc = Output(UInt(addrBitWidth.W))
    })

    val CRMD = new CRMD()
    val PRMD = new PRMD()
    val ESTAT = new ESTAT()
    val ERA = new ERA()
    val EENTRY = new EENTRY()
    val SAVE0 = new SAVE0()
    val SAVE1 = new SAVE1()
    val SAVE2 = new SAVE2()
    val SAVE3 = new SAVE3()

    val csrList = Seq(
        CRMD,
        PRMD,
        ESTAT,
        ERA,
        EENTRY,
        SAVE0,
        SAVE1,
        SAVE2,
        SAVE3
    )

    io.csr.rdata := 0.U
    for (x <- csrList){
        when(x.id === io.csr.raddr){
            io.csr.rdata := x.info.asUInt
        }
    }

    io.pc_stop := io.csr.excp =/= 0.U
    io.dnpc := Mux(io.csr.excp === 1.U, EENTRY.info.asUInt, ERA.info.asUInt)
    when (io.csr.excp === 1.U){
        PRMD.info.pplv := CRMD.info.plv
        PRMD.info.pie  := CRMD.info.ie
        CRMD.info.plv := "b00".U
        CRMD.info.ie  := "b0".U
        ERA.write(io.csr.pc)
        ESTAT.info.esubcode := io.csr.esubcode
        ESTAT.info.ecode := io.csr.ecode
    }.elsewhen(io.csr.excp === 2.U){
        CRMD.info.plv := PRMD.info.pplv
        CRMD.info.ie := PRMD.info.pie
        when (ESTAT.info.ecode === "h3F".U(6.W)){
            CRMD.info.da := "b0".U 
            CRMD.info.pg := "b1".U
        }
    }
    when (io.csr.wen === 1.U) {
        val mask = Mux(io.csr.mask_we === 1.U, io.csr.mask, "b11111111111111111111111111111111".U)
        for (x <- csrList) {
            when(x.id === io.csr.waddr) {
                val value = ((mask & io.csr.wdata) | (~mask & x.info.asUInt))
                x.write(value)
                when(x.id === CRMDID && value(4) === 1.U) {
                    CRMD.info.datf := "b01".U
                    CRMD.info.datm := "b01".U
                }
            }
        }
    }
}

/*
Ecode EsubCode 
0x0   0        INT 中断
0x1   0        PIL ld无效
0x2   0        PIS st无效
0x3   0        PIF 取指无效
0x4   0        PME 页修改
0x7   0        PPI  页特权等级不合规
0x8   0        ADEF 取指地址错
0x8   1        ADEM 取指地址访问错
0x9   0        ALE  地址非对齐
0xB   0        SYS  系统调用例外
0xC   0        BRK  断点例外
0xD   0        INE  指令不存在
0xE   0        IPE  指令特权等级错
0xF   0        FPD  浮点指令未使能例外
0x12  0        FPE  基础浮点指令例外
0x1A-0x3E 保留
0x3F  0        TLBR TLB重
*/
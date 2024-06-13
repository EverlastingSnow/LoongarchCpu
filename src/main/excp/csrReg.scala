package loongarch32i

import chisel3._ 
import chisel3.util._ 
import Myconsts._ 

trait BaseCsr{
    val id : UInt
    val info : Data 
    val w_we : UInt
    def write(value : UInt) = {
        info := ((~w_we & info.asUInt) | (w_we & value)).asTypeOf(info)
    }
}

class CRMD_info extends Bundle {
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
    override val w_we = "b00000000000000000000000_11_11_1_1_1_11".U
    override val info = RegInit({
        val init = WireDefault(0.U.asTypeOf(new CRMD_info))
        init.da := 1.U
        init
    })
}

class PRMD_info extends Bundle {
    val zero = UInt(29.W)
    val pie  = UInt(1.W)
    val pplv = UInt(2.W)
}
class PRMD extends BaseCsr {
    override val id = PRMDID
    override val w_we = "b00000000000000000000000000000_1_11".U
    override val info = RegInit(0.U.asTypeOf(new PRMD_info))
}

class ECFG_info extends Bundle {
    val zero2      = UInt(19.W)
    val lie_12_11  = UInt(2.W)
    val zero1      = UInt(1.W)
    val lie_9_0    = UInt(10.W) 
}
class ECFG extends BaseCsr {
    override val id = ECFGID
    override val w_we = "b0000000000000000000_11_0_1111111111".U
    override val info = RegInit(0.U.asTypeOf(new ECFG_info))
}

class ESTAT_info extends Bundle {
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
    override val w_we = "b0_000000000_000000_000_0_0_0_00000000_11".U
    override val info = RegInit(0.U.asTypeOf(new ESTAT_info))
}

class ERA_info extends Bundle {
    // val pc = 0.U(32.W)
    val pc = UInt(addrBitWidth.W)
}
class ERA extends BaseCsr {
    override val id = ERAID
    override val w_we = "b11111111111111111111111111111111".U
    override val info = RegInit(0.U.asTypeOf(new ERA_info))
}

class BADV_info extends Bundle {
    val vaddr = UInt(addrBitWidth.W)
}
class BADV extends BaseCsr {
    override val id = BADVID
    override val w_we = "b11111111111111111111111111111111".U
    override val info = RegInit(0.U.asTypeOf(new BADV_info))
}

class EENTRY_info extends Bundle {
    val va = UInt(26.W)
    val zero = UInt(6.W)
}
class EENTRY extends BaseCsr {
    override val id = EENTRYID
    override val w_we = "b11111111111111111111111111000000".U
    override val info = RegInit(0.U.asTypeOf(new EENTRY_info))
}

class SAVE extends Bundle {
    val data = UInt(dataBitWidth.W)
}
class SAVE0 extends BaseCsr {
    override val id = SAVE0ID
    override val w_we = "b11111111111111111111111111111111".U
    override val info = RegInit(0.U.asTypeOf(new SAVE))
}
class SAVE1 extends BaseCsr {
    override val id = SAVE1ID
    override val w_we = "b11111111111111111111111111111111".U
    override val info = RegInit(0.U.asTypeOf(new SAVE))
}
class SAVE2 extends BaseCsr {
    override val id = SAVE2ID
    override val w_we = "b11111111111111111111111111111111".U
    override val info = RegInit(0.U.asTypeOf(new SAVE))
}
class SAVE3 extends BaseCsr {
    override val id = SAVE3ID
    override val w_we = "b11111111111111111111111111111111".U
    override val info = RegInit(0.U.asTypeOf(new SAVE))
}

class TID_info extends Bundle {
    val tid = UInt(32.W)
}
class TID extends BaseCsr {
    override val id = TIDID
    override val w_we = "b11111111111111111111111111111111".U
    override val info = RegInit(0.U.asTypeOf(new TID_info))
}

class TCFG_info extends Bundle {
    val zero     = UInt((dataBitWidth - TIMEN).W)
    val initval  = UInt((TIMEN - 2).W)
    val periodic = UInt(1.W)
    val en       = UInt(1.W)
}
class TCFG extends BaseCsr {
    override val id = TCFGID
    override val w_we = Cat(Fill(32 - TIMEN, 0.U), Fill(TIMEN, 1.U))
    override val info = RegInit(0.U.asTypeOf(new TCFG_info))
}

class TVAL_info extends Bundle {
    val zero = UInt(((dataBitWidth - TIMEN).W))
    val timeval = UInt(TIMEN.W)
}
class TVAL extends BaseCsr {
    override val id = TVALID
    override val w_we = "b00000000000000000000000000000000".U
    override val info = RegInit(0.U.asTypeOf(new TVAL_info))
}

class TICLR_info extends Bundle {
    val zero = UInt(31.W)
    val clr  = UInt(1.W)
}
class TICLR extends BaseCsr {
    override val id = TICLRID
    override val w_we = "b00000000000000000000000000000001".U
    override val info = RegInit(0.U.asTypeOf(new TICLR_info))
}

class CSR extends Module {
    val io = IO(new Bundle{
        val csr = Flipped(new csr_info())
//pc_stop异常跳转信号,dnpc Pc寄存器下一跳地址
        val pc_stop = Output(Bool())
        val dnpc = Output(UInt(addrBitWidth.W))
//Int_en INT异常信号,ct stable_counter
        val Int_en = Output(UInt(1.W))
        val ct = new counter_info()
    })

    val CRMD = new CRMD()
    val PRMD = new PRMD()
    val ECFG = new ECFG()
    val ESTAT = new ESTAT()
    val ERA = new ERA()
    val BADV = new BADV()
    val EENTRY = new EENTRY()
    val SAVE0 = new SAVE0()
    val SAVE1 = new SAVE1()
    val SAVE2 = new SAVE2()
    val SAVE3 = new SAVE3()
    val TID   = new TID()
    val TCFG  = new TCFG()
    val TVAL  = new TVAL()
    val TICLR = new TICLR()

    val stableCounter = RegInit(0.U(64.W))
    stableCounter := Mux(stableCounter === "hffffffffffffffff".U, 0.U, stableCounter + 1.U)

    io.ct.counterH  := stableCounter(63, 32)
    io.ct.counterL  := stableCounter(31, 0)
    io.ct.counterID := TID.info.tid

    io.Int_en := (Cat(ESTAT.info.is_12, ESTAT.info.is_11, ESTAT.info.is_9_2, ESTAT.info.is_1_0) 
    & Cat(ECFG.info.lie_12_11, ECFG.info.lie_9_0)).orR & CRMD.info.ie

    val csrList = Seq(
        CRMD,
        PRMD,
        ECFG,
        ESTAT,
        ERA,
        BADV,
        EENTRY,
        SAVE0,
        SAVE1,
        SAVE2,
        SAVE3,
        TID,
        TCFG,
        TVAL,
        TICLR
    )
    val mask = Mux(io.csr.mask_we === 1.U, io.csr.mask, "b11111111111111111111111111111111".U)
//自带时钟
    when(io.csr.wen === 1.U && io.csr.waddr === TCFGID){
        val value = ((mask & io.csr.wdata) | (~mask & TCFG.info.asUInt))
        TVAL.info.timeval := Cat(value(TIMEN - 1, 2), 1.U(2.W)) //PROBLEM:1.U?0.U?
    }.elsewhen(TCFG.info.en === 1.U){
        when(TVAL.info.timeval === 0.U && TCFG.info.periodic === 1.U){
            TVAL.info.timeval := Mux(TCFG.info.periodic === 1.U, Cat(TCFG.info.initval, Fill(2, 0.U)), 0.U)
        }.otherwise{
            TVAL.info.timeval := TVAL.info.timeval - 1.U
        }
    }.otherwise{
        
    }

    val TVAL_info_timeval_last = RegNext(TVAL.info.timeval)
    when(TCFG.info.en === 1.U && TVAL.info.timeval === 0.U && TVAL_info_timeval_last === 1.U){
        ESTAT.info.is_11 := 1.U
    }
//读csr
    io.csr.rdata := 0.U
    for (x <- csrList){
        when(x.id === io.csr.raddr){
            when(x.id === TICLRID){
                io.csr.rdata := 0.U(dataBitWidth.W)
            }.otherwise {
                io.csr.rdata := x.info.asUInt
            }
        }
    }
//异常信息保存
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
        when(io.csr.badv === 1.U) {BADV.write(io.csr.badaddr)}
    }.elsewhen(io.csr.excp === 2.U){
        CRMD.info.plv := PRMD.info.pplv
        CRMD.info.ie := PRMD.info.pie
        when (ESTAT.info.ecode === "h3F".U(6.W)){
            CRMD.info.da := "b0".U 
            CRMD.info.pg := "b1".U
        }
    }
//写csr
    when (io.csr.wen === 1.U) {
        for (x <- csrList) {
            when(x.id === io.csr.waddr) {
                val value = ((mask & io.csr.wdata) | (~mask & x.info.asUInt))
                x.write(value)
                when(x.id === CRMDID && value(4) === 1.U) {
                    CRMD.info.datf := "b01".U
                    CRMD.info.datm := "b01".U
                }.elsewhen(x.id === TICLRID && value(0) === 1.U){
                    ESTAT.info.is_11 := 0.U
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
        //CRMD (31,9)0 (8,7)DATM (6,5)DATF (4)PG (3)DA (2)IE (1,0)PLV
        //PRMD (31,3)0 (2)PIE (1,0)PPLV
        //EUEN (31,1)0 (0)FPE
        //ECFG (31,13)0 (12,11)LIE[12:11] (10)0 (9,0)LIE[9:0]
        //ESTAT (31)0 (30,22)EsubCode (21:16)Ecode (15,13)0 (12)IS[12] (11)IS[11] (10)0 (9,2)IS[9:2] (1,0)IS[1:0]
        //ERA  (31,0) PC
        //0.U(instBitWidth.W), BADV (31,0) vaddr
        //EENTRY (31,6)va (5, 0)0
        //0.U(instBitWidth.W), CPUID (31,9)0 (8,0)CoreID
        //SAVE0 
        //SAVE1
        //SAVE2
        //SAVE3

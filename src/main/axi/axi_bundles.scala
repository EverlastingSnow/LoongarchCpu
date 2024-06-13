package loongarch32i

import chisel3._
import chisel3.util._ 
import Myconsts._

class AW_info extends Bundle{
    val id    = Output(UInt(4.W))
    val addr  = Output(UInt(addrBitWidth.W))
    val len   = Output(UInt(8.W))
    val size  = Output(UInt(3.W))
    val burst = Output(UInt(2.W))
    val lock  = Output(UInt(2.W))
    val cache = Output(UInt(4.W))
    val prot  = Output(UInt(3.W))
    val valid = Output(UInt(1.W))
    val ready = Input(UInt(1.W))
}

class W_info extends Bundle{
    val id    = Output(UInt(4.W))
    val data  = Output(UInt(dataBitWidth.W))
    val strb  = Output(UInt(8.W))
    val last  = Output(UInt(1.W))
    val valid = Output(UInt(1.W))
    val ready = Input(UInt(1.W)) 
}

class B_info extends Bundle{
    val id    = Output(UInt(4.W))
    val resp  = Output(UInt(2.W))
    val valid = Output(UInt(1.W))
    val ready = Input(UInt(1.W))
}

class AR_info extends Bundle{
    val id    = Output(UInt(4.W))
    val addr  = Output(UInt(addrBitWidth.W))
    val len   = Output(UInt(8.W))
    val size  = Output(UInt(3.W))
    val burst = Output(UInt(2.W))
    val lock  = Output(UInt(2.W))
    val cache = Output(UInt(4.W))
    val prot  = Output(UInt(3.W))
    val valid = Output(UInt(1.W))
    val ready = Input(UInt(1.W))
}

class R_info extends Bundle{
    val id    = Output(UInt(4.W))
    val data  = Output(UInt(dataBitWidth.W))
    val resp  = Output(UInt(2.W))
    val last  = Output(UInt(1.W))
    val valid = Output(UInt(1.W))
    val ready = Input(UInt(1.W))  
}
class AXI_info extends Bundle {
    val ar = new AR_info()
    val r = Flipped(new R_info())
    val aw = new AW_info()
    val w = new W_info()
    val b = Flipped(new B_info())
}
class AXI_R extends Bundle{
    val ar = new AR_info()
    val r = Flipped(new R_info())
}
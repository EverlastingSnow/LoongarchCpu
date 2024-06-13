/*
AR通道（read request channel），用于发送读事务的地址信息和控制信息。作为读请求的发起操作
R通道（read data channel），读事务用于接受读取数据的通道，和一些控制信息。也用于读事务的响应
AW通道（write request channel），用于发送写事务的地址信息和控制信息。作为写请求的发起操作
W通道（write data channel），写事务用于传输写入数据的通道，和一些控制信息
B通道（write response channel），专门用于写事务的响应，和一些控制信息
所有通道都通过同步握手（ready-valid handshake）的方式确认其一次transfer的完成（具体见原文A3.2.1），各个通的握手信号分别是{AR, R, AW, W, B}VALID和{AR, R, AW, W, B}READY。

读事务中必须AR先握手，然后R再握手。（或者说必须先完成AR的transfer，再完成R的transfer）
而写事务中则在B握手前必须先完成AW和W的握手，注意W可能有多个（多个write transfer），那么B必须在最后一次W握手后再握手。
（或者说必须先完成所有AW和W的transfer，再完成B的transfer）（注意AW和W之间没有顺序要求，也可以同时发起AW和W的transfer）

The information source uses the VALID signal to show when valid data or control
information is available on the channel. The destination uses the READY signal to
show when it can accept the data. Both the read data channel and the write data channel
also include a LAST signal to indicate when the transfer of the final data item within a
transaction takes place


*/

package loongarch32i

import chisel3._
import chisel3.util._ 
import Myconsts._

class ICACHE extends Module{
    val io = IO(new Bundle{
        val in = Flipped(new sram_info())
        val icache = new AXI_R()
    })
    val R = WireDefault(0.U.asTypeOf(Flipped(new R_info)))
    val AR = WireDefault(0.U.asTypeOf(new AR_info))
    AR.burst := 1.U
//InstRead    
    AR.id := 0.U
    AR.addr := io.in.addr
    AR.size := io.in.size

    val sr_idle :: sr_waitARready :: sr_waitRvalid :: sr_waitGo :: Nil = Enum(4)
    val Rstate = RegInit(sr_idle)
    Rstate := MuxLookup(Rstate, sr_idle)(List(
    sr_idle         -> Mux(AR.valid         === 1.U, sr_waitARready, sr_idle),
    sr_waitARready  -> Mux(AR.ready         === 1.U, sr_waitRvalid, sr_waitARready),
    sr_waitRvalid   -> Mux(R.valid          === 1.U, sr_waitGo,  sr_waitRvalid),
    sr_waitGo       -> Mux(io.in.ready_go   === 1.U, sr_idle,  sr_waitGo)  
    ))

    AR.valid := MuxCase(0.U, Seq(
    //(Rstate === sr_waitARready && AR.ready === 1.U) -> 0.U,
    (Rstate === sr_waitRvalid)                      -> 0.U,
    (Rstate === sr_idle  && io.in.req === 1.U)      -> 1.U,
    (Rstate === sr_waitARready)                     -> 1.U,
    ))
    
    R.ready := MuxCase(0.U, Seq(
    (Rstate === sr_waitARready && AR.ready === 1.U) -> 1.U,
    (Rstate === sr_waitRvalid)                      -> 1.U    
    ))

    io.icache.ar  <> AR 
    io.icache.r   <> R
    
    val rdata = RegInit(0.U(instBitWidth.W))
    when (Rstate === sr_waitRvalid && R.valid === 1.U) {rdata := io.icache.r.data}

    io.in.rdata   := rdata
    io.in.addr_ok := (Rstate === sr_idle).asUInt
    io.in.data_ok := (Rstate === sr_waitGo).asUInt
}

class DCACHE extends Module{
    val io = IO(new Bundle{
        val in = Flipped(new sram_info())
        val dcache = new AXI_info()
    })
    val R  = WireDefault(0.U.asTypeOf(Flipped(new R_info)))
    val AR = WireDefault(0.U.asTypeOf(new AR_info))
        AR.burst := 1.U  
    val AW = WireDefault(0.U.asTypeOf(new AW_info))
        AW.id    := 1.U
        AW.burst := 1.U
    val W  = WireDefault(0.U.asTypeOf(new W_info))
        W.id   := 1.U 
        W.last := 1.U
    val B  = WireDefault(0.U.asTypeOf(Flipped(new B_info)))
//DataRead
    AR.id   := 1.U
    AR.addr := io.in.addr
    AR.size := io.in.size


    val sr_idle :: sr_waitARready :: sr_waitRvalid :: sr_waitGo :: Nil = Enum(4)
    val Rstate = RegInit(sr_idle)
    Rstate := MuxLookup(Rstate, sr_idle)(List(
    sr_idle         -> Mux(AR.valid         === 1.U, sr_waitARready, sr_idle),
    sr_waitARready  -> Mux(AR.ready         === 1.U, sr_waitRvalid, sr_waitARready),
    sr_waitRvalid   -> Mux(R.valid          === 1.U, sr_waitGo,  sr_waitRvalid),
    sr_waitGo       -> Mux(io.in.ready_go   === 1.U, sr_idle,  sr_waitGo)  
    ))

    AR.valid := MuxCase(0.U, Seq(
    //(Rstate === sr_waitARready && AR.ready === 1.U) -> 0.U,
    (Rstate === sr_waitRvalid)                                          -> 0.U,
    (Rstate === sr_idle  && io.in.req === 1.U && io.in.wr === 0.U)      -> 1.U,
    (Rstate === sr_waitARready)                                         -> 1.U,
    ))
    
    R.ready := MuxCase(0.U, Seq(
    (Rstate === sr_waitARready && AR.ready === 1.U) -> 1.U,
    (Rstate === sr_waitRvalid)                      -> 1.U    
    ))

    io.dcache.ar  <> AR 
    io.dcache.r   <> R
    
    val rdata = RegInit(0.U(instBitWidth.W))
    when (Rstate === sr_waitRvalid && R.valid === 1.U) {rdata := io.dcache.r.data}

    io.in.rdata   := rdata

//DataWrite

    AW.id    := 1.U
    AW.addr  := io.in.addr
    AW.size  := io.in.size
    B.valid := io.dcache.b.valid

    W.id   := 1.U
    W.data := io.in.wdata


    val sw_idle :: sw_waitWAWready :: sw_waitWready :: sw_waitAWready :: sw_waitBvalid :: sw_waitGo ::Nil = Enum(6)
    val Wstate = RegInit(sw_idle)
    Wstate := MuxLookup(Wstate, sw_idle)(List(
    sw_idle         -> Mux(AW.valid === 1.U && W.valid === 1.U, sw_waitWAWready, sw_idle),
    sw_waitWAWready -> Mux(AW.ready === 1.U && W.ready === 1.U, sw_waitBvalid, 
                                          Mux(AW.ready === 1.U, sw_waitWready, 
                                          Mux(W.ready  === 1.U, sw_waitAWready, sw_waitWAWready))),
    sw_waitWready   -> Mux(W.ready        === 1.U, sw_waitBvalid, sw_waitWready),
    sw_waitAWready  -> Mux(AW.ready       === 1.U, sw_waitBvalid, sw_waitAWready),
    sw_waitBvalid   -> Mux(B.valid        === 1.U, sw_waitGo,     sw_waitBvalid),
    sw_waitGo       -> Mux(io.in.ready_go === 1.U, sw_idle,       sw_waitGo)
    ))

    AW.valid := MuxCase(0.U, Seq(
    (Wstate === sw_idle && io.in.req === 1.U && io.in.wr === 1.U) -> 1.U,
    (Wstate === sw_waitWready)                                    -> 0.U,
    (Wstate === sw_waitWAWready || Wstate === sw_waitAWready)     -> 1.U    
    ))

    W.valid  := MuxCase(0.U, Seq(
    (Wstate === sw_idle && io.in.req === 1.U && io.in.wr === 1.U) -> 1.U,
    (Wstate === sw_waitAWready)                                   -> 0.U,
    (Wstate === sw_waitWAWready || Wstate === sw_waitWready)      -> 1.U    
    ))

    B.ready  := MuxCase(0.U, Seq(
    (Wstate === sw_waitWAWready && AW.ready === 1.U && W.ready === 1.U) -> 1.U,
    (Wstate === sw_waitAWready  && AW.ready === 1.U)                    -> 1.U,
    (Wstate === sw_waitWready   && W.ready  === 1.U)                    -> 1.U,
    (Wstate === sw_waitBvalid)                                          -> 1.U
    ))

    io.dcache.w  <> W 
    io.dcache.aw <> AW
    io.dcache.b  <> B


    io.in.addr_ok := Mux(io.in.wr === 0.U, (Rstate === sr_idle).asUInt, (Wstate === sw_idle).asUInt)
    io.in.data_ok := Mux(io.in.wr === 0.U, (Rstate === sr_waitGo).asUInt, (Wstate === sw_waitGo).asUInt)
}

class AXI extends Module{
    val io = IO(new Bundle{
        val icache = Flipped(new AXI_R())
        val dcache = Flipped(new AXI_info())
        val axi = new AXI_info()
    })

    val AR_sel_lock = RegInit(0.U(1.W))
    val AR_sel_id   = RegInit(0.U(1.W))

    val ar_id       = Mux(AR_sel_lock === 1.U, AR_sel_id, io.dcache.ar.valid)
    //AR_sel_lock    := Mux(io.axi.ar.valid === 1.U && io.axi.ar.ready === 1.U, 1.U, 0.U)
    //AR_sel_id      := Mux(io.axi.ar.valid === 1.U && io.axi.ar.ready === 1.U, ar_id, 0.U)
    
    when(io.axi.ar.valid === 1.U){
        when(io.axi.ar.ready === 1.U){
            AR_sel_lock := 0.U
        }.otherwise{
            AR_sel_lock := 1.U
            AR_sel_id   := ar_id
        }
    }

    io.axi.ar.id    := Cat(0.U(3.W), ar_id)
    io.axi.ar.valid := Mux(ar_id === 1.U, io.dcache.ar.valid, io.icache.ar.valid)
    io.axi.ar.addr  := Mux(ar_id === 1.U, io.dcache.ar.addr,  io.icache.ar.addr)

    
    val R_sel = io.axi.r.id(0)
    io.icache.r.id    := io.axi.r.id
    io.icache.r.valid := (R_sel === 0.U) && io.axi.r.valid === 1.U
    io.icache.r.data  := io.axi.r.data
    io.icache.r.last  := io.axi.r.last
    io.icache.r.resp  := io.axi.r.resp

    io.icache.ar.ready := io.axi.ar.ready
    
    io.dcache.r.id    := io.axi.r.id
    io.dcache.r.valid := (R_sel === 1.U) && io.axi.r.valid === 1.U
    io.dcache.r.data  := io.axi.r.data
    io.dcache.r.last  := io.axi.r.last
    io.dcache.r.resp  := io.axi.r.resp

    io.dcache.ar.ready := io.axi.ar.ready

    when (io.axi.ar.id === 1.U){
        io.axi.ar.len   := io.icache.ar.len
        io.axi.ar.size  := io.icache.ar.size
        io.axi.ar.burst := io.icache.ar.burst
        io.axi.ar.lock  := io.icache.ar.lock
        io.axi.ar.cache := io.icache.ar.cache
        io.axi.ar.prot  := io.icache.ar.prot
    }.otherwise{
        io.axi.ar.len   := io.dcache.ar.len
        io.axi.ar.size  := io.dcache.ar.size
        io.axi.ar.burst := io.dcache.ar.burst
        io.axi.ar.lock  := io.dcache.ar.lock
        io.axi.ar.cache := io.dcache.ar.cache
        io.axi.ar.prot  := io.dcache.ar.prot
    }

    io.axi.r.ready    := Mux(io.axi.ar.id === 0.U, io.icache.r.ready, io.dcache.r.ready)

    io.axi.w  <> io.dcache.w
    io.axi.aw <> io.dcache.aw
    io.axi.b  <> io.dcache.b
}



package loongarch32i

import chisel3._
import chisel3.util._
import Myconsts._

class IfuMessage extends Bundle {
    val inst = Output(UInt(instBitWidth.W))
    val pc = Output(UInt(addrBitWidth.W))
    val csrBadv = Output(UInt(1.W))
    val csrBadaddr = Output(UInt(addrBitWidth.W))
    val valid = Output(Bool())
}
class IFU extends Module {
    val io = IO(new Bundle{
        val br = new br_info()
        val inst = new sram_info()
        val out = new IfuMessage()
        val idu_allowin = Input(Bool())
        val idu_stop  = Input(Bool())
        val pc_stop = Input(UInt(1.W))
        val dnpc = Input(UInt(addrBitWidth.W))
    })

    val to_ifu_valid = RegNext(!reset.asBool) & !reset.asBool
    val ifu_ready_go = Wire(Bool()) 
    ifu_ready_go := true.B
    val ifu_valid = RegInit(false.B)
    val ifu_allowin = ((~ifu_valid) || (io.idu_allowin && ifu_ready_go && io.inst.data_ok === 1.U)) && !io.idu_stop
    when(ifu_allowin) {ifu_valid := to_ifu_valid}
    
    val snPc = Wire(UInt(addrBitWidth.W))
    val dnPc = Wire(UInt(addrBitWidth.W))
    val pc = RegInit(PCStart.U(addrBitWidth.W))

    val csr_pc_stop = RegNext(io.pc_stop)
    val csr_dnpc    = RegNext(io.dnpc)

    io.out.valid := ifu_valid && ifu_ready_go && (io.br.brTaken === 0.U) && !io.idu_stop && !csr_pc_stop && io.inst.data_ok === 1.U 
    
    
    snPc := pc + "h4".U;
    dnPc := Mux(csr_pc_stop === 1.U,
        csr_dnpc,
        Mux(io.br.brTaken === 1.U, io.br.brTarget, snPc
    ))
        
    when(reset.asBool === true.B){
        pc := "h1bfffffc".U(addrBitWidth.W)
    }
    when(to_ifu_valid && ifu_allowin){
        pc := dnPc
    }

    io.out.csrBadv := (pc(1,0) =/= 0.U).asUInt
    io.out.csrBadaddr:= pc
    
    io.inst.req := to_ifu_valid && ifu_allowin
    io.inst.wr  := 0.U
    io.inst.wstrb := 0.U(4.W)
    io.inst.size:= 2.U
    io.inst.addr := dnPc
    io.inst.wdata := Fill(32, 0.U)
    io.inst.ready_go := io.idu_allowin.asUInt 

    io.out.inst := Mux(io.out.valid && io.out.csrBadv === 0.U && io.inst.data_ok === 1.U, io.inst.rdata, 0.U(instBitWidth.W))
    //io.out.inst := Mux(io.out.valid && io.out.csrBadv === 0.U, io.inst.rdata, 0.U(instBitWidth.W))
    io.out.pc := pc

}

/*
addr -> slave
master <-addr_ok
master <-data_ok rdata
*/
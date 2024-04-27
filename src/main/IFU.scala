package loongarch32i

import chisel3._
import chisel3.util._
import Myconsts._

class IfuMessage extends Bundle {
    val inst = Output(UInt(instBitWidth.W))
    val pc = Output(UInt(addrBitWidth.W))
    val valid = Output(Bool())
}
class IFU extends Module {
    val io = IO(new Bundle{
        val br = new br_info()
        val inst = new inst_info()
        val out = new IfuMessage()
        val idu_allowin = Input(Bool())
    })

    val to_ifu_valid = (reset.asBool === false.B)
    val ifu_ready_go = true.B 
    val ifu_valid = RegInit(false.B)
    val ifu_allowin = (~ifu_valid) || (io.idu_allowin && ifu_ready_go)
    when(ifu_allowin) {ifu_valid := to_ifu_valid}
    io.out.valid := ifu_valid && ifu_ready_go

    val snPc = Wire(UInt(addrBitWidth.W))
    val dnPc = Wire(UInt(addrBitWidth.W))
    val pc = RegInit(PCStart.U(addrBitWidth.W))

    
    snPc := pc + "h4".U;
    dnPc := Mux(io.br.brTaken === 1.U, io.br.brTarget, snPc)

    // val if_id_inst = Reg(UInt(instBitWidth.W))
    // val if_id_pc = Reg(UInt(addrBitWidth.W))
        
    when(reset.asBool === true.B){
        pc := "h1bfffffc".U(addrBitWidth.W)
    }
    when(to_ifu_valid && ifu_allowin){
        // if_id_inst := io.in.inst_sram_rdata
        // if_id_pc := dnPc
        pc := dnPc
    }

    
    //io.out.ready := !io.out.valid || (io.out.valid & io.in.ready)

    io.inst.inst_sram_en := to_ifu_valid && ifu_allowin
    //io.inst.inst_sram_en := 1.U
    io.inst.inst_sram_we := Fill(4, 0.U)
    io.inst.inst_sram_addr := dnPc
    io.inst.inst_sram_wdata := Fill(32, 0.U)

    io.out.inst := io.inst.inst_sram_rdata
    io.out.pc := pc
}
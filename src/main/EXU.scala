package loongarch32i

import chisel3._
import chisel3.util._
import Myconsts._

class ExuMessage extends Bundle {
    val pc = Output(UInt(addrBitWidth.W))
    val aluRes = Output(UInt(dataBitWidth.W))
    // val pcJump = Output(UInt(1.W))
    val resFromMem = Output(UInt(1.W))
    val grWe = Output(UInt(1.W))    
    val dest = Output(UInt(5.W))
}
class EXU extends Module{
    val io = IO(new Bundle{
        val in = Flipped(Decoupled(new IduMessage))
        val data_sram_rdata = Input(UInt(dataBitWidth.W))    
        val data_sram_en = Output(UInt(1.W))
        val data_sram_we = Output(UInt(4.W))
        val data_sram_addr = Output(UInt(dataBitWidth.W))
        val data_sram_wdata = Output(UInt(dataBitWidth.W))
        val out = Decoupled(new ExuMessage)
    })

    val ex_me_pc = Wire(UInt(addrBitWidth.W))
    val ex_me_aluRes = Wire(UInt(dataBitWidth.W))
    val ex_me_resFromMem = RegInit(UInt(1.W))
    val ex_me_grWe = RegInit(UInt(1.W))
    val ex_me_dest = RegInit(UInt(5.W))

    val memRes = Wire(UInt(dataBitWidth.W))
    memRes := io.data_sram_rdata


    val u_alu = Module(new alu)

    u_alu.io.aluOp := io.in.bits.aluOp
    u_alu.io.aluSrc1 := io.in.bits.aluSrc1
    u_alu.io.aluSrc2 := io.in.bits.aluSrc2

    io.out.bits.aluRes := ex_me_aluRes
    io.out.bits.resFromMem := ex_me_resFromMem
    io.out.bits.grWe := ex_me_grWe
    io.out.bits.dest := ex_me_dest
    io.out.bits.pc := ex_me_pc

    val dataReceived = RegInit(false.B)
    when(io.in.valid && !dataReceived && reset === false.B){
        ex_me_pc := io.in.bits.pc
        ex_me_aluRes := u_alu.io.aluRes
        ex_me_resFromMem := io.in.bits.resFromMem
        ex_me_grWe := io.in.bits.grWe
        ex_me_dest := io.in.bits.dest
        dataReceived := true.B
    }.elsewhen(io.out.ready && reset === false.B){
        dataReceived := false.B
    }
    io.in.ready := !dataReceived && io.out.ready
    io.out.valid := dataReceived && io.in.valid

    io.data_sram_en := 1.U 
    io.data_sram_we := Mux((io.in.bits.memWe === 1.U && io.out.valid), Fill(4, 1.U), Fill(4, 0.U))
    io.data_sram_addr := u_alu.io.aluRes
    io.data_sram_wdata := io.in.bits.rfdata
}
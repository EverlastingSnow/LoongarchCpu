package loongarch32i

import chisel3._
import chisel3.util._
import Myconsts._

class MemMessage extends Bundle {
    val aluRes = Output(UInt(dataBitWidth.W))
    val grWe = Output(UInt(1.W))
    val dest = Output(UInt(5.W))
    val pc = Output(UInt(addrBitWidth.W))
    val resFrom = Output(UInt(resTypeLen.W))
    val valid = Output(Bool())
}
class MEM extends Module {
    val io = IO(new Bundle{
        val in = Flipped(new ExuMessage())
        val out = new MemMessage()
        val mem_allowin = Output(Bool())
        val mem_stop    = Output(Bool())
        val wbu_allowin = Input(Bool())
        val wbu_stop    = Input(Bool())
        val data_sram_rdata = Input(UInt(dataBitWidth.W))
        val foward = new foward_info()

        val in_csr = Flipped(new csr_op_info())
        val out_csr = new csr_op_info()       
    })
    
    val ex_me_pc = RegInit(0.U(addrBitWidth.W))
    val ex_me_aluRes = RegInit(0.U(dataBitWidth.W))
    val ex_me_resFrom = RegInit(0.U(resTypeLen.W))
    val ex_me_grWe = RegInit(0.U(1.W))
    val ex_me_dest = RegInit(0.U(5.W)) 
    val ex_me_rfdata = RegInit(0.U(dataBitWidth.W))
    val ex_me_wordType = RegInit(0.U(wordTypeLen.W))
    val ex_me_ldaddr   = RegInit(0.U(2.W))

    val ex_me_csrBadv     = RegInit(0.U(1.W))
    val ex_me_csrBadaddr  = RegInit(0.U(addrBitWidth.W))
    val ex_me_csrExcp     = RegInit(0.U(2.W))
    val ex_me_csrEcode    = RegInit(0.U(6.W))
    val ex_me_csrEsubcode = RegInit(0.U(9.W))
    val ex_me_csrPc       = RegInit(0.U(addrBitWidth.W))
    val ex_me_csrMaskWe   = RegInit(0.U(1.W))
    val ex_me_csrMask     = RegInit(0.U(dataBitWidth.W))
    val ex_me_csrWen      = RegInit(0.U(1.W))
    val ex_me_csrWaddr    = RegInit(0.U(ctrlRegLen.W))
    val ex_me_csrWdata    = RegInit(0.U(dataBitWidth.W))
    val ex_me_csrRaddr    = RegInit(0.U(ctrlRegLen.W))

    val mem_ready_go = true.B 
    val mem_valid = RegInit(false.B)
    val mem_allowin = (~mem_valid) || (io.wbu_allowin && mem_ready_go)
    when(mem_allowin || io.wbu_stop) {mem_valid := io.in.valid && !io.wbu_stop}
    io.out.valid := mem_ready_go && mem_valid && !io.wbu_stop
    io.mem_allowin := mem_allowin
    when (mem_allowin && io.in.valid){
        ex_me_pc := io.in.pc
        ex_me_aluRes := io.in.aluRes
        ex_me_resFrom := io.in.resFrom
        ex_me_grWe := io.in.grWe
        ex_me_dest := io.in.dest
        ex_me_rfdata := io.in.rfdata
        ex_me_wordType := io.in.wordType
        ex_me_ldaddr   := io.in.ldaddr

        ex_me_csrBadaddr := io.in_csr.badaddr
        ex_me_csrBadv  := io.in_csr.badv
        ex_me_csrExcp := io.in_csr.excp
        ex_me_csrEcode := io.in_csr.ecode
        ex_me_csrEsubcode := io.in_csr.esubcode
        ex_me_csrMaskWe := io.in_csr.mask_we
        ex_me_csrMask := io.in_csr.mask
        ex_me_csrWen := io.in_csr.wen
        ex_me_csrWaddr := io.in_csr.waddr
        ex_me_csrWdata := io.in_csr.wdata
        ex_me_csrRaddr := io.in_csr.raddr
        ex_me_csrPc    := io.in_csr.pc
    }

    val rdataB = Wire(UInt(8.W))
    rdataB := MuxCase(io.data_sram_rdata(7, 0), Seq(
        (ex_me_ldaddr === 0.U) -> io.data_sram_rdata(7, 0),
        (ex_me_ldaddr === 1.U) -> io.data_sram_rdata(15, 8),
        (ex_me_ldaddr === 2.U) -> io.data_sram_rdata(23, 16),
        (ex_me_ldaddr === 3.U) -> io.data_sram_rdata(31, 24)
    ))

    val rdataH = Wire(UInt(16.W))
    rdataH := MuxCase(io.data_sram_rdata(15, 0), Seq(
        (ex_me_ldaddr === 0.U) -> io.data_sram_rdata(15, 0),
        (ex_me_ldaddr === 2.U) -> io.data_sram_rdata(31, 16)
    ))

    val finalRes = Wire(UInt(dataBitWidth.W))
    finalRes := Mux(ex_me_resFrom === 1.U, 
    MuxCase(io.data_sram_rdata, Seq(
        (ex_me_wordType === W) -> io.data_sram_rdata,
        (ex_me_wordType === B) -> Cat(Fill(24, rdataB(7)), rdataB(7, 0)),
        (ex_me_wordType === H) -> Cat(Fill(16, rdataH(15)), rdataH(15, 0)),
        (ex_me_wordType === BU) -> Cat(Fill(24, 0.U), rdataB(7, 0)),
        (ex_me_wordType === HU) -> Cat(Fill(16, 0.U), rdataH(15, 0))
    ))
    , ex_me_aluRes)

    io.foward.csr_choke := ex_me_grWe & mem_valid & (ex_me_csrWaddr === 0x40.U).asUInt & !(io.wbu_stop)
    io.foward.w_valid := ex_me_grWe & mem_valid & (ex_me_dest =/= 0.U).asUInt & !(io.wbu_stop)
    io.foward.w_choke := ex_me_grWe & mem_valid & (ex_me_dest =/= 0.U).asUInt & (ex_me_resFrom === 2.U) & !io.wbu_stop
    io.foward.waddr := ex_me_dest
    io.foward.wdata := finalRes

    io.out.aluRes := finalRes
    io.out.grWe := ex_me_grWe
    io.out.dest := ex_me_dest
    io.out.pc := ex_me_pc
    io.out.resFrom := ex_me_resFrom
    io.mem_stop := io.wbu_stop || (mem_valid && ex_me_csrExcp =/= 0.U)

    io.out_csr.badv := ex_me_csrBadv
    io.out_csr.badaddr := ex_me_csrBadaddr
    io.out_csr.excp := ex_me_csrExcp
    io.out_csr.ecode := ex_me_csrEcode
    io.out_csr.esubcode := ex_me_csrEsubcode 
    io.out_csr.mask_we := ex_me_csrMaskWe
    io.out_csr.mask := ex_me_csrMask
    io.out_csr.wen := ex_me_csrWen
    io.out_csr.waddr := ex_me_csrWaddr 
    io.out_csr.wdata := ex_me_csrWdata
    io.out_csr.raddr := ex_me_csrRaddr
    io.out_csr.pc    := ex_me_csrPc
}
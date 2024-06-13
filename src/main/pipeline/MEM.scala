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
        val data_sram_data_ok = Input(UInt(1.W))
        val foward = new foward_info()

        val in_csr = Flipped(new csr_op_info())
        val out_csr = new csr_op_info()       
    })

    val mem_ready_go = true.B 
    val mem_valid = RegInit(false.B)
    val mem_allowin = (~mem_valid) || (io.wbu_allowin && mem_ready_go)
    when(mem_allowin || io.wbu_stop) {mem_valid := io.in.valid && !io.wbu_stop}
     io.out.valid := mem_ready_go && mem_valid && !io.wbu_stop
    //io.out.valid := mem_ready_go && mem_valid && !io.wbu_stop
    io.mem_allowin := mem_allowin

    val wr_mem_reg_enable = mem_allowin && io.in.valid
    val ex_me = RegEnable(io.in, wr_mem_reg_enable)
    val ex_me_csr = RegEnable(io.in_csr, wr_mem_reg_enable)
//ld指令
    val rdataB = Wire(UInt(8.W))
    rdataB := MuxCase(io.data_sram_rdata(7, 0), Seq(
        (ex_me.ldaddr === 0.U) -> io.data_sram_rdata(7, 0),
        (ex_me.ldaddr === 1.U) -> io.data_sram_rdata(15, 8),
        (ex_me.ldaddr === 2.U) -> io.data_sram_rdata(23, 16),
        (ex_me.ldaddr === 3.U) -> io.data_sram_rdata(31, 24)
    ))

    val rdataH = Wire(UInt(16.W))
    rdataH := MuxCase(io.data_sram_rdata(15, 0), Seq(
        (ex_me.ldaddr === 0.U) -> io.data_sram_rdata(15, 0),
        (ex_me.ldaddr === 2.U) -> io.data_sram_rdata(31, 16)
    ))

    val finalRes = Wire(UInt(dataBitWidth.W))
    finalRes := Mux(ex_me.resFrom === 1.U, 
    MuxCase(io.data_sram_rdata, Seq(
        (ex_me.wordType === W) -> io.data_sram_rdata,
        (ex_me.wordType === B) -> Cat(Fill(24, rdataB(7)), rdataB(7, 0)),
        (ex_me.wordType === H) -> Cat(Fill(16, rdataH(15)), rdataH(15, 0)),
        (ex_me.wordType === BU) -> Cat(Fill(24, 0.U), rdataB(7, 0)),
        (ex_me.wordType === HU) -> Cat(Fill(16, 0.U), rdataH(15, 0))
    ))
    , ex_me.aluRes)

    io.foward.csr_choke := ex_me.grWe & mem_valid & (ex_me_csr.waddr === 0x40.U).asUInt & !(io.wbu_stop)
    io.foward.w_valid   := ex_me.grWe & mem_valid & (ex_me.dest =/= 0.U).asUInt & !(io.wbu_stop)
    io.foward.w_choke   := ex_me.grWe & mem_valid & (ex_me.dest =/= 0.U).asUInt & (ex_me.resFrom === 2.U) & !io.wbu_stop
    io.foward.waddr     := ex_me.dest
    io.foward.wdata     := finalRes

    io.out.aluRes  := finalRes
    io.out.grWe    := ex_me.grWe
    io.out.dest    := ex_me.dest
    io.out.pc      := ex_me.pc
    io.out.resFrom := ex_me.resFrom
    io.mem_stop    := io.wbu_stop || (mem_valid && ex_me_csr.excp =/= 0.U)

    io.out_csr.badv     := ex_me_csr.badv
    io.out_csr.badaddr  := ex_me_csr.badaddr
    io.out_csr.excp     := ex_me_csr.excp
    io.out_csr.ecode    := ex_me_csr.ecode
    io.out_csr.esubcode := ex_me_csr.esubcode 
    io.out_csr.mask_we  := ex_me_csr.mask_we
    io.out_csr.mask     := ex_me_csr.mask
    io.out_csr.wen      := ex_me_csr.wen
    io.out_csr.waddr    := ex_me_csr.waddr 
    io.out_csr.wdata    := ex_me_csr.wdata
    io.out_csr.raddr    := ex_me_csr.raddr
    io.out_csr.pc       := ex_me_csr.pc
}
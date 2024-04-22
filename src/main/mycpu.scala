package loongarch32i

import chisel3._
import chisel3.util._
import Myconsts._

class mycpuIO extends Bundle {
    //inst sram
    val inst_sram_we = Output(Bool())
    val inst_sram_addr = Output(UInt(addrBitWidth.W))
    val inst_sram_wdata = Output(UInt(instBitWidth.W))
    val inst_sram_rdata = Input(UInt(instBitWidth.W))
    //data sram
    val data_sram_we = Output(UInt(1.W))
    val data_sram_addr = Output(UInt(addrBitWidth.W))
    val data_sram_wdata = Output(UInt(dataBitWidth.W))
    val data_sram_rdata = Input(UInt(dataBitWidth.W))
    //trace debug
    val debug_wb_pc = Output(UInt(addrBitWidth.W))
    val debug_wb_rf_we = Output(UInt(4.W))
    val debug_wb_rf_wnum = Output(UInt(5.W))
    val debug_wb_rf_wdata = Output(UInt(dataBitWidth.W))
}

class mycpu extends Module {
    val io = IO(new mycpuIO())
    
    val valid = RegInit(true.B)
    val pc = RegInit(PCStart.U(addrBitWidth.W))

    val brTaken = Wire(Bool())
    val snPc = Wire(UInt(addrBitWidth.W))
    val dnPc = Wire(UInt(addrBitWidth.W))
    val inst = Wire(UInt(addrBitWidth.W))

    val op31_26 = Wire(UInt(6.W))
    val op25_22 = Wire(UInt(4.W))
    val op21_20 = Wire(UInt(2.W))
    val op19_15 = Wire(UInt(5.W))
    val rd = Wire(UInt(5.W))
    val rj = Wire(UInt(5.W))
    val rk = Wire(UInt(5.W))
    val i12 = Wire(UInt(12.W))
    val i20 = Wire(UInt(20.W))
    val i16 = Wire(UInt(16.W))
    val i26 = Wire(UInt(26.W))
    val op31_26d = Wire(UInt(64.W))
    val op25_22d = Wire(UInt(16.W))
    val op21_20d = Wire(UInt(4.W))
    val op19_15d = Wire(UInt(32.W))

    val instAddW = Wire(UInt(1.W))
    val instSubW = Wire(UInt(1.W))
    val instSlt = Wire(UInt(1.W))
    val instSltu = Wire(UInt(1.W))
    val instNor = Wire(UInt(1.W))
    val instAnd = Wire(UInt(1.W))
    val instOr = Wire(UInt(1.W))
    val instXor = Wire(UInt(1.W))
    val instSlliW = Wire(UInt(1.W))
    val instSrliW = Wire(UInt(1.W))
    val instSraiW = Wire(UInt(1.W))
    val instAddiW = Wire(UInt(1.W))
    val instLdW = Wire(UInt(1.W))
    val instStW = Wire(UInt(1.W))
    val instJirl = Wire(UInt(1.W))
    val instB = Wire(UInt(1.W))
    val instBl = Wire(UInt(1.W))
    val instBeq = Wire(UInt(1.W))
    val instBne = Wire(UInt(1.W))
    val instLu12iW = Wire(UInt(1.W))

    val brTarget = Wire(UInt(addrBitWidth.W))

    when(reset === true.B){
        valid := false.B
        pc := "h1bfffffc".U(addrBitWidth.W)
    }.otherwise{
        valid := true.B
        pc := dnPc
    }

    snPc := pc + "h4".U;
    dnPc := Mux(brTaken, brTarget, snPc)

    io.inst_sram_we := false.B;
    io.inst_sram_addr := pc;
    io.inst_sram_wdata := 0.U(instBitWidth.W)
    inst := io.inst_sram_rdata;

    op31_26 := inst(31, 26)
    op25_22 := inst(25, 22)
    op21_20 := inst(21, 20)
    op19_15 := inst(19, 15)
    rd := inst(4, 0)    
    rj := inst(9, 5)
    rk := inst(14, 10)
    i12 := inst(21, 10)    
    i20 := inst(24, 5)
    i16 := inst(25, 10)
    i26 := Cat(inst(9, 0), inst(25, 10))

    val deco0 = Module(new decoder6_64)
    deco0.io.in := op31_26
    op31_26d := deco0.io.out

    val deco1 = Module(new decoder4_16)
    deco1.io.in := op25_22
    op25_22d := deco1.io.out

    val deco2 = Module(new decoder2_4)
    deco2.io.in := op21_20
    op21_20d := deco2.io.out

    val deco3 = Module(new decoder5_32)
    deco3.io.in := op19_15
    op19_15d := deco3.io.out

    instAddW := op31_26d(0) & op25_22d(0) & op21_20d(1) && op19_15d(0)
    instSubW := op31_26d(0) & op25_22d(0) & op21_20d(1) && op19_15d(2)
    instSlt  := op31_26d(0) & op25_22d(0) & op21_20d(1) && op19_15d(4)
    instSltu := op31_26d(0) & op25_22d(0) & op21_20d(1) && op19_15d(5)
    instNor  := op31_26d(0) & op25_22d(0) & op21_20d(1) && op19_15d(8)
    instAnd  := op31_26d(0) & op25_22d(0) & op21_20d(1) && op19_15d(9)
    instOr   := op31_26d(0) & op25_22d(0) & op21_20d(1) && op19_15d(10)
    instXor  := op31_26d(0) & op25_22d(0) & op21_20d(1) && op19_15d(11)
    instSlliW:= op31_26d(0) & op25_22d(1) & op21_20d(0) && op19_15d(1)
    instSrliW:= op31_26d(0) & op25_22d(1) & op21_20d(0) && op19_15d(9)
    instSraiW:= op31_26d(0) & op25_22d(1) & op21_20d(0) && op19_15d(17)
    instAddiW:= op31_26d(0) & op25_22d(10)
    instLdW := op31_26d(10) & op25_22d(2)
    instStW := op31_26d(10) & op25_22d(6)
    instJirl:= op31_26d(19)
    instB   := op31_26d(20)
    instBl  := op31_26d(21)
    instBeq := op31_26d(22)
    instBne := op31_26d(23)
    instLu12iW:= op31_26d(5) & (~inst(25))

    val aluOp = Wire(UInt(12.W))
    aluOp := Cat(instAddW | instAddiW | instLdW | instStW | instJirl | instBl, 
    instSubW, instSlt, instSltu, instAnd, instNor, instOr, instXor, instSlliW, instSrliW, instSraiW, instLu12iW)

    val need_ui5 = Wire(UInt(1.W))
    val need_si12 = Wire(UInt(1.W))
    val need_si16 = Wire(UInt(1.W))
    val need_si20 = Wire(UInt(1.W))
    val need_si26 = Wire(UInt(1.W))
    val src2_is_4 = Wire(UInt(1.W))

    need_ui5 := instSlliW | instSrliW | instSraiW
    need_si12 := instAddiW | instLdW | instStW
    need_si16 := instJirl | instBeq | instBne
    need_si20 := instLu12iW
    need_si26 := instB | instBl
    src2_is_4 := instJirl | instBl

    val imm = Wire(UInt(dataBitWidth.W))
    //val extend1 = Module(new SignExtender(32, dataBitWidth))
    //val extend2 = Module(new SignExtender(32, dataBitWidth))
    //extend1.io.original := Cat(i20(19, 0), 0.U(12.W))
    //extend2.io.original := Cat(Fill(20, i12(11)), i12(11, 0))
    imm := Mux(src2_is_4 === 1.U, "h4".U(dataBitWidth.W),
     Mux(need_si20 === 1.U, Cat(i20(19, 0), 0.U(12.W)),  
     Mux(need_ui5  === 1.U, rk, Cat(Fill(20, i12(11)), i12(11, 0)))))
    val brOffs = Wire(UInt(addrBitWidth.W))

    //val extend3 = Module(new SignExtender(32, addrBitWidth))
    //val extend4 = Module(new SignExtender(32, addrBitWidth))
    //extend3.io.original := Cat(Fill(4, i26(25)), i26(25, 0), 0.U(2.W))   
    //extend4.io.original := Cat(Fill(14, i16(15)), i16(15, 0), 0.U(2.W))
    brOffs := Mux(need_si26 === 1.U, Cat(Fill(4, i26(25)), i26(25, 0), 0.U(2.W)), 
    Cat(Fill(14, i16(15)), i16(15, 0), 0.U(2.W)))

    val jirlOffs = Wire(UInt(addrBitWidth.W))
    //val extend5 = Module(new SignExtender(32, addrBitWidth))
    //extend5.io.original := Cat(Fill(14, i16(15)), i16(15, 0), 0.U(2.W))
    jirlOffs := Cat(Fill(14, i16(15)), i16(15, 0), 0.U(2.W))

    val srcRegIsRd = Wire(UInt(1.W))
    val src1IsPc = Wire(UInt(1.W))
    val src2IsImm = Wire(UInt(1.W))
    srcRegIsRd := instBeq | instBne | instStW
    src1IsPc := instJirl | instBl
    src2IsImm := instSlliW | instSrliW | instSraiW | instLdW | instStW | instLu12iW | instJirl | instBl | instAddiW
    
    val resFromMem = Wire(UInt(1.W))
    val dstIsR1 = Wire(UInt(1.W))
    val grWe = Wire(UInt(1.W))
    val memWe = Wire(UInt(1.W))
    val dest = Wire(UInt(5.W))
    resFromMem := instLdW
    dstIsR1 := instBl
    grWe := ~instStW & ~instBeq & ~instBne & ~instB
    memWe := instStW
    dest := Mux(dstIsR1 === 1.U, 1.U(5.W), rd)

    val rfRaddr1 = Wire(UInt(5.W))
    val rfRaddr2 = Wire(UInt(5.W))
    val rfRdata1 = Wire(UInt(dataBitWidth.W))
    val rfRdata2 = Wire(UInt(dataBitWidth.W))
    val rfWe     = Wire(UInt(1.W))
    val rfWaddr  = Wire(UInt(5.W))
    val rfWdata  = Wire(UInt(dataBitWidth.W))

    rfRaddr1 := rj
    rfRaddr2 := Mux(srcRegIsRd === 1.U, rd, rk)

    val u_regfile = Module(new regFile)
    u_regfile.io.raddr1 := rfRaddr1
    u_regfile.io.raddr2 := rfRaddr2
    rfRdata1 := u_regfile.io.rdata1
    rfRdata2 := u_regfile.io.rdata2
    u_regfile.io.we     := rfWe 
    u_regfile.io.waddr  := rfWaddr 
    u_regfile.io.wdata  := rfWdata

    val rjValue = Wire(UInt(dataBitWidth.W))
    val rkValue = Wire(UInt(dataBitWidth.W))

    rjValue := rfRdata1
    rkValue := rfRdata2

    val rjEqRd = Wire(UInt(1.W))
    rjEqRd := (rjValue === rkValue)

    brTaken := ((instBeq === 1.U && rjEqRd === 1.U) || (instBne === 1.U && !rjEqRd === 1.U) || (instJirl === 1.U) || (instBl === 1.U) || (instB === 1.U)) && valid
    brTarget := Mux((instBeq === 1.U || instBne === 1.U || instBl === 1.U || instB === 1.U), pc + brOffs, rjValue + jirlOffs)

    val aluSrc1 = Wire(UInt(dataBitWidth.W))
    val aluSrc2 = Wire(UInt(dataBitWidth.W))
    val aluRes  = Wire(UInt(dataBitWidth.W))

    aluSrc1 := Mux(src1IsPc === 1.U, pc(addrBitWidth - 1, 0), rjValue)
    aluSrc2 := Mux(src2IsImm === 1.U, imm, rkValue)

    val u_alu = Module(new alu)

    u_alu.io.aluOp := aluOp
    u_alu.io.aluSrc1 := aluSrc1
    u_alu.io.aluSrc2 := aluSrc2
    aluRes :=u_alu.io.aluRes

    io.data_sram_we := memWe === 1.U && valid 
    io.data_sram_addr := aluRes
    io.data_sram_wdata := rkValue

    val memRes = Wire(UInt(dataBitWidth.W))
    memRes := io.data_sram_rdata

    val finalRes = Wire(UInt(dataBitWidth.W))
    finalRes := Mux(resFromMem === 1.U, memRes, aluRes)

    rfWe := grWe === 1.U && valid
    rfWaddr := dest
    rfWdata := finalRes

    io.debug_wb_pc := pc 
    io.debug_wb_rf_we := Cat(rfWe, rfWe, rfWe, rfWe)
    io.debug_wb_rf_wnum := dest
    io.debug_wb_rf_wdata := finalRes

}
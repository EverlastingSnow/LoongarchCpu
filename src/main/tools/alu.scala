package loongarch32i

import chisel3._
import chisel3.util._
import Myconsts._

class aluIo extends Bundle {
    val aluOp = Input(UInt(12.W))
    val aluSrc1 = Input(UInt(dataBitWidth.W))
    val aluSrc2 = Input(UInt(dataBitWidth.W))
    val aluRes = Output(UInt(dataBitWidth.W))
}

class alu extends Module {
    val io = IO(new aluIo)

    val opAdd = Wire(UInt(1.W))
    val opSub = Wire(UInt(1.W))
    val opSlt = Wire(UInt(1.W))
    val opSltu = Wire(UInt(1.W))
    val opAnd = Wire(UInt(1.W))
    val opNor = Wire(UInt(1.W))
    val opOr = Wire(UInt(1.W))
    val opXor = Wire(UInt(1.W))
    val opSll = Wire(UInt(1.W))
    val opSrl = Wire(UInt(1.W))
    val opSra = Wire(UInt(1.W))
    val opLui = Wire(UInt(1.W))

    opAdd := io.aluOp === 1.U
    opSub := io.aluOp === 2.U
    opSlt := io.aluOp === 3.U
    opSltu := io.aluOp === 4.U
    opAnd := io.aluOp === 5.U
    opNor := io.aluOp === 6.U
    opOr := io.aluOp === 7.U
    opXor := io.aluOp === 8.U
    opSll := io.aluOp === 9.U
    opSrl := io.aluOp === 10.U
    opSra := io.aluOp === 11.U
    opLui := io.aluOp === 12.U

    val addSubRes = Wire(UInt(dataBitWidth.W))
    val sltRes = Wire(UInt(dataBitWidth.W))
    val sltuRes = Wire(UInt(dataBitWidth.W))
    val andRes = Wire(UInt(dataBitWidth.W))
    val norRes = Wire(UInt(dataBitWidth.W))
    val orRes = Wire(UInt(dataBitWidth.W))
    val xorRes = Wire(UInt(dataBitWidth.W))
    val luiRes = Wire(UInt(dataBitWidth.W))
    val sllRes = Wire(UInt(dataBitWidth.W))
    val sr64Res = Wire(UInt(64.W))
    val srRes = Wire(UInt(dataBitWidth.W))

    val adderA = Wire(UInt(dataBitWidth.W))
    val adderB = Wire(UInt(dataBitWidth.W))
    val adderCin = Wire(UInt(1.W))
    val adderAll = Wire(UInt((dataBitWidth + 1).W))
    val adderRes = Wire(UInt(dataBitWidth.W))
    val adderCout = Wire(UInt(1.W))

    adderA   := io.aluSrc1
    adderB   := Mux(((opSub | opSlt) | opSltu) === 1.U, ~io.aluSrc2, io.aluSrc2)
    adderCin := Mux(((opSub | opSlt) | opSltu) === 1.U, 1.U, 0.U)
    adderAll := adderA +& adderB +& adderCin
    adderCout := adderAll(dataBitWidth)
    adderRes := adderAll(dataBitWidth - 1, 0)

    addSubRes := adderRes

    sltRes := Cat(Fill(31, 0.U), 
    (io.aluSrc1(31) & ~io.aluSrc2(31)) | 
    ((~(io.aluSrc1(31) ^ io.aluSrc2(31))) & adderRes(31))
    )

    sltuRes := Cat(Fill(31, 0.U), ~adderCout)
    andRes := io.aluSrc1 & io.aluSrc2
    orRes := io.aluSrc1 | io.aluSrc2
    norRes := ~orRes
    xorRes := io.aluSrc1 ^ io.aluSrc2
    luiRes := io.aluSrc2

    sllRes := io.aluSrc1 << io.aluSrc2(4, 0)
    sr64Res := Cat(Fill(32, (opSra & io.aluSrc1(31))), io.aluSrc1(31, 0)) >> io.aluSrc2(4, 0) //QUESTION

    srRes := sr64Res(31, 0)
    io.aluRes := MuxCase(0.U, Seq(
        ((opAdd | opSub) === 1.U) -> addSubRes,
        (opSlt === 1.U) -> sltRes,
        (opSltu === 1.U) -> sltuRes,
        (opAnd === 1.U) -> andRes,
        (opNor === 1.U) -> norRes,
        (opOr === 1.U) -> orRes,
        (opXor === 1.U) -> xorRes,
        (opLui === 1.U) -> luiRes,
        (opSll === 1.U) -> sllRes,
        ((opSra | opSrl) === 1.U) -> srRes
    ))
/*
    io.aluRes := (Fill(dataBitWidth, (opAdd | opSub)) & addSubRes) |
                 (Fill(dataBitWidth, opSlt)             & sltRes)    |
                 (Fill(dataBitWidth, opSltu)            & sltuRes)   |
                 (Fill(dataBitWidth, opAnd)             & andRes)    |
                 (Fill(dataBitWidth, opNor)             & norRes)    |
                 (Fill(dataBitWidth, opOr)              & orRes)     |
                 (Fill(dataBitWidth, opXor)             & xorRes)    |
                 (Fill(dataBitWidth, opLui)             & luiRes)    |
                 (Fill(dataBitWidth, opSll)             & sllRes)    |
                 (Fill(dataBitWidth, (opSrl | opSra))   & srRes)
*/
}
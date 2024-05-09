package loongarch32i

import chisel3._
import chisel3.util._
import Myconsts._

class aluIo extends Bundle {
    val aluOp = Input(UInt(aluOpLen.W))
    val aluSrc1 = Input(UInt(dataBitWidth.W))
    val aluSrc2 = Input(UInt(dataBitWidth.W))
    val aluRes = Output(UInt(dataBitWidth.W))
    val aluReady = Output(Bool())
}

class alu extends Module {
    val io = IO(new aluIo)

    val opAdd = WireInit(0.U(1.W))
    val opSub = WireInit(0.U(1.W))
    val opSlt = WireInit(0.U(1.W))
    val opSltu = WireInit(0.U(1.W))
    val opAnd = WireInit(0.U(1.W))
    val opNor = WireInit(0.U(1.W))
    val opOr = WireInit(0.U(1.W))
    val opXor = WireInit(0.U(1.W))
    val opSll = WireInit(0.U(1.W))
    val opSrl = WireInit(0.U(1.W))
    val opSra = WireInit(0.U(1.W))
    val opLui = WireInit(0.U(1.W))
    val opMul = WireInit(0.U(1.W))
    val opMulh = WireInit(0.U(1.W))
    val opMulhu = WireInit(0.U(1.W))
    val opDiv  = WireInit(0.U(1.W))
    val opDivu = WireInit(0.U(1.W))
    val opMod  = WireInit(0.U(1.W))
    val opModu = WireInit(0.U(1.W))

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
    opMul := io.aluOp === 13.U
    opMulh := io.aluOp === 14.U
    opMulhu:= io.aluOp === 15.U
    opDiv  := io.aluOp === 16.U
    opDivu := io.aluOp === 17.U
    opMod  := io.aluOp === 18.U
    opModu := io.aluOp === 19.U
    
    val addSubRes = WireInit(0.U(dataBitWidth.W))
    val sltRes = WireInit(0.U(dataBitWidth.W))
    val sltuRes = WireInit(0.U(dataBitWidth.W))
    val andRes = WireInit(0.U(dataBitWidth.W))
    val norRes = WireInit(0.U(dataBitWidth.W))
    val orRes = WireInit(0.U(dataBitWidth.W))
    val xorRes = WireInit(0.U(dataBitWidth.W))
    val luiRes = WireInit(0.U(dataBitWidth.W))
    val sllRes = WireInit(0.U(dataBitWidth.W))
    val sr64Res = WireInit(0.U(64.W))
    val srRes = WireInit(0.U(dataBitWidth.W))

    val adderA = WireInit(0.U(dataBitWidth.W))
    val adderB = WireInit(0.U(dataBitWidth.W))
    val adderCin = WireInit(0.U(1.W))
    val adderAll = WireInit(0.U((dataBitWidth + 1).W))
    val adderRes = WireInit(0.U(dataBitWidth.W))
    val adderCout = WireInit(0.U(1.W))

    val mulRes = WireInit(0.U(hiLoBitWidth.W))

    val quotient  = WireInit(0.U(dataBitWidth.W))
    val remainder = WireInit(0.U(dataBitWidth.W))
    
    val myDiv = Module(new Div())
    val myMul = Module(new Mul())

    myDiv.io.start := Mux((opDiv | opMod | opDivu | opModu) === 1.U, true.B, false.B)
    myDiv.io.src1 := Mux((opDiv | opMod | opDivu | opModu) === 1.U, io.aluSrc1, 0.U)
    myDiv.io.src2 := Mux((opDiv | opMod | opDivu | opModu) === 1.U, io.aluSrc2, 0.U)
    myDiv.io.signed := !(opDivu | opModu) 
    
    myMul.io.start := Mux((opMul | opMulh| opMulhu) === 1.U, true.B, false.B)
    myMul.io.src1 := Mux((opMul | opMulh | opMulhu) === 1.U, io.aluSrc1, 0.U)
    myMul.io.src2 := Mux((opMul | opMulh | opMulhu) === 1.U, io.aluSrc2, 0.U)
    myMul.io.signed := !opMulhu

    io.aluReady := Mux((opMul | opMulh | opMulhu) === 1.U, myMul.io.ready, Mux((opDiv | opMod | opDivu | opModu) === 1.U, myDiv.io.ready, true.B))

    when(opSub === 1.U || opSlt === 1.U || opSltu === 1.U || opAdd === 1.U){
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
    }.elsewhen(opAnd === 1.U){
        andRes := io.aluSrc1 & io.aluSrc2
    }.elsewhen(opOr ===  1.U || opNor === 1.U){
        orRes := io.aluSrc1 | io.aluSrc2
        norRes := ~orRes
    }.elsewhen(opXor === 1.U){
        xorRes := io.aluSrc1 ^ io.aluSrc2
    }.elsewhen(opLui === 1.U){
        luiRes := io.aluSrc2
    }.elsewhen(opSll === 1.U || opSra === 1.U || opSrl === 1.U){
        sllRes := io.aluSrc1 << io.aluSrc2(4, 0)
        sr64Res := Cat(Fill(32, (opSra & io.aluSrc1(31))), io.aluSrc1(31, 0)) >> io.aluSrc2(4, 0) //QUESTION
        srRes := sr64Res(31, 0)
    }

    when(myMul.io.ready){
        mulRes := myMul.io.result
        myMul.io.allow_to_go := true.B
    }.otherwise{
        myMul.io.allow_to_go := false.B
        mulRes := 0.U
    }
    when(myDiv.io.ready){
        remainder := myDiv.io.result(63, 32)
        quotient := myDiv.io.result(31, 0)
        myDiv.io.allow_to_go := true.B
    }.otherwise{
        myDiv.io.allow_to_go := false.B
        remainder := 0.U
        quotient := 0.U
    }
    io.aluRes := MuxCase(0.U, Seq(
        ((opAdd | opSub) === 1.U) -> addSubRes,
        (opSlt === 1.U)           -> sltRes,
        (opSltu === 1.U)          -> sltuRes,
        (opAnd === 1.U)           -> andRes,
        (opNor === 1.U)           -> norRes,
        (opOr === 1.U)            -> orRes,
        (opXor === 1.U)           -> xorRes,
        (opLui === 1.U)           -> luiRes,
        (opSll === 1.U)           -> sllRes,
        ((opSra | opSrl) === 1.U) -> srRes,
        (opMul === 1.U)           -> mulRes(31, 0),
        ((opMulh|opMulhu) === 1.U)-> mulRes(63, 32),
        ((opDiv | opDivu) === 1.U)-> quotient(31, 0),
        ((opMod | opModu) === 1.U)-> remainder(31, 0)
    ))

}
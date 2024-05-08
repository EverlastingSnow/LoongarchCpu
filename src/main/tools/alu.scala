package loongarch32i

import chisel3._
import chisel3.util._
import Myconsts._

class aluIo extends Bundle {
    val aluOp = Input(UInt(aluOpLen.W))
    val aluSrc1 = Input(UInt(dataBitWidth.W))
    val aluSrc2 = Input(UInt(dataBitWidth.W))
    val aluRes = Output(UInt(dataBitWidth.W))
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

    // val op1s = WireInit(0.U((dataBitWidth + 1).W))
    // val op2s = WireInit(0.U((dataBitWidth + 1).W))
    val MulResS = WireInit(0.U((2 * (dataBitWidth)).W))
    
    // val op1u = WireInit(0.U((dataBitWidth + 1).W))
    // val op2u = WireInit(0.U((dataBitWidth + 1).W))
    val MulResU = WireInit(0.U((2 * (dataBitWidth)).W))
    val quotient  = WireInit(0.U(dataBitWidth.W))
    val remainder = WireInit(0.U(dataBitWidth.W))
    
    // val dividedS = WireInit(0.U((dataBitWidth + 1).W))
    // val divisorS = WireInit(0.U((dataBitWidth + 1).W))
    // val quotientS = WireInit(0.U((dataBitWidth + 1).W))
    // val remainderS = WireInit(0.U((dataBitWidth + 1).W))

    // val dividedU = WireInit(0.U((dataBitWidth + 1).W))
    // val divisorU = WireInit(0.U((dataBitWidth + 1).W))
    // val quotientU = WireInit(0.U((dataBitWidth + 1).W))
    // val remainderU = WireInit(0.U((dataBitWidth + 1).W))

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
    }.elsewhen(opMul === 1.U || opMulh === 1.U){
        //op1s := Cat(io.aluSrc1(31), io.aluSrc1)
        //op2s := Cat(io.aluSrc2(31), io.aluSrc2)
        MulResS := (io.aluSrc1.asSInt * io.aluSrc2.asSInt).asUInt
    }.elsewhen(opMulhu === 1.U){
        // op1u := Cat(0.U, io.aluSrc1)
        // op2u := Cat(0.U, io.aluSrc2)
        MulResU := io.aluSrc1 * io.aluSrc2
    }.elsewhen(opDiv === 1.U || opMod === 1.U || opDivu === 1.U || opModu === 1.U){
        /*
        dividedS := Cat(io.aluSrc1(31), io.aluSrc1)
        divisorS := Cat(io.aluSrc2(31), io.aluSrc2)
        quotientS := dividedS / divisorS
        remainderS := dividedS - divisorS * quotientS*/
        val div_signed = (opDiv | opMod).asBool

        val dividend_signed = io.aluSrc1(31) & div_signed
        val divisor_signed  = io.aluSrc2(31) & div_signed

        val dividend_abs = Mux(dividend_signed, (-io.aluSrc1).asUInt, io.aluSrc1.asUInt)
        val divisor_abs  = Mux(divisor_signed, (-io.aluSrc2).asUInt, io.aluSrc2.asUInt)

        val quotient_signed  = (io.aluSrc1(31) ^ io.aluSrc2(31)) & div_signed
        val remainder_signed = io.aluSrc1(31) & div_signed

        val quotient_abs  = dividend_abs / divisor_abs
        val remainder_abs = dividend_abs - quotient_abs * divisor_abs
        quotient  := Mux(quotient_signed, ((-quotient_abs).asSInt).asUInt, quotient_abs)
        remainder := Mux(remainder_signed, ((-remainder_abs).asSInt).asUInt, remainder_abs)
    }
    // .elsewhen(opDivu === 1.U || opModu === 1.U){
    //     dividedU := Cat(0.U, io.aluSrc1)
    //     divisorU := Cat(0.U, io.aluSrc2)
    //     quotientU := dividedU / divisorU
    //     remainderU := dividedU - divisorU * quotientU
    // }

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
        (opMul === 1.U)           -> MulResS(31, 0),
        (opMulh === 1.U)          -> MulResS(63, 32),
        (opMulhu=== 1.U)          -> MulResU(63, 32),
        ((opDiv | opDivu) === 1.U)-> quotient(31, 0),
        ((opMod | opModu) === 1.U)-> remainder(31, 0)
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
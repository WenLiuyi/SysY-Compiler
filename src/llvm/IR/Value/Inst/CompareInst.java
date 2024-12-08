package llvm.IR.Value.Inst;

public class CompareInst extends Instruction{
    // <result> = icmp <cond> <ty> <op1>, <op2>
    public BinaryOpType opType;

    public CompareInst(BinaryOpType opType) {
        super();
        this.opType = opType;
    }
}

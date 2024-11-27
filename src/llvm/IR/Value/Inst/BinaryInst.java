package llvm.IR.Value.Inst;

public class BinaryInst extends Instruction{
    /* 包含的 LLVM IR 指令形式 :
     a + - *  / % b,
     a >  <  >= <= == != b
     */
    public BinaryOpType opType;

    public BinaryInst(BinaryOpType opType) {
        super();
        this.opType = opType;
    }
}

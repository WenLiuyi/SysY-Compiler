package llvm.IR.Value;

// 每个 User 的 Use 关系数量不定（一条指令中的操作数数量不定，参数调用中参数个数不限）
// User子类：Function、BasicBlock 和 Instruction
public class User extends Value {
}

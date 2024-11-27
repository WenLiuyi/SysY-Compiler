package llvm.IR;

import llvm.IR.Value.Value;

//LLVM的一个编译单元，按文法定义存储：函数列表、main函数、上下文语义记录LlvmContext
public class Module extends Value {
    public LlvmContext llvmContext;     // 记录全局的 Use 关系
    public Module() {
        this.llvmContext = new LlvmContext(this);

    }
}

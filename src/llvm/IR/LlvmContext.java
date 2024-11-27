package llvm.IR;

import java.util.ArrayList;
import llvm.IR.Value.*;

// LLVM 编译单元Module的语义记录，保存类型字典、Use关系（一个Module对应一个LlvmContext）
public class LlvmContext extends Value {
    public Module module;
    public ArrayList<Function> functionsList;
    public ArrayList<GlobalVariable> globalVariablesList;
    public ArrayList<Function> libraryFuncList;
    public ArrayList<Value> stringList;

    public LlvmContext(Module module) {
        super();
        this.module = module;
        this.functionsList = new ArrayList<Function>();
        this.globalVariablesList=new ArrayList<GlobalVariable>();
        this.libraryFuncList=new ArrayList<Function>();
        this.stringList=new ArrayList<Value>();
    }
}

package llvm.IR.Value;

import java.util.ArrayList;

// 每个 User 的 Use 关系数量不定（一条指令中的操作数数量不定，参数调用中参数个数不限）
// User子类：Function、BasicBlock 和 Instruction
public class User extends Value {
    public User(){
        super();
    }
    public User(ValueType valueType,Type type){
        super(valueType,type);
    }
    public User(ValueType valueType,Type type,String name){
        super(valueType,type,name);
    }
}

package llvm.IR.Value;

import llvm.IR.Value.Inst.Instruction;
import frontend.Tree.Exp.*;

import java.util.ArrayList;

// 全局变量
public class GlobalVariable extends Value {
    public ArrayList<Instruction> instList;     // 记录：计算全局变量值时的指令

    public GlobalVariable(Type type, String name,Ident ident) {
        super(ValueType.GlobalVariableTy, type, name);
        this.ident = ident;
        instList = new ArrayList<Instruction>();
    }
    public int getLength(){    // 获取bit数
        if(type.equals(Type.IntegerTyID)) return 32;
        else return 8;
    }
}

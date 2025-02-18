package llvm.IR.Value;

import backend.Mips.MSFunction;
import llvm.IR.SlotTracker;
import llvm.IR.Value.Inst.AllocaInst;
import llvm.IR.Value.Inst.Instruction;

import java.util.ArrayList;


public class Function extends User {
    public ArrayList<BasicBlock> basicBlockList;
    public ArrayList<Value> paramList;
    public SlotTracker slotTracker; // 中间代码打印时，为当前函数域分配虚拟寄存器

    public ArrayList<Value> valueList;  //
    public ArrayList<Value> allocatedValueList;     // AllocaInst指令中的value
    public ArrayList<AllocaInst> allocaInstList;

    public Function prototype_function;     // 原型：调用时创建的函数，以定义时的函数为原型
    public MSFunction msFunction;

    public Function() {
        super();
        this.basicBlockList = new ArrayList<BasicBlock>();
        this.paramList=new ArrayList<Value>();
        this.slotTracker = new SlotTracker();
        this.slotTracker.function=this;
    }
    public Function(ValueType valueType,Type type,String name) {
        super(valueType,type);
        this.basicBlockList = new ArrayList<BasicBlock>();
        this.paramList=new ArrayList<Value>();
        this.slotTracker = new SlotTracker();
        this.slotTracker.function=this;
        this.valueList=new ArrayList<>();
        this.allocatedValueList=new ArrayList<>();
        this.allocaInstList=new ArrayList<>();
        this.name = name;   // 函数名称
    }
}

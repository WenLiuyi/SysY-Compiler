package llvm.IR.Value;

import llvm.IR.SlotTracker;

import java.util.ArrayList;

public class Function extends User {
    public ArrayList<BasicBlock> basicBlockList;
    public ArrayList<Value> paramList;
    public SlotTracker slotTracker; // 中间代码打印时，为当前函数域分配虚拟寄存器

    public ArrayList<Value> valueList;  //

    public Function() {
        super();
        this.basicBlockList = new ArrayList<BasicBlock>();
        this.paramList=new ArrayList<Value>();
        this.slotTracker = new SlotTracker();
    }
    public Function(ValueType valueType,Type type,String name) {
        super(valueType,type);
        this.basicBlockList = new ArrayList<BasicBlock>();
        this.paramList=new ArrayList<Value>();
        this.slotTracker = new SlotTracker();
        this.valueList=new ArrayList<>();
        this.name = name;   // 函数名称
    }
}

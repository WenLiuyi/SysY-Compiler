package llvm.IR.Value;

import frontend.IndexTable;
import frontend.SymTable;
import llvm.IR.Value.Inst.Instruction;

import java.util.ArrayList;

public class BasicBlock extends Value {
    public int id;      // 编号
    public ArrayList<Instruction> instList;     //基本块内部，由若干指令（Instruction）组成
    public IndexTable.IndexTab indexTab;    // 当前符号表的索引项
    public SymTable symTable;   // 当前符号表

    public BasicBlock(int id,SymTable symTable) {
        super();
        this.id = id;
        this.symTable = symTable;
        instList = new ArrayList<Instruction>();
    }
}

package llvm.IR;

import llvm.IR.Value.Function;
import llvm.IR.Value.Value;
import frontend.Tree.Exp.*;

import java.util.ArrayList;
import java.util.HashMap;

// 在中间代码打印时,为一个函数域分配虚拟寄存器,记录各语句对应的虚拟寄存器编号
public class SlotTracker extends Value {
    public HashMap<Value, Integer> slot;
    public int reg;
    public Function function;

    public SlotTracker() {
        this.slot = new HashMap<>();
        this.reg=0;
    }
    public void allocReg(){
        this.reg++;     // 函数形参的寄存器已分配完毕
        int len=this.function.valueList.size();
        for(int i=0;i<len;i++){         // 给function中的所有value，逐个分配寄存器
            Value inserted_value=this.function.valueList.get(i);
            if(!this.slot.containsKey(inserted_value)){
                insert(inserted_value);
                this.reg++;
            }
        }
    }
    // 插入函数：插入键值对(value, reg)
    public void insert(Value value){
        this.slot.put(value,this.reg);
    }
    // 查找函数：根据键查找对应的寄存器值
    public int getReg(Value value,boolean insert){      // insert=true：找不到则插入
        if(this.slot.containsKey(value)) return this.slot.get(value);
        if(insert){
            insert(value);
            return this.reg++;
        }else{
            return -1;
        }
    }
    // 查找slot中name与给定ident的name相同的Value
    public Value existsWithName(Ident ident) {
        // 遍历slot中的每一个Value
        for (Value value : this.slot.keySet()) {
            // 假设Value类有getName()方法，返回值的名字
            if (value.name.equals(ident.name)) {
                return value;
            }
        }
        return null; // 找不到匹配的value
    }
}

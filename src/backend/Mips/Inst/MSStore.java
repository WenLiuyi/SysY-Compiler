package backend.Mips.Inst;

import backend.Mips.MSModule;
import backend.Mips.Operand.*;

import llvm.IR.Value.*;

public class MSStore extends MSInst {
    public MSOperand src;
    public MSOperand dst;
    public MSOperand off;
    public boolean isNeedUpdate = false;
    public Value updateValue;

    public MSStore(MSModule msModule,MSOperand src, MSOperand dst, MSOperand off) {
        this.msModule=msModule;
        this.src = src;this.dst = dst;this.off = off;
        if(dst instanceof Reg dst_reg) this.regUse.add(dst_reg);    //dst的值是被使用的
        if(src instanceof Reg src_reg) this.regUse.add(src_reg);
        if(off instanceof Reg off_reg) this.regUse.add(off_reg);
        addToMSInstList();
    }
    public void setOff(MSOperand off) {
        this.off = off;
    }
    public void setNeedUpdate(boolean needUpdate) {
        isNeedUpdate = needUpdate;
    }
    public void setUpdateValue(Value updateValue) {
        this.updateValue = updateValue;
    }

    @Override
    public void replaceReg(MSOperand oldReg, MSOperand newReg) {
        super.replaceReg(oldReg, newReg);
        if (oldReg.equals(dst)) dst = newReg;
        if (oldReg.equals(src)) src = newReg;
        if (oldReg.equals(off)) off = newReg;
    }

    @Override
    public String toString() {
        return "sw " + src + ", " + off + "(" + dst + ")";
    }
}


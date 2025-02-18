package backend.Mips.Inst;

import backend.Mips.MSModule;
import backend.Mips.Operand.*;

import llvm.IR.Value.*;

public class MSLoad extends MSInst {
    public MSOperand dst;
    public MSOperand src;
    public MSOperand off;
    public boolean isNeedUpdate = false;
    public Value updateValue;

    public MSLoad(MSModule msModule,MSOperand dst, MSOperand src, MSOperand off) {
        this.msModule=msModule;
        this.dst = dst;this.src = src;this.off = off;
        if(dst instanceof Reg dst_reg) this.regDef.add(dst_reg);
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
        if (oldReg.equals(off)) {
            off = newReg;
            super.replaceReg(oldReg, newReg);
        }
    }

    @Override
    public String toString() {
        return "lw " + dst + ", " + off + "(" + src + ")";
    }
}


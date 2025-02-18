package backend.Mips.Inst;

import backend.Mips.*;
import backend.Mips.Operand.*;

public class MSBinary extends MSInst {
    public MSOperand dst;//从左到右
    public MSOperand left;
    public MSOperand right;
    public String type;// 包括slt等
    public boolean isBeforeCall = false;

    public MSBinary(MSModule msModule,String type, MSOperand dst, MSOperand left, MSOperand right) {
        this.msModule=msModule;
        this.dst = dst;this.left = left;this.right = right;
        this.type=type;

        if(dst instanceof Reg dst_reg) this.regDef.add(dst_reg);
        if(left instanceof Reg left_reg) this.regUse.add(left_reg);
        if(right instanceof Reg right_reg) this.regUse.add(right_reg);
        addToMSInstList();
    }
    public int getOff2Sp() {
        Imm imm = (Imm) right;
        return imm.getNum();
    }

    @Override
    public void replaceReg(MSOperand oldReg, MSOperand newReg) {
        super.replaceReg(oldReg, newReg);
        if (oldReg.equals(dst)) this.dst = newReg;
        if (oldReg.equals(left)) this.left = newReg;
        if (oldReg.equals(right)) this.right=newReg;
    }

    @Override
    public String toString() {
        if ("sub".equals(type)  && right instanceof Imm) {
            return "add" + " " + dst + ", " + left + ", " + ((Imm) right).getNumSub();
        }
        return type + " " + dst + ", " + left + ", " + right;
    }
}

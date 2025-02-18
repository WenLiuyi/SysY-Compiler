package backend.Mips.Inst;

import backend.Mips.MSModule;
import backend.Mips.Operand.*;

// 跳转指令
public class MSBranch extends MSInst {
    public String condType;// bne, bnez, j等均可
    public MSOperand value1;
    public MSOperand value2;
    public MSOperand dst;

    public MSBranch(MSModule msModule, String condType, MSOperand target) {
        this.msModule=msModule;
        this.condType = condType;//无条件跳转, 有j和jal
        this.dst = target;
        addToMSInstList();
    }

    public MSBranch(MSModule msModule,String condType, MSOperand value1, MSOperand target) {
        this.msModule=msModule;
        this.condType = condType;
        this.value1 = value1;
        if(value1 instanceof Reg value_reg) this.regUse.add(value_reg);
        this.dst = target;
        addToMSInstList();
    }

    @Override
    public void replaceReg(MSOperand oldReg, MSOperand newReg) {
        super.replaceReg(oldReg, newReg);
        if (oldReg.equals(value1)) {
            value1 = newReg;
        }
        if (oldReg.equals(value2)) {
            value2 = newReg;
        } else {
//            System.err.println("replaceReg! in jump");
        }
    }

    @Override
    public String toString() {
        //无条件跳转
        if (value1 == null) {
            return condType + " " + dst;
        } else if (value2 == null) {
            return condType + " " + value1 + ", " + dst;
        } else {
            return condType + " " + value1 + ", " + value2 + ", " + dst;
        }
    }

}


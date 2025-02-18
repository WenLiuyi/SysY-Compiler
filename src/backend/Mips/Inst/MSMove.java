package backend.Mips.Inst;

import backend.Mips.MSModule;
import backend.Mips.Operand.*;

public class MSMove extends MSInst {    // MIPS 中的 move 指令:
    // 将一个寄存器或立即数的值复制到另一个寄存器, 或通过某种偏移量进行访问
    public MSOperand dst;      // 目标操作数
    public MSOperand src;      // 源操作数
    public MSOperand off = null;// la $t0, label($t0)
    //偏移量，用于 la 指令（加载地址指令）等，表示地址计算时的偏移量

    public MSMove(MSModule msModule,MSOperand dst, MSOperand src) {
        this.msModule=msModule;
        this.dst = dst;this.src = src;
        if(dst instanceof Reg dst_reg) this.regDef.add(dst_reg);
        if(src instanceof Reg src_reg) this.regUse.add(src_reg);
        addToMSInstList();
    }

    public MSMove(MSModule msModule,MSOperand left, MSOperand right, MSOperand offset) {
        this.msModule=msModule;this.dst = left;this.src = right;this.off = offset;
        if(dst instanceof Reg dst_reg) this.regDef.add(dst_reg);
        if(src instanceof Reg src_reg) this.regUse.add(src_reg);
        if(offset instanceof Reg offset_reg) this.regUse.add(offset_reg);
        addToMSInstList();
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
        if (src instanceof MSLabel && off == null) {
            return "la " + dst + ", " + src;
        } else if (src instanceof MSLabel) {
            return "la " + dst + ", " + src + "(" + off + ")";
        } else if (src instanceof Imm) {
            return "li " + dst + ", " + src;
        } else {
            return "move " + dst + ", " + src;
        }
    }
}

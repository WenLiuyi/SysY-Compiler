package backend.Mips.Inst;

import backend.Mips.MSModule;
import backend.Mips.Operand.*;
import java.util.ArrayList;

public class MSInst {
    public MSModule msModule;
    public final ArrayList<Reg> regDef = new ArrayList<>();
    public final ArrayList<Reg> regUse = new ArrayList<>();

    public void replaceReg(MSOperand oldReg, MSOperand newReg) {
        for (int i = 0; i < regUse.size(); i++) {
            if (regUse.get(i).equals(oldReg)) {
                regUse.set(i, (Reg) newReg);
            }
        }
        for (int i = 0; i < regDef.size(); i++) {
            if (regDef.get(i).equals(oldReg)) {
                regDef.set(i, (Reg) newReg);
            }
        }
    }
    public void addToMSInstList(){
        if(msModule.msNowBlock!=null){
            msModule.msNowBlock.msInstList.add(this);
        }else{
            // 不属于基本块的move指令：全局变量初始化
            msModule.msNowGlobalValue.msInstList.add(this);
        }
    }
}

package backend.Mips.Inst;

import backend.Mips.MSModule;

public class MSSelf extends MSInst{
    public String inst;

    public MSSelf(MSModule msModule,String str) {
        this.msModule=msModule;
        this.inst = str;
        addToMSInstList();
    }
    @Override
    public String toString() {
        return inst;
    }
}


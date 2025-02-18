package backend.Mips;

import backend.Mips.Inst.*;
import backend.Mips.Operand.*;

import java.util.*;

public class RegAllocator {
    public MSModule msModule;
    public ArrayList<PhyReg> phyRegs = new ArrayList<>();   // 存储所有物理寄存器的列表

    public ArrayList<MSFunction> functions;
    public ArrayList<PhyReg> tempRegPool = new ArrayList<>();   // 临时寄存器池
    public HashMap<VirReg, PhyReg> vir2phy = new HashMap<>();  // 虚拟寄存器到物理寄存器的映射.
    public ArrayList<VirReg> virRegPool = new ArrayList<>();    // 当前活跃的虚拟寄存器池

    public HashSet<VirReg> defRegs = new HashSet<>();   // 存储当前函数中定义的寄存器
    public MSFunction nowFunction;  // 当前正在处理的函数
    public MSBlock nowBlock;        // 当前正在处理的基本块
    public int nowOff2Sp = 0;       // 当前栈偏移量
    public RegAllocator(MSModule msModule) {
        this.msModule=msModule;
        functions = msModule.msFunctionList;
    }

    /*
    临时寄存器分配策略:
        1. 遇到寄存器, 如果有对应的全局寄存器, 那么不分配(目前省略);
        2. 如果没有对应的, 那么从栈上相应的溢出区来lw, 分配给一个临时寄存器, 产生映射关系.
        3. 如果临时寄存器不够了, 那么将一个临时寄存器放回到栈上, 再来分配.
          每个基本块退出时,将用了的临时寄存器放回栈上.
     */
    public void allocTempRegs() {
        //进行临时寄存器的分配
        for (MSFunction function : functions) {
            allocTempRegsInFunc(function);
        }
    }

    public void allocTempRegsInFunc(MSFunction function) {
        nowFunction = function;
        int len=function.msBasicBlocksList.size();
        for(int i=0;i<len;i++){
            nowBlock = function.msBasicBlocksList.get(i);
            allocTempRegsInBlock(nowBlock);    // 为函数的每个基本块，分配临时寄存器
        }
    }

    public void allocTempRegsInBlock(MSBlock block) {
        restoreTempRegPool();
        int len=block.msInstList.size();
        int i,j;
        for(i=0;i<len;i++){
            MSInst msInst=block.msInstList.get(i);

            if(msInst instanceof MSLoad msLoad){
                if(msLoad.src instanceof VirReg){
                    PhyReg phyreg_src=vir2phy.get(msLoad.src);
                    msLoad.replaceReg(msLoad.src,phyreg_src);
                }
                if(msLoad.dst instanceof VirReg){
                    PhyReg phyReg_dst=vir2phy.get(msLoad.dst);
                    if(phyReg_dst==null){
                        phyReg_dst=tempRegPool.get(0);
                        vir2phy.put((VirReg) msLoad.dst,phyReg_dst);
                        tempRegPool.remove(0);
                    }
                    msLoad.replaceReg(msLoad.dst,phyReg_dst);
                }
            }
            else if(msInst instanceof MSBinary msBinary){
                if(msBinary.dst instanceof VirReg){
                    PhyReg phyreg_dst=vir2phy.get(msBinary.dst);
                    if(phyreg_dst==null){
                        phyreg_dst=tempRegPool.get(0);
                        vir2phy.put((VirReg) msBinary.dst,phyreg_dst);
                        tempRegPool.remove(0);
                    }
                    msBinary.replaceReg(msBinary.dst,phyreg_dst);
                }
                if(msBinary.left instanceof VirReg){
                    PhyReg phyreg_left=vir2phy.get(msBinary.left);
                    msBinary.replaceReg(msBinary.left,phyreg_left);
                }
                if(msBinary.right instanceof VirReg){
                    PhyReg phyreg_right=vir2phy.get(msBinary.right);
                    msBinary.replaceReg(msBinary.left,phyreg_right);
                }
            }
            else if(msInst instanceof MSMove msMove){
                if(msMove.dst instanceof VirReg){
                    PhyReg phyReg=tempRegPool.get(0);
                    vir2phy.put((VirReg) msMove.dst,phyReg);
                    tempRegPool.remove(0);
                    msMove.replaceReg(msMove.dst,phyReg);
                }
            }
            else if(msInst instanceof MSStore msStore){
                if(msStore.src instanceof VirReg){
                    PhyReg phyReg=tempRegPool.get(0);
                    vir2phy.put((VirReg) msStore.src,phyReg);
                    tempRegPool.remove(0);
                    msStore.replaceReg(msStore.src,phyReg);
                }
            }
            else if(msInst instanceof MSBranch msBranch){
                if(msBranch.value1==null){  // 无条件跳转

                }else{
                    if(msBranch.value1 instanceof VirReg){
                        PhyReg phyreg_cond=vir2phy.get(msBranch.value1);
                        msBranch.replaceReg(msBranch.value1,phyreg_cond);
                    }
                }
            }
        }
    }
    // 恢复临时寄存器池, 每次进入基本块都要进行该操作.
    public void restoreTempRegPool() {
        tempRegPool.clear();
        tempRegPool.addAll(PhyReg.getTempRegs());
        virRegPool.clear();
        vir2phy.clear();
        defRegs.clear();
    }
}


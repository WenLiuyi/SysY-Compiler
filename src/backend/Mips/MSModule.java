package backend.Mips;

import backend.Mips.Inst.*;
import llvm.IR.Value.*;
import llvm.IR.Value.Inst.StoreInst;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import backend.Mips.Operand.*;

public class MSModule {
    public llvm.IR.Module irModule;
    public ArrayList<MSGlobalValue> msGlobalValueList;

    public ArrayList<MSFunction> msFunctionList;
    public ArrayList<Value> msStringList;
    public MSFunction msNowFunc;
    public MSBlock msNowBlock;
    public MSGlobalValue msNowGlobalValue;
    // llvm中基本块到mips中基本块的映射
    public HashMap<BasicBlock, MSBlock> llvmBlock_MSBlock_Map = new HashMap<>();

    public HashMap<Value, Pair<Reg, Integer>> memoryMap = new HashMap<>();
    public HashMap<String, Value>globalMap=new HashMap<>();
    public int gpOff=0, spOff=0;

    public MSModule(llvm.IR.Module irModule) {
        this.irModule = irModule;
        msGlobalValueList=new ArrayList<>();
        msFunctionList=new ArrayList<>();
        msStringList=new ArrayList<>();
    }

    public void initialize() {
        //1. 全局常量/变量
        int len=irModule.llvmContext.globalVariablesList.size();
        for(int i=0;i<len;i++) {
            llvm.IR.Value.GlobalVariable globalVariable=irModule.llvmContext.globalVariablesList.get(i);
            MSGlobalValue msGlobalValue = null;

            // 1. 普通常量/变量
            if(!globalVariable.isArray){
                MSValueType type;
                if(globalVariable.type.equals(Type.IntegerTyID)){
                    type=MSValueType.word;
                }else{
                    type=MSValueType._byte;
                }
                msGlobalValue = new MSGlobalValue(this,type, globalVariable.name, globalVariable);
            }
            else{
                StoreInst storeInst=(StoreInst) (globalVariable.instList.get(globalVariable.instList.size()-1));
                Value data=storeInst.usesList.get(0).usee;

                if(data.string!=null){      // 2. 字符串
                    msGlobalValue = new MSGlobalValue(this,MSValueType.str, globalVariable.name, globalVariable,data.string);
                }
                else{       // 3. 数组
                    msGlobalValue = new MSGlobalValue(this,MSValueType.array, globalVariable.name, globalVariable, data);
                }
            }
            msGlobalValueList.add(msGlobalValue);
        }

        // 除main函数以外的所有函数
        len=irModule.llvmContext.functionsList.size();
        for(int i=0;i<len;i++){
            llvm.IR.Value.Function func=irModule.llvmContext.functionsList.get(i);  // 第i个函数
            if(!func.name.equals("main")){
                MSFunction msFunction=new MSFunction(this,func, false);
                func.msFunction=msFunction;
                msFunctionList.add(msFunction);
            }else{
                msFunctionList.add(new MSFunction(this,func, true));
            }
        }
        msNowBlock = null;
        msNowFunc = null;
    }
    public MSGlobalValue getGlobalValue(Value value){
        int len=this.msGlobalValueList.size();
        for(int i=0;i<len;i++){
            MSGlobalValue msGlobalValue=msGlobalValueList.get(i);
            if(msGlobalValue.value.equals(value)) return msGlobalValue;
        }
        return null;
    }
    public void getGp(Value value){     // 为整型/字符型变量分配空间
        if(memoryMap.containsKey(value)) return;
        globalMap.put(value.name,value);
        memoryMap.put(value,new Pair<>(PhyReg.gp, gpOff));
        this.gpOff+=4;
    }
    public void getGpArray(Value value,int offset){
                /*  例：# @arr = global [5 x i32] [i32 1, i32 2, i32 3, i32 4, i32 0]
                addu $t0, $gp, 12
                sw $t0, 8($gp)  */
        if(memoryMap.containsKey(value)) return;
        getGp(value);
        new MSBinary(this,"add",PhyReg.t0,PhyReg.gp,new Imm(offset));
        new MSStore(this,PhyReg.t0,PhyReg.gp,new Imm(this.gpOff-4));
    }
    public void getSp(Value value) {
        if (memoryMap.containsKey(value)) return;
        this.spOff -= 4;
        memoryMap.put(value, new Pair<>(PhyReg.sp, spOff));
    }
}

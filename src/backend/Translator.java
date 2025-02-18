package backend;

import backend.Mips.*;
import backend.Mips.Inst.MSSelf;
import frontend.WriteFile;

import java.util.ArrayList;

public class Translator {
    public llvm.IR.Module llvmModule;  // llvm模块的头节点
    public MSModule msModule;
    public ArrayList<String> outputs;

    public Translator(llvm.IR.Module llvmModule) {
        this.llvmModule = llvmModule;
        this.msModule=new MSModule(llvmModule);
        this.outputs=new ArrayList<>();
    }

    public void translate(){
        this.msModule.initialize();     // 初始化
        translateToMips();

        WriteFile writer = new WriteFile();
        writer.write("mips_vir.txt",outputs);

        // 分配寄存器
        RegAllocator regAllocator=new RegAllocator(this.msModule);
        regAllocator.allocTempRegs();
        translateToMips();
        writer.write("mips.txt",outputs);
    }
    public void translateToMips(){
        outputs.clear();

        // .data段生成：全局数据段（包括字符串）
        outputs.add(".data");
        // 常量字符串
        int len=llvmModule.llvmContext.stringList.size();
        for(int i=0;i<len;i++){
            String str=".str";
            if(i>0) str+="."+Integer.toString(i);
            str+=": .asciiz \"";

            String string=llvmModule.llvmContext.stringList.get(i).string;
            int curPos=0,strlen=string.length();
            while(curPos<strlen){
                if(curPos+2<strlen && string.charAt(curPos)=='\\'){
                    String subStr=string.substring(curPos+1,curPos+3);
                    switch (subStr){
                        case "08":str+="\\b";break;
                        case "0C":str+="\\f";break;
                        case "0A":str+="\\n";break;
                        case "0D":str+="\\r";break;
                        case "09":str+="\\t";break;
                        case "5C":str+="\\\\";break;
                        case "27":str+="\\'";break;
                        case "22":str+="\\\"";break;
                        case "00":str+="\\0";break;
                        default:str+=subStr;break;
                    }
                    curPos+=3;continue;
                }
                str+=string.charAt(curPos);
                curPos++;
            }

            str+="\"";
            outputs.add(str);
        }

        // .text段生成：
        outputs.add(".text\n");
        for (MSGlobalValue msglobalValue : msModule.msGlobalValueList) {
            outputs.add(msglobalValue.toString());
        }

        outputs.add("j func_main");

        for (MSFunction msfunction : msModule.msFunctionList) {
            outputs.add(msfunction.toString());
        }
    }
}

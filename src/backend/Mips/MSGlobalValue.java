package backend.Mips;

import llvm.IR.Value.*;
import backend.Mips.Operand.*;
import backend.Mips.Inst.*;

import java.util.ArrayList;
import java.util.HashSet;

public class MSGlobalValue {
    /*public static final String word = ".word";
    public static final String array = ".word";
    public static final String str = ".asciiz";
    private String type;//.word, .asciiz */
    public MSModule msModule;
    public MSValueType type;
    private String name;
    //    private String content;
    public llvm.IR.Value.Value value;
    public String dataPresented;

    public ArrayList<MSInst> msInstList = new ArrayList<>();

    public int arrayLength;

    // 普通变量/常量：加载到寄存器 $t2 中，并将其存储到相应的内存地址
    public MSGlobalValue(MSModule msModule,MSValueType type, String name, Value value) {
        this.msModule=msModule;this.type = type;this.name = name;this.value=value;
        msModule.msNowGlobalValue=this;
        String typeStr;
        if(type.equals(MSValueType.word)) typeStr=".word";
        else typeStr=".byte";
        //this.dataPresented=name+": "+typeStr+" "+value.num;
        this.dataPresented="";

        msModule.getGp(value);       // 分配空间，将到对应内存地址的映射，存进memoryMap
        /* 例： li $t2, 0
                sw $t2, 0($gp)
         */
        new MSMove(msModule,PhyReg.t2,new Imm(value.num));
        new MSStore(msModule,PhyReg.t2,PhyReg.gp,new Imm(msModule.gpOff-4));
    }
    // 字符串: 在Translator的translate方法中处理
    public MSGlobalValue(MSModule msModule,MSValueType type, String name, Value value, String str) {
        this.msModule=msModule;this.type = type;this.name = name;this.value=value;
        str=str.replace("\n", "\\n");
        this.dataPresented="";
    }

    // 数组：为数组分配内存；如果数组初始化了，则为每个元素生成 MIPS 代码，将其存储到对应的内存地址。
    public MSGlobalValue(MSModule msModule ,MSValueType type, String name, Value value, Value arrayData) {
        this.msModule=msModule;this.type = type;this.name = name;this.value=value;
        this.arrayLength=arrayData.arrayLength;

        this.dataPresented="";
        /*  例：# @arr = global [5 x i32] [i32 1, i32 2, i32 3, i32 4, i32 0]
                addu $t0, $gp, 12
                sw $t0, 8($gp)

                li $t2, 1
                lw $t0, 8($gp)
                addu $t0, $t0, 0
                sw $t2, 0($t0)

                li $t2, 2
                lw $t0, 8($gp)
                addu $t0, $t0, 4
                sw $t2, 0($t0)              */
        int cnt=arrayData.array.size(),maxlen=this.arrayLength;

        msModule.getGpArray(value,4*maxlen);
        if(arrayData.zeroinitialized){
            //str+=" 0:"+maxlen;
        }
        else{       // 已经初始化：逐个元素，存入对应的内存地址
            for(int j=0;j<maxlen;j++){
                // 1. li $t2, 1     加载元素值
                int num;
                if(j<cnt) num=(Integer)arrayData.array.get(j);
                else num=0;
                new MSMove(msModule,PhyReg.t2,new Imm(num));
                // 2. lw $t0, 8($gp)    加载数组起始地址
                new MSLoad(msModule,PhyReg.t0,PhyReg.gp,new Imm(msModule.gpOff-4));
                // 3. addu $t0, $t0, 4  处理偏移量
                if(j>0) new MSBinary(msModule,"add",PhyReg.t0,PhyReg.t0,new Imm(4*j-4));
                // 4. sw $t2, 0($t0)    存储至相应内存地址
                new MSStore(msModule,PhyReg.t2,PhyReg.t0,new Imm(0));
            }
        }
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        int len=this.msInstList.size();
        for(int i=0;i<len;i++){
            sb.append(msInstList.get(i).toString());
            sb.append("\n");
        }
        return sb.toString();
    }
}

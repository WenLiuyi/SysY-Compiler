package backend.Mips;

import backend.Mips.Inst.*;
import backend.Mips.Operand.*;

import llvm.*;
import llvm.IR.Value.*;
import llvm.IR.Value.Inst.AllocaInst;

import java.awt.print.PrinterJob;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

public class MSFunction {
    public MSModule msModule;
    public llvm.IR.Value.Function llvmFunction;     // 对应的llvm函数
    public String name;       // 函数名称
    public ArrayList<MSBlock> msBasicBlocksList;    // 函数基本块
    public int msBlockID;

    public HashMap<Value, MSLabel> block2Label=new HashMap<>();     // 基本块到名称的映射

    /*
    存储value距离当前函数栈底的距离, 包括参数等.
        参数距离当前函数栈底的距离是负数, 因为是由调用函数方保存的;
        其他变量距离栈底的距离是正数
     */
    public HashMap<Value, Integer> value2Stack;
    //存储value和虚拟寄存器的映射关系
    public HashMap<Value, Reg> value2Reg = new HashMap<>();
    // 存储MSGlobalValue的地址和虚拟寄存器的映射关系
    /*例：la   $t0, c             # 加载 @c 的地址到 $t0
        lw   $t1, 0($t0)        # 加载 @c 的值到 $t1
        这里存储：(c, $t0)的键值对
     */
    //public HashMap<MSGlobalValue,Reg>globalAddr2Reg=new HashMap<>();

    //只用于从寄存器池分配寄存器
    public HashSet<Integer> usedRegs;
    public ArrayList<Value> paramList;

    //全部统一, 记录离栈底的距离, 均为正数; 要求sp进行偏移的量, 如果是增大栈, 那么是正数; 减小栈, 则是负数
    //注意, 函数初始化之后, stackTop将是一个定值, 指向数组区和溢出区之间的位置
    public int stackTop = 0;
    public boolean isMainFunc = false;
    //虚拟寄存器
    public List<VirReg> virRegs = new ArrayList<>();
    //记录虚拟寄存器距离栈底的距离
    public HashMap<VirReg, Integer> vir2Stack = new HashMap<>();
    //记录虚拟寄存器区的大小字节, 不包括参数的虚拟寄存器
    public Imm virSize = new Imm(0);
    public HashMap<Value, Integer> globalRegsMap;
    public HashMap<VirReg, PhyReg> vir2Phy = new HashMap<>();

    public MSFunction(MSModule msModule,llvm.IR.Value.Function function, boolean isMainFunc) {
        this.value2Reg = new HashMap<>();
        value2Stack = new HashMap<>();
        usedRegs = new HashSet<>();

        this.msModule = msModule;
        this.msModule.msNowFunc = this;
        this.isMainFunc = isMainFunc;
        this.globalRegsMap = function.slotTracker.slot;
        this.name = "func_" + function.name;
        this.llvmFunction=function;
        this.msBlockID=1;

        paramList = function.paramList;
        function.msFunction=this;

        this.msBasicBlocksList = new ArrayList<>();
        ArrayList<BasicBlock> llvmBasicBlockList = function.basicBlockList;

        msBasicBlocksList.add(new MSBlock(msModule,this,llvmBasicBlockList.get(0), 0,true));
        for (int i = 1; i < llvmBasicBlockList.size(); i++) {
            msBasicBlocksList.add(new MSBlock(msModule,this,llvmBasicBlockList.get(i),i, false));
            this.msModule.llvmBlock_MSBlock_Map.put(llvmBasicBlockList.get(i), msBasicBlocksList.get(i));
        }

        // 设置虚拟寄存器的存储区
        setVirSize();
    }

    /*
    为数组分配地址空间, allocas数组依次从栈底到栈顶(从上到下)排布, 数组元素是从上到下一次排布.
     * 进入函数时, 参数区已经在栈底了, 因此更新stackTop. 为参数分配虚拟寄存器, 为数组分配空间
     */
    public void allocForFunc(ArrayList<Value> paramList, ArrayList<AllocaInst> allocaInstList) {
        /*  params：函数参数列表。
            allocaInst：局部变量（栈分配）指令的列表。
            stackTop：当前栈顶的指针。 */

        // 1. 为参数分配虚拟寄存器
        int paramNum=paramList.size();
        this.stackTop =0;
        // 2. 为函数参数/局部变量分配空间: stackTop加上参数数量的4倍（每个参数占4字节），表示为函数的参数区预留空间。
        int alloc_size = 0, allocNum=allocaInstList.size(),tmp=0;
        // alloc_size记录元素个数
        for(int i=0;i<allocNum;i++){
            Value value=allocaInstList.get(i).usesList.get(0).usee;
            value2Stack.put(value, stackTop + tmp);
            if(value.type.equals(Type.IntArrayTyID)||value.type.equals(Type.CharArrayTyID)){
                alloc_size+=value.arrayLength;      // 数组长度
                tmp+=4 * value.arrayLength;
            }else{
                alloc_size+=1;tmp+=4;
            }
        }
        //moveSp(4 * alloc_size);
        stackTop += 4 * alloc_size;
    }

    // 连同stackTop一起移动栈指针. 增大栈的空间, 则是正数, 减小栈的空间, 则是负数.
    public void moveSp(int off) {
        if (off == 0) return;
        // 例：add $sp, $sp, -12
        new MSBinary(msModule,"add", PhyReg.sp, PhyReg.sp, new Imm(off));
    }
    public Reg allocVirReg(Value value) {
        VirReg reg = new VirReg();      // 1. 创建虚拟寄存器
        value2Reg.put(value, reg);      // 2. 寄存器映射
        virRegs.add(reg);               // 3. 更新虚拟寄存器集合
        /*if(globalRegsMap.get(value) != null) {
            // 查找与 value 对应的全局寄存器, 如果存在：将该全局寄存器映射到一个物理寄存器（PhyReg）
            vir2Phy.put(reg, PhyReg.getGlobalReg(globalRegsMap.get(value)));
        }*/
        return reg;
    }
    // 如果该虚拟寄存器对应一个只会使用一次的变量, 如数字/字符等, 则不为其分配栈空间
    public Reg allocVirReg(boolean isUseOnlyOnce) {
        VirReg virReg = new VirReg();
        if (!isUseOnlyOnce) {
            virRegs.add(virReg);
        }
        return virReg;
    }
    public void spMoveBeforeCall(int off) {
        MSBinary msBinary=new MSBinary(msModule,"sub", PhyReg.sp, PhyReg.sp, new Imm(virSize, off));
        msBinary.isBeforeCall=true;
    }

    // 溢出区是否包括虚拟寄存器, 判断虚拟寄存器是否需要保存到栈上
    public boolean includeVirReg(VirReg reg) {
        return vir2Stack.containsKey(reg);
    }

    public int getVirSize() {
        return virSize.getNum();
    }

    // 将虚拟寄存器映射到栈上, 作为溢出区; 同时设定溢出区的大小字节
    public void setVirSize() {

    }
    // 获取value对应的寄存器, 如果不存在，那么分配一个寄存器
    /*  1. value是全局变量/常量(value.isGlobalValue==true)，则对应的寄存器是其加载到的地址：
            la   $t0, c             # 加载 @c 的地址到 $t0
     */
    public Reg getReg(Value value) {
        if(value.isIntChar){
            Reg reg = allocVirReg(true);
            new MSMove(msModule,reg, new Imm(value.num));
            return reg;
        }
        Reg reg = value2Reg.get(value);
        if (reg == null) {
            reg = allocVirReg(value);
        }
        return reg;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(":\n");
        int len=msBasicBlocksList.size();
        for(int i=0;i<len;i++){
            sb.append(msBasicBlocksList.get(i).toString());
        }
        return sb.toString();
    }
}

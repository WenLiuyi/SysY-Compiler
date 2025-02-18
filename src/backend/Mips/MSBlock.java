package backend.Mips;

import backend.Mips.Inst.*;
import backend.Mips.Operand.*;
import frontend.Tree.Exp.Ident;
import llvm.IR.*;
import llvm.IR.Value.*;
import llvm.IR.Value.Inst.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class MSBlock {
    public String lable_name;
    public ArrayList<MSInst> msInstList = new ArrayList<>();
    public MSModule msModule;
    public MSFunction msFunction;

    public boolean isFirstBlock;    // 当前是否是第一个基本块
    public int storeParam;  // 当前为第一个基本块时，alloca指令后的前几个store指令，将参数加载至栈空间

    public MSLabel msLabel;

    // 创建mips基本块
    public MSBlock(MSModule msModule, MSFunction msFunction, BasicBlock basicBlock, int id,boolean isFirstBlock) {
        // isFirstBlock：如果为true, 则为函数的第一个基本块, 进行分配空间等操作
        this.msModule=msModule;
        this.msModule.msNowBlock = this;
        this.msFunction = msFunction;
        this.isFirstBlock=isFirstBlock;
        this.storeParam=0;
        ArrayList<Instruction> irInstList = basicBlock.instList;
        ArrayList<Value> paramList=msFunction.paramList;    // 该基本块对应函数的参数

        int i = 0, j = 0, size = irInstList.size(), param_size = paramList.size();

        //如果是第一个基本块, 则进行地址的分配.
        if (isFirstBlock) {
            ArrayList<AllocaInst> allocaInstList=this.msFunction.llvmFunction.allocaInstList;
            //this.msFunction.allocForFunc(msFunction.paramList,allocaInstList);

            int param_remained=paramList.size();     // 剩余的参数数量
            for (i = 0; param_remained > 0; i++) {
                param_remained--;
                // 从栈（$sp）加载参数到寄存器 $t0 中: 栈中的参数是按照倒序存储
                new MSLoad(msModule,PhyReg.t0,PhyReg.sp,new Imm(4*param_remained));
                msModule.getSp(paramList.get(i));
                new MSStore(msModule,PhyReg.t0,getReg(paramList.get(i)),new Imm(getOffset(paramList.get(i))));
            }
            param_remained = 0;
        }
        MSLabel msLabel=msFunction.block2Label.get(basicBlock.value);
        if(msLabel!=null){
            this.msLabel=msLabel;
            this.lable_name=msLabel.label;
        }
        for (i=0; i < size; i++) {
            Instruction inst=irInstList.get(i);
            if(!(inst instanceof AllocaInst)){
                addToMap(inst);
            }

            transInst(inst);
        }
    }
    public void addToMap(Instruction inst){
        if(inst instanceof BinaryInst || inst instanceof CompareInst || inst instanceof BranchInst || inst instanceof ReturnInst
                || inst instanceof XOrInst || inst instanceof GetelementInst){
            msModule.getSp(inst.usesList.get(0).usee);
        }else if(inst instanceof LoadInst || inst instanceof StoreInst || inst instanceof TruncInst || inst instanceof ZextInst){
            msModule.getSp(inst.usesList.get(1).usee);
        }else if(inst instanceof CallInst callInst && callInst.usesList.size()>1){
            Value value_call_dst=callInst.usesList.get(0).usee;
            msModule.getSp(value_call_dst);
        }
    }

    // llvm中间代码转后端的主函数
    public void transInst(Instruction inst) {
        if (inst instanceof StoreInst storeInst) {
            parseStore(storeInst);
        } else if (inst instanceof LoadInst loadInst) {
            parseLoad(loadInst);
        } else if (inst instanceof AllocaInst) {
            return;
        } else if (inst instanceof BinaryInst || inst instanceof CompareInst) {
            parseBinary(inst);
        } else if (inst instanceof CallInst callInst) {
            parseCallInst(callInst);
        } else if (inst instanceof ReturnInst returnInst) {
            parseReturnInst(returnInst);
        } else if (inst instanceof GetelementInst getelementInst) {
            parseGetelementInst(getelementInst);
        } else if (inst instanceof BranchInst branchInst) {
            parseBranchInst(branchInst);
        }else if(inst instanceof ZextInst zextInst){
            parseZextInst(zextInst);
        }else if(inst instanceof TruncInst truncInst){
            parseTruncInst(truncInst);
        }
    }
    public void parseTruncInst(TruncInst truncInst){
        Value trunc_src=truncInst.usesList.get(0).usee,trunc_dst=truncInst.usesList.get(1).usee;
        msModule.memoryMap.put(trunc_dst,msModule.memoryMap.get(trunc_src));
    }
    public void parseZextInst(ZextInst zextInst){
        Value zext_src=zextInst.usesList.get(0).usee,zext_dst=zextInst.usesList.get(1).usee;
        msModule.memoryMap.put(zext_dst,msModule.memoryMap.get(zext_src));
    }
    public void parseReturnInst(ReturnInst returnInst){
        if (!returnInst.usesList.isEmpty()) {
            Value value_returned=returnInst.usesList.get(0).usee;
            new MSLoad(msModule,PhyReg.v0,getReg(value_returned),new Imm(getOffset(value_returned)));
        }
        new MSSelf(msModule,"jr $ra");
    }
    public void parseCallInst(CallInst callInst){
        Use use_call_func;String str=".str",funcName;
        if(callInst.usesList.size()==1){    // void函数
            // call指令：call void <name>(<...args>)
            use_call_func=callInst.usesList.get(0);
        }else{
            use_call_func=callInst.usesList.get(1);
        }
        Function called_function=(Function)(use_call_func.usee);
        int param_cnt=called_function.paramList.size();
        funcName=use_call_func.usee.name;

        if (funcName.equals("getint")) {
            Value value_call_dst=callInst.usesList.get(0).usee;
            // move  $t4, $v0   函数的返回值
            new MSSelf(msModule,"li $v0, 5\nsyscall");
            new MSStore(msModule,PhyReg.v0,getReg(value_call_dst),new Imm(getOffset(value_call_dst)));
        } else if (funcName.equals("putint")) {
            Value param=called_function.paramList.get(0);
            if(param.isIntChar){        // 数字/字符
                new MSMove(msModule,PhyReg.a0,new Imm(param.num));
            }
            else{ // 参数为字符串
                new MSLoad(msModule,PhyReg.a0,getReg(param),new Imm(getOffset(param)));
            }
            new MSSelf(msModule,"li $v0, 1\nsyscall");
        } else if (funcName.equals("putch")) {
            Value param=called_function.paramList.get(0);
            if(param.isIntChar){        // 数字/字符
                new MSMove(msModule,PhyReg.a0,new Imm(param.num));
            }
            else{ // 参数为字符串
                new MSLoad(msModule,PhyReg.a0,getReg(param),new Imm(getOffset(param)));
            }
            new MSSelf(msModule,"li $v0, 11\nsyscall");
        } else if (funcName.equals("putstr")) {
            new MSSelf(msModule,"li $v0, 4\nsyscall");
        } else {
            new MSStore(msModule,PhyReg.ra,PhyReg.sp,new Imm(msModule.spOff-4));
            for (int i = 0; i < param_cnt; i++) {
                Value cur_param=called_function.paramList.get(i);
                new MSLoad(msModule,PhyReg.t0,getReg(cur_param),new Imm(getOffset(cur_param)));
                new MSStore(msModule,PhyReg.t0,PhyReg.sp,new Imm(msModule.spOff-(i+1)*4));
            }
            new MSBinary(msModule,"add",PhyReg.sp,PhyReg.sp,new Imm(msModule.spOff-param_cnt*4));
            new MSSelf(msModule,"jal func_"+funcName);
            new MSBinary(msModule,"add",PhyReg.sp,PhyReg.sp,new Imm(-msModule.spOff+param_cnt*4));
            new MSLoad(msModule,PhyReg.ra,PhyReg.sp,new Imm(msModule.spOff-4));

            if(callInst.usesList.size()>1){
                Value value_call_dst=callInst.usesList.get(0).usee;
                // move  $t4, $v0   函数的返回值
                new MSStore(msModule,PhyReg.v0,getReg(value_call_dst),new Imm(getOffset(value_call_dst)));
            }
        }
    }
    public void parseBranchInst(BranchInst branchInst){
        if(branchInst.usesList.size()==1){
            // 跳转指令: br label <dest>
            Value value_dst=branchInst.usesList.get(0).usee;
            MSLabel msLabel_dst;
            if(!msFunction.block2Label.containsKey(value_dst)){
                msLabel_dst=new MSLabel(msFunction.name+"_"+msFunction.msBlockID++);
                msFunction.block2Label.put(value_dst,msLabel_dst);
            }else{
                msLabel_dst=msFunction.block2Label.get(value_dst);
            }
            new MSBranch(msModule,"j",msLabel_dst);
        }else{
            // 跳转指令: br i1 <cond>, label <iftrue>, label <iffalse>
            /*  例：%6=icmp ne i32 %5, 0
                        br i1 %6, label %7, label %8
                    转化为：
                        bnez $t0, label_true    # 如果 $t0 != 0（即 $s0 > $s1），跳转到 label_true
                        j label_false           # 否则跳转到 label_false
                 */

            Value value_cond=branchInst.usesList.get(0).usee;
            Value value_iffalse=branchInst.usesList.get(1).usee,value_iftrue=branchInst.usesList.get(2).usee;
            String str="";

            MSLabel msLabel_true;
            if(!msFunction.block2Label.containsKey(value_iftrue)){
                msLabel_true=new MSLabel(msFunction.name+"_"+msFunction.msBlockID++);
                msFunction.block2Label.put(value_iftrue,msLabel_true);
            }else{
                msLabel_true=msFunction.block2Label.get(value_iftrue);
            }

            MSLabel msLabel_false;
            if(!msFunction.block2Label.containsKey(value_iffalse)){
                msLabel_false=new MSLabel(msFunction.name+"_"+msFunction.msBlockID++);
                msFunction.block2Label.put(value_iffalse,msLabel_false);
            }else{
                msLabel_false=msFunction.block2Label.get(value_iffalse);
            }
            new MSLoad(msModule,PhyReg.t0,getReg(value_cond),new Imm(getOffset(value_cond)));
            new MSBranch(msModule,"bnez",PhyReg.t0,msLabel_true);
            new MSBranch(msModule,"j",msLabel_false);
        }
    }
    public void parseGetelementInst(GetelementInst gepInst){
        // <result> = getelementptr <ty>, ptr <ptrval>{, <ty> <idx>}*
        new MSSelf(msModule,"# getInst here");
        Value value_dst=gepInst.usesList.get(0).usee,value_array=gepInst.usesList.get(1).usee,value_pos=gepInst.usesList.get(2).usee;
        int offsetNum;
        if(value_array.type.equals(Type.IntArrayTyID)||value_array.type.equals(Type.CharArrayTyID)){

        }
        /*  例：# %1 = getelementptr [8 x i32], [8 x i32]* @arr, i32 0, i32 1
                lw $t2, 8($gp)
                sw $t2, -40($sp)

                addu $t2, $t2, 4
                sw $t2, -40($sp)
         */
        new MSLoad(msModule,PhyReg.t2,getReg(value_array),new Imm(getOffset(value_array)));
        new MSStore(msModule,PhyReg.t2,getReg(value_dst),new Imm(getOffset(value_dst)));
        int lastOff = 0;

        if(value_pos.isIntChar){    // pos是数字或字符
            if(value_pos.num!=0){
                new MSBinary(msModule,"add",PhyReg.t2,PhyReg.t2,new Imm(value_pos.num*4));
            }
            new MSStore(msModule,PhyReg.t2,getReg(value_dst),new Imm(getOffset(value_dst)));
        }else{
            Reg regPos=getReg(value_pos);
            new MSStore(msModule,PhyReg.t2,getReg(value_dst),new Imm(getOffset(value_dst)));
        }
    }
    public void parseStore(StoreInst storeInst){
        Value value_src=storeInst.usesList.get(0).usee,value_dst=storeInst.usesList.get(1).usee;
        if(value_src.isIntChar){
            new MSMove(msModule,PhyReg.t0,new Imm(value_src.num));
        }else{
            new MSLoad(msModule,PhyReg.t0,getReg(value_src),new Imm(getOffset(value_src)));
        }
        new MSStore(msModule,PhyReg.t0,getReg(value_dst),new Imm(getOffset(value_dst)));
    }

    public void parseLoad(LoadInst loadInst){
        new MSSelf(msModule,"# load");     //Notes

        Value value_src=loadInst.usesList.get(0).usee,value_dst=loadInst.usesList.get(1).usee;

        new MSLoad(msModule,PhyReg.t0,getReg(value_src),new Imm(getOffset(value_src)));
        new MSStore(msModule,getReg(value_dst),PhyReg.t0,new Imm(getOffset(value_dst)));
    }

    public void parseBinary(Instruction inst){
        BinaryOpType opType;
        if(inst instanceof BinaryInst binaryInst) opType=binaryInst.opType;
        else{
            CompareInst compareInst=(CompareInst) inst;
            opType=compareInst.opType;
        }
        new MSSelf(msModule,"# "+opType);     //Notes
        if(opType.equals(BinaryOpType.add)) calc(inst, "add");
        else if(opType.equals(BinaryOpType.sub)) calc(inst, "sub");
        else if(opType.equals(BinaryOpType.mul)) calc(inst, "mul");
        else if(opType.equals(BinaryOpType.div)) calc(inst, "div");
        else if(opType.equals(BinaryOpType.mod)) calc(inst, "rem");
        else if(opType.equals(BinaryOpType.lss)) calc(inst, "sll");
        else if(opType.equals(BinaryOpType.leq)) calc(inst, "sle");
        else if(opType.equals(BinaryOpType.gre)) calc(inst, "sgt");
        else if(opType.equals(BinaryOpType.geq)) calc(inst, "sge");
        else if(opType.equals(BinaryOpType.eq)) calc(inst, "seq");
        else if(opType.equals(BinaryOpType.ne)) calc(inst, "sne");
        else{
            Value value_dst=inst.usesList.get(0).usee;
            Value value_left=inst.usesList.get(1).usee;

            if(value_left.isIntChar){
                new MSMove(msModule,PhyReg.t0,new Imm(value_left.num));
            }else{
                new MSLoad(msModule,PhyReg.t0,getReg(value_left),new Imm(getOffset(value_left)));
            }
            new MSSelf(msModule,"not $t1, $t0");
            new MSStore(msModule,PhyReg.t1,getReg(value_dst),new Imm(getOffset(value_dst)));
        }
    }

    public void transBranchInst(BranchInst branchInst){
        if(branchInst.usesList.size()==1){
            Value value_dst=branchInst.usesList.get(0).usee;
            MSLabel msLabel_dst;
            if(!msFunction.block2Label.containsKey(value_dst)){
                msLabel_dst=new MSLabel(msFunction.name+"_"+msFunction.msBlockID++);
                msFunction.block2Label.put(value_dst,msLabel_dst);
            }else{
                msLabel_dst=msFunction.block2Label.get(value_dst);
            }
            new MSBranch(msModule,"j",msLabel_dst);
        }else{
            // 跳转指令: br i1 <cond>, label <iftrue>, label <iffalse>
            Value value_cond=branchInst.usesList.get(0).usee;
            Value value_iffalse=branchInst.usesList.get(1).usee,value_iftrue=branchInst.usesList.get(2).usee;
            String str="";
            if(value_cond.valueType.equals(ValueType.FalseTy)){

            }
            else if(value_cond.valueType.equals(ValueType.TrueTy)){

            }else{
                /*  例：%6=icmp ne i32 %5, 0
                        br i1 %6, label %7, label %8
                    转化为：
                        bnez $t0, label_true    # 如果 $t0 != 0（即 $s0 > $s1），跳转到 label_true
                        j label_false           # 否则跳转到 label_false
                 */
                CompareInst compareInst=value_cond.compareInst;Value value_left=compareInst.usesList.get(1).usee;
                Reg reg_dst= msFunction.getReg(value_left);

                MSLabel msLabel_true;
                if(!msFunction.block2Label.containsKey(value_iftrue)){
                    msLabel_true=new MSLabel(msFunction.name+"_"+msFunction.msBlockID++);
                    msFunction.block2Label.put(value_iftrue,msLabel_true);
                }else{
                    msLabel_true=msFunction.block2Label.get(value_iftrue);
                }
                new MSBranch(msModule,"bnez",reg_dst,msLabel_true);

                MSLabel msLabel_false;
                if(!msFunction.block2Label.containsKey(value_iffalse)){
                    msLabel_false=new MSLabel(msFunction.name+"_"+msFunction.msBlockID++);
                    msFunction.block2Label.put(value_iffalse,msLabel_false);
                }else{
                    msLabel_false=msFunction.block2Label.get(value_iffalse);
                }
                new MSBranch(msModule,"j",msLabel_false);

            }
        }
    }
    public void transCompareInst(CompareInst compareInst){
        String op="";BinaryOpType opType=compareInst.opType;
        Value value_dst=compareInst.usesList.get(0).usee;
        Value value_left=compareInst.usesList.get(1).usee,value_right=compareInst.usesList.get(2).usee;
        Reg reg_dst,reg_left,reg_right;

        if(value_left.isIntChar && value_left.num==0){
            reg_left=PhyReg.zero;
        }else{
            reg_left= msFunction.getReg(value_left);
        }
        if(value_right.isIntChar && value_right.num==0){
            reg_right=PhyReg.zero;
        }else{
            reg_right= msFunction.getReg(value_right);
        }

        if(opType.equals(BinaryOpType.lss)) op="slt";
        else if(opType.equals(BinaryOpType.gre)) {
            /* 例：%6=icmp sgt i32 %5, 0   转为：
                slt $t1, $zero, $t0   # 如果 $t0 > 0，$t1 = 1，否则 $t1 = 0
             */
            reg_dst= msFunction.getReg(value_dst);
            new MSBinary(msModule,"sgt",reg_dst,reg_left,reg_right);
        }
        else if(opType.equals(BinaryOpType.leq)) op="sle";
        else if(opType.equals(BinaryOpType.geq)){
            /* 例：%4 = icmp sge i32 %3, 0    转为：
                slt  $t2, $t0, $zero  # $t2 = 1 如果 $t0 < 0（即 %3 < 0），否则 0
                xori $t2, $t2, 1       # $t2 = 1 如果 $t0 >= 0，否则 0
             */
            op="sge";
            reg_dst= msFunction.getReg(value_dst);
            new MSBinary(msModule,"slt",reg_dst,reg_left,reg_right);
            new MSBinary(msModule,"xori",reg_dst,reg_dst,new Imm(1));
        }
        else if(opType.equals(BinaryOpType.mod)) op="srem";
        else if(opType.equals(BinaryOpType.eq)) op="eq";
        else {
            op="ne";
            value_dst.compareInst=compareInst;
        }
    }
    public void transZextInst(ZextInst zextInst){
        Value zext_src=zextInst.usesList.get(0).usee,zext_dst=zextInst.usesList.get(1).usee;
        Reg reg_src= msFunction.getReg(zext_src);
        msFunction.value2Reg.put(zext_dst, reg_src);    // 映射到同一个寄存器
    }
    public void transCallInst(CallInst callInst){
        Use use_call_func;String str=".str",funcName;
        if(callInst.usesList.size()==1){    // void函数
            // call指令：call void <name>(<...args>)
            use_call_func=callInst.usesList.get(0);
        }else{
            use_call_func=callInst.usesList.get(1);
        }
        funcName=use_call_func.usee.name;

        if(funcName.equals("putstr")){
            new MSSelf(msModule,"li $v0, 4"); // 系统调用代码：打印字符串
            // 例：la $a0, .str           # 加载字符串地址到 $a0
            Function called_function=(Function)(use_call_func.usee);
            Value printedStr=called_function.paramList.get(0);
            if(printedStr.stringID>0) str+="."+printedStr.stringID;
            new MSMove(msModule,PhyReg.a0,new MSLabel(str));
            // syscall
            new MSSelf(msModule,"syscall");
            return;
        }
        else if(funcName.equals("getint")){
                /*例：%3=call i32 @getint()
                    li   $v0, 5              # 系统调用 5: 获取整数
                    syscall
                 */
            //new MSSelf(msModule,"li $v0, 5\nsyscall\n");
            //new MSMove(msModule,PhyReg.v0,new Imm(5));
            return;
        }else{
            /*  为调用的函数创建运行栈：从栈底到栈顶：
                局部变量 -> ret addr -> 函数参数    */

            // 为实参分配空间
            Function called_function=(Function)(use_call_func.usee);
            MSFunction msFunction=called_function.prototype_function.msFunction;
            // subu  $sp, $sp, <stackTop>+4   // 移动栈指针，为被调用的函数的参数、局部变量分配更多空间
            new MSBinary(msModule,"sub",PhyReg.sp,PhyReg.sp,new Imm(msFunction.stackTop+4));

            // 将实参逐个加载至内存空间
            int param_cnt=called_function.paramList.size(),dis=0;
            for(int j=0;j<param_cnt;j++){
                Value argu=called_function.paramList.get(j);
                Reg reg_src;
                if(argu.isIntChar && argu.num==0){
                    reg_src=PhyReg.zero;
                }else{
                    reg_src= msFunction.getReg(argu);
                }
                new MSStore(msModule,reg_src,PhyReg.sp,new Imm(4*dis));
            }

            // 跳转并链接到函数:
            // 在调用函数时，当前指令的下一条指令（即 jal 后的指令）会被存储到 $ra 中，以便函数执行完后能够返回
            new MSBranch(msModule,"jal",new MSLabel("func_"+called_function.name));
            // 从被调用者返回后：
            // add $sp, $sp, 4    恢复栈指针，弹出保存的返回地址 $ra
            msFunction.moveSp(4);
            // lw    $fp, 0($sp)    从栈中加载保存的帧指针 $fp，并恢复它的值
            new MSLoad(msModule,PhyReg.fp,PhyReg.sp,new Imm(0));
            // add $sp, $sp, 4  再次移动栈指针，恢复栈指针之前保存的状态
            msFunction.moveSp(4);
            // lw    $ra, 0($sp)    从栈中加载返回地址 $ra, $ra 存储的是调用 f 之后，MAIN 函数的下一条指令地址
            new MSLoad(msModule,PhyReg.ra,PhyReg.sp,new Imm(0));
        }
        if(callInst.usesList.size()>1){
            Use use_call_dst=callInst.usesList.get(0);
            // move  $t4, $v0   函数的返回值
            Reg reg_dst= msFunction.getReg(use_call_dst.usee);
            new MSMove(msModule,reg_dst,PhyReg.v0);
        }

    }
    public void transReturnInst(ReturnInst returnInst){
        if(msModule.msNowFunc.isMainFunc){
            new MSSelf(msModule,"li $v0, 10\nsyscall");
        }
    }
    public void transStoreInst(StoreInst storeInst){
        Value value_src=storeInst.usesList.get(0).usee,value_pointer=storeInst.usesList.get(1).usee;
        if(this.isFirstBlock && this.storeParam<msFunction.paramList.size()){
            /*例：define dso_local i32 @f(i32 %0
                %6=alloca i32
                ......
                store i32 %0, i32* %6 -> lw v0, 4($sp), 从栈上加载参数到虚拟寄存器
             */
            /*Reg reg_dst= msFunction.getReg(value_pointer);
            int dis=msFunction.value2Stack.get(value_pointer);
            new MSLoad(msModule,reg_dst,PhyReg.sp,new Imm(dis));*/
            this.storeParam++;
            return;
        }
        else{
            Reg reg_src;
            if(value_src.isIntChar && value_src.num==0){
                // store i32 0, i32* @a 或 store i32 0, i32* %1
                reg_src=PhyReg.zero;
            }else{
                reg_src= msFunction.getReg(value_src);
            }
            if(value_pointer.isGlobalValue){
                /* 例：store i32 %8, i32* @a
                    sw   $t7, 0($t5)        # 将 $t7 存储回 @a
                 */
                Reg reg_addr=msFunction.getReg(value_pointer);
                new MSStore(msModule,reg_src,reg_addr,new Imm(0));
            }else{
                /*例：store i32 %4, i32* %1
                    sw   $t2, 0($sp)        # 将 $t2 存储到栈中（局部变量 %1）
                 */
                int dis=msFunction.value2Stack.get(value_pointer);
                new MSStore(msModule,reg_src,PhyReg.fp,new Imm(dis));
            }
        }
    }
    public void transLoadInst(LoadInst loadInst){
        Value value_src=loadInst.usesList.get(0).usee,value_dst=loadInst.usesList.get(1).usee;
        Type type=value_src.type;
        if(value_src.isGlobalValue){
            /*例：%3 = load i32, i32* @c
            la   $t0, c             # 加载 @c 的地址到 $t0
            lw   $t1, 0($t0)        # 加载 @c 的值到 $t1 */
            Reg reg_addr=msFunction.getReg(value_src);
            new MSMove(msModule,reg_addr,new MSLabel(value_src.ident.name));// 1. 加载地址：la   $t0, c
            Reg reg_dst= msFunction.getReg(value_dst);
            new MSLoad(msModule,reg_dst,reg_addr,new Imm(0));
        }else{
            /*例：%5 = load i32, i32* %1
            lw   $t3, 0($sp)        # 从栈中加载 %1 的值到 $t3
             */
            Reg reg_dst= msFunction.getReg(value_dst);
            int dis=msFunction.value2Stack.get(value_src);
            new MSLoad(msModule,reg_dst,PhyReg.fp,new Imm(dis));
        }
    }
    public void transBinaryInst(Instruction inst) {
        BinaryOpType opType;
        if(inst instanceof BinaryInst binaryInst) opType=binaryInst.opType;
        else{
            CompareInst compareInst=(CompareInst) inst;
            opType=compareInst.opType;
        }
        Value value_dst=inst.usesList.get(0).usee;
        Value value_left=inst.usesList.get(1).usee,value_right=inst.usesList.get(2).usee;
        String opTypeStr="";
        if(opType.equals(BinaryOpType.add)) opTypeStr="add";
        else if(opType.equals(BinaryOpType.sub)) opTypeStr="sub";
        else if(opType.equals(BinaryOpType.mul)) opTypeStr="mul";
        else if(opType.equals(BinaryOpType.div)) opTypeStr="sdiv";
        else if(opType.equals(BinaryOpType.mod)) opTypeStr="srem";

        MSOperand reg_left,reg_right;

        if(value_left.isIntChar){    // 数字/字符
            reg_left = new Imm(value_left.num);
        }else{
            reg_left=msFunction.getReg(value_left);
        }
        if(value_right.isIntChar){    // 数字/字符
            reg_right = new Imm(value_right.num);
        }else{
            reg_right=msFunction.getReg(value_right);
        }
        if(reg_left instanceof Imm && reg_right instanceof Imm){
            reg_left=msFunction.getReg(value_left);
        }
        new MSBinary(msModule,opTypeStr, msFunction.getReg(value_dst), reg_left, reg_right);
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if(this.lable_name!=null){
            sb.append(lable_name).append(":\n");
        }
        int len=msInstList.size();
        for(int i=0;i<len;i++){
            sb.append(msInstList.get(i)).append("\n");
        }
        sb.append("\n");
        return sb.toString();
    }
    public void calc(Instruction inst, String op) {
        Value value_dst=inst.usesList.get(0).usee;
        Value value_left=inst.usesList.get(1).usee,value_right=inst.usesList.get(2).usee;

        if(value_left.isIntChar){
            new MSMove(msModule,PhyReg.t0,new Imm(value_left.num));
        }else{
            new MSLoad(msModule,PhyReg.t0,getReg(value_left),new Imm(getOffset(value_left)));
        }
        if(value_right.isIntChar){
            new MSMove(msModule,PhyReg.t1,new Imm(value_left.num));
        }else{
            new MSLoad(msModule,PhyReg.t1,getReg(value_right),new Imm(getOffset(value_right)));
        }
        new MSBinary(msModule,op,PhyReg.t2,PhyReg.t0,PhyReg.t1);
        new MSStore(msModule,PhyReg.t2,getReg(value_dst),new Imm(getOffset(value_dst)));
    }
    public Reg getReg(Value value){
        Value value_final;
        if(!msModule.memoryMap.containsKey(value)){
            value_final=msModule.globalMap.get(value.name);
        }else{
            value_final=value;
        }
        return msModule.memoryMap.get(value_final).getFirst();
    }
    public Integer getOffset(Value value){
        Value value_final;
        if(!msModule.memoryMap.containsKey(value)){
            value_final=msModule.globalMap.get(value.name);
        }else{
            value_final=value;
        }
        return msModule.memoryMap.get(value_final).getSecond();
    }
}

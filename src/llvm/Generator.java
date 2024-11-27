package llvm;

import frontend.Analyzer;
import frontend.Tree.CompUnit;
import frontend.Tree.Node;
import frontend.Tree.Exp.*;
import frontend.*;
import llvm.IR.SymTableStack;
import llvm.IR.Value.*;
import llvm.IR.Value.Inst.*;

import java.util.ArrayList;
import java.util.Stack;

public class Generator {
    public Analyzer analyzer;
    public Visitor visitor;
    public Node treeHead;   // 语法树AST
    public llvm.IR.Module llvmHead;  // llvm结构的头节点
    public IndexTable indexTable;   // 当前Module的索引表
    public SymTableStack symTableStack; // 栈式符号表

    public ArrayList<String> contents;

    public Value curLLVMValue;

    public Generator(Analyzer analyzer) {
        this.analyzer = analyzer;
        this.treeHead=analyzer.lexer.grammar.TreeHead;
        this.indexTable=analyzer.lexer.semantics.indexTable;
        this.symTableStack=new SymTableStack();
        contents=new ArrayList<String>();
    }
    public void generate(){
        visitor = new Visitor(this.treeHead);
        //visitor.printVisit(this.treeHead);
        // 新建全局符号表，添加至栈式符号表
        symTableStack.enterScope(0);
        // 生成llvm IR中间代码结构
        visitor.visit((CompUnit) treeHead,this.indexTable,this.symTableStack);
        this.llvmHead = visitor.llvmHead;
        this.generateIR();

        WriteFile writer = new WriteFile();
        writer.write("llvm_ir.txt",contents);
        writer.write("llvm_ir.ll",contents);
    }
    public void generateIR(){
        this.curLLVMValue=llvmHead;
        WriteFile writer = new WriteFile();

        // 声明库函数
        int len=llvmHead.llvmContext.libraryFuncList.size();
        for(int i=0;i<len;i++){
            llvm.IR.Value.Function func=llvmHead.llvmContext.libraryFuncList.get(i);
            String funcType=getValueType(func);
            if(funcType.isEmpty()) funcType="void";
            String str="declare "+funcType+" @"+func.name+"(";

            int param_cnt=func.paramList.size();
            for(int j=0;j<param_cnt;j++){
                str+=getValueType(func.paramList.get(j));
                if(j!=param_cnt-1) str+=", ";
            }
            str+=")";
            contents.add(str);System.out.println(str);
        }

        // 输出全局变量
        len=llvmHead.llvmContext.globalVariablesList.size();
        for(int i=0;i<len;i++){
            GlobalVariable globalVariable=llvmHead.llvmContext.globalVariablesList.get(i);    // 第i个全局变量
            String valueType=getElementValueType(globalVariable);

            String constOrVar="";
            if(globalVariable.isConst) constOrVar="constant";
            else constOrVar="global";
            if(!globalVariable.isArray){
                String str="@"+globalVariable.name+"=dso_local "+constOrVar+" "+valueType +" "+calGlobal(globalVariable);
                contents.add(str);System.out.println(str);
            }
            else{
                StoreInst storeInst=(StoreInst) (globalVariable.instList.get(globalVariable.instList.size()-1));
                Value data=storeInst.usesList.get(0).usee;

                String str="@"+globalVariable.name+"=dso_local "+constOrVar+" ["+data.arrayLength+" x "+valueType+"] ";

                if(data.zeroinitialized){
                    str+=" zeroinitializer";
                }else if(data.string!=null){
                    int strlen=data.string.length();
                    str+="c\""+data.string;
                    for(int j=strlen;j<data.arrayLength;j++){
                        str+="\\00";
                    }
                    str+="\"";
                } else{
                    str+="[";
                    int cnt=data.array.size(),maxlen=data.arrayLength;
                    for(int j=0;j<cnt;j++){
                        str+=valueType+" "+data.array.get(j);
                        if(j!=cnt-1||cnt!=maxlen) str+=", ";
                    }
                    for(int j=cnt;j<maxlen;j++){
                        str+=valueType+" "+0;
                        if(j!=maxlen-1) str+=", ";
                    }
                    str+="]";
                }
                contents.add(str);System.out.println(str);
            }
        }

        // 输出常量字符串
        len=llvmHead.llvmContext.stringList.size();
        for(int i=0;i<len;i++){
            String str="@.str";
            if(i>0) str+="."+Integer.toString(i);
            str+="=private unnamed_addr constant [";

            String string=llvmHead.llvmContext.stringList.get(i).string;
            str+=llvmHead.llvmContext.stringList.get(i).stringLength;
            str+=" x i8] c\""+string+"\"";
            contents.add(str);System.out.println(str);
        }

        // 输出函数
        len=llvmHead.llvmContext.functionsList.size();
        for(int i=0;i<len;i++){
            llvm.IR.Value.Function func=llvmHead.llvmContext.functionsList.get(i);  // 第i个函数
            visitFunc(func);
        }

    }
    public void visitFunc(llvm.IR.Value.Function func){
        String funcType=getValueType(func);
        if(funcType.isEmpty()) funcType="void";
        String funcHead="define dso_local "+funcType+" @"+func.name+"(";
        int paramNum=func.paramList.size();
        for(int i=0;i<paramNum;i++){
            Value param=func.paramList.get(i);
            String paramHead=getValueType(param);
            funcHead+=paramHead+" %"+func.slotTracker.getReg(param,true);
            if(i<paramNum-1) funcHead+=", ";
        }
        funcHead+=") {";
        contents.add(funcHead);System.out.println(funcHead);

        func.slotTracker.reg+=1;
        int len=func.basicBlockList.size();
        for(int i=0;i<len;i++){
            visitBlock(func,func.basicBlockList.get(i),funcType);
        }
        contents.add("}");System.out.println("}");
    }
    public void visitBlock(Function func,BasicBlock basicBlock,String funcType){
        System.out.println("<enter basicblock "+basicBlock.id+">");
        int len=basicBlock.instList.size();

        for(int i=0;i<len;i++){
            Instruction inst=basicBlock.instList.get(i);
            if(inst instanceof AllocaInst){
                // <result> = alloca <type>, 例：%3 = alloca i32
                Use use=inst.usesList.get(0);
                func.slotTracker.insert(use.usee);func.slotTracker.reg++;

                Type type=use.usee.type;
                String str="%"+(func.slotTracker.reg-1)+"=alloca "+getValueType(use.usee);
                contents.add(str);System.out.println(str);
            }
            else if(inst instanceof BinaryInst binaryInst){
                Use use_sum=inst.usesList.get(0),use_add1=inst.usesList.get(1),use_add2=inst.usesList.get(2);
                func.slotTracker.insert(use_sum.usee);     // 分配一个寄存器存储和
                int reg_dst=func.slotTracker.reg;func.slotTracker.reg++;

                int reg_src1=func.slotTracker.getReg(use_add1.usee,false),reg_src2=func.slotTracker.getReg(use_add2.usee,false);
                String str1="",str2="";
                if(use_add1.usee.isIntChar){    // 数字/字符
                    str1=Integer.toString(use_add1.usee.num);
                }else if(reg_src1<0){
                    Ident ident=use_add1.usee.ident;     // 局部变量中找不到，则为全局变量
                    str1="@"+ident.name;
                }else{
                    str1="%"+func.slotTracker.getReg(use_add1.usee,false);
                }
                if(use_add2.usee.isIntChar){
                    str2=Integer.toString(use_add2.usee.num);
                }else if(reg_src2<0){
                    Ident ident=use_add2.usee.ident;     // 局部变量中找不到，则为全局变量
                    str2="@"+ident.name;
                }else{
                    str2="%"+func.slotTracker.getReg(use_add2.usee,false);
                }

                String op="";
                if(binaryInst.opType.equals(BinaryOpType.add)) op="add nsw ";
                else if(binaryInst.opType.equals(BinaryOpType.sub)) op="sub nsw ";
                else if(binaryInst.opType.equals(BinaryOpType.mul)) op="mul nsw ";
                else if(binaryInst.opType.equals(BinaryOpType.div)) op="sdiv ";
                else op="srem ";

                String str="%"+reg_dst+"="+op+getValueType(use_sum.usee)+" "+str1+", "+str2;
                contents.add(str);System.out.println(str);
            }
            else if(inst instanceof UnaryInst){

            }
            else if(inst instanceof CompareInst compareInst){
                // <result> = icmp <cond> <ty> <op1>, <op2>
                Use use_result=inst.usesList.get(0),use_left=inst.usesList.get(1),use_right=inst.usesList.get(2);
                func.slotTracker.insert(use_result.usee);     // 分配一个寄存器存储比较结果
                int reg_dst=func.slotTracker.reg;func.slotTracker.reg++;

                int reg_src1=func.slotTracker.getReg(use_left.usee,false),reg_src2=func.slotTracker.getReg(use_right.usee,false);
                String str1="",str2="";
                if(use_left.usee.isIntChar){    // 数字/字符
                    str1=Integer.toString(use_left.usee.num);
                }else if(reg_src1<0){
                    Ident ident=use_left.usee.ident;     // 局部变量中找不到，则为全局变量
                    str1="@"+ident.name;
                }else{
                    str1="%"+func.slotTracker.getReg(use_left.usee,false);
                }
                if(use_right.usee.isIntChar){
                    str2=Integer.toString(use_right.usee.num);
                }else if(reg_src2<0){
                    Ident ident=use_right.usee.ident;     // 局部变量中找不到，则为全局变量
                    str2="@"+ident.name;
                }else{
                    str2="%"+func.slotTracker.getReg(use_right.usee,false);
                }

                String op="";
                if(compareInst.opType.equals(BinaryOpType.lss)) op="slt";
                else if(compareInst.opType.equals(BinaryOpType.gre)) op="sgt";
                else if(compareInst.opType.equals(BinaryOpType.leq)) op="sle";
                else if(compareInst.opType.equals(BinaryOpType.geq)) op="sge";
                else if(compareInst.opType.equals(BinaryOpType.mod)) op="srem";
                else op="ne";

                String str="%"+reg_dst+"=icmp "+op+" "+getValueType(use_left.usee)+" "+str1+", "+str2;
                contents.add(str);System.out.println(str);
            }
            else if(inst instanceof BranchInst){
                if(inst.usesList.size()==1){

                }else{
                    // 跳转指令: br i1 <cond>, label <iftrue>, label <iffalse>
                    Use use_cond=inst.usesList.get(0),use_iffalse=inst.usesList.get(1),use_iftrue=inst.usesList.get(2);
                    String str="br i1 %"+func.slotTracker.getReg(use_cond.usee,false);

                    func.slotTracker.insert(use_iffalse.usee);int reg_false=func.slotTracker.reg++;
                    str+=", label %"+reg_false;
                    // iffalse


                    contents.add(str);System.out.println(str);
                }

            }else if(inst instanceof CallInst){
                Use use_call_func;String str;
                if(inst.usesList.size()==1){    // void函数
                    // call指令：call void <name>(<...args>)
                    use_call_func=inst.usesList.get(0);
                    str="call void @"+use_call_func.usee.name+"(";

                }else{      // int/char型函数
                    // call指令：<result> = call [ret attrs] <ty> <name>(<...args>)
                    Use use_call_dst=inst.usesList.get(0);use_call_func=inst.usesList.get(1);
                    func.slotTracker.insert(use_call_dst.usee);
                    str="%"+Integer.toString(func.slotTracker.reg)+"=call "+getValueType(use_call_dst.usee)+" @"+use_call_func.usee.name+"(";
                    func.slotTracker.reg++;
                }

                Function called_function=(Function)(use_call_func.usee);
                int param_cnt=called_function.paramList.size();
                for(int j=0;j<param_cnt;j++){
                    Value param=called_function.paramList.get(j);
                    if(param.isIntChar){        // 数字/字符
                        str+=getValueType(param)+" "+param.num;
                    }
                    else if(param.type.equals(Type.StringTyID)){ // 参数为字符串
                        String str_arr="["+param.stringLength+" x i8]";
                        String strLabel="@.str";
                        if(param.stringID>0) strLabel+="."+param.stringID;
                        str+="i8* getelementptr inbounds ("+str_arr+", "+str_arr+"* "+strLabel+", i64 0, i64 0)";
                    }else{      // int/char型参数
                        str+=getValueType(param)+" %"+func.slotTracker.getReg(param,false);
                    }
                    if(j<param_cnt-1) str+=", ";
                }
                str+=")";
                contents.add(str);System.out.println(str);
            }
            else if(inst instanceof JumpInst){

            }
            else if(inst instanceof LoadInst){
                // <result> = load <ty>, ptr <pointer>, 例：%5 = load i32, i32* %3
                Use use_src=inst.usesList.get(0),use_dst=inst.usesList.get(1);
                int reg_src=func.slotTracker.getReg(use_src.usee,false);
                if(reg_src<0){
                    Ident ident=use_src.usee.ident;     // 局部变量中找不到，则为全局变量
                    String str="%"+func.slotTracker.getReg(use_dst.usee,true)
                            +"=load "+getValueType(use_src.usee)+", "+getValueType(use_src.usee)+"* @"+ident.name;
                    contents.add(str);System.out.println(str);
                }else{
                    String str="%"+func.slotTracker.getReg(use_dst.usee,true)
                            +"=load "+getValueType(use_src.usee)+", "+getValueType(use_src.usee)+"* %"+func.slotTracker.getReg(use_src.usee,false);
                    contents.add(str);System.out.println(str);
                }
            }
            else if(inst instanceof StoreInst){
                // store <ty> <value>, ptr <pointer>, 例：store i32 %0, i32* %3
                Use use_src=inst.usesList.get(0),use_dst=inst.usesList.get(1);
                int reg_value=func.slotTracker.getReg(use_src.usee,false);
                int reg_pointer=func.slotTracker.getReg(use_dst.usee,false);
                Ident ident=use_dst.usee.ident;

                String str_value="",str_pointer="";
                if(reg_value<0){    // value是数字或字符
                    //updateGlobal(ident,use_src.usee.num);
                    str_value=Integer.toString(use_src.usee.num);
                }else{
                    str_value="%"+Integer.toString(reg_value);
                }

                if(reg_pointer<0){  // 局部变量中找不到，则为全局变量
                    str_pointer="* @"+ident.name;
                }else{
                    str_pointer="* %"+func.slotTracker.getReg(use_dst.usee,true);
                }
                String str="store "+getValueType(use_src.usee)+" "+str_value+", "+getValueType(use_dst.usee)+str_pointer;
                contents.add(str);System.out.println(str);
            }
            else if(inst instanceof ReturnInst){
                if(inst.usesList.isEmpty()){
                    String str="ret void";
                    contents.add(str);System.out.println(str);
                }else{
                    Use use=inst.usesList.get(0);
                    if(use.usee.isIntChar){
                        String str="ret "+funcType+" "+use.usee.num;
                        contents.add(str);System.out.println(str);
                    }
                    int reg=func.slotTracker.getReg(use.usee,false);
                    if(reg<0){
                        Ident ident=use.usee.ident;     // 局部变量中找不到，则为全局变量
                        //String str="ret "+funcType+" @"+ident.name;
                        //contents.add(str);System.out.println(str);
                    }else{
                        String str="ret "+funcType+" %"+reg;
                        contents.add(str);System.out.println(str);
                    }
                }
            }
            else if(inst instanceof TruncInst){
                Use use_trunc_src=inst.usesList.get(0),use_trunc_dst=inst.usesList.get(1);
                int reg_dst=func.slotTracker.reg;
                func.slotTracker.insert(use_trunc_dst.usee);func.slotTracker.reg++;

                String str_value_src="";
                if(use_trunc_src.usee.isIntChar){
                    str_value_src=Integer.toString(use_trunc_src.usee.num);
                }else{
                    int reg_src=func.slotTracker.getReg(use_trunc_src.usee,false);
                    str_value_src="%"+Integer.toString(reg_src);
                }

                String str="%"+reg_dst+"=trunc "+getValueType(use_trunc_src.usee)+" "+str_value_src+" to "+getValueType(use_trunc_dst.usee);
                contents.add(str);System.out.println(str);
            }
            else if(inst instanceof ZextInst){
                Use use_zext_src=inst.usesList.get(0),use_zext_dst=inst.usesList.get(1);
                int reg_dst=func.slotTracker.reg;
                func.slotTracker.insert(use_zext_dst.usee);func.slotTracker.reg++;

                String str_value_src="";
                if(use_zext_src.usee.isIntChar){
                    str_value_src=Integer.toString(use_zext_src.usee.num);
                }else{
                    int reg_src=func.slotTracker.getReg(use_zext_src.usee,false);
                    str_value_src="%"+Integer.toString(reg_src);
                }

                String str="%"+reg_dst+"=zext "+getValueType(use_zext_src.usee)+" "+str_value_src+" to "+getValueType(use_zext_dst.usee);
                contents.add(str);System.out.println(str);
            }
            else if(inst instanceof GetelementInst){
                // <result> = getelementptr <ty>, ptr <ptrval>{, <ty> <idx>}*
                Use use_dst=inst.usesList.get(0),use_array=inst.usesList.get(1),use_pos=inst.usesList.get(2);
                func.slotTracker.insert(use_dst.usee);int reg_dst=func.slotTracker.reg++;

                String str_array="",str_pos="";
                int reg_array=func.slotTracker.getReg(use_array.usee,false);
                if(reg_array<0){
                    str_array="@"+use_array.usee.ident.name;
                }else{
                    str_array="%"+reg_array;
                }

                if(use_pos.usee.isIntChar){    // pos是数字或字符
                    str_pos=Integer.toString(use_pos.usee.num);
                }else{
                    str_pos="%"+func.slotTracker.getReg(use_pos.usee,false);
                }
                String str="%"+reg_dst+"=getelementptr inbounds "+getElementValueType(use_array.usee)+", "+
                        getElementValueType(use_array.usee)+"* "+str_array;

                if(use_array.usee.type.equals(Type.IntArrayTyID)||use_array.usee.type.equals(Type.CharArrayTyID)){
                    str+=", "+"i32 0";
                }
                str+=", "+getValueType(use_pos.usee)+" "+str_pos;
                contents.add(str);System.out.println(str);
            }
        }
    }
    public String getValueType(Value value){
        Type type=value.type;
        if(type.equals(Type.IntegerTyID)) return "i32";
        else if(type.equals(Type.CharTyID)) return "i8";
        else if(type.equals(Type.BooleanTyID)) return "i1";
        else if(type.equals(Type.StringTyID)||value.type.equals(Type.CharPointerTyID)) return "i8*";
        else if(type.equals(Type.IntPointerTyID)) return "i32*";
        else if(type.equals(Type.IntArrayTyID)||type.equals(Type.CharArrayTyID)){
            int size=value.arrayLength;String str_type="";
            if(type.equals(Type.IntArrayTyID)) str_type="i32";
            else str_type="i8";
            return "["+size+" x "+str_type+"]";
        }
        else return "";
    }
    public String getElementValueType(Value value){ // 获取数组元素的类型
        Type type=value.type;
        if(type.equals(Type.IntegerTyID) || type.equals(Type.IntPointerTyID)) return "i32";
        else if(type.equals(Type.CharTyID) || type.equals(Type.StringTyID)||type.equals(Type.CharPointerTyID)) return "i8";
        else if(type.equals(Type.BooleanTyID)) return "i1";
        else if(type.equals(Type.IntArrayTyID)||type.equals(Type.CharArrayTyID)){
            int size=value.arrayLength;String str_type="";
            if(type.equals(Type.IntArrayTyID)) str_type="i32";
            else str_type="i8";
            return "["+size+" x "+str_type+"]";
        }
        else return "";
    }

    public int calGlobal(GlobalVariable global){
        int len=global.instList.size(),ans=0;
        for(int i=0;i<len;i++){
            Instruction inst=global.instList.get(i);
            /*if(inst instanceof BinaryInst binaryInst){
                System.out.println(binaryInst.toString()+" "+binaryInst.opType+": ");
                Use use1=binaryInst.usesList.get(0),use2=binaryInst.usesList.get(1),use3=binaryInst.usesList.get(2);
                System.out.println(use1.usee+" "+use1.usee.num+" "+use2.usee+" "+use2.usee.num+" "+use3.usee+" "+use3.usee.num+" ");
            }*/
            if(inst instanceof StoreInst storeInst){
                Use use_data=storeInst.usesList.get(0);
                ans=use_data.usee.num;
                //System.out.println(storeInst.toString()+" "+use_data.usee);
            }
        }
        global.num=ans;     // 将值存入全局变量
        return ans;
    }

    public void updateGlobal(Ident ident,int num){  // 将ident对应的全局变量的值，更新为num
        int len=llvmHead.llvmContext.globalVariablesList.size();
        for(int i=0;i<len;i++){
            GlobalVariable globalVariable=llvmHead.llvmContext.globalVariablesList.get(i);    // 第i个全局变量
            if(!globalVariable.name.equals(ident.name)) continue;
            // 找到和ident同名的全局变量
            globalVariable.num=num;
        }
    }
}

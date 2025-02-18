package llvm;
import frontend.IndexTable;
import frontend.LexType;
import frontend.SymTable;
import frontend.Tree.*;
import frontend.Tree.Const.ConstDecl;
import frontend.Tree.Const.ConstDef;
import frontend.Tree.Const.ConstExp;
import frontend.Tree.Const.ConstInitVal;
import frontend.Tree.Func.*;
import frontend.Tree.Stmt.Block;
import frontend.Tree.Stmt.ForStmt;
import frontend.Tree.Stmt.Stmt;
import frontend.Tree.Var.InitVal;
import frontend.Tree.Var.VarDecl;
import frontend.Tree.Var.VarDef;
import frontend.Tree.Exp.*;

import llvm.IR.Value.*;
import llvm.IR.*;
import llvm.IR.Value.Inst.*;

import java.util.ArrayList;
import java.util.Stack;

public class Visitor {      // 遍历语法树
    public Node treeHead;
    public Node curNode;
    public int scope_no;
    public ArrayList<String> info;

    public llvm.IR.Module llvmHead;  // llvm结构的头节点
    public LlvmContext llvmCtx;
    public IndexTable indexTable;

    public Value curValue;
    public Function curFunction;
    public BasicBlock curBlock;     // 当前所在的基本块
    public int blockId;     // 基本块编号

    public int allocaNum;   // AllocaInst数目

    public SymTableStack symTableStack;

    public Stack<BasicBlock> outloopBlock_Stack;      // 当前for循环的出口基本块（break跳转至此）
    public Stack<BasicBlock> forStmtBlock_Stack;     // 当前for循环的第二个forStmt对应基本块（continue跳转至此）

    public Visitor(){}
    public Visitor(Node treeHead){
        this.treeHead = treeHead;
        this.curNode = treeHead;
        this.scope_no = 0;      // 初始化为全局作用域编号：0
        this.blockId=1;
        this.allocaNum=0;
        info=new ArrayList<String>();
        outloopBlock_Stack=new Stack<>();
        forStmtBlock_Stack=new Stack<>();
    }

    void visit(CompUnit compUnit,IndexTable indexTable,SymTableStack symTableStack) {
        this.scope_no=compUnit.scope_no;
        this.llvmHead=new llvm.IR.Module();
        this.llvmCtx=this.llvmHead.llvmContext;
        this.indexTable=indexTable;
        this.symTableStack=symTableStack;

        this.curValue=llvmHead;
        this.curBlock=null;
        this.add_putint_putch_putstr_to_Library();   //添加库函数
        // 编译单元 CompUnit → {Decl} {FuncDef} MainFuncDef
        int len=compUnit.visited;
        for(int i=0;i<=len;i++){
            curNode=compUnit.next.get(i);
            if(curNode instanceof Decl) visit_Decl((Decl)curNode);
            else if(curNode instanceof FuncDef) visit_FuncDef((FuncDef)curNode);
            else if(curNode instanceof MainFuncDef) visit_MainFuncDef((MainFuncDef)curNode);
        }
        curNode=curNode.pre;
    }
    void visit_Decl(Decl decl) {
        this.scope_no=decl.scope_no;
        // 声明 Decl → ConstDecl | VarDecl
        curNode=decl.next.get(0);
        if(curNode instanceof ConstDecl) visit_ConstDecl((ConstDecl)curNode);
        else visit_VarDecl((VarDecl)curNode);

        curNode=curNode.pre;
    }
    void visit_ConstDecl(ConstDecl constDecl) {
        this.scope_no=constDecl.scope_no;
        // 常量声明 ConstDecl → 'const' BType ConstDef { ',' ConstDef } ';'
        int len=constDecl.visited;
        for(int i=0;i<=len;i++){
            curNode=constDecl.next.get(i);
            visit_ConstDef((ConstDef)curNode);
        }
        curNode=curNode.pre;
    }
    void visit_ConstDef(ConstDef constDef) {
        this.scope_no=constDef.scope_no;
        // 常量定义 ConstDef → Ident [ '[' ConstExp ']' ] '=' ConstInitVal
        Ident ident=(Ident)(constDef.next.get(0));
        int len=constDef.visited;

        Type type=ident.getType();
        if(type.equals(Type.IntPointerTyID)) type=Type.IntArrayTyID;
        else if(type.equals(Type.CharPointerTyID)) type=Type.CharArrayTyID;
        Value constant=new Value(ValueType.ConstantTy,type,ident.name);  // 新定义的常量标识符
        constant.ident=ident;
        Value constData=new Value(ValueType.ConstantDataTy,type,ident.name); // 常量数据

        Instruction storeInst=new StoreInst();        // store指令
        // Use关系：store constData, constant
        Use use_dst=new Use(constant,storeInst),use_src=new Use(constData,storeInst);
        llvmCtx.usesList.add(use_src);storeInst.usesList.add(use_src);
        llvmCtx.usesList.add(use_dst);storeInst.usesList.add(use_dst);

        if(this.scope_no!=0){      // 局部变量
            // 1. alloca指令：%3 = alloca i32
            AllocaInst allocaInst=new AllocaInst();
            // Use关系：alloca <reg-type> for funcFParam
            Use use_alloca=new Use(constant,allocaInst);
            llvmCtx.usesList.add(use_alloca);allocaInst.usesList.add(use_alloca);
            constant.withAllocatedSpace=true;
            // constant加入当前function的valueList
            curFunction.valueList.add(constant);
            curFunction.allocatedValueList.add(constant);
            curFunction.allocaInstList.add(allocaInst);
            curBlock.instList.add(allocaInst);
        }

        if(len==1){
            // 1. ConstDef → Ident '=' ConstInitVal
            if(this.scope_no==0){   // 全局常量
                GlobalVariable globalVariable=new GlobalVariable(ident.getType(),ident.name,ident);
                llvmCtx.globalVariablesList.add(globalVariable);
                this.curValue=globalVariable;
                globalVariable.isConst=true;
            }
            curNode=curNode.next.get(curNode.visited);
            visit_ConstInitVal((ConstInitVal)curNode,constant,constData);
            if(this.scope_no!=0){          // 局部变量
                this.curBlock.instList.add(storeInst);
            }
            curNode=curNode.pre;
        }else if(len==2){
            // 2. ConstDef → Ident '[' ConstExp ']'  '=' ConstInitVal
            if(this.scope_no==0) {       // 全局常量
                GlobalVariable globalVariable = new GlobalVariable(ident.getType(), ident.name, ident);
                llvmCtx.globalVariablesList.add(globalVariable);
                this.curValue = globalVariable;
                globalVariable.isArray = true;globalVariable.isConst = true;
            }
            curNode=curNode.next.get(1);        // ConstDef -> ConstExp
            Value size=new Value(ValueType.ConstantDataTy,Type.IntegerTyID);
            visit_ConstExp((ConstExp) curNode,size);
            constData.arrayLength=size.num;      // 存储数组长度
            constant.arrayLength=size.num;
            System.out.println("const array size:"+size.num);
            curNode=curNode.pre;

            constData.array=new ArrayList<>();   // data初始化为数组
            constData.zeroinitialized=false;

            curNode=curNode.next.get(curNode.visited);
            if(curNode.visited<0) constData.zeroinitialized=true;   // ConstInitVal -> '{' '}'
            else visit_ConstInitVal((ConstInitVal)curNode,constant,constData);
            curNode=curNode.pre;
        }

        if(this.scope_no==0){
            GlobalVariable globalVariable=(GlobalVariable)this.curValue;
            globalVariable.instList.add(storeInst);
            globalVariable.num=storeInst.usesList.get(0).usee.num;
            constant.isGlobalValue=true;
        }

        // 栈式符号表插入新定义的常量:
        // 全局常量：无需分配寄存器；局部常量：已通过alloca指令分配寄存器
        constant.num=storeInst.usesList.get(0).usee.num;
        symTableStack.addSymTab(ident.symTab,constant);
    }
    void visit_ConstInitVal(ConstInitVal constInitVal,Value constant,Value constData){
        this.scope_no=constInitVal.scope_no;
        // 常量初值 ConstInitVal → ConstExp | '{' [ ConstExp { ',' ConstExp } ] '}' | StringConst
        // 1.常表达式初值 2.一维数组初值
        int len=constInitVal.visited;
        if(constInitVal.multipleInitVal){
            // 1. ConstInitVal → '{' [ ConstExp { ',' ConstExp } ] '}'
            if(this.scope_no==0){       // 全局变量
                for(int i=0;i<=len;i++){    // 遍历constInitVal的所有初始值
                    curNode=constInitVal.next.get(i);
                    visit_ConstExp((ConstExp) curNode,constData);   // 数值存储于data.num中
                    constData.array.add(constData.num);   // 将值存入数组
                    curNode=curNode.pre;
                }
            }else{
                // (1)中%2的类型是[3 x i32]*; (2)中%2的类型是i32*
                GetelementInst getelementInst_init=new GetelementInst();
                Type pointerType;
                if(constData.type.equals(Type.IntArrayTyID)) pointerType=Type.IntegerTyID;
                else pointerType=Type.CharTyID;
                Value value_dst_init=new Value(ValueType.VariableTy,pointerType);
                Value pos_init=new Value(ValueType.VariableDataTy,Type.IntegerTyID);pos_init.num=0;pos_init.isIntChar=true;

                Use use_getelement_dst_init=new Use(value_dst_init,getelementInst_init);
                Use use_array_init=new Use(constant,getelementInst_init),use_pos_init=new Use(pos_init,getelementInst_init);
                llvmCtx.usesList.add(use_getelement_dst_init);getelementInst_init.usesList.add(use_getelement_dst_init);
                llvmCtx.usesList.add(use_array_init);getelementInst_init.usesList.add(use_array_init);
                llvmCtx.usesList.add(use_pos_init);getelementInst_init.usesList.add(use_pos_init);

                if(this.curBlock!=null) {
                    this.curBlock.instList.add(getelementInst_init);
                    // value_dst_init加入当前function的valueList
                    curFunction.valueList.add(value_dst_init);
                }

                Value pre_array=value_dst_init;
                for(int i=0;i<=len;i++){    // 遍历initVal的所有初始值
                    curNode=constInitVal.next.get(i);
                    Type eleType;
                    if(constant.type.equals(Type.IntArrayTyID)) eleType=Type.IntegerTyID;
                    else eleType=Type.CharTyID;
                    Value eleNum=new Value(ValueType.VariableTy,eleType);
                    visit_ConstExp((ConstExp) curNode,eleNum);   // 数值存储于data.num中
                    constData.array.add(eleNum.num);   // 将值存入数组
                    curNode=curNode.pre;

                    // 1. getelementptr指令
                    if(i>0){
                        GetelementInst getelementInst=new GetelementInst();
                        Value value_dst=new Value(ValueType.VariableTy,pre_array.type);
                        Value pos=new Value(ValueType.VariableDataTy,Type.IntegerTyID);pos.num=1;pos.isIntChar=true;

                        Use use_getelement_dst=new Use(value_dst,getelementInst);
                        Use use_array=new Use(pre_array,getelementInst),use_pos=new Use(pos,getelementInst);
                        llvmCtx.usesList.add(use_getelement_dst);getelementInst.usesList.add(use_getelement_dst);
                        llvmCtx.usesList.add(use_array);getelementInst.usesList.add(use_array);
                        llvmCtx.usesList.add(use_pos);getelementInst.usesList.add(use_pos);

                        if(this.curBlock!=null) {
                            this.curBlock.instList.add(getelementInst);
                            // value_dst加入当前function的valueList
                            curFunction.valueList.add(value_dst);
                        }
                        pre_array=value_dst;
                    }

                    // store指令
                    StoreInst storeInst=new StoreInst();
                    Use use_dst=new Use(pre_array,storeInst),use_src=new Use(eleNum,storeInst);
                    llvmCtx.usesList.add(use_src);storeInst.usesList.add(use_src);
                    llvmCtx.usesList.add(use_dst);storeInst.usesList.add(use_dst);

                    if(this.curBlock!=null) {
                        this.curBlock.instList.add(storeInst);
                    }
                }
            }
        }
        else if(constInitVal.next.get(0) instanceof ConstExp){
            // 2. ConstInitVal → ConstExp
            curNode=constInitVal.next.get(0);
            visit_ConstExp((ConstExp)curNode,constData);
            curNode=curNode.pre;
        }else{
            // 3. ConstInitVal -> StringConst
            _String str=(_String)constInitVal.next.get(0);
            constData.string=str.string;

            if(this.scope_no!=0){   // 局部变量
                add_local_string(str,constant,constant.arrayLength);
            }
        }
    }
    void visit_VarDecl(VarDecl varDecl) {
        this.scope_no=varDecl.scope_no;
        // 变量声明 VarDecl → BType VarDef { ',' VarDef } ';'
        int len=varDecl.visited;
        for(int i=0;i<=len;i++){
            curNode=varDecl.next.get(i);
            visit_VarDef((VarDef)curNode);
            curNode=curNode.pre;
        }
    }
    void visit_VarDef(VarDef varDef) {
        this.scope_no=varDef.scope_no;
        // 变量定义 VarDef → Ident [ '[' ConstExp ']' ] | Ident [ '[' ConstExp ']' ] '=' InitVal

        Ident ident=(Ident)(varDef.next.get(0));
        Node lastNode=varDef.next.get(varDef.next.size()-1);
        int len=varDef.visited;

        Type type=ident.getType();
        if(type.equals(Type.IntPointerTyID)) type=Type.IntArrayTyID;
        else if(type.equals(Type.CharPointerTyID)) type=Type.CharArrayTyID;
        Value var=new Value(ValueType.VariableTy,type,ident.name);  // 新定义的变量标识符
        var.ident=ident;
        Value data=new Value(ValueType.VariableDataTy,type,ident.name); // 变量数据

        StoreInst storeInst=new StoreInst();        // store指令
        // Use关系：store data, var
        Use use_dst=new Use(var,storeInst),use_src=new Use(data,storeInst);
        llvmCtx.usesList.add(use_src);storeInst.usesList.add(use_src);
        llvmCtx.usesList.add(use_dst);storeInst.usesList.add(use_dst);

        if(this.scope_no!=0){      // 局部变量
            // 1. alloca指令：%3 = alloca i32
            AllocaInst allocaInst=new AllocaInst();
            // Use关系：alloca <reg-type>
            Use use_alloca=new Use(var,allocaInst);
            llvmCtx.usesList.add(use_alloca);allocaInst.usesList.add(use_alloca);
            var.withAllocatedSpace=true;
            // var加入当前function的valueList
            curFunction.valueList.add(var);
            curFunction.allocatedValueList.add(var);
            curFunction.allocaInstList.add(allocaInst);
            curBlock.instList.add(allocaInst);
        }

        if(lastNode instanceof InitVal){
            Value curVar=var;
            if(this.scope_no==0) {   // 全局常量
                GlobalVariable globalVariable = new GlobalVariable(ident.getType(), ident.name, ident);
                llvmCtx.globalVariablesList.add(globalVariable);
                this.curValue = globalVariable;
                curVar=globalVariable;
            }
            if(len==1){
                // 1. VarDef → Ident '=' InitVal
                curNode=curNode.next.get(curNode.visited);
                visit_InitVal((InitVal)curNode,var,data);
                if(scope_no!=0){
                    this.curBlock.instList.add(storeInst);
                }
                curNode=curNode.pre;
            }else{
                // 2. VarDef → Ident '[' ConstExp ']' '=' InitVal
                // (1)获取数组长度
                curVar.isArray=true;
                curNode=curNode.next.get(1);        // VarDef -> ConstExp
                Value size=new Value(ValueType.ConstantDataTy,Type.IntegerTyID);
                visit_ConstExp((ConstExp) curNode,size);
                data.arrayLength=size.num;      // 存储数组长度
                var.arrayLength=size.num;
                System.out.println("var array size:"+size.num);
                curNode=curNode.pre;

                data.array=new ArrayList<>();   // data初始化为数组
                data.zeroinitialized=false;

                curNode=curNode.next.get(curNode.visited);
                if(curNode.visited<0) data.zeroinitialized=true;   // ConstInitVal -> '{' '}'
                else visit_InitVal((InitVal)curNode,var,data);
                curNode=curNode.pre;
            }
        }else{
            // 2. VarDef → Ident [ '[' ConstExp ']' ]
            Value curVar=var;
            if(this.scope_no==0) {   // 全局常量
                GlobalVariable globalVariable = new GlobalVariable(ident.getType(), ident.name, ident);
                llvmCtx.globalVariablesList.add(globalVariable);
                this.curValue = globalVariable;
                curVar=globalVariable;
            }
            if(len==0){
                // 3. VarDef → Ident
            }else{
                // 4. VarDef → Ident '[' ConstExp ']'
                curVar.isArray=true;
                curNode=curNode.next.get(1);        // VarDef -> ConstExp
                Value size=new Value(ValueType.ConstantDataTy,Type.IntegerTyID);
                visit_ConstExp((ConstExp) curNode,size);
                data.arrayLength=size.num;      // 存储数组长度
                var.arrayLength=size.num;
                System.out.println("var array size:"+size.num);
                curNode=curNode.pre;

                data.array=new ArrayList<>();   // data初始化为数组
            }
        }
        if(scope_no==0){
            GlobalVariable globalVariable=(GlobalVariable)this.curValue;
            globalVariable.instList.add(storeInst);
            globalVariable.num=storeInst.usesList.get(0).usee.num;
            var.isGlobalValue=true;
        }

        // 栈式符号表插入新定义的变量:
        // 全局变量：无需分配寄存器；局部变量：已通过alloca指令分配寄存器
        var.num=storeInst.usesList.get(0).usee.num;
        symTableStack.addSymTab(ident.symTab,var);
        System.out.println("add:"+ident.symTab.name+" "+symTableStack.curElement.scope_no);
    }
    void visit_InitVal(InitVal initVal,Value var,Value data){
        this.scope_no=initVal.scope_no;
        // 变量初值 InitVal → Exp | '{' [ Exp { ',' Exp } ] '}' | StringConst
        // 1.表达式初值 2.一维数组初值
        int len=initVal.visited;
        if(initVal.multipleInitVal){
            // 1. InitVal → '{' [ Exp { ',' Exp } ] '}'
            if(this.scope_no==0){       // 全局变量
                for(int i=0;i<=len;i++){    // 遍历initVal的所有初始值
                    curNode=initVal.next.get(i);
                    visit_Exp((Exp)curNode,data);   // 数值存储于data.num中
                    data.array.add(data.num);   // 将值存入数组
                    System.out.println("array add:"+data.num);
                    curNode=curNode.pre;
                }
            }
            else{      // 局部变量
                // 将数组加载至新分配空间的变量var中
                // getelementptr指令：<result> = getelementptr <ty>, ptr <ptrval>{, <ty> <idx>}*

                // 区分：
                //  %1 = alloca [3 x i32]
                //  (1) %2 = getelementptr inbounds [3 x i32], [3 x i32]* %1, i32 0
                //  (2) %2 = getelementptr inbounds [3 x i32], [3 x i32]* %1, i32 0, i32 0
                // (1)中%2的类型是[3 x i32]*; (2)中%2的类型是i32*
                GetelementInst getelementInst_init=new GetelementInst();
                Type pointerType;
                if(data.type.equals(Type.IntArrayTyID)) pointerType=Type.IntegerTyID;
                else pointerType=Type.CharTyID;
                Value value_dst_init=new Value(ValueType.VariableTy,pointerType);
                Value pos_init=new Value(ValueType.VariableDataTy,Type.IntegerTyID);pos_init.num=0;pos_init.isIntChar=true;

                Use use_getelement_dst_init=new Use(value_dst_init,getelementInst_init);
                Use use_array_init=new Use(var,getelementInst_init),use_pos_init=new Use(pos_init,getelementInst_init);
                llvmCtx.usesList.add(use_getelement_dst_init);getelementInst_init.usesList.add(use_getelement_dst_init);
                llvmCtx.usesList.add(use_array_init);getelementInst_init.usesList.add(use_array_init);
                llvmCtx.usesList.add(use_pos_init);getelementInst_init.usesList.add(use_pos_init);

                if(this.curBlock!=null) {
                    this.curBlock.instList.add(getelementInst_init);
                    // value_dst_init加入当前function的valueList
                    curFunction.valueList.add(value_dst_init);
                }

                Value pre_array=value_dst_init;
                for(int i=0;i<=len;i++){    // 遍历initVal的所有初始值
                    curNode=initVal.next.get(i);
                    Type eleType;
                    if(var.type.equals(Type.IntArrayTyID)) eleType=Type.IntegerTyID;
                    else eleType=Type.CharTyID;
                    Value eleNum=new Value(ValueType.VariableTy,eleType);
                    visit_Exp((Exp)curNode,eleNum);   // 数值存储于data.num中
                    data.array.add(eleNum.num);   // 将值存入数组
                    curNode=curNode.pre;

                    // 1. getelementptr指令
                    if(i>0){
                        GetelementInst getelementInst=new GetelementInst();
                        Value value_dst=new Value(ValueType.VariableTy,pre_array.type);
                        Value pos=new Value(ValueType.VariableDataTy,Type.IntegerTyID);pos.num=1;pos.isIntChar=true;

                        Use use_getelement_dst=new Use(value_dst,getelementInst);
                        Use use_array=new Use(pre_array,getelementInst),use_pos=new Use(pos,getelementInst);
                        llvmCtx.usesList.add(use_getelement_dst);getelementInst.usesList.add(use_getelement_dst);
                        llvmCtx.usesList.add(use_array);getelementInst.usesList.add(use_array);
                        llvmCtx.usesList.add(use_pos);getelementInst.usesList.add(use_pos);

                        if(this.curBlock!=null) {
                            this.curBlock.instList.add(getelementInst);
                            // value_dst加入当前function的valueList
                            curFunction.valueList.add(value_dst);
                        }
                        pre_array=value_dst;
                    }

                    // store指令
                    StoreInst storeInst=new StoreInst();
                    Use use_dst=new Use(pre_array,storeInst),use_src=new Use(eleNum,storeInst);
                    llvmCtx.usesList.add(use_src);storeInst.usesList.add(use_src);
                    llvmCtx.usesList.add(use_dst);storeInst.usesList.add(use_dst);

                    if(this.curBlock!=null) {
                        this.curBlock.instList.add(storeInst);
                    }
                }
            }
            return;
        }else if(initVal.next.get(0) instanceof Exp){
            // 2. InitVal → Exp
            curNode=initVal.next.get(0);
            visit_Exp((Exp)curNode,data);
            curNode=curNode.pre;
        }else{
            // 3. InitVal -> StringConst
            _String str=(_String)initVal.next.get(0);
            data.string=str.string;
            if(this.scope_no!=0){   // 局部变量
                add_local_string(str,var,var.arrayLength);
            }
        }
    }
    void visit_FuncDef(FuncDef funcDef) {
        this.scope_no=curNode.next.get(curNode.visited).scope_no;   // Block的scope_no
        this.allocaNum=0;
        // 函数定义 FuncDef → FuncType Ident '(' [FuncFParams] ')' Block
        Ident ident=(Ident)(curNode.next.get(0));Type type;
        if(ident.symTab.type.equals(LexType.INT_FUN_IDENFR)) type=Type.IntegerTyID;
        else if(ident.symTab.type.equals(LexType.CHAR_FUN_IDENFR)) type=Type.CharTyID;
        else type=Type.VoidTyID;

        llvm.IR.Value.Function function=new llvm.IR.Value.Function(ValueType.FunctionTy,type, ident.name);
        llvmCtx.functionsList.add(function);
        this.curValue=function;this.curFunction=function;

        BasicBlock basicBlock=new BasicBlock(this.blockId++,getSymTable(this.scope_no));
        function.basicBlockList.add(basicBlock);this.curBlock=basicBlock;
        symTableStack.enterScope(this.scope_no);    // 栈式符号表，创建新的作用域

        Node node=curNode.next.get(1);
        if(node instanceof FuncFParams){
            // 1. FuncDef → FuncType Ident '(' FuncFParams ')' Block, 有参数
            curNode=node;visit_FuncFParams((FuncFParams) curNode);
        }else{
            // 2. FuncDef → FuncType Ident '(' ')' Block, 无参数
        }
        curNode=funcDef.next.get(curNode.visited);
        visit_Block((Block)curNode,true);

        curNode=curNode.pre;
    }
    void visit_MainFuncDef(MainFuncDef mainFuncDef) {
        this.scope_no=mainFuncDef.scope_no;
        this.allocaNum=0;
        // 主函数定义 MainFuncDef → 'int' 'main' '(' ')' Block
        llvm.IR.Value.Function function=new llvm.IR.Value.Function(ValueType.FunctionTy,Type.IntegerTyID,"main");
        llvmCtx.functionsList.add(function);
        this.curValue=function;this.curFunction=function;     // 每个function由多个基本块组成

        BasicBlock basicBlock=new BasicBlock(this.blockId++,getSymTable(this.scope_no));
        function.basicBlockList.add(basicBlock);this.curBlock=basicBlock;
        symTableStack.enterScope(this.scope_no);    // 栈式符号表，创建新的作用域

        curNode=mainFuncDef.next.get(0);
        visit_Block((Block)curNode,true);

        curNode=curNode.pre;
    }
    void visit_FuncFParams(FuncFParams funcFParams) {
        this.scope_no=funcFParams.scope_no;
        // 函数形参表 FuncFParams → FuncFParam { ',' FuncFParam }
        int len=funcFParams.visited;
        for(int i=0;i<=len;i++){
            curNode=funcFParams.next.get(i);    // FuncFParams -> FuncFParam
            visit_FuncFParam((FuncFParam)curNode,i);
            curNode=curNode.pre;
        }
    }
    void visit_FuncFParam(FuncFParam funcFParam,int cnt) {  // cnt为当前形参的序号
        this.scope_no=funcFParam.scope_no;
        // 函数形参 FuncFParam → BType Ident ['[' ']']
        Ident ident=(Ident)(funcFParam.next.get(0));
        Value param=new Value(ValueType.VariableDataTy,ident.getType(),ident.name);param.ident=ident;
        Use use_param=new Use(param,(User)curValue);    // 建立function对param的使用关系
        llvmCtx.usesList.add(use_param);
        llvm.IR.Value.Function function=(llvm.IR.Value.Function)curValue;
        function.paramList.add(param);      // param插入当前函数的参数表

        // 1. alloca指令：%3 = alloca i32
        AllocaInst allocaInst=new AllocaInst();
        // Use关系：alloca <reg-type> for funcFParam
        Value new_param=new Value(ValueType.VariableDataTy,ident.getType(),ident.name);new_param.ident=ident;
        Use use_dst=new Use(new_param,allocaInst);
        llvmCtx.usesList.add(use_dst);allocaInst.usesList.add(use_dst);
        new_param.withAllocatedSpace=true;
        // new_param加入当前function的valueList
        curFunction.valueList.add(new_param);
        curFunction.allocatedValueList.add(new_param);
        curFunction.allocaInstList.add(allocaInst);     // alloca指令插入到当前基本块
        curBlock.instList.add(allocaInst);

        // 2. store指令：store i32 %0, i32* %3
        StoreInst storeInst=new StoreInst();
        // Use关系：store var, data (var是局部变量)
        Use use_src=new Use(param,storeInst),use_dst_2=new Use(new_param,storeInst);
        llvmCtx.usesList.add(use_src);storeInst.usesList.add(use_src);
        llvmCtx.usesList.add(use_dst_2);storeInst.usesList.add(use_dst_2);
        this.curBlock.instList.add(storeInst); // store指令插入到当前基本块

        //function.slotTracker.stack.add(new_param);
        // 栈式符号表插入新定义的形式参数:
        // 局部变量：已通过alloca指令分配寄存器
        symTableStack.addSymTab(ident.symTab,new_param);
        new_param.correspondsFuncFParam=true;

        curNode=curNode.pre;    // FuncFParam <- FuncFParams
    }
    void visit_Block(Block block,Boolean created) {
        this.scope_no=block.scope_no;
        //参数created：如果block属于FuncDef，则其作用域已创建
        if(!created) symTableStack.enterScope(this.scope_no);    // 栈式符号表，创建新的作用域
        else symTableStack.curElement.scope_no=block.scope_no;
        // 语句块 Block → '{' { BlockItem } '}'
        // 语句块项 BlockItem → Decl | Stmt

        int len=block.visited;
        for(int i=0;i<=len;i++){
            curNode=block.next.get(i);
            if(curNode instanceof Decl) visit_Decl((Decl)curNode);
            else if(curNode instanceof Stmt stmt) {
                visit_Stmt((Stmt)curNode);
                if(stmt.isBreak) {
                    block.hasBreakStmt=true;curNode=curNode.pre;break;
                }
                else if(stmt.isContinue) {
                    block.hasContinueStmt=true;curNode=curNode.pre;break;
                }
                else if(stmt.isReturn){
                    block.hasReturnStmt=true;curNode=curNode.pre;break;
                }
            }
            curNode=curNode.pre;    // Decl/Stmt <- Block
        }

        if(curNode.pre instanceof FuncDef funcDef && funcDef.isVoid){
            Instruction returnInst=new ReturnInst();        // return指令
            if(this.curBlock!=null) this.curBlock.instList.add(returnInst);
        }
        symTableStack.exitScope();
        if(symTableStack.curElement.scope_no==0) this.curBlock=null;

    }
    void visit_Stmt(Stmt stmt) {
        this.scope_no=stmt.scope_no;
        if(stmt.isIf){
            llvm.IR.Value.Function curFunction=(llvm.IR.Value.Function) this.curValue;
            // 1. 条件语句：Stmt → 'if' '(' Cond ')' Stmt1 [ 'else' Stmt2 ] BasicBlock3
            // Cond
            curNode=curNode.next.get(0);    // Stmt -> Cond
            Value value_true_final=new Value(ValueType.BasicBlockTy,Type.LabelTyID);
            Value value_false_final=new Value(ValueType.BasicBlockTy,Type.LabelTyID);
            CondType condType=visit_Cond((Cond)this.curNode,value_true_final,value_false_final);
            curNode=curNode.pre;

            Value output;
            BasicBlock output_block=new BasicBlock(this.blockId++,getSymTable(this.scope_no));
            System.out.println(condType);

            if(condType.equals(CondType.True)){
                System.out.println("cond is true;");
                // Cond条件为真: 进入其下属的Stmt1对应基本块
                curNode=curNode.next.get(1);    // Stmt -> Stmt1
                visit_Stmt((Stmt)this.curNode);
                curNode=curNode.pre;
                return;
            }
            else if(condType.equals(CondType.False)){
                System.out.println("cond is false;");
                // Cond条件为假: 判断else语句是否存在，若存在，进入其下属的Stmt2对应基本块；若不存在，离开
                if(stmt.hasElse){
                    curNode=curNode.next.get(2);    // Stmt -> Stmt2
                    visit_Stmt((Stmt)this.curNode);
                    curNode=curNode.pre;
                }else{
                    // 不存在else语句时，不切换基本块，直接进入下一条语句
                }
                return;
            }
            else{
                // Cond条件待定
                curNode=curNode.next.get(1);    // Stmt -> Stmt1
                visit_Stmt((Stmt)this.curNode);
                curNode=curNode.pre;

                if(stmt.hasElse){
                    output=new Value(ValueType.BasicBlockTy,Type.LabelTyID);
                }else{
                    // 不存在else语句时，跳转至下一个基本块
                    output=value_false_final;
                }

                int inst_num=curBlock.instList.size();
                if(inst_num>0 && curBlock.instList.get(inst_num-1) instanceof ReturnInst){
                    // 若上一个指令为ret指令，不再加入跳转指令
                }
                else if(this.curNode.next.get(1) instanceof Stmt stmt1 && !stmt1.isBreak && !stmt1.isContinue && !stmt1.isReturn){
                    BranchInst branchInst_endIf=new BranchInst();
                    Use use_if=new Use(output,branchInst_endIf);
                    llvmCtx.usesList.add(use_if);branchInst_endIf.usesList.add(use_if);
                    this.curBlock.instList.add(branchInst_endIf);
                }
                else{
                    // 对于break, continue语句: 跳转命令在stmt1中添加
                }

                if(stmt.hasElse){
                    BasicBlock stmt2_block=new BasicBlock(this.blockId++,getSymTable(this.scope_no));
                    stmt2_block.value=value_false_final;
                    curFunction.basicBlockList.add(stmt2_block); // 函数的基本块列表中，插入Stmt2基本块
                    curFunction.valueList.add(value_false_final);
                    this.curBlock=stmt2_block;

                    curNode=curNode.next.get(2);    // Stmt -> Stmt2
                    visit_Stmt((Stmt)this.curNode);
                    curNode=curNode.pre;

                    inst_num=curBlock.instList.size();
                    if(inst_num>0 && curBlock.instList.get(inst_num-1) instanceof ReturnInst){
                        // 若上一个指令为ret指令，不再加入跳转指令
                    }else if(this.curNode.next.get(2) instanceof Stmt stmt2 && !stmt2.isBreak && !stmt2.isContinue && !stmt2.isReturn){
                        BranchInst branchInst_endElse=new BranchInst();
                        Use use_else=new Use(output,branchInst_endElse);
                        llvmCtx.usesList.add(use_else);branchInst_endElse.usesList.add(use_else);
                        this.curBlock.instList.add(branchInst_endElse);
                    }

                }
                output_block.value=output;
                this.curBlock=output_block;
                curFunction.basicBlockList.add(output_block);
                curFunction.valueList.add(output);
            }
        }
        else if(stmt.isFor){
            // Stmt -> 'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt
            if(stmt.visited==0){
                // 1. ForStmt与Cond全部缺省:
                BranchInst branchInst=new BranchInst();
                // 跳转指令: br label <dest>, 对应循环体内的首个基本块
                Value value_br_dst=new Value(ValueType.BasicBlockTy,Type.LabelTyID);
                Use use_dst=new Use(value_br_dst,branchInst);
                llvmCtx.usesList.add(use_dst);branchInst.usesList.add(use_dst);
                this.curBlock.instList.add(branchInst);

                // 进入Stmt，创建新基本块
                curFunction.valueList.add(value_br_dst);
                BasicBlock basicBlock=new BasicBlock(this.blockId++,getSymTable(this.scope_no));
                this.curBlock=basicBlock;
                basicBlock.value=value_br_dst;
                curFunction.basicBlockList.add(basicBlock);

                // 出循环后的基本块
                Value value_outLoop=new Value(ValueType.BasicBlockTy,Type.LabelTyID);
                BasicBlock outLoop_block=new BasicBlock(this.blockId++,getSymTable(this.scope_no));
                this.outloopBlock_Stack.add(outLoop_block);
                outLoop_block.value=value_outLoop;

                // 第二个ForStmt不存在, cond不存在: 遇到continue, 直接进入循环体Stmt对应的基本块
                this.forStmtBlock_Stack.add(basicBlock);

                // 循环体内Stmt语句
                curNode=curNode.next.get(0);
                visit_Stmt((Stmt)this.curNode);
                curNode=curNode.pre;

                // 循环体结束时，跳转回循环体开始的基本块
                Stmt stmt1=(Stmt)(curNode.next.get(0));
                if(!stmt1.isBreak && !stmt1.isContinue && !stmt1.isReturn){
                    curBlock.instList.add(branchInst);
                }

                // 将出口基本块加入basicBlockList
                curFunction.basicBlockList.add(outLoop_block);
                curFunction.valueList.add(value_outLoop);
                this.curBlock=outLoop_block;

                // outloopBlock_Stack弹出当前for循环对应的出口基本块
                int size=outloopBlock_Stack.size();
                if(size>0 && outloopBlock_Stack.get(size-1).equals(outLoop_block)){
                    outloopBlock_Stack.pop();
                }
                // forStmtBlock_Stack弹出当前for循环的第二个ForStmt对应的基本块
                size=forStmtBlock_Stack.size();
                if(size>0 && forStmtBlock_Stack.get(size-1).equals(basicBlock)){
                    forStmtBlock_Stack.pop();
                }
            }
            else if(stmt.visited==1){
                // 2. ForStmt与Cond中缺省两个，3种情况
                if(stmt.with_first_forStmt){
                    // 2.1 只有第一个ForStmt
                    curNode=curNode.next.get(0);    // Stmt -> ForStmt
                    visit_ForStmt((ForStmt) curNode);
                    curNode=curNode.pre;

                    BranchInst branchInst=new BranchInst();
                    // 跳转指令: br label <dest>, 对应循环体内的首个基本块
                    Value value_br_dst=new Value(ValueType.BasicBlockTy,Type.LabelTyID);
                    Use use_dst=new Use(value_br_dst,branchInst);
                    llvmCtx.usesList.add(use_dst);branchInst.usesList.add(use_dst);
                    this.curBlock.instList.add(branchInst);

                    // 进入Stmt对应的新基本块
                    curFunction.valueList.add(value_br_dst);
                    BasicBlock basicBlock=new BasicBlock(this.blockId++,getSymTable(this.scope_no));
                    this.curBlock=basicBlock;
                    basicBlock.value=value_br_dst;
                    curFunction.basicBlockList.add(basicBlock);

                    // 出循环后的基本块
                    Value value_outLoop=new Value(ValueType.BasicBlockTy,Type.LabelTyID);
                    BasicBlock outLoop_block=new BasicBlock(this.blockId++,getSymTable(this.scope_no));
                    this.outloopBlock_Stack.add(outLoop_block);
                    outLoop_block.value=value_outLoop;

                    // 第二个ForStmt不存在, cond不存在: 遇到continue, 直接进入循环体Stmt对应的基本块
                    this.forStmtBlock_Stack.add(basicBlock);

                    // 循环体内Stmt语句
                    curNode=curNode.next.get(1);
                    visit_Stmt((Stmt)this.curNode);
                    curNode=curNode.pre;

                    // 循环体结束时，跳转回循环体开始的基本块
                    Stmt stmt1=(Stmt)(curNode.next.get(1));
                    if(!stmt1.isBreak && !stmt1.isContinue && !stmt1.isReturn){
                        curBlock.instList.add(branchInst);
                    }

                    // 将出口基本块加入basicBlockList
                    curFunction.basicBlockList.add(outLoop_block);
                    curFunction.valueList.add(value_outLoop);
                    this.curBlock=outLoop_block;

                    // outloopBlock_Stack弹出当前for循环对应的出口基本块
                    int size=outloopBlock_Stack.size();
                    if(size>0 && outloopBlock_Stack.get(size-1).equals(outLoop_block)){
                        outloopBlock_Stack.pop();
                    }
                    // forStmtBlock_Stack弹出当前for循环的第二个ForStmt对应的基本块
                    size=forStmtBlock_Stack.size();
                    if(size>0 && forStmtBlock_Stack.get(size-1).equals(basicBlock)){
                        forStmtBlock_Stack.pop();
                    }
                }
                else if(stmt.with_cond){
                    // 2.2 只有Cond
                    Value value_cond_block=new Value(ValueType.BasicBlockTy,Type.LabelTyID);
                    // 原基本块跳转至Cond对应基本块的指令
                    BranchInst branchInst_cond=new BranchInst();
                    // 跳转指令: br label <dest>, 对应循环体内的首个基本块
                    Use use_dst=new Use(value_cond_block,branchInst_cond);
                    llvmCtx.usesList.add(use_dst);branchInst_cond.usesList.add(use_dst);
                    this.curBlock.instList.add(branchInst_cond);

                    // 为Cond创建新基本块
                    curFunction.valueList.add(value_cond_block);
                    BasicBlock basicBlock_cond=new BasicBlock(this.blockId++,getSymTable(this.scope_no));
                    // 跳转至Cond对应的基本块
                    this.curBlock=basicBlock_cond;
                    basicBlock_cond.value=value_cond_block;
                    curFunction.basicBlockList.add(basicBlock_cond);

                    curNode=curNode.next.get(0);    // Stmt -> Cond
                    Value value_true_final=new Value(ValueType.BasicBlockTy,Type.LabelTyID);
                    Value value_false_final=new Value(ValueType.BasicBlockTy,Type.LabelTyID);
                    CondType condType=visit_Cond((Cond) curNode,value_true_final,value_false_final);
                    curNode=curNode.pre;

                    // 出循环后的基本块
                    Value value_out_loop;
                    BasicBlock outLoop_block=new BasicBlock(this.blockId++,getSymTable(this.scope_no));
                    this.outloopBlock_Stack.add(outLoop_block);

                    if(condType.equals(CondType.True) || condType.equals(CondType.False)){
                        Value br_cond;
                        if(condType.equals(CondType.True)){
                            // Cond条件为真:
                            br_cond=new Value(ValueType.TrueTy,Type.BooleanTyID);
                        }else{
                            br_cond=new Value(ValueType.FalseTy,Type.BooleanTyID);
                        }
                        BranchInst branchInst_true=new BranchInst();
                        // value_br_dst对应循环下属的Stmt1基本块；value_out_loop对应跳出循环外的首个基本块
                        Value value_br_dst=new Value(ValueType.BasicBlockTy,Type.LabelTyID);
                        value_out_loop=new Value(ValueType.BasicBlockTy,Type.BooleanTyID);
                        outLoop_block.value=value_out_loop;

                        Use use_cond=new Use(br_cond,branchInst_true);
                        Use use_true=new Use(value_br_dst,branchInst_true),use_false=new Use(value_out_loop,branchInst_true);

                        llvmCtx.usesList.add(use_cond);branchInst_true.usesList.add(use_cond);
                        llvmCtx.usesList.add(use_false);branchInst_true.usesList.add(use_false);
                        llvmCtx.usesList.add(use_true);branchInst_true.usesList.add(use_true);

                        this.curBlock.instList.add(branchInst_true);

                        // 进入Stmt对应的新基本块
                        curFunction.valueList.add(value_br_dst);
                        BasicBlock basicBlock=new BasicBlock(this.blockId++,getSymTable(this.scope_no));
                        this.curBlock=basicBlock;
                        basicBlock.value=value_br_dst;
                        curFunction.basicBlockList.add(basicBlock);
                    }
                    else{
                        // Cond条件待定: 此时已进入Stmt对应的基本块
                        value_out_loop=value_false_final;
                        outLoop_block.value=value_out_loop;
                    }
                    // 第二个ForStmt不存在: 遇到continue, 直接进入cond对应的基本块
                    this.forStmtBlock_Stack.add(basicBlock_cond);

                    curNode=curNode.next.get(1);
                    visit_Stmt((Stmt)this.curNode);
                    curNode=curNode.pre;

                    // 循环体结束时，跳转回Cond基本块，再次判断
                    Stmt stmt1=(Stmt)(curNode.next.get(1));
                    if(!stmt1.isBreak && !stmt1.isContinue && !stmt1.isReturn){
                        curBlock.instList.add(branchInst_cond);
                    }

                    // 跳出循环
                    this.curBlock=outLoop_block;
                    curFunction.valueList.add(value_out_loop);
                    curFunction.basicBlockList.add(outLoop_block);

                    // outloopBlock_Stack弹出当前for循环对应的出口基本块
                    int size=outloopBlock_Stack.size();
                    if(size>0 && outloopBlock_Stack.get(size-1).equals(outLoop_block)){
                        outloopBlock_Stack.pop();
                    }
                    // forStmtBlock_Stack弹出当前for循环的第二个ForStmt对应的基本块
                    size=forStmtBlock_Stack.size();
                    if(size>0 && forStmtBlock_Stack.get(size-1).equals(basicBlock_cond)){
                        forStmtBlock_Stack.pop();
                    }
                }
                else{
                    // 2.3 只有第二个ForStmt
                    BranchInst branchInst_stmt=new BranchInst();
                    // 跳转至循环体Stmt对应的基本块的指令
                    Value value_stmt=new Value(ValueType.BasicBlockTy,Type.LabelTyID);
                    Use use_stmt_dst=new Use(value_stmt,branchInst_stmt);
                    llvmCtx.usesList.add(use_stmt_dst);branchInst_stmt.usesList.add(use_stmt_dst);
                    this.curBlock.instList.add(branchInst_stmt);

                    // 进入循环体Stmt对应的基本块
                    curFunction.valueList.add(value_stmt);
                    BasicBlock basicBlock_stmt=new BasicBlock(this.blockId++,getSymTable(this.scope_no));
                    this.curBlock=basicBlock_stmt;
                    basicBlock_stmt.value=value_stmt;
                    curFunction.basicBlockList.add(basicBlock_stmt);

                    // 出循环后的基本块
                    Value value_outLoop=new Value(ValueType.BasicBlockTy,Type.LabelTyID);
                    BasicBlock outLoop_block=new BasicBlock(this.blockId++,getSymTable(this.scope_no));
                    this.outloopBlock_Stack.add(outLoop_block);
                    outLoop_block.value=value_outLoop;

                    // 创建第二个ForStmt的基本块
                    Value value_self_change=new Value(ValueType.BasicBlockTy,Type.LabelTyID);
                    BasicBlock basicBlock_forStmt=new BasicBlock(this.blockId++,getSymTable(this.scope_no));
                    this.forStmtBlock_Stack.add(basicBlock_forStmt);
                    basicBlock_forStmt.value=value_self_change;

                    // 执行循环体内Stmt中的语句
                    curNode=curNode.next.get(1);
                    visit_Stmt((Stmt) curNode);
                    curNode=curNode.pre;

                    Stmt stmt1=(Stmt)(curNode.next.get(1));
                    if(!stmt1.isBreak && !stmt1.isContinue && !stmt1.isReturn){
                        // 循环体结束时，跳转至第二个ForStmt对应的基本块
                        BranchInst branchInst_forStmt=new BranchInst();
                        // 跳转指令: br label <dest>, 对应第二个ForStmt的基本块
                        Use use_forstmt_dst=new Use(value_self_change,branchInst_forStmt);
                        llvmCtx.usesList.add(use_forstmt_dst);branchInst_forStmt.usesList.add(use_forstmt_dst);
                        this.curBlock.instList.add(branchInst_forStmt);
                    }

                    // 进入第二个ForStmt的基本块
                    curFunction.valueList.add(value_self_change);
                    this.curBlock=basicBlock_forStmt;
                    curFunction.basicBlockList.add(basicBlock_forStmt);

                    // 执行第二个ForStmt中的语句
                    curNode=curNode.next.get(0);
                    visit_ForStmt((ForStmt) curNode);
                    curNode=curNode.pre;

                    // 再次跳转至循环体内Stmt对应的基本块
                    curBlock.instList.add(branchInst_stmt);

                    // 将出口基本块加入basicBlockList
                    curFunction.basicBlockList.add(outLoop_block);
                    curFunction.valueList.add(value_outLoop);
                    this.curBlock=outLoop_block;

                    // outloopBlock_Stack弹出当前for循环对应的出口基本块
                    int size=outloopBlock_Stack.size();
                    if(size>0 && outloopBlock_Stack.get(size-1).equals(outLoop_block)){
                        outloopBlock_Stack.pop();
                    }
                    // forStmtBlock_Stack弹出当前for循环的第二个ForStmt对应的基本块
                    size=forStmtBlock_Stack.size();
                    if(size>0 && forStmtBlock_Stack.get(size-1).equals(basicBlock_forStmt)){
                        forStmtBlock_Stack.pop();
                    }
                }
            }
            else if(stmt.visited==2){
                // 3. ForStmt与Cond中缺省一个，3种情况
                if(!stmt.with_first_forStmt){
                    // 3.1 缺首个ForStmt
                    Value value_cond_block=new Value(ValueType.BasicBlockTy,Type.LabelTyID);
                    // 原基本块跳转至Cond对应基本块的指令
                    BranchInst branchInst_cond=new BranchInst();
                    // 跳转指令: br label <dest>, 对应循环体内的首个基本块
                    Use use_dst=new Use(value_cond_block,branchInst_cond);
                    llvmCtx.usesList.add(use_dst);branchInst_cond.usesList.add(use_dst);
                    this.curBlock.instList.add(branchInst_cond);

                    // 为Cond创建新基本块
                    curFunction.valueList.add(value_cond_block);
                    BasicBlock basicBlock_cond=new BasicBlock(this.blockId++,getSymTable(this.scope_no));
                    // 跳转至Cond对应的基本块
                    this.curBlock=basicBlock_cond;
                    basicBlock_cond.value=value_cond_block;
                    curFunction.basicBlockList.add(basicBlock_cond);

                    curNode=curNode.next.get(0);    // Stmt -> Cond
                    Value value_true_final=new Value(ValueType.BasicBlockTy,Type.LabelTyID);
                    Value value_false_final=new Value(ValueType.BasicBlockTy,Type.LabelTyID);
                    CondType condType=visit_Cond((Cond) curNode,value_true_final,value_false_final);
                    curNode=curNode.pre;

                    // 创建出口基本块
                    Value value_out_loop;
                    BasicBlock outLoop_block=new BasicBlock(this.blockId++,getSymTable(this.scope_no));
                    this.outloopBlock_Stack.add(outLoop_block);

                    // 创建第二个ForStmt对应的基本块
                    Value value_self_change=new Value(ValueType.BasicBlockTy,Type.LabelTyID);
                    BasicBlock basicBlock_forStmt=new BasicBlock(this.blockId++,getSymTable(this.scope_no));
                    this.forStmtBlock_Stack.add(basicBlock_forStmt);
                    basicBlock_forStmt.value=value_self_change;

                    if(condType.equals(CondType.True) || condType.equals(CondType.False)){
                        Value br_cond;
                        if(condType.equals(CondType.True)){
                            // Cond条件为真:
                            br_cond=new Value(ValueType.TrueTy,Type.BooleanTyID);
                        }else{
                            br_cond=new Value(ValueType.FalseTy,Type.BooleanTyID);
                        }
                        BranchInst branchInst_true=new BranchInst();
                        // value_br_dst对应循环下属的Stmt1基本块；value_out_loop对应跳出循环外的首个基本块
                        Value value_br_dst=new Value(ValueType.BasicBlockTy,Type.LabelTyID);
                        value_out_loop=new Value(ValueType.BasicBlockTy,Type.BooleanTyID);
                        outLoop_block.value=value_out_loop;

                        Use use_cond=new Use(br_cond,branchInst_true);
                        Use use_true=new Use(value_br_dst,branchInst_true),use_false=new Use(value_out_loop,branchInst_true);

                        llvmCtx.usesList.add(use_cond);branchInst_true.usesList.add(use_cond);
                        llvmCtx.usesList.add(use_false);branchInst_true.usesList.add(use_false);
                        llvmCtx.usesList.add(use_true);branchInst_true.usesList.add(use_true);

                        this.curBlock.instList.add(branchInst_true);

                        // 进入Stmt对应的新基本块
                        curFunction.valueList.add(value_br_dst);
                        BasicBlock basicBlock=new BasicBlock(this.blockId++,getSymTable(this.scope_no));
                        this.curBlock=basicBlock;
                        basicBlock.value=value_br_dst;
                        curFunction.basicBlockList.add(basicBlock);
                    }
                    else{
                        // Cond条件待定: 此时已进入Stmt对应的基本块
                        value_out_loop=value_false_final;
                        outLoop_block.value=value_out_loop;
                    }
                    // 执行循环体内Stmt中的语句
                    curNode=curNode.next.get(2);    // Stmt -> Stmt1
                    visit_Stmt((Stmt)this.curNode);
                    curNode=curNode.pre;

                    Stmt stmt1=(Stmt)(curNode.next.get(2));
                    if(!stmt1.isBreak && !stmt1.isContinue && !stmt1.isReturn){
                        // 循环体结束时，跳转至第二个ForStmt对应的基本块
                        BranchInst branchInst_forStmt=new BranchInst();
                        // 跳转指令: br label <dest>, 对应第二个ForStmt的基本块
                        Use use_forstmt_dst=new Use(value_self_change,branchInst_forStmt);
                        llvmCtx.usesList.add(use_forstmt_dst);branchInst_forStmt.usesList.add(use_forstmt_dst);
                        this.curBlock.instList.add(branchInst_forStmt);
                    }

                    // 进入第二个ForStmt的基本块
                    curFunction.valueList.add(value_self_change);
                    this.curBlock=basicBlock_forStmt;
                    curFunction.basicBlockList.add(basicBlock_forStmt);

                    // 执行第二个ForStmt中的语句
                    curNode=curNode.next.get(1);
                    visit_ForStmt((ForStmt) curNode);
                    curNode=curNode.pre;

                    // 循环体结束时，跳转回Cond基本块，再次判断
                    curBlock.instList.add(branchInst_cond);

                    // 跳出循环
                    curFunction.valueList.add(value_out_loop);
                    this.curBlock=outLoop_block;
                    curFunction.basicBlockList.add(outLoop_block);

                    // outloopBlock_Stack弹出当前for循环对应的出口基本块
                    int size=outloopBlock_Stack.size();
                    if(size>0 && outloopBlock_Stack.get(size-1).equals(outLoop_block)){
                        outloopBlock_Stack.pop();
                    }
                    // forStmtBlock_Stack弹出当前for循环的第二个ForStmt对应的基本块
                    size=forStmtBlock_Stack.size();
                    if(size>0 && forStmtBlock_Stack.get(size-1).equals(basicBlock_forStmt)){
                        forStmtBlock_Stack.pop();
                    }
                }
                else if(!stmt.with_cond){
                    // 3.2 缺Cond
                    curNode=curNode.next.get(0);    // Stmt -> ForStmt
                    visit_ForStmt((ForStmt) curNode);
                    curNode=curNode.pre;

                    BranchInst branchInst_stmt=new BranchInst();
                    // 跳转指令: br label <dest>, 对应循环体内的首个基本块
                    Value value_br_dst=new Value(ValueType.BasicBlockTy,Type.LabelTyID);
                    Use use_dst=new Use(value_br_dst,branchInst_stmt);
                    llvmCtx.usesList.add(use_dst);branchInst_stmt.usesList.add(use_dst);
                    this.curBlock.instList.add(branchInst_stmt);

                    // 进入Stmt对应的新基本块
                    curFunction.valueList.add(value_br_dst);
                    BasicBlock basicBlock=new BasicBlock(this.blockId++,getSymTable(this.scope_no));
                    this.curBlock=basicBlock;
                    basicBlock.value=value_br_dst;
                    curFunction.basicBlockList.add(basicBlock);

                    // 出循环后的基本块
                    Value value_outLoop=new Value(ValueType.BasicBlockTy,Type.LabelTyID);
                    BasicBlock outLoop_block=new BasicBlock(this.blockId++,getSymTable(this.scope_no));
                    this.outloopBlock_Stack.add(outLoop_block);
                    outLoop_block.value=value_outLoop;

                    // 创建第二个ForStmt对应的基本块
                    Value value_self_change=new Value(ValueType.BasicBlockTy,Type.LabelTyID);
                    BasicBlock basicBlock_forStmt=new BasicBlock(this.blockId++,getSymTable(this.scope_no));
                    forStmtBlock_Stack.add(basicBlock_forStmt);
                    basicBlock_forStmt.value=value_self_change;

                    // 循环体内Stmt语句
                    curNode=curNode.next.get(2);
                    visit_Stmt((Stmt)this.curNode);
                    curNode=curNode.pre;

                    Stmt stmt1=(Stmt)(curNode.next.get(2));
                    if(!stmt1.isBreak && !stmt1.isContinue && !stmt1.isReturn){
                        // 循环体结束时，跳转至第二个ForStmt对应的基本块
                        BranchInst branchInst_forStmt=new BranchInst();
                        // 跳转指令: br label <dest>, 对应第二个ForStmt的基本块
                        Use use_forstmt_dst=new Use(value_self_change,branchInst_forStmt);
                        llvmCtx.usesList.add(use_forstmt_dst);branchInst_forStmt.usesList.add(use_forstmt_dst);
                        this.curBlock.instList.add(branchInst_forStmt);
                    }

                    // 进入第二个ForStmt的基本块
                    curFunction.valueList.add(value_self_change);
                    this.curBlock=basicBlock_forStmt;
                    curFunction.basicBlockList.add(basicBlock_forStmt);

                    // 执行第二个ForStmt中的语句
                    curNode=curNode.next.get(1);
                    visit_ForStmt((ForStmt) curNode);
                    curNode=curNode.pre;

                    // 再次跳转至循环体内Stmt对应的基本块
                    curBlock.instList.add(branchInst_stmt);

                    // 将出口基本块加入basicBlockList
                    curFunction.basicBlockList.add(outLoop_block);
                    curFunction.valueList.add(value_outLoop);
                    this.curBlock=outLoop_block;

                    // outloopBlock_Stack弹出当前for循环对应的出口基本块
                    int size=outloopBlock_Stack.size();
                    if(size>0 && outloopBlock_Stack.get(size-1).equals(outLoop_block)){
                        outloopBlock_Stack.pop();
                    }
                    // forStmtBlock_Stack弹出当前for循环的第二个ForStmt对应的基本块
                    size=forStmtBlock_Stack.size();
                    if(size>0 && forStmtBlock_Stack.get(size-1).equals(basicBlock_forStmt)){
                        forStmtBlock_Stack.pop();
                    }
                }
                else{
                    // 3.3 缺第二个forStmt
                    curNode=curNode.next.get(0);    // Stmt -> ForStmt
                    visit_ForStmt((ForStmt) curNode);
                    curNode=curNode.pre;

                    Value value_cond_block=new Value(ValueType.BasicBlockTy,Type.LabelTyID);
                    // 原基本块跳转至Cond对应基本块的指令
                    BranchInst branchInst_cond=new BranchInst();
                    // 跳转指令: br label <dest>, 对应循环体内的首个基本块
                    Use use_dst=new Use(value_cond_block,branchInst_cond);
                    llvmCtx.usesList.add(use_dst);branchInst_cond.usesList.add(use_dst);
                    this.curBlock.instList.add(branchInst_cond);

                    // 为Cond创建新基本块
                    curFunction.valueList.add(value_cond_block);
                    BasicBlock basicBlock_cond=new BasicBlock(this.blockId++,getSymTable(this.scope_no));
                    // 跳转至Cond对应的基本块
                    this.curBlock=basicBlock_cond;
                    basicBlock_cond.value=value_cond_block;
                    curFunction.basicBlockList.add(basicBlock_cond);

                    curNode=curNode.next.get(1);    // Stmt -> Cond
                    Value value_true_final=new Value(ValueType.BasicBlockTy,Type.LabelTyID);
                    Value value_false_final=new Value(ValueType.BasicBlockTy,Type.LabelTyID);
                    CondType condType=visit_Cond((Cond) curNode,value_true_final,value_false_final);
                    curNode=curNode.pre;

                    Value value_out_loop;
                    BasicBlock outLoop_block=new BasicBlock(this.blockId++,getSymTable(this.scope_no));
                    this.outloopBlock_Stack.add(outLoop_block);

                    if(condType.equals(CondType.True) || condType.equals(CondType.False)){
                        Value br_cond;
                        if(condType.equals(CondType.True)){
                            // Cond条件为真:
                            br_cond=new Value(ValueType.TrueTy,Type.BooleanTyID);
                        }else{
                            br_cond=new Value(ValueType.FalseTy,Type.BooleanTyID);
                        }
                        BranchInst branchInst_true=new BranchInst();
                        // value_br_dst对应循环下属的Stmt1基本块；value_out_loop对应跳出循环外的首个基本块
                        Value value_br_dst=new Value(ValueType.BasicBlockTy,Type.LabelTyID);
                        value_out_loop=new Value(ValueType.BasicBlockTy,Type.BooleanTyID);
                        outLoop_block.value=value_out_loop;

                        Use use_cond=new Use(br_cond,branchInst_true);
                        Use use_true=new Use(value_br_dst,branchInst_true),use_false=new Use(value_out_loop,branchInst_true);

                        llvmCtx.usesList.add(use_cond);branchInst_true.usesList.add(use_cond);
                        llvmCtx.usesList.add(use_false);branchInst_true.usesList.add(use_false);
                        llvmCtx.usesList.add(use_true);branchInst_true.usesList.add(use_true);

                        this.curBlock.instList.add(branchInst_true);

                        // 进入Stmt对应的新基本块
                        curFunction.valueList.add(value_br_dst);
                        BasicBlock basicBlock=new BasicBlock(this.blockId++,getSymTable(this.scope_no));
                        this.curBlock=basicBlock;
                        basicBlock.value=value_br_dst;
                        curFunction.basicBlockList.add(basicBlock);
                    }
                    else{
                        // Cond条件待定
                        value_out_loop=value_false_final;
                        outLoop_block.value=value_out_loop;
                    }
                    // 第二个ForStmt不存在: 遇到continue, 进入cond对应的基本块
                    this.forStmtBlock_Stack.add(basicBlock_cond);

                    curNode=curNode.next.get(2);
                    visit_Stmt((Stmt)this.curNode);
                    curNode=curNode.pre;

                    // 循环体结束时，跳转回Cond基本块，再次判断
                    Stmt stmt1=(Stmt)(curNode.next.get(2));
                    if(!stmt1.isBreak && !stmt1.isContinue && !stmt1.isReturn){
                        curBlock.instList.add(branchInst_cond);
                    }

                    // 跳出循环
                    curFunction.valueList.add(value_out_loop);
                    this.curBlock=outLoop_block;
                    curFunction.basicBlockList.add(outLoop_block);

                    // outloopBlock_Stack弹出当前for循环对应的出口基本块
                    int size=outloopBlock_Stack.size();
                    if(size>0 && outloopBlock_Stack.get(size-1).equals(outLoop_block)){
                        outloopBlock_Stack.pop();
                    }
                    // forStmtBlock_Stack弹出当前for循环的第二个ForStmt对应的基本块
                    size=forStmtBlock_Stack.size();
                    if(size>0 && forStmtBlock_Stack.get(size-1).equals(basicBlock_cond)){
                        forStmtBlock_Stack.pop();
                    }
                }
            }
            else{
                // 无缺省
                curNode=curNode.next.get(0);    // Stmt -> ForStmt
                visit_ForStmt((ForStmt) curNode);
                curNode=curNode.pre;

                Value value_cond_block=new Value(ValueType.BasicBlockTy,Type.LabelTyID);
                // 原基本块跳转至Cond对应基本块的指令
                BranchInst branchInst_cond=new BranchInst();
                // 跳转指令: br label <dest>, 对应循环体内的首个基本块
                Use use_dst=new Use(value_cond_block,branchInst_cond);
                llvmCtx.usesList.add(use_dst);branchInst_cond.usesList.add(use_dst);
                this.curBlock.instList.add(branchInst_cond);

                // 为Cond创建新基本块
                curFunction.valueList.add(value_cond_block);
                BasicBlock basicBlock_cond=new BasicBlock(this.blockId++,getSymTable(this.scope_no));
                // 跳转至Cond对应的基本块
                this.curBlock=basicBlock_cond;
                basicBlock_cond.value=value_cond_block;
                curFunction.basicBlockList.add(basicBlock_cond);

                curNode=curNode.next.get(1);    // Stmt -> Cond
                Value value_true_final=new Value(ValueType.BasicBlockTy,Type.LabelTyID);
                Value value_false_final=new Value(ValueType.BasicBlockTy,Type.LabelTyID);
                CondType condType=visit_Cond((Cond) curNode,value_true_final,value_false_final);
                curNode=curNode.pre;

                // 创建出口基本块
                Value value_out_loop;
                BasicBlock outLoop_block=new BasicBlock(this.blockId++,getSymTable(this.scope_no));
                this.outloopBlock_Stack.add(outLoop_block);

                // 创建第二个ForStmt对应的基本块
                Value value_self_change=new Value(ValueType.BasicBlockTy,Type.LabelTyID);
                BasicBlock basicBlock_forStmt=new BasicBlock(this.blockId++,getSymTable(this.scope_no));
                forStmtBlock_Stack.add(basicBlock_forStmt);
                basicBlock_forStmt.value=value_self_change;

                if(condType.equals(CondType.True) || condType.equals(CondType.False)){
                    Value br_cond;
                    if(condType.equals(CondType.True)){
                        // Cond条件为真:
                        br_cond=new Value(ValueType.TrueTy,Type.BooleanTyID);
                    }else{
                        br_cond=new Value(ValueType.FalseTy,Type.BooleanTyID);
                    }
                    BranchInst branchInst_true=new BranchInst();
                    // value_br_dst对应循环下属的Stmt1基本块；value_out_loop对应跳出循环外的首个基本块
                    Value value_br_dst=new Value(ValueType.BasicBlockTy,Type.LabelTyID);
                    value_out_loop=new Value(ValueType.BasicBlockTy,Type.BooleanTyID);
                    outLoop_block.value=value_out_loop;

                    Use use_cond=new Use(br_cond,branchInst_true);
                    Use use_true=new Use(value_br_dst,branchInst_true),use_false=new Use(value_out_loop,branchInst_true);

                    llvmCtx.usesList.add(use_cond);branchInst_true.usesList.add(use_cond);
                    llvmCtx.usesList.add(use_false);branchInst_true.usesList.add(use_false);
                    llvmCtx.usesList.add(use_true);branchInst_true.usesList.add(use_true);

                    this.curBlock.instList.add(branchInst_true);

                    // 进入Stmt对应的新基本块
                    curFunction.valueList.add(value_br_dst);
                    BasicBlock basicBlock=new BasicBlock(this.blockId++,getSymTable(this.scope_no));
                    this.curBlock=basicBlock;
                    basicBlock.value=value_br_dst;
                    curFunction.basicBlockList.add(basicBlock);
                }
                else{
                    // Cond条件待定: 此时已进入Stmt对应的基本块
                    value_out_loop=value_false_final;
                    outLoop_block.value=value_out_loop;
                }

                curNode=curNode.next.get(3);    // Stmt -> Stmt1
                visit_Stmt((Stmt)this.curNode);
                curNode=curNode.pre;

                Stmt stmt1=(Stmt)(curNode.next.get(3));
                if(!stmt1.isBreak && !stmt1.isContinue && !stmt1.isReturn){
                    // 循环体结束时，跳转至第二个ForStmt对应的基本块
                    BranchInst branchInst_forStmt=new BranchInst();
                    // 跳转指令: br label <dest>, 对应第二个ForStmt的基本块
                    Use use_forstmt_dst=new Use(value_self_change,branchInst_forStmt);
                    llvmCtx.usesList.add(use_forstmt_dst);branchInst_forStmt.usesList.add(use_forstmt_dst);
                    this.curBlock.instList.add(branchInst_forStmt);
                }

                // 进入第二个ForStmt的基本块
                curFunction.valueList.add(value_self_change);
                this.curBlock=basicBlock_forStmt;
                curFunction.basicBlockList.add(basicBlock_forStmt);

                // 执行第二个ForStmt中的语句
                curNode=curNode.next.get(2);
                visit_ForStmt((ForStmt) curNode);
                curNode=curNode.pre;

                // 循环体结束时，跳转回Cond基本块，再次判断
                curBlock.instList.add(branchInst_cond);

                // 跳出循环
                curFunction.valueList.add(value_out_loop);
                this.curBlock=outLoop_block;
                curFunction.basicBlockList.add(outLoop_block);

                // outloopBlock_Stack弹出当前for循环对应的出口基本块
                int size=outloopBlock_Stack.size();
                if(size>0 && outloopBlock_Stack.get(size-1).equals(outLoop_block)){
                    outloopBlock_Stack.pop();
                }
                // forStmtBlock_Stack弹出当前for循环的第二个ForStmt对应的基本块
                size=forStmtBlock_Stack.size();
                if(size>0 && forStmtBlock_Stack.get(size-1).equals(basicBlock_forStmt)){
                    forStmtBlock_Stack.pop();
                }
            }
        }
        else if(stmt.isReturn){
            // Stmt -> 'return' [Exp] ';'
            if(stmt.visited>=0){
                // 1. Stmt -> 'return' Exp ';'
                curNode=curNode.next.get(0);
                Instruction returnInst=new ReturnInst();        // return指令
                Function function=(Function)curValue;
                Value data=new Value(ValueType.VariableDataTy,function.type);   // 函数返回值
                //function.slotTracker.stack.add(data);
                // Use关系：return value
                Use use_dst=new Use(data,returnInst);
                llvmCtx.usesList.add(use_dst);returnInst.usesList.add(use_dst);

                visit_Exp((Exp) curNode,data);
                if(this.curBlock!=null)this.curBlock.instList.add(returnInst);

                curNode=curNode.pre;
            }else{
                // 2. Stmt -> 'return' ';'
                Instruction returnInst=new ReturnInst();        // return指令
                if(this.curBlock!=null)this.curBlock.instList.add(returnInst);
            }
        }
        else if(stmt.isPrintf){
            // 'printf''('StringConst {','Exp}')'';'
            _String _str=(_String)curNode.next.get(0);
            String str=_str.string;
            int argu_cnt=stmt.visited,str_cnt=_str.visited;
            ArrayList<Value> arguList=new ArrayList<Value>();

            // 2. 获取参数类型
            ArrayList <Type> arguTypeList=new ArrayList<Type>();
            for(int i=0;i<=str_cnt;i++){
                _String subStr=(_String)(_str.next.get(i));
                if(subStr.isPrintedInt) arguTypeList.add(Type.IntegerTyID);
                else if(subStr.isPrintedChar) arguTypeList.add(Type.CharTyID);
            }

            // 3. 加载待打印的参数
            int j=0;
            for(int i=1;i<=argu_cnt;i++){
                curNode=curNode.next.get(i);    // Stmt -> Exp
                Value value_src=new Value(ValueType.VariableDataTy,arguTypeList.get(j++));
                visit_Exp((Exp)curNode,value_src);
                arguList.add(value_src);
                curNode=curNode.pre;
            }
            argu_cnt=0;

            // 4. 调用call指令进行输出
            j=0;
            for(int i=0;i<=str_cnt;i++){
                _String subStr=(_String)(_str.next.get(i));
                CallInst callInst=new CallInst();

                String funcName="";
                if(subStr.isPrintedInt) funcName="putint";
                else if(subStr.isPrintedChar) funcName="putch";
                else funcName="putstr";

                // call指令：call void @putint(i32 ~)
                // call指令：call void @putch(i32 ~)
                // call指令：call void @putstr(i8* getelementptr inbounds ([2 x i8], [2 x i8]* @.str, i64 0, i64 0))
                Function function=new llvm.IR.Value.Function(ValueType.FunctionTy,Type.VoidTyID,funcName);
                if(subStr.isPrintedInt){
                    Value argu=arguList.get(argu_cnt++);
                    function.paramList.add(argu);
                }else if(subStr.isPrintedChar){
                    Value argu=arguList.get(argu_cnt++);
                    // 扩展指令：<result> = zext <ty> <value> to <ty2>, char转int
                    ZextInst zextInst=new ZextInst();
                    Value zext_dst=new Value(ValueType.VariableDataTy,Type.IntegerTyID);
                    Use use_zext_src=new Use(argu,zextInst),use_zext_dst=new Use(zext_dst,zextInst);
                    llvmCtx.usesList.add(use_zext_src);zextInst.usesList.add(use_zext_src);
                    llvmCtx.usesList.add(use_zext_dst);zextInst.usesList.add(use_zext_dst);

                    function.paramList.add(zext_dst);
                    this.curBlock.instList.add(zextInst);
                    // zext_dst加入当前function的valueList
                    curFunction.valueList.add(zext_dst);

                    //function.paramList.add(argu);
                } else{
                    add_Private_String(subStr,function);
                }
                Use use_call_func=new Use(function,callInst);   // 存储putint函数
                llvmCtx.usesList.add(use_call_func);callInst.usesList.add(use_call_func);

                if(this.curBlock!=null) this.curBlock.instList.add(callInst);   // call指令插入到当前基本块
            }
        }
        else if(stmt.isBreak){
            // Stmt -> 'break' ';'
            // 跳转至outloopBlock
            BranchInst branchInst=new BranchInst();
            BasicBlock outloopBlock=outloopBlock_Stack.get(outloopBlock_Stack.size()-1);   // 获取当前for循环的出口基本块
            //outloopBlock_Stack.pop();
            if(outloopBlock.value==null){
                Value outLoop=new Value(ValueType.BasicBlockTy,Type.LabelTyID);
                outloopBlock.value=outLoop;
            }
            Value br_dst=outloopBlock.value;
            Use use_dst=new Use(br_dst,branchInst);
            llvmCtx.usesList.add(use_dst);branchInst.usesList.add(use_dst);
            this.curBlock.instList.add(branchInst);
        }
        else if(stmt.isContinue){
            // Stmt -> 'continue' ';'
            // 跳转至forStmtBlock
            BranchInst branchInst=new BranchInst();
            BasicBlock forStmtBlock=forStmtBlock_Stack.get(forStmtBlock_Stack.size()-1);
            if(forStmtBlock.value==null){
                Value value_forStmt=new Value(ValueType.BasicBlockTy,Type.LabelTyID);
                forStmtBlock.value=value_forStmt;
            }
            Value br_dst=forStmtBlock.value;
            Use use_dst=new Use(br_dst,branchInst);
            llvmCtx.usesList.add(use_dst);branchInst.usesList.add(use_dst);
            this.curBlock.instList.add(branchInst);
        }
        else if(stmt.isGetint){
            // Stmt -> LVal '=' 'getint''('')'';'
            LVal lVal=(LVal)(stmt.next.get(0));
            Ident ident=(Ident)(lVal.next.get(0));

            // LVal部分
            Value value_init_dst=searchIdent(ident);
            if(value_init_dst==null) value_init_dst=new Value(ValueType.VariableTy,ident.getType(),ident.name);
            value_init_dst.ident=ident;

            Value value_dst;
            if(lVal.visited==0){
                // LVal -> Ident
                value_dst=value_init_dst;
            }else{
                curNode=curNode.next.get(0);    // Stmt -> LVal
                value_dst=new Value(ValueType.VariableTy,ident.getElementType());
                visit_LVal(lVal,value_init_dst,value_dst,false);
                curNode=curNode.pre;        // LVal <- Stmt
            }

            // getint部分
            llvm.IR.Value.Function function=new llvm.IR.Value.Function(ValueType.FunctionTy,Type.IntegerTyID,"getint");
            Value value_src=new Value(ValueType.VariableDataTy,Type.IntegerTyID);
            // call指令：<result> = call [ret attrs] <ty> <name>(<...args>)
            CallInst callInst=new CallInst();
            Use use_call_dst=new Use(value_src,callInst);   // 存储执行getint函数得到的值
            Use use_call_func=new Use(function,callInst);   // 存储function
            llvmCtx.usesList.add(use_call_dst);callInst.usesList.add(use_call_dst);
            llvmCtx.usesList.add(use_call_func);callInst.usesList.add(use_call_func);

            this.curBlock.instList.add(callInst); // call指令插入到当前基本块
            curFunction.valueList.add(value_src);

            Instruction storeInst=new StoreInst();        // store指令
            // Use关系：store data, var
            Use use_dst=new Use(value_dst,storeInst),use_src=new Use(value_src,storeInst);
            llvmCtx.usesList.add(use_src);storeInst.usesList.add(use_src);
            llvmCtx.usesList.add(use_dst);storeInst.usesList.add(use_dst);
            if(this.curBlock!=null) {
                this.curBlock.instList.add(storeInst); // store指令插入到当前基本块
            }
        }
        else if(stmt.isGetchar){
            // Stmt -> LVal '=' 'getchar''('')'';'
            LVal lVal=(LVal)(stmt.next.get(0));
            Ident ident = (Ident)(lVal.next.get(0));

            // LVal部分
            Value value_init_dst=searchIdent(ident);
            if(value_init_dst==null) value_init_dst=new Value(ValueType.VariableTy,ident.getType(),ident.name);
            value_init_dst.ident=ident;

            Value value_dst;
            if(lVal.visited==0){
                // LVal -> Ident
                value_dst=value_init_dst;
            }else{
                curNode=curNode.next.get(0);    // Stmt -> LVal
                value_dst=new Value(ValueType.VariableTy,ident.getElementType());
                visit_LVal(lVal,value_init_dst,value_dst,false);
                curNode=curNode.pre;        // LVal <- Stmt
            }

            // getchar部分
            llvm.IR.Value.Function function=new llvm.IR.Value.Function(ValueType.FunctionTy,Type.BooleanTyID,"getchar");
            Value value_src=new Value(ValueType.VariableDataTy,Type.IntegerTyID);
            // 1. call指令：<result> = call [ret attrs] <ty> <name>(<...args>)
            CallInst callInst=new CallInst();
            Use use_call_dst=new Use(value_src,callInst);   // 存储执行getchar函数得到的值
            Use use_call_func=new Use(function,callInst);   // 存储function
            llvmCtx.usesList.add(use_call_dst);callInst.usesList.add(use_call_dst);
            llvmCtx.usesList.add(use_call_func);callInst.usesList.add(use_call_func);
            this.curBlock.instList.add(callInst); // call指令插入到当前基本块
            curFunction.valueList.add(value_src);

            // 2. trunc..to指令: <result> = trunc <ty> <value> to <ty2>
            TruncInst truncInst=new TruncInst();
            Value trunc_dst=new Value(ValueType.VariableTy,Type.CharTyID);
            Use use_trunc_src=new Use(value_src,truncInst),use_trunc_dst=new Use(trunc_dst,truncInst);
            llvmCtx.usesList.add(use_trunc_src);truncInst.usesList.add(use_trunc_src);
            llvmCtx.usesList.add(use_trunc_dst);truncInst.usesList.add(use_trunc_dst);
            this.curBlock.instList.add(truncInst);
            // trunc_dst加入当前function的valueList
            curFunction.valueList.add(trunc_dst);

            Instruction storeInst=new StoreInst();        // store指令
            // Use关系：store data, var
            Use use_dst=new Use(value_dst,storeInst),use_src=new Use(trunc_dst,storeInst);
            llvmCtx.usesList.add(use_src);storeInst.usesList.add(use_src);
            llvmCtx.usesList.add(use_dst);storeInst.usesList.add(use_dst);
            if(this.curBlock!=null) {
                this.curBlock.instList.add(storeInst); // store指令插入到当前基本块
            }
        }
        else if(stmt.visited>=0 && stmt.next.get(0) instanceof Block block){
            // Stmt -> Block
            curNode=curNode.next.get(0);
            visit_Block((Block)curNode,false);
            if(block.hasBreakStmt) stmt.isBreak=true;
            else if(block.hasContinueStmt) stmt.isContinue=true;
            else if(block.hasReturnStmt) stmt.isReturn=true;
            curNode=curNode.pre;
        }
        else{
            // Stmt → LVal '=' Exp ';' | [Exp] ';'
            int len = stmt.visited;
            if(len<0){
                // 1. Stmt → ';'
                return;
            } else if (curNode.next.get(0) instanceof LVal) {
                // 1. Stmt → LVal '=' Exp ';'
                LVal lVal=(LVal)(stmt.next.get(0));
                Ident ident = (Ident)(lVal.next.get(0));

                // LVal部分
                Value value_init_dst;
                Value value_src=searchIdent(ident);

                if(ident.symTab.isFuncParam && lVal.visited>0){
                    /* 数组作为形参传入：define dso_local i32 @f3(i32* noundef %0) 则有：
                            %5 = alloca i32*
                            store i32* %0, i32** %5     */
                    Type arrType;
                    if(value_src.type.equals(Type.IntArrayTyID)||value_src.type.equals(Type.IntPointerTyID)) arrType=Type.IntPointerTyID;
                    else arrType=Type.CharPointerTyID;

                    value_init_dst=new Value(ValueType.VariableTy,arrType);
                    LoadInst loadInst=new LoadInst();
                    Use use_src=new Use(value_src,loadInst),use_dst=new Use(value_init_dst,loadInst);
                    llvmCtx.usesList.add(use_src);loadInst.usesList.add(use_src);
                    llvmCtx.usesList.add(use_dst);loadInst.usesList.add(use_dst);

                    this.curBlock.instList.add(loadInst);
                    // value_init_dst加入当前function的valueList
                    curFunction.valueList.add(value_init_dst);
                }else{
                    value_init_dst=value_src;
                }

                Value value_dst;
                if(lVal.visited==0){
                    // LVal -> Ident
                    value_dst=value_init_dst;
                }else{
                    // LVal -> Ident '[' Exp ']'
                    curNode=curNode.next.get(0);    // Stmt -> LVal
                    value_dst=new Value(ValueType.VariableTy,ident.getElementType());
                    visit_LVal(lVal,value_init_dst,value_dst,false);
                    curNode=curNode.pre;        // LVal <- Stmt
                }

                System.out.println("value_dst: "+value_dst+" "+ident.name+" "+ident.getElementType());
                if(value_dst==null) value_dst=new Value(ValueType.VariableTy,ident.getElementType(),ident.name);
                value_dst.ident=ident;
                Value data=new Value(ValueType.VariableDataTy,ident.getElementType());

                curNode=stmt.next.get(1);   // Stmt -> Exp
                visit_Exp((Exp) curNode,data);
                curNode=curNode.pre;        // Exp <- Stmt
                value_dst.num=data.num;

                Instruction storeInst=new StoreInst();        // store指令
                // Use关系：store data, var
                Use use_dst=new Use(value_dst,storeInst),use_src=new Use(data,storeInst);
                llvmCtx.usesList.add(use_src);storeInst.usesList.add(use_src);
                llvmCtx.usesList.add(use_dst);storeInst.usesList.add(use_dst);
                if(this.curBlock!=null) {
                    this.curBlock.instList.add(storeInst); // store指令插入到当前基本块
                }
            }else{
                // 2. Stmt → Exp ';'
                curNode=curNode.next.get(0);
                Value data=new Value(ValueType.VariableDataTy,Type.IntegerTyID);
                visit_Exp((Exp) curNode,data);
                curNode=curNode.pre;
            }
        }
    }
    void visit_ForStmt(ForStmt forStmt){
        // ForStmt → LVal '=' Exp
        LVal lVal=(LVal)(forStmt.next.get(0));
        Ident ident = (Ident)(lVal.next.get(0));

        // LVal部分
        Value value_init_dst;
        Value value_src=searchIdent(ident);

        if(ident.symTab.isFuncParam && lVal.visited>0){
                    /* 数组作为形参传入：define dso_local i32 @f3(i32* noundef %0) 则有：
                            %5 = alloca i32*
                            store i32* %0, i32** %5     */
            Type arrType;
            if(value_src.type.equals(Type.IntArrayTyID)||value_src.type.equals(Type.IntPointerTyID)) arrType=Type.IntPointerTyID;
            else arrType=Type.CharPointerTyID;

            value_init_dst=new Value(ValueType.VariableTy,arrType);
            LoadInst loadInst=new LoadInst();
            Use use_src=new Use(value_src,loadInst),use_dst=new Use(value_init_dst,loadInst);
            llvmCtx.usesList.add(use_src);loadInst.usesList.add(use_src);
            llvmCtx.usesList.add(use_dst);loadInst.usesList.add(use_dst);

            this.curBlock.instList.add(loadInst);
            // value_init_dst加入当前function的valueList
            curFunction.valueList.add(value_init_dst);
        }else{
            value_init_dst=value_src;
        }

        Value value_dst;
        if(lVal.visited==0){
            // LVal -> Ident
            value_dst=value_init_dst;
        }else{
            // LVal -> Ident '[' Exp ']'
            curNode=curNode.next.get(0);    // ForStmt -> LVal
            value_dst=new Value(ValueType.VariableTy,ident.getElementType());
            visit_LVal(lVal,value_init_dst,value_dst,false);
            curNode=curNode.pre;        // LVal <- ForStmt
        }

        System.out.println("value_dst: "+value_dst+" "+ident.name+" "+ident.getElementType());
        if(value_dst==null) value_dst=new Value(ValueType.VariableTy,ident.getElementType(),ident.name);
        value_dst.ident=ident;
        Value data=new Value(ValueType.VariableDataTy,ident.getElementType());

        curNode=forStmt.next.get(1);   // ForStmt -> Exp
        visit_Exp((Exp) curNode,data);
        curNode=curNode.pre;        // ForStmt <- Stmt
        value_dst.num=data.num;

        Instruction storeInst=new StoreInst();        // store指令
        // Use关系：store data, var
        Use use_dst=new Use(value_dst,storeInst),use_src=new Use(data,storeInst);
        llvmCtx.usesList.add(use_src);storeInst.usesList.add(use_src);
        llvmCtx.usesList.add(use_dst);storeInst.usesList.add(use_dst);
        if(this.curBlock!=null) {
            this.curBlock.instList.add(storeInst); // store指令插入到当前基本块
        }
    }
     void visit_LVal(LVal lval,Value array,Value element,boolean loaded) {
        this.scope_no=lval.scope_no;
        // 左值表达式 LVal → Ident ['[' Exp ']'] //1.普通变量、常量 2.一维数组
        int len=lval.visited;
        Ident ident=(Ident)(lval.next.get(0));
        if(len==0){
            // 1. LVal → Ident
        }else{
            curNode=curNode.next.get(1);    // LVal -> Exp
            Value pos=new Value(ValueType.VariableDataTy,Type.IntegerTyID);
            visit_Exp((Exp) curNode,pos);
            curNode=curNode.pre;        // Exp <- LVal

            Value value_dst;
            if(loaded){
                // 当前LVal为被加载对象，即用于给其他的赋值
                value_dst=new Value(ValueType.VariableTy,ident.getElementType());
            }else{
                // LVal = Exp ;
                value_dst=element;
            }
            // getelementptr指令：<result> = getelementptr <ty>, ptr <ptrval>{, <ty> <idx>}*
            GetelementInst getelementInst=new GetelementInst();
            Use use_dst=new Use(value_dst,getelementInst);
            Use use_array=new Use(array,getelementInst),use_pos=new Use(pos,getelementInst);
            llvmCtx.usesList.add(use_dst);getelementInst.usesList.add(use_dst);
            llvmCtx.usesList.add(use_array);getelementInst.usesList.add(use_array);
            llvmCtx.usesList.add(use_pos);getelementInst.usesList.add(use_pos);

            if(this.curBlock!=null) {
                this.curBlock.instList.add(getelementInst);
                if(!curFunction.valueList.contains(value_dst)){
                    curFunction.valueList.add(value_dst);
                }
            }

            // load指令
            if(loaded){
                LoadInst loadInst=new LoadInst();
                Use use_load_src=new Use(value_dst,loadInst),use_load_dst=new Use(element,loadInst);
                llvmCtx.usesList.add(use_load_src);loadInst.usesList.add(use_load_src);
                llvmCtx.usesList.add(use_load_dst);loadInst.usesList.add(use_load_dst);

                if(this.curBlock!=null) this.curBlock.instList.add(loadInst);
                // 在当前function的valueList中搜寻element: 若无, element加入当前function的valueList
                if(!curFunction.valueList.contains(element)){
                    curFunction.valueList.add(element);
                }
            }
        }
    }
    CondType visit_Cond(Cond cond,Value value_true,Value value_false){
        // 其下属的LOrExp一定为真时，返回true；否则，返回false
        this.scope_no=cond.scope_no;
        System.out.println("cond");
        // 条件表达式 Cond → LOrExp
        curNode=curNode.next.get(0);
        CondType condType=visit_LOrExp((LOrExp)curNode,value_true,value_false);

        curNode=curNode.pre;
        return condType;
    }
    CondType visit_LOrExp(LOrExp lorExp,Value value_true_final,Value value_false_final){
        // 当前if语句一定为真时，返回true；否则，返回false
        this.scope_no=lorExp.scope_no;
        System.out.println("lorExp");
        // 逻辑或表达式 LOrExp → LAndExp | LOrExp '||' LAndExp
        int len=lorExp.visited;

        boolean isFalse=true;
        Boolean result_isIntChar=true;int result_num=1;
        Value result_data=null;

        for(int i=0;i<=len;i++){
            curNode=curNode.next.get(i);
            result_data=new Value(ValueType.VariableDataTy,Type.IntegerTyID);

            if(i!=len){
                Value value_false=new Value(ValueType.BasicBlockTy,Type.LabelTyID);
                visit_LAndExp((LAndExp)curNode,result_data,value_true_final,value_false,false);
            }else{
                visit_LAndExp((LAndExp)curNode,result_data,value_true_final,value_false_final,true);
            }
            // 注：每个LAndExp，进入的是前一个LAndExp的false跳转的基本块
            // 从LAndExp返回时的状态：
            // 1. 当前LAndExp不是最后一个：
            //  1.1 若当前LAndExp一定为真，且是第一个LAndExp，此时已进入Cond为true时对应的基本块，返回CondType.True;
            //  1.2 若当前LAndExp一定为假，则跳过，继续判断下一个LAndExp；
            //  1.3 若当前LAndExp一定为真，且不是第一个LAndExp，跳转命令：br i1 true, label %5, label %10

            // 2. 当前LAndExp是最后一个：

            if(result_data.isIntChar && result_data.num!=0 && result_isIntChar){
                // 当前LAndExp为真，当前及之前所有的LAndExp均为数字/字符，无需继续判断
                System.out.println("check true");
                curNode=curNode.pre;return CondType.True;
            }else if(result_data.isIntChar && result_data.num==0){
                // 当前LAndExp为假
            }else{
                result_isIntChar = false;
                isFalse = false;

                if(result_data.isIntChar && result_data.num!=0){
                    // 当前LAndExp为真, 跳转命令形如：br i1 true, label %5, label %10
                    Value br_cond=new Value(ValueType.TrueTy,Type.BooleanTyID),false_to_next_landExp=new Value(ValueType.BasicBlockTy,Type.LabelTyID);
                    BranchInst branchInst=new BranchInst();

                    Use use_cond=new Use(br_cond,branchInst);
                    Use use_iffalse=new Use(false_to_next_landExp,branchInst),use_iftrue=new Use(value_true_final,branchInst);

                    llvmCtx.usesList.add(use_cond);branchInst.usesList.add(use_cond);
                    llvmCtx.usesList.add(use_iffalse);branchInst.usesList.add(use_iffalse);
                    llvmCtx.usesList.add(use_iftrue);branchInst.usesList.add(use_iftrue);

                    this.curBlock.instList.add(branchInst);

                    // 进入一个新的基本块
                    BasicBlock basicBlock=new BasicBlock(this.blockId++,getSymTable(this.scope_no));
                    this.curBlock=basicBlock;
                    curFunction.basicBlockList.add(basicBlock);
                    basicBlock.value=false_to_next_landExp;
                    // false_to_next_landExp加入当前function的valueList
                    curFunction.valueList.add(false_to_next_landExp);
                }
            }
            curNode=curNode.pre;
        }
        if(!result_isIntChar && result_data.isIntChar && result_data.num==0){
            // 最后一个LAndExp为0
            // 提前进入了上一个LAndExp的false跳转的基本块，现在退出
            curFunction.basicBlockList.remove(curFunction.basicBlockList.size()-1);
            curFunction.valueList.remove(curFunction.valueList.size()-1);
            // 修改上一个LAndExp的跳转命令中，false对应基本块的关联值
            BasicBlock last_block=curFunction.basicBlockList.get(curFunction.basicBlockList.size()-1);
            if(last_block.instList.get(last_block.instList.size()-1) instanceof BranchInst branchInst){
                branchInst.usesList.get(1).usee=value_false_final;
            }
        }
        if(isFalse) return CondType.False;

        // 3. 进入Cond为true对应stmt1基本块
        curFunction.valueList.add(value_true_final);
        BasicBlock basicBlock=new BasicBlock(this.blockId++,getSymTable(this.scope_no));
        this.curBlock=basicBlock;
        curFunction.basicBlockList.add(basicBlock);
        basicBlock.value=value_true_final;

        return CondType.UnKnown;
    }
    void visit_LAndExp(LAndExp lAndExp,Value result,Value value_true_final,Value value_false,boolean isLast){
        this.scope_no=lAndExp.scope_no;
        System.out.println("lAndExp");
        // 逻辑与表达式 LAndExp → EqExp | LAndExp '&&' EqExp
        int len=lAndExp.visited;

        result.isIntChar=true;
        result.num=1;

        Value result_dst=null,result_data=null;

        for(int i=0;i<=len;i++){
            curNode=curNode.next.get(i);
            result_dst=new Value(ValueType.VariableTy,Type.IntegerTyID);
            result_data=new Value(ValueType.VariableDataTy,Type.IntegerTyID);
            boolean hasComparedInst=visit_EqExp((EqExp)curNode,result_data);
            Value br_cond;

            // 注：每个EqExp，进入的是前一个EqExp的true跳转的基本块
            // 从EqExp返回时的状态：
            // 1. 当前EqExp不是最后一个：
            //  1.1 若当前EqExp一定为假，且是第一个EqExp，返回;
            //  1.2 若当前EqExp一定为真，则跳过，继续判断下一个EqExp；
            //  1.3 若当前EqExp一定为假，且不是第一个EqExp，跳转命令：br i1 false, label %5, label %10

            if(result_data.isIntChar && result_data.num==0 && result.isIntChar){
                // 1.1 当前EqExp为假，当前及之前所有的EqExp均为数字/字符，无需继续判断
                result.num=0;
                curNode=curNode.pre;
                return;
            }else if(result_data.isIntChar && result_data.num!=0){
                // 当前EqExp一定为真
                // 1.2 当前EqExp不是该LAndExp的最后一个，即i<len:继续判断
                // 2. 当前EqExp是该LAndExp的最后一个，即i=len:
            }
            else{
                result.isIntChar=false;
                if(result_data.isIntChar && result_data.num==0){
                    // 1.3 当前EqExp为假, 跳转命令形如：br i1 false, label %5, label %10
                    br_cond=new Value(ValueType.FalseTy,Type.BooleanTyID);
                }
                else if(!hasComparedInst){
                    br_cond=result_dst;
                    // 比较指令: <result> = icmp <cond> <ty> <op1>, <op2>
                    Value zero=new Value(ValueType.VariableTy,Type.IntegerTyID);zero.num=0;zero.isIntChar=true;
                    CompareInst compareInst=new CompareInst(BinaryOpType.ne);
                    Use use_result=new Use(result_dst,compareInst);
                    Use use_left=new Use(result_data,compareInst),use_right=new Use(zero,compareInst);

                    llvmCtx.usesList.add(use_result);compareInst.usesList.add(use_result);
                    llvmCtx.usesList.add(use_left);compareInst.usesList.add(use_left);
                    llvmCtx.usesList.add(use_right);compareInst.usesList.add(use_right);

                    this.curBlock.instList.add(compareInst);
                    // result_dst加入当前function的valueList
                    curFunction.valueList.add(result_dst);
                }else{
                    br_cond=result_data;
                }

                // 跳转指令: br i1 <cond>, label <iftrue>, label <iffalse>
                BranchInst branchInst=new BranchInst();
                Value value_true;
                if(i!=len){
                    value_true=new Value(ValueType.BasicBlockTy,Type.LabelTyID);
                }else{
                    value_true=value_true_final;
                }

                Use use_cond=new Use(br_cond,branchInst);
                Use use_iffalse=new Use(value_false,branchInst),use_iftrue=new Use(value_true,branchInst);

                llvmCtx.usesList.add(use_cond);branchInst.usesList.add(use_cond);
                llvmCtx.usesList.add(use_iffalse);branchInst.usesList.add(use_iffalse);
                llvmCtx.usesList.add(use_iftrue);branchInst.usesList.add(use_iftrue);

                this.curBlock.instList.add(branchInst);
                if(i!=len){     // 切换至新的基本块
                    BasicBlock basicBlock=new BasicBlock(this.blockId++,getSymTable(this.scope_no));
                    this.curBlock=basicBlock;
                    curFunction.basicBlockList.add(basicBlock);
                    basicBlock.value=value_true;
                    // value_true加入当前function的valueList
                    curFunction.valueList.add(value_true);
                }
            }
            curNode=curNode.pre;
        }
        if(result.isIntChar && result.num!=0){
            // 1. 当前LAndExp为真，其上级LOrExp一定为真，不存在false情况
            return;
        }else if(result.isIntChar){
            // 2. 当前LAndExp为假，无需创建新基本块
            return;
        }
        /*else if(result_data.isIntChar && result_data.num==0){
            // 最后一个EqExp为0
        }*/
        else if(result_data.isIntChar && result_data.num!=0){
            // 最后一个EqExp为1
            // 提前进入了上一个EqExp的true跳转的基本块，现在退出
            curFunction.basicBlockList.remove(curFunction.basicBlockList.size()-1);
            curFunction.valueList.remove(curFunction.valueList.size()-1);
            // 修改上一个EqExp的跳转命令中，true对应基本块的关联值
            BasicBlock last_block=curFunction.basicBlockList.get(curFunction.basicBlockList.size()-1);
            if(last_block.instList.get(last_block.instList.size()-1) instanceof BranchInst branchInst){
                if(branchInst.usesList.size()==1){
                    branchInst.usesList.get(0).usee=value_true_final;
                }
                else branchInst.usesList.get(2).usee=value_true_final;
            }
        }
        if(!isLast){
            // value_false加入当前function的valueList:
            // value_false绑定当前LAndExp,退到上一级LOrExp后, 继续检查下一个LAndExp
            curFunction.valueList.add(value_false);
            BasicBlock basicBlock=new BasicBlock(this.blockId++,getSymTable(this.scope_no));
            this.curBlock=basicBlock;
            curFunction.basicBlockList.add(basicBlock);
            basicBlock.value=value_false;
        }
    }
    boolean visit_EqExp(EqExp eqExp,Value result){
        // 已经有比较指令，返回true；否则，返回false
        this.scope_no=eqExp.scope_no;
        System.out.println("eqExp");
        // 相等性表达式 EqExp → RelExp | EqExp ('==' | '!=') RelExp
        int len=eqExp.visited;
        if(len==0){
            // EqExp → RelExp
            curNode=curNode.next.get(0);
            visit_RelExp((RelExp)curNode,result);
            curNode=curNode.pre;
        }else{
            curNode=curNode.next.get(0);    // EqExp -> RelExp
            Value value1=new Value(ValueType.VariableDataTy,result.type);
            visit_RelExp((RelExp)curNode,value1);
            curNode=curNode.pre;        // RelExp <- EqExp
            Value curValue1=value1;
            /*if(!curFunction.valueList.contains(value1)){
                curFunction.valueList.add(value1);
            }*/

            for(int i=1;i<=len;i++){
                curNode=curNode.next.get(i);    // EqExp -> RelExp
                Value value2=new Value(ValueType.VariableDataTy,result.type);
                visit_RelExp((RelExp)curNode,value2);
                /*if(!curFunction.valueList.contains(value2)){
                    curFunction.valueList.add(value2);
                }*/

                BinaryOpType opType=((RelExp)curNode).opType;

                Value subEqResult;
                if(i==len) subEqResult=result;
                else subEqResult=new Value(ValueType.VariableDataTy,Type.IntegerTyID);

                CompareInst compareInst=new CompareInst(opType);
                Use use_result=new Use(subEqResult,compareInst);
                Use use_left=new Use(curValue1,compareInst),use_right=new Use(value2,compareInst);
                llvmCtx.usesList.add(use_result);compareInst.usesList.add(use_result);
                llvmCtx.usesList.add(use_left);compareInst.usesList.add(use_left);
                llvmCtx.usesList.add(use_right);compareInst.usesList.add(use_right);

                this.curBlock.instList.add(compareInst);
                // subEqResult加入当前function的valueList
                curFunction.valueList.add(subEqResult);

                curValue1=subEqResult;

                curNode=curNode.pre;        // RelExp <- EqExp
            }
        }
        return len!=0;
    }
    void visit_RelExp(RelExp relExp,Value result){
        this.scope_no=relExp.scope_no;
        System.out.println("relExp");
        // 关系表达式 RelExp → AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp
        int len=relExp.visited;
        if(len==0){
            curNode=curNode.next.get(0);    // 1. RelExp → AddExp
            visit_AddExp((AddExp) curNode,result,true);
            curNode=curNode.pre;
        }else{
            curNode=curNode.next.get(0);    // RelExp -> AddExp
            Value value1=new Value(ValueType.VariableDataTy,result.type);
            visit_AddExp((AddExp)curNode,value1,true);
            curNode=curNode.pre;    // AddExp <- RelExp
            Value curValue1=value1;
            /*if(!curFunction.valueList.contains(value1)){
                curFunction.valueList.add(value1);
            }*/

            for(int i=1;i<=len;i++){
                // 2. RelExp -> RelExp ('<' | '>' | '<=' | '>=') AddExp
                curNode=curNode.next.get(i);    // RelExp -> AddExp
                Value value2=new Value(ValueType.VariableDataTy,result.type);

                Value comparedResult=new Value(ValueType.VariableDataTy,Type.BooleanTyID);
                visit_AddExp((AddExp)curNode,value2,true);

                BinaryOpType opType=((AddExp)curNode).opType;

                if(opType.equals(BinaryOpType.lss)){
                    if(curValue1.num<value2.num) comparedResult.num=1;
                    else comparedResult.num=0;
                }else if(opType.equals(BinaryOpType.gre)){
                    if(curValue1.num>value2.num) comparedResult.num=1;
                    else comparedResult.num=0;
                }else if(opType.equals(BinaryOpType.leq)){
                    if(curValue1.num<=value2.num) comparedResult.num=1;
                    else comparedResult.num=0;
                }else{
                    if(curValue1.num>=value2.num) comparedResult.num=1;
                    else comparedResult.num=0;
                }
                CompareInst compareInst=new CompareInst(opType);
                Use use_result=new Use(comparedResult,compareInst);
                Use use_left=new Use(curValue1,compareInst),use_right=new Use(value2,compareInst);
                llvmCtx.usesList.add(use_result);compareInst.usesList.add(use_result);
                llvmCtx.usesList.add(use_left);compareInst.usesList.add(use_left);
                llvmCtx.usesList.add(use_right);compareInst.usesList.add(use_right);

                this.curBlock.instList.add(compareInst);
                // subResult加入当前function的valueList
                curFunction.valueList.add(comparedResult);

                Value subResult;
                if(i==len) subResult=result;
                else subResult=new Value(ValueType.VariableDataTy,result.type);

                // 扩展指令：<result> = zext <ty> <value> to <ty2>
                ZextInst zextInst=new ZextInst();
                Use zext_src=new Use(comparedResult,zextInst),zext_dst=new Use(subResult,zextInst);
                llvmCtx.usesList.add(zext_src);zextInst.usesList.add(zext_src);
                llvmCtx.usesList.add(zext_dst);zextInst.usesList.add(zext_dst);

                this.curBlock.instList.add(zextInst);
                // subResult加入当前function的valueList
                curFunction.valueList.add(subResult);

                curValue1=subResult;

                curNode=curNode.pre;    // AddExp <- RelExp
            }
        }
    }

    void visit_ConstExp(ConstExp constExp,Value constData) {
        this.scope_no=constExp.scope_no;
        // 常量表达式 ConstExp → AddExp
        curNode=curNode.next.get(0);    // ConstExp -> AddExp
        visit_AddExp((AddExp) curNode,constData,true);

        curNode=curNode.pre;
    }
    void visit_Exp(Exp exp,Value data) {
        this.scope_no=exp.scope_no;
        // 表达式 Exp → AddExp
        curNode=curNode.next.get(0);    // Exp -> AddExp
        visit_AddExp((AddExp) curNode,data,true);
        curNode=curNode.pre;
    }
    void visit_AddExp(AddExp addExp,Value data,Boolean added) {
        // added：是否要将binaryInst加入基本块
        this.scope_no=addExp.scope_no;
        // 来源：Exp → AddExp; ConstExp → AddExp
        // 加减表达式 AddExp → MulExp | AddExp ('+' | '−') MulExp
        // Use关系：store sum, data

        // 原有的语法树中，AddExp的子节点含多个并列的MulExp（无AddExp），这里要两两提取
        // 例：a=1+2+3;拆做：a=(1+2)+3; 1+2再拆做1和2相加
        int len=addExp.visited;Value subSum;

        if(len==0){
            // 1. AddExp → MulExp
            curNode=curNode.next.get(0);
            visit_MulExp((MulExp)curNode,data,added);
            curNode=curNode.pre;
        }
        else{
            curNode=curNode.next.get(0);    // AddExp -> MulExp

            Value value1=new Value(ValueType.VariableDataTy,data.type);
            visit_MulExp((MulExp)curNode,value1,added);
            BinaryOpType opType=((MulExp)curNode).opType;
            if(opType.equals(BinaryOpType.sub)) value1.num=-value1.num;
            data.num=value1.num;
            System.out.println(opType+" "+data.num);

            curNode=curNode.pre;    // MulExp <- AddExp
            Value curValue1=value1;

            for(int i=1;i<=len;i++)
            {
                // 2. AddExp → AddExp ('+' | '−') MulExp
                curNode=curNode.next.get(i);    // AddExp -> MulExp
                Value value2=new Value(ValueType.VariableDataTy,data.type);
                visit_MulExp((MulExp)curNode,value2,added);

                opType=((MulExp)curNode).opType;
                if(opType.equals(BinaryOpType.sub)) data.num-=value2.num;
                else data.num+=value2.num;

                // Use关系：add curValue1, value2 -> curSum -> curValue1
                if(i==len) subSum=data;
                else subSum=new Value(ValueType.BinaryOperatorTy,data.type);
                Instruction binaryInst=new BinaryInst(((MulExp)curNode).opType);binaryInst.valueType=ValueType.StoreInstTy;

                // binaryInst 这里为求和语句，usesList中第一个、第二个、第三个元素分别对应和、第一个加数、第二个加数
                Use binaryOp_sum=new Use(subSum,binaryInst);
                Use binaryOp_1=new Use(curValue1,binaryInst),binaryOp_2=new Use(value2,binaryInst);
                llvmCtx.usesList.add(binaryOp_sum);binaryInst.usesList.add(binaryOp_sum);
                llvmCtx.usesList.add(binaryOp_1);binaryInst.usesList.add(binaryOp_1);
                llvmCtx.usesList.add(binaryOp_2);binaryInst.usesList.add(binaryOp_2);

                curValue1=subSum;

                if(this.curValue instanceof GlobalVariable globalVariable){
                    globalVariable.instList.add(0,binaryInst);
                }else if(added){
                    this.curBlock.instList.add(binaryInst);
                    //subSum加入当前function的valueList
                    curFunction.valueList.add(subSum);
                }
                curNode=curNode.pre;    // MulExp <- AddExp
            }
        }
    }
    void visit_MulExp(MulExp mulExp,Value data,boolean added) {
        this.scope_no=mulExp.scope_no;
        // 乘除模表达式 MulExp → UnaryExp | MulExp ('*' | '/' | '%') UnaryExp
        int len=mulExp.visited;Value subProduct;

        if(len==0){
            // 1. MulExp → UnaryExp
            curNode=curNode.next.get(0);
            visit_UnaryExp((UnaryExp)curNode,data);
            curNode=curNode.pre;
        }else{
            curNode=curNode.next.get(0);    // MulExp -> UnaryExp
            Value value1=new Value(ValueType.VariableDataTy,data.type);
            visit_UnaryExp((UnaryExp) curNode,value1);
            curNode=curNode.pre;    // UnaryExp <- MulExp
            Value curValue1=value1;

            data.num=value1.num;
            for(int i=1;i<=len;i++){
                curNode=curNode.next.get(i);    // MulExp -> UnaryExp
                Value value2=new Value(ValueType.BinaryOperatorTy,data.type);
                visit_UnaryExp((UnaryExp)curNode,value2);

                BinaryOpType opType=((UnaryExp)curNode).opType;
                if(opType.equals(BinaryOpType.mul)) data.num*=value2.num;
                else if(opType.equals(BinaryOpType.div) && value2.num!=0) data.num/=value2.num;
                else if(value2.num!=0)data.num%=value2.num;

                // Use关系：mul subProduct, value2 -> curProduct
                if(i==len) subProduct=data;
                else subProduct=new Value(ValueType.BinaryOperatorTy,data.type);
                Instruction binaryInst=new BinaryInst(((UnaryExp)curNode).opType);binaryInst.valueType=ValueType.BinaryOperatorTy;

                // binaryInst 这里为求积语句，usesList中第一个、第二个、第三个元素分别对应积、第一个乘数、第二个乘数
                Use binaryOp_product=new Use(subProduct,binaryInst);
                Use binaryOp_1=new Use(curValue1,binaryInst);Use binaryOp_2=new Use(value2,binaryInst);
                llvmCtx.usesList.add(binaryOp_product);binaryInst.usesList.add(binaryOp_product);
                llvmCtx.usesList.add(binaryOp_1);binaryInst.usesList.add(binaryOp_1);
                llvmCtx.usesList.add(binaryOp_2);binaryInst.usesList.add(binaryOp_2);

                curValue1=subProduct;

                if(this.curValue instanceof GlobalVariable globalVariable){
                    globalVariable.instList.add(0,binaryInst);
                }else if(added){
                    this.curBlock.instList.add(binaryInst);
                    // subProduct加入当前function的valueList
                    if(!curFunction.valueList.contains(subProduct)){
                        curFunction.valueList.add(subProduct);
                    }
                }
                curNode=curNode.pre;    // 回到MulExp
            }
        }
    }
    void type_Convert(Value data_init,Value data){
        // 进行类型转换
        if(data.type.equals(Type.IntegerTyID)){
            // 扩展指令：<result> = zext <ty> <value> to <ty2>, char转int
            ZextInst zextInst=new ZextInst();
            Use use_zext_src=new Use(data_init,zextInst),use_zext_dst=new Use(data,zextInst);
            llvmCtx.usesList.add(use_zext_src);zextInst.usesList.add(use_zext_src);
            llvmCtx.usesList.add(use_zext_dst);zextInst.usesList.add(use_zext_dst);

            if(this.curBlock!=null) {
                this.curBlock.instList.add(zextInst);
                if(!curFunction.valueList.contains(data)){
                    curFunction.valueList.add(data);
                }
            }else{
                data.num=data_init.num;
            }
        }
        else{
            // 截断指令：<result> = trunc <ty> <value> to <ty2>, int转char
            TruncInst truncInst=new TruncInst();
            Use use_trunc_src=new Use(data_init,truncInst),use_trunc_dst=new Use(data,truncInst);
            llvmCtx.usesList.add(use_trunc_src);truncInst.usesList.add(use_trunc_src);
            llvmCtx.usesList.add(use_trunc_dst);truncInst.usesList.add(use_trunc_dst);

            if(this.curBlock!=null) {
                this.curBlock.instList.add(truncInst);
                // data加入当前function的valueList
                if(!curFunction.valueList.contains(data)){
                    curFunction.valueList.add(data);
                }
            }
            else{
                data.num=data_init.num;
            }
        }
    }
    void visit_UnaryExp(UnaryExp unaryExp,Value data) {
        this.scope_no=unaryExp.scope_no;
        // 一元表达式 UnaryExp → PrimaryExp | Ident '(' [FuncRParams] ')' | UnaryOp UnaryExp
        curNode=curNode.next.get(0);
        if(curNode instanceof PrimaryExp primaryExp){
            // 1. UnaryExp → PrimaryExp
            //预判是否出现类型不一致的情况
            Type target_type=data.type;
            if(primaryExp.next.get(0) instanceof LVal lval){
                int len=lval.visited;
                Ident ident=(Ident)(lval.next.get(0));
                // UnaryExp -> PrimaryExp -> LVal -> Ident ['[' Exp ']']
                if(!ident.checkType(data.type)){
                    System.out.println("mismatch ident:"+ident.name+" "+data.type+" "+ident.symTab.type);
                    Value data_init=new Value(ValueType.VariableDataTy,target_type);
                    visit_PrimaryExp((PrimaryExp)curNode,data_init);
                    type_Convert(data_init,data);
                }else{
                    visit_PrimaryExp((PrimaryExp)curNode,data);
                }
            }else if(primaryExp.next.get(0) instanceof _Number && target_type.equals(Type.CharTyID)){
                Value data_init=new Value(ValueType.VariableDataTy,target_type);
                visit_PrimaryExp((PrimaryExp)curNode,data_init);
                type_Convert(data_init,data);
            }
            else if(primaryExp.next.get(0) instanceof _Character && target_type.equals(Type.IntegerTyID)){
                Value data_init=new Value(ValueType.VariableDataTy,target_type);
                visit_PrimaryExp((PrimaryExp)curNode,data_init);
                type_Convert(data_init,data);
            }
            else {
                visit_PrimaryExp((PrimaryExp)curNode,data);
            }

        }else if(curNode instanceof Ident ident){
            // 2. UnaryExp → Ident '(' [FuncRParams] ')'
            data.isCalledFunc=true;
            Type funcType=Type.VoidTyID;
            int len=llvmCtx.functionsList.size();

            // 被调用的函数
            Function called_function=new Function(ValueType.FunctionTy,funcType,ident.name),prototype=null;
            for(int i=0;i<len;i++){     // 检索被调用的函数的类型
                if(llvmCtx.functionsList.get(i).name.equals(ident.name)){
                    funcType=llvmCtx.functionsList.get(i).type;
                    prototype=llvmCtx.functionsList.get(i);
                    called_function.prototype_function=prototype;   // 定义的函数原型
                    break;
                }
            }
            Type target_type=data.type;
            CallInst callInst=new CallInst();
            Value data_init=data;
            if(!funcType.equals(Type.VoidTyID)){    // 有返回值的函数
                if(!ident.checkType(target_type)){
                    data_init=new Value(ValueType.VariableDataTy,funcType); // 后续需要类型转换
                }
                Use use_call_dst=new Use(data_init,callInst);
                llvmCtx.usesList.add(use_call_dst);callInst.usesList.add(use_call_dst);
            }
            Use use_call_func=new Use(called_function,callInst);
            llvmCtx.usesList.add(use_call_func);callInst.usesList.add(use_call_func);

            if(unaryExp.visited>0){
                // UnaryExp → Ident '(' FuncRParams ')' , 实参列表存在
                curNode=curNode.pre;    // Ident <- UnaryExp
                curNode=curNode.next.get(1);    // UnaryExp -> FuncRParams

                visit_FuncRParams((FuncRParams)curNode,called_function,prototype);
            }else{
                // UnaryExp → Ident , 实参列表不存在
            }

            if(this.curBlock!=null) this.curBlock.instList.add(callInst);
            if(!funcType.equals(Type.VoidTyID)){
                // data_init加入当前function的valueList
                curFunction.valueList.add(data_init);
            }

            if(!funcType.equals(Type.VoidTyID) && !ident.checkType(target_type)){
                type_Convert(data_init,data);
            }
        }else{
            // 3. UnaryExp → UnaryOp UnaryExp
            BinaryOpType opType=((UnaryExp)curNode).opType;
            Value data_init;
            if(opType.equals(BinaryOpType.sub)){    // 例：a=-3*5;
                data_init=new Value(ValueType.VariableDataTy,data.type);
                visit_UnaryExp((UnaryExp)curNode,data_init);

                // binaryInst 这里为求差语句
                BinaryInst binaryInst=new BinaryInst(BinaryOpType.sub);
                Value zero=new Value(ValueType.ConstantDataTy,Type.IntegerTyID);zero.num=0;zero.isIntChar=true;
                Use binaryOp_sum=new Use(data,binaryInst);
                Use binaryOp_1=new Use(zero,binaryInst),binaryOp_2=new Use(data_init,binaryInst);
                llvmCtx.usesList.add(binaryOp_sum);binaryInst.usesList.add(binaryOp_sum);
                llvmCtx.usesList.add(binaryOp_1);binaryInst.usesList.add(binaryOp_1);
                llvmCtx.usesList.add(binaryOp_2);binaryInst.usesList.add(binaryOp_2);

                if(this.curValue instanceof GlobalVariable globalVariable){
                    globalVariable.instList.add(0,binaryInst);
                }else{
                    this.curBlock.instList.add(binaryInst);
                    // data加入当前function的valueList
                    curFunction.valueList.add(data);
                }
                data.num=-data_init.num;
            }else if(opType.equals(BinaryOpType.not)){
                // UnaryExp -> ! UnaryExp, 仅出现在条件表达式中
                data_init=new Value(ValueType.VariableDataTy,data.type);
                Value data_or_src=new Value(ValueType.VariableDataTy,Type.BooleanTyID);
                visit_UnaryExp((UnaryExp)curNode,data_init);

                // 1. 比较指令: <result> = icmp <cond> <ty> <op1>, <op2>
                Value zero=new Value(ValueType.VariableTy,Type.IntegerTyID);zero.num=0;zero.isIntChar=true;
                CompareInst compareInst=new CompareInst(opType);
                Use use_result=new Use(data_or_src,compareInst);
                Use use_left=new Use(data_init,compareInst),use_right=new Use(zero,compareInst);

                llvmCtx.usesList.add(use_result);compareInst.usesList.add(use_result);
                llvmCtx.usesList.add(use_left);compareInst.usesList.add(use_left);
                llvmCtx.usesList.add(use_right);compareInst.usesList.add(use_right);

                this.curBlock.instList.add(compareInst);
                // data_or_srcss加入当前function的valueList
                curFunction.valueList.add(data_or_src);

                // 2. 异或指令：<result> = xor <ty> <op1>, <op2>
                XOrInst xOrInst=new XOrInst();
                Value zext_src=new Value(ValueType.VariableDataTy,Type.BooleanTyID);
                Value xor_true=new Value(ValueType.TrueTy,Type.BooleanTyID);
                Use use_xor_dst=new Use(zext_src,xOrInst);
                Use use_xor_left=new Use(data_or_src,xOrInst),use_xor_right=new Use(xor_true,xOrInst);

                llvmCtx.usesList.add(use_xor_dst);xOrInst.usesList.add(use_xor_dst);
                llvmCtx.usesList.add(use_xor_left);xOrInst.usesList.add(use_xor_left);
                llvmCtx.usesList.add(use_xor_right);xOrInst.usesList.add(use_xor_right);

                this.curBlock.instList.add(xOrInst);
                // zext_src加入当前function的valueList
                curFunction.valueList.add(zext_src);

                // 3. 扩展指令: <result> = zext <ty> <value> to <ty2>
                ZextInst zextInst=new ZextInst();
                Use use_zext_src=new Use(zext_src,zextInst),use_zext_dst=new Use(data,zextInst);
                llvmCtx.usesList.add(use_zext_src);zextInst.usesList.add(use_zext_src);
                llvmCtx.usesList.add(use_zext_dst);zextInst.usesList.add(use_zext_dst);

                this.curBlock.instList.add(zextInst);
                // data加入当前function的valueList
                curFunction.valueList.add(data);
            }
            else{
                visit_UnaryExp((UnaryExp)curNode,data);
            }
        }
        curNode=curNode.pre;    // 回到UnaryExp
    }
    void visit_FuncRParams(FuncRParams funcRParams,Function called_function, Function prototype_function) {
        this.scope_no=funcRParams.scope_no;
        // 函数实参表 FuncRParams → Exp { ',' Exp }
        int len=curNode.visited;
        for(int i=0;i<=len;i++){
            curNode=curNode.next.get(i);    // FuncRParams -> Exp
            Value argu_src=new Value(ValueType.VariableDataTy,prototype_function.paramList.get(i).type);
            System.out.println("param type:"+prototype_function.paramList.get(i).type+", name:"+prototype_function.paramList.get(i).name);
            Use use_argu=new Use(argu_src,called_function); // 建立function对argy的调用关系
            llvmCtx.usesList.add(use_argu);
            called_function.paramList.add(argu_src);

            visit_Exp((Exp)curNode,argu_src);

            curNode=curNode.pre;
        }
    }
    void visit_PrimaryExp(PrimaryExp primaryExp,Value data) {
        this.scope_no=primaryExp.scope_no;
        // 基本表达式 PrimaryExp → '(' Exp ')' | LVal | Number | Character
        curNode=curNode.next.get(0);
        if(curNode instanceof Exp){
            visit_Exp((Exp)curNode,data);
        }
        else if(curNode instanceof LVal lVal){
            // Use关系：load dst, src  将src中的值，加载到dst
            Ident ident=(Ident)(lVal.next.get(0));
            Value value_src=searchIdent(ident);
            //System.out.println("value_src type:"+value_src.type+", name:"+ident.name);
            if(value_src==null){
                int len=llvmCtx.globalVariablesList.size();
                for(int i=0;i<len;i++){
                    GlobalVariable globalVariable=llvmCtx.globalVariablesList.get(i);
                    if(globalVariable.name.equals(ident.name)) value_src=globalVariable;
                }
            }
            int len=lVal.visited;

            if(len==0){
                // 1. LVal -> Ident    普通变量、常量
                Type dst_type=Type.IntPointerTyID;
                if(identIsArray(ident,dst_type)){       // dst_type设置为int/char型指针类型
                    // 传入整个指向数组的指针，例如：arr
                    // 1. value_src对应形式参数
                    if(value_src.correspondsFuncFParam){
                        LoadInst loadInst=new LoadInst();
                        Use use_load_src=new Use(value_src,loadInst),use_load_dst=new Use(data,loadInst);
                        llvmCtx.usesList.add(use_load_src);loadInst.usesList.add(use_load_src);
                        llvmCtx.usesList.add(use_load_dst);loadInst.usesList.add(use_load_dst);

                        if(this.curBlock!=null) {
                            this.curBlock.instList.add(loadInst);
                            if(!curFunction.valueList.contains(data)){
                                curFunction.valueList.add(data);
                            }
                        }
                    }
                    else{
                        GetelementInst getelementInst=new GetelementInst();
                        Value pos=new Value(ValueType.VariableDataTy,Type.IntegerTyID);pos.num=0;pos.isIntChar=true;

                        Use use_getelement_dst=new Use(data,getelementInst);
                        Use use_array=new Use(value_src,getelementInst),use_pos=new Use(pos,getelementInst);
                        llvmCtx.usesList.add(use_getelement_dst);getelementInst.usesList.add(use_getelement_dst);
                        llvmCtx.usesList.add(use_array);getelementInst.usesList.add(use_array);
                        llvmCtx.usesList.add(use_pos);getelementInst.usesList.add(use_pos);

                        if(this.curBlock!=null) {
                            this.curBlock.instList.add(getelementInst);
                            if(!curFunction.valueList.contains(data)){
                                curFunction.valueList.add(data);
                            }
                        }
                    }
                }
                else{
                    LoadInst loadInst=new LoadInst();
                    Use use_src=new Use(value_src,loadInst),use_dst=new Use(data,loadInst);
                    llvmCtx.usesList.add(use_src);loadInst.usesList.add(use_src);
                    llvmCtx.usesList.add(use_dst);loadInst.usesList.add(use_dst);

                    if(this.curBlock!=null) {
                        this.curBlock.instList.add(loadInst);
                        // 在当前function的valueList中搜寻data: 若无, data加入当前function的valueList
                        if(!curFunction.valueList.contains(data)){
                            curFunction.valueList.add(data);
                        }
                    }
                }
                data.type=ident.getType();   // 传回数据类型
                data.num=value_src.num;
            }else{
                // 2. LVal -> Ident '[' Exp ']'   一维数组
                // value_src是Ident对应的数组
                Type arrType;
                if(value_src.type.equals(Type.IntArrayTyID)||value_src.type.equals(Type.IntPointerTyID)) arrType=Type.IntPointerTyID;
                else arrType=Type.CharPointerTyID;

                Value value_init_dst=value_src;
                if(ident.symTab.isFuncParam){
                    /* 数组作为形参传入：define dso_local i32 @f3(i32* noundef %0) 则有：
                            %5 = alloca i32*
                            store i32* %0, i32** %5     */
                    value_init_dst=new Value(data.valueType,arrType);
                    LoadInst loadInst=new LoadInst();
                    Use use_src=new Use(value_src,loadInst),use_dst=new Use(value_init_dst,loadInst);
                    llvmCtx.usesList.add(use_src);loadInst.usesList.add(use_src);
                    llvmCtx.usesList.add(use_dst);loadInst.usesList.add(use_dst);

                    if(this.curBlock!=null) {
                        this.curBlock.instList.add(loadInst);
                        curFunction.valueList.add(value_init_dst);
                    }
                }

                //visit_LVal(lVal,value_init_dst,data,true);
                visit_LVal(lVal,value_init_dst,data,true);

                data.type=ident.getElementType();       // 传回数据类型
            }

        }
        else if(curNode instanceof _Number number){
            data.num=number.num;data.isIntChar=true;
            data.type=Type.IntegerTyID;     // 传回当前数据类型
        }
        else if(curNode instanceof _Character character){
            data.num=character.ch;data.isIntChar=true;
            data.type=Type.CharTyID;
        }
        curNode=curNode.pre;
    }
    public Value searchIdent(Ident ident){
        // 寻找当前ident对应的符号
        SymTableStack.Element element;  // element是栈式符号表中，当前层对应元素
        int cur=this.symTableStack.symTable_Stack.size()-1;
        for(element=this.symTableStack.curElement;cur>=0 && element.scope_no>=0;){
            ArrayList<SymTableStack.SymTabEntry> currentSymTabList=element.symTabEntryList;
            boolean exists=false;
            int symTable_len=currentSymTabList.size();
            for(int i=0;i<symTable_len;i++) {
                SymTableStack.SymTabEntry entry=currentSymTabList.get(i);
                if (entry.symTab.name.equals(ident.name)) {
                    exists=true;return entry.value;
                }
            }
            if(cur==0) break;
            element=this.symTableStack.symTable_Stack.get(--cur);
        }
        return null;
    }

    public Type getType(LexType lexType) {
        if(lexType.equals(LexType.INT_CONST_IDENFR)||lexType.equals(LexType.INT_VAR_IDENFR)){
            return Type.IntegerTyID;
        }else if(lexType.equals(LexType.CHAR_CONST_IDENFR)||lexType.equals(LexType.CHAR_VAR_IDENFR)){
            return Type.CharTyID;
        }else return Type.CharTyID;
    }
    public SymTable getSymTable(int no){     //获取编号为no的符号表
        IndexTable.IndexTab newTab=this.indexTable.indexTabList.get(no);
        return newTab.pointer;
    }
    public void add_putint_putch_putstr_to_Library(){
        // 声明库函数
        // getint函数
        llvm.IR.Value.Function func_getint=new llvm.IR.Value.Function(ValueType.FunctionTy,Type.IntegerTyID,"getint");
        llvmCtx.libraryFuncList.add(func_getint);

        // getchar函数
        llvm.IR.Value.Function func_getchar=new llvm.IR.Value.Function(ValueType.FunctionTy,Type.IntegerTyID,"getchar");
        llvmCtx.libraryFuncList.add(func_getchar);

        // putint函数
        llvm.IR.Value.Function func_putint=new llvm.IR.Value.Function(ValueType.FunctionTy,Type.VoidTyID,"putint");
        Value param1=new Value(ValueType.VariableDataTy,Type.IntegerTyID);
        Use use_param_1=new Use(param1,func_putint);    // 建立function对param的使用关系
        func_putint.paramList.add(param1);      // param插入当前函数的参数表
        llvmCtx.usesList.add(use_param_1);llvmCtx.libraryFuncList.add(func_putint);

        // putchar函数
        llvm.IR.Value.Function func_putch=new llvm.IR.Value.Function(ValueType.FunctionTy,Type.VoidTyID,"putch");
        Value param2=new Value(ValueType.VariableDataTy,Type.IntegerTyID);
        Use use_param_2=new Use(param2,func_putch);    // 建立function对param的使用关系
        func_putch.paramList.add(param2);      // param插入当前函数的参数表
        llvmCtx.usesList.add(use_param_2);llvmCtx.libraryFuncList.add(func_putch);

        // putstr函数
        llvm.IR.Value.Function func_putstr=new llvm.IR.Value.Function(ValueType.FunctionTy,Type.VoidTyID,"putstr");
        Value param3=new Value(ValueType.VariableDataTy,Type.StringTyID);
        Use use_param_3=new Use(param3,func_putch);    // 建立function对param的使用关系
        func_putstr.paramList.add(param3);      // param插入当前函数的参数表
        llvmCtx.usesList.add(use_param_3);llvmCtx.libraryFuncList.add(func_putstr);
    }
    public boolean identIsArray(Ident ident,Type elePointerType){
        LexType lexType=ident.symTab.type;
        if(lexType.equals(LexType.INT_VAR_ARRAY_IDENFR)||lexType.equals(LexType.CHAR_VAR_ARRAY_IDENFR)
        ||lexType.equals(LexType.INT_CONST_ARRAY_IDENFR)||lexType.equals(LexType.CHAR_CONST_ARRAY_IDENFR)){
            elePointerType=Type.IntPointerTyID;
            return true;
        }else{
            elePointerType=Type.CharPointerTyID;
            return false;
        }
    }
    public void add_Private_String(_String subStr,Function function){
        Value value_string=new Value(ValueType.VariableTy,Type.StringTyID);
        String llvm_str="";
        int strlen=subStr.string.length(),curPos=0;
        while(curPos<strlen){
            Character ch=subStr.string.charAt(curPos);
            switch(ch){
                case '\b':llvm_str+="\\08";break;
                case '\f':llvm_str+="\\0C";break;
                case '\n':llvm_str+="\\0A";break;
                case '\r':llvm_str+="\\0D";break;
                case '\t':llvm_str+="\\09";break;
                case '\\':llvm_str+="\\5C";break;
                case '\'':llvm_str+="\\27";break;
                case '\"':llvm_str+="\\22";break;
                case '\0':llvm_str+="\\00";break;
                default:llvm_str+=ch;break;
            }
            curPos+=1;
            value_string.stringLength+=1;
            /*if(curPos<strlen-1 && subStr.string.charAt(curPos)=='\\'){
                llvm_str+='\\';
                char ch=subStr.string.charAt(curPos+1);
                switch(ch){
                    case 'a':llvm_str+="07";curPos+=2;break;
                    case 'b':llvm_str+="08";curPos+=2;break;
                    case 'f':llvm_str+="0C";curPos+=2;break;
                    case 'n':llvm_str+="0A";curPos+=2;break;
                    case 'r':llvm_str+="0D";curPos+=2;break;
                    case 't':llvm_str+="09";curPos+=2;break;
                    case 'v':llvm_str+="0B";curPos+=2;break;
                    case '\\':llvm_str+="5C";curPos+=2;break;
                    case '\'':llvm_str+="27";curPos+=2;break;
                    case '\"':llvm_str+="22";curPos+=2;break;
                    case '\0':llvm_str+="00";curPos+=2;break;
                    default:llvm_str+=ch;curPos+=1;break;
                }
            }else{
                llvm_str+=subStr.string.charAt(curPos);
                curPos++;
            }
            value_string.stringLength+=1;*/
        }
        llvm_str+="\\00";value_string.stringLength+=1;
        value_string.string=llvm_str;
        value_string.stringID=llvmCtx.stringList.size();
        function.paramList.add(value_string);
        llvmCtx.stringList.add(value_string);
    }
    public void add_local_string(_String str,Value dst,int arrLength){
        // 将局部常量/变量的char数组，初始化为字符串时，存储
        // 获取指向字符串的指针
        GetelementInst getelementInst_init=new GetelementInst();
        Type pointerType=Type.CharTyID;
        Value value_dst_init=new Value(ValueType.VariableTy,pointerType);
        Value pos_init=new Value(ValueType.VariableDataTy,Type.IntegerTyID);pos_init.num=0;pos_init.isIntChar=true;

        Use use_getelement_dst_init=new Use(value_dst_init,getelementInst_init);
        Use use_array_init=new Use(dst,getelementInst_init),use_pos_init=new Use(pos_init,getelementInst_init);
        llvmCtx.usesList.add(use_getelement_dst_init);getelementInst_init.usesList.add(use_getelement_dst_init);
        llvmCtx.usesList.add(use_array_init);getelementInst_init.usesList.add(use_array_init);
        llvmCtx.usesList.add(use_pos_init);getelementInst_init.usesList.add(use_pos_init);

        if(this.curBlock!=null) {
            this.curBlock.instList.add(getelementInst_init);
            // value_dst_init加入当前function的valueList
            curFunction.valueList.add(value_dst_init);
        }

        // 存储字符串
                /*String funcName="llvm.memcpy.p0i8.p0i8.i64";
                Function function=new llvm.IR.Value.Function(ValueType.FunctionTy,Type.VoidTyID,funcName);
                function.paramList.add(value_dst_init);

                add_Private_String(str,function);       // 增加private constant型字符串
                */
        Value pre_array=value_dst_init;
        int strlen=str.string.length();
        for(int i=0;i<arrLength;i++){    // 遍历initVal的所有初始值
            Type eleType=Type.CharTyID;
            Value eleNum=new Value(ValueType.VariableTy,eleType);eleNum.isIntChar=true;
            if(i<strlen){
                eleNum.num=Integer.valueOf(str.string.charAt(i));
            }else{
                eleNum.num=0;
            }

            // 1. getelementptr指令
            if(i>0){
                GetelementInst getelementInst=new GetelementInst();
                Value value_dst=new Value(ValueType.VariableTy,pre_array.type);
                Value pos=new Value(ValueType.VariableDataTy,Type.IntegerTyID);pos.num=1;pos.isIntChar=true;

                Use use_getelement_dst=new Use(value_dst,getelementInst);
                Use use_array=new Use(pre_array,getelementInst),use_pos=new Use(pos,getelementInst);
                llvmCtx.usesList.add(use_getelement_dst);getelementInst.usesList.add(use_getelement_dst);
                llvmCtx.usesList.add(use_array);getelementInst.usesList.add(use_array);
                llvmCtx.usesList.add(use_pos);getelementInst.usesList.add(use_pos);

                if(this.curBlock!=null) {
                    this.curBlock.instList.add(getelementInst);
                    // value_dst加入当前function的valueList
                    curFunction.valueList.add(value_dst);
                }
                pre_array=value_dst;
            }

            // store指令
            StoreInst storeInst=new StoreInst();
            Use use_dst=new Use(pre_array,storeInst),use_src=new Use(eleNum,storeInst);
            llvmCtx.usesList.add(use_src);storeInst.usesList.add(use_src);
            llvmCtx.usesList.add(use_dst);storeInst.usesList.add(use_dst);

            if(this.curBlock!=null) {
                this.curBlock.instList.add(storeInst);
            }
        }
    }

    public void printVisit(Node node){
        System.out.println(node.toString()+" "+node.visited);
        if(node.visited<0){
            System.out.println("return to "+node.pre.toString());
            return;
        }
        for(int i=0;i<=node.visited;i++) {
            printVisit(node.next.get(i));
        }
        System.out.println("return to "+node.pre.toString());
    }
}

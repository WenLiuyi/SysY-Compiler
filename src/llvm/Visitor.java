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
import frontend.Tree.Stmt.Stmt;
import frontend.Tree.Var.InitVal;
import frontend.Tree.Var.VarDecl;
import frontend.Tree.Var.VarDef;
import frontend.Tree.Exp.*;

import llvm.IR.Value.*;
import llvm.IR.*;
import llvm.IR.Value.Inst.*;

import java.util.ArrayList;

public class Visitor {      // 遍历语法树
    public Node treeHead;
    public Node curNode;
    public int scope_no;
    public ArrayList<String> info;

    public llvm.IR.Module llvmHead;  // llvm结构的头节点
    public LlvmContext llvmCtx;
    public IndexTable indexTable;

    public Value curValue;
    public BasicBlock curBlock;     // 当前所在的基本块
    public int blockId;     // 基本块编号

    public SymTableStack symTableStack;

    public Visitor(){}
    public Visitor(Node treeHead){
        this.treeHead = treeHead;
        this.curNode = treeHead;
        this.scope_no = 0;      // 初始化为全局作用域编号：0
        this.blockId=1;
        info=new ArrayList<String>();
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
            this.curBlock.instList.add(allocaInst); // alloca指令插入到当前基本块

            // constant加入当前function的valueList
            Function curFunction=(Function)curValue;
            curFunction.valueList.add(constant);
        }

        if(len==1){
            // 1. ConstDef → Ident '=' ConstInitVal
            if(this.scope_no==0){   // 全局常量
                GlobalVariable globalVariable=new GlobalVariable(ident.getType(),ident.name,ident);
                llvmCtx.globalVariablesList.add(globalVariable);
                this.curValue=globalVariable;
                globalVariable.isConst=true;
            }else{          // 局部变量
                // 需要通过 alloca 指令分配一块内存，才能对其进行 load/store 操作
                AllocaInst allocaInst=new AllocaInst();
            }
        }else if(len==2){
            // 2. ConstDef → Ident  '[' ConstExp ']'  '=' ConstInitVal
            Value curConstant=constant;
            if(this.scope_no==0) {       // 全局常量
                GlobalVariable globalVariable = new GlobalVariable(ident.getType(), ident.name, ident);
                llvmCtx.globalVariablesList.add(globalVariable);
                this.curValue = globalVariable;
                globalVariable.isArray = true;globalVariable.isConst = true;
                curConstant=globalVariable;
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
        }

        if(this.scope_no==0){
            GlobalVariable globalVariable=(GlobalVariable)this.curValue;
            globalVariable.instList.add(storeInst);
            globalVariable.num=storeInst.usesList.get(0).usee.num;
        }

        curNode=curNode.next.get(curNode.visited);
        if(curNode.visited<0) constData.zeroinitialized=true;   // ConstInitVal -> '{' '}'
        else visit_ConstInitVal((ConstInitVal)curNode,constData);

        // 栈式符号表插入新定义的常量:
        // 全局常量：无需分配寄存器；局部常量：已通过alloca指令分配寄存器
        constant.num=storeInst.usesList.get(0).usee.num;
        symTableStack.addSymTab(ident.symTab,constant);

        curNode=curNode.pre;
    }
    void visit_ConstInitVal(ConstInitVal constInitVal,Value constData){
        this.scope_no=constInitVal.scope_no;
        // 常量初值 ConstInitVal → ConstExp | '{' [ ConstExp { ',' ConstExp } ] '}' | StringConst
        // 1.常表达式初值 2.一维数组初值
        int len=constInitVal.visited;
        if(constInitVal.multipleInitVal){
            // 1. ConstInitVal → '{' ConstExp { ',' ConstExp } '}'
            for(int i=0;i<=len;i++){    // 遍历initVal的所有初始值
                curNode=constInitVal.next.get(i);
                visit_ConstExp((ConstExp)curNode,constData);   // 数值存储于data.num中
                constData.array.add(constData.num);   // 将值存入数组
                System.out.println("array add:"+constData.num);
            }
        }
        else if(constInitVal.next.get(0) instanceof ConstExp){
            // 2. ConstInitVal → ConstExp
            curNode=constInitVal.next.get(0);
            visit_ConstExp((ConstExp)curNode,constData);
        }else{
            // 3. ConstInitVal -> StringConst
            _String str=(_String)constInitVal.next.get(0);
            constData.string=str.string;
        }
        curNode=curNode.pre;
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
            this.curBlock.instList.add(allocaInst); // alloca指令插入到当前基本块
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
        }

        // 栈式符号表插入新定义的变量:
        // 全局变量：无需分配寄存器；局部变量：已通过alloca指令分配寄存器
        var.num=storeInst.usesList.get(0).usee.num;
        symTableStack.addSymTab(ident.symTab,var);
        System.out.println("add:"+ident.symTab.name+" "+symTableStack.curElement.scope_no);

        curNode=curNode.pre;
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
            }else{      // 局部变量
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
                else pointerType=Type.CharPointerTyID;
                Value value_dst_init=new Value(ValueType.VariableTy,pointerType);
                Value pos_init=new Value(ValueType.VariableDataTy,Type.IntegerTyID);pos_init.num=0;pos_init.isIntChar=true;

                Use use_getelement_dst_init=new Use(value_dst_init,getelementInst_init);
                Use use_array_init=new Use(var,getelementInst_init),use_pos_init=new Use(pos_init,getelementInst_init);
                llvmCtx.usesList.add(use_getelement_dst_init);getelementInst_init.usesList.add(use_getelement_dst_init);
                llvmCtx.usesList.add(use_array_init);getelementInst_init.usesList.add(use_array_init);
                llvmCtx.usesList.add(use_pos_init);getelementInst_init.usesList.add(use_pos_init);

                if(this.curBlock!=null) this.curBlock.instList.add(getelementInst_init);

                Value pre_array=value_dst_init;
                for(int i=0;i<=len;i++){    // 遍历initVal的所有初始值
                    curNode=initVal.next.get(i);
                    Type eleType;
                    if(var.type.equals(Type.IntArrayTyID)) eleType=Type.IntegerTyID;
                    else eleType=Type.CharTyID;
                    Value eleNum=new Value(ValueType.VariableTy,eleType);
                    visit_Exp((Exp)curNode,eleNum);   // 数值存储于data.num中
                    data.array.add(eleNum.num);   // 将值存入数组

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

                        if(this.curBlock!=null) this.curBlock.instList.add(getelementInst);
                        pre_array=value_dst;
                    }

                    // store指令
                    StoreInst storeInst=new StoreInst();
                    Use use_dst=new Use(pre_array,storeInst),use_src=new Use(eleNum,storeInst);
                    llvmCtx.usesList.add(use_src);storeInst.usesList.add(use_src);
                    llvmCtx.usesList.add(use_dst);storeInst.usesList.add(use_dst);

                    if(this.curBlock!=null) this.curBlock.instList.add(storeInst);
                    curNode=curNode.pre;
                }
            }
        }else if(initVal.next.get(0) instanceof Exp){
            // 2. InitVal → Exp
            curNode=initVal.next.get(0);
            visit_Exp((Exp)curNode,data);
        }else{
            // 3. InitVal -> StringConst
            _String str=(_String)initVal.next.get(0);
            data.string=str.string;
        }
        curNode=curNode.pre;
    }
    void visit_FuncDef(FuncDef funcDef) {
        this.scope_no=curNode.next.get(curNode.visited).scope_no;   // Block的scope_no
        // 函数定义 FuncDef → FuncType Ident '(' [FuncFParams] ')' Block
        Ident ident=(Ident)(curNode.next.get(0));Type type;
        if(ident.symTab.type.equals(LexType.INT_FUN_IDENFR)) type=Type.IntegerTyID;
        else if(ident.symTab.type.equals(LexType.CHAR_FUN_IDENFR)) type=Type.CharTyID;
        else type=Type.VoidTyID;

        llvm.IR.Value.Function function=new llvm.IR.Value.Function(ValueType.FunctionTy,type, ident.name);
        llvmCtx.functionsList.add(function);
        this.curValue=function;

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
        // 主函数定义 MainFuncDef → 'int' 'main' '(' ')' Block
        llvm.IR.Value.Function function=new llvm.IR.Value.Function(ValueType.FunctionTy,Type.IntegerTyID,"main");
        llvmCtx.functionsList.add(function);
        this.curValue=function;     // 每个function由多个基本块组成

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

        /*if(identIsArray(ident)){

        }*/

        // 1. alloca指令：%3 = alloca i32
        AllocaInst allocaInst=new AllocaInst();
        // Use关系：alloca <reg-type> for funcFParam
        Value new_param=new Value(ValueType.VariableDataTy,ident.getType(),ident.name);new_param.ident=ident;
        Use use_dst=new Use(new_param,allocaInst);
        llvmCtx.usesList.add(use_dst);allocaInst.usesList.add(use_dst);
        this.curBlock.instList.add(cnt,allocaInst); // alloca指令插入到当前基本块

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

        curNode=curNode.pre;    // FuncFParam <- FuncFParams
    }
    void visit_Block(Block block,Boolean created) {
        this.scope_no=block.scope_no;
        //参数created：如果block属于FuncDef，则其作用域已创建
        if(!created) symTableStack.enterScope(this.scope_no);    // 栈式符号表，创建新的作用域
        else symTableStack.curElement.scope_no=block.scope_no;
        // 语句块 Block → '{' { BlockItem } '}'
        // 语句块项 BlockItem → Decl | Stmt
        /*
        if(curNode.pre instanceof MainFuncDef){
            // %1 = alloca i32, align 4
            // store i32 0, i32* %1, align 4
            AllocaInst allocaInst=new AllocaInst();
            Value value_dst=new Value(ValueType.VariableDataTy,Type.IntegerTyID);
            Use use_alloca=new Use(value_dst,allocaInst);
            llvmCtx.usesList.add(use_alloca);allocaInst.usesList.add(use_alloca);

            StoreInst storeInst=new StoreInst();
            Value zero=new Value(ValueType.VariableDataTy,Type.IntegerTyID);
            zero.num=0;zero.isIntChar=true;
            Use use_src=new Use(zero,storeInst),use_dst=new Use(value_dst,storeInst);
            llvmCtx.usesList.add(use_src);storeInst.usesList.add(use_src);
            llvmCtx.usesList.add(use_dst);storeInst.usesList.add(use_dst);

            this.curBlock.instList.add(allocaInst);
            this.curBlock.instList.add(storeInst);
        }*/

        int len=block.visited;
        for(int i=0;i<=len;i++){
            curNode=block.next.get(i);
            if(curNode instanceof Decl) visit_Decl((Decl)curNode);
            else if(curNode instanceof Stmt) visit_Stmt((Stmt)curNode);
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
        if(stmt.isIf){/*
            llvm.IR.Value.Function curFunction=(llvm.IR.Value.Function) this.curValue;
            // 1. 条件语句：Stmt → 'if' '(' Cond ')' Stmt1 [ 'else' Stmt2 ] BasicBlock3
            // Cond
            //BasicBlock cond_Block=new BasicBlock(this.blockId++,getSymTable(this.scope_no));
            curNode=curNode.next.get(0);    // Stmt -> Cond

            //curFunction.basicBlockList.add(cond_Block);     // 函数的基本块列表中，插入Cond基本块
            //this.curBlock=cond_Block;
            visit_Cond((Cond)this.curNode);
            curNode=curNode.pre;

            // Stmt 1
            BasicBlock stmt1_Block=new BasicBlock(this.blockId++,getSymTable(this.scope_no));
            curNode=curNode.next.get(1);    // Stmt -> Stmt1

            curFunction.basicBlockList.add(stmt1_Block);     // 函数的基本块列表中，插入Stmt1基本块
            this.curBlock=stmt1_Block;
            visit_Stmt((Stmt)this.curNode);
            curNode=curNode.pre;

            // Stmt 2
            if(stmt.hasElse){
                BasicBlock stmt2_Block=new BasicBlock(this.blockId++,getSymTable(this.scope_no));
                curNode=curNode.next.get(2);    // Stmt -> Stmt2

                curFunction.basicBlockList.add(stmt2_Block);     // 函数的基本块列表中，插入Stmt2基图片本块
                this.curBlock=stmt2_Block;
                visit_Stmt((Stmt)this.curNode);
                curNode=curNode.pre;
            }*/
        }else if(stmt.isFor){
            // Stmt -> 'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt
        }else if(stmt.isReturn){
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
        }else if(stmt.isPrintf){
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
                    if(this.curBlock!=null)this.curBlock.instList.add(zextInst);

                    //function.paramList.add(argu);
                } else{
                    Value value_string=new Value(ValueType.VariableTy,Type.StringTyID);
                    String llvm_str="";
                    int strlen=subStr.string.length(),curPos=0;
                    while(curPos<strlen){
                        if(curPos<strlen-1 && subStr.string.charAt(curPos)=='\\'){
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
                        value_string.stringLength+=1;
                    }
                    llvm_str+="\\00";value_string.stringLength+=1;
                    value_string.string=llvm_str;
                    value_string.stringID=llvmCtx.stringList.size();
                    function.paramList.add(value_string);
                    llvmCtx.stringList.add(value_string);
                }
                Use use_call_func=new Use(function,callInst);   // 存储putint函数
                llvmCtx.usesList.add(use_call_func);callInst.usesList.add(use_call_func);

                if(this.curBlock!=null) this.curBlock.instList.add(callInst);   // call指令插入到当前基本块
            }
        }else if(stmt.isBreak){
            // Stmt -> 'break' ';'
        }else if(stmt.isContinue){
            // Stmt -> 'continue' ';'
        }else if(stmt.isGetint){
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
            if(this.curBlock!=null) this.curBlock.instList.add(callInst); // call指令插入到当前基本块

            Instruction storeInst=new StoreInst();        // store指令
            // Use关系：store data, var
            Use use_dst=new Use(value_dst,storeInst),use_src=new Use(value_src,storeInst);
            llvmCtx.usesList.add(use_src);storeInst.usesList.add(use_src);
            llvmCtx.usesList.add(use_dst);storeInst.usesList.add(use_dst);
            if(this.curBlock!=null) this.curBlock.instList.add(storeInst); // store指令插入到当前基本块
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
            if(this.curBlock!=null) this.curBlock.instList.add(callInst); // call指令插入到当前基本块

            // 2. trunc..to指令: <result> = trunc <ty> <value> to <ty2>
            TruncInst truncInst=new TruncInst();
            Value trunc_dst=new Value(ValueType.VariableTy,Type.CharTyID);
            Use use_trunc_src=new Use(value_src,truncInst),use_trunc_dst=new Use(trunc_dst,truncInst);
            llvmCtx.usesList.add(use_trunc_src);truncInst.usesList.add(use_trunc_src);
            llvmCtx.usesList.add(use_trunc_dst);truncInst.usesList.add(use_trunc_dst);
            if(this.curBlock!=null) this.curBlock.instList.add(truncInst);

            Instruction storeInst=new StoreInst();        // store指令
            // Use关系：store data, var
            Use use_dst=new Use(value_dst,storeInst),use_src=new Use(trunc_dst,storeInst);
            llvmCtx.usesList.add(use_src);storeInst.usesList.add(use_src);
            llvmCtx.usesList.add(use_dst);storeInst.usesList.add(use_dst);
            if(this.curBlock!=null) this.curBlock.instList.add(storeInst); // store指令插入到当前基本块
        }
        else if(stmt.visited>=0 && stmt.next.get(0) instanceof Block block){
            // Stmt -> Block
            curNode=curNode.next.get(0);
            visit_Block((Block)curNode,false);
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

                    if(this.curBlock!=null) this.curBlock.instList.add(loadInst);
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
                if(this.curBlock!=null) this.curBlock.instList.add(storeInst); // store指令插入到当前基本块
            }else{
                // 2. Stmt → Exp ';'
                /*curNode=curNode.next.get(0);
                Value data=new Value(ValueType.VariableDataTy,Type.IntegerTyID);
                visit_Exp((Exp) curNode,data);
                curNode=curNode.pre;*/
            }
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

            if(this.curBlock!=null) this.curBlock.instList.add(getelementInst);

            // load指令
            if(loaded){
                LoadInst loadInst=new LoadInst();
                Use use_load_src=new Use(value_dst,loadInst),use_load_dst=new Use(element,loadInst);
                llvmCtx.usesList.add(use_load_src);loadInst.usesList.add(use_load_src);
                llvmCtx.usesList.add(use_load_dst);loadInst.usesList.add(use_load_dst);

                if(this.curBlock!=null) this.curBlock.instList.add(loadInst);
            }
        }
    }
    void visit_Cond(Cond cond){
        this.scope_no=cond.scope_no;
        System.out.println("cond");
        // 条件表达式 Cond → LOrExp
        curNode=curNode.next.get(0);
        visit_LOrExp((LOrExp)curNode);

        curNode=curNode.pre;
    }
    void visit_LOrExp(LOrExp lorExp){
        this.scope_no=lorExp.scope_no;
        System.out.println("lorExp");
        // 逻辑或表达式 LOrExp → LAndExp | LOrExp '||' LAndExp
        int len=lorExp.visited;
        if(len==0){
            // LOrExp -> LAndExp
            curNode=curNode.next.get(0);
            Value result=new Value(ValueType.VariableDataTy,Type.IntegerTyID);
            visit_LAndExp((LAndExp)curNode,result);
            curNode=curNode.pre;return;
        }

        curNode=curNode.next.get(0);
        Value result_1=new Value(ValueType.VariableDataTy,Type.IntegerTyID);
        visit_LAndExp((LAndExp)curNode,result_1);
        curNode=curNode.pre;

        if(result_1.isIntChar && result_1.num==0){ // 当前LAndExp为真，无需继续判断

        }
        Value value_true=new Value(ValueType.BasicBlockTy,Type.LabelTyID);
        for(int i=1;i<=len;i++){
            curNode=curNode.next.get(i);
            Value result_dst=new Value(ValueType.VariableTy,Type.IntegerTyID);
            Value result_data=new Value(ValueType.VariableDataTy,Type.IntegerTyID);
            visit_LAndExp((LAndExp)curNode,result_data);

            if(result_data.isIntChar && result_data.num==0){ // 当前LAndExp为真，无需继续判断

            }else{
                // 比较指令: <result> = icmp <cond> <ty> <op1>, <op2>
                Value zero=new Value(ValueType.VariableTy,Type.IntegerTyID);zero.num=0;zero.isIntChar=true;
                CompareInst compareInst=new CompareInst(BinaryOpType.ne);
                Use use_result=new Use(result_dst,compareInst);
                Use use_left=new Use(result_data,compareInst),use_right=new Use(zero,compareInst);

                llvmCtx.usesList.add(use_result);compareInst.usesList.add(use_result);
                llvmCtx.usesList.add(use_left);compareInst.usesList.add(use_left);
                llvmCtx.usesList.add(use_right);compareInst.usesList.add(use_right);

                if(this.curBlock!=null) this.curBlock.instList.add(compareInst);

                // 跳转指令: br i1 <cond>, label <iftrue>, label <iffalse>
                BranchInst branchInst=new BranchInst();
                Value value_false=new Value(ValueType.BasicBlockTy,Type.LabelTyID);

                Use use_cond=new Use(result_dst,branchInst);
                Use use_iffalse=new Use(value_false,branchInst),use_iftrue=new Use(value_true,branchInst);

                llvmCtx.usesList.add(use_cond);branchInst.usesList.add(use_cond);
                llvmCtx.usesList.add(use_iffalse);branchInst.usesList.add(use_iffalse);
                llvmCtx.usesList.add(use_iftrue);branchInst.usesList.add(use_iftrue);

                if(this.curBlock!=null) this.curBlock.instList.add(branchInst);
            }
            curNode=curNode.pre;
        }
    }
    void visit_LAndExp(LAndExp lAndExp,Value result){
        this.scope_no=lAndExp.scope_no;
        System.out.println("lAndExp");
        // 逻辑与表达式 LAndExp → EqExp | LAndExp '&&' EqExp
        int len=lAndExp.visited;
        if(len==0){
            // LAndExp → EqExp
            curNode=curNode.next.get(0);
            visit_EqExp((EqExp)curNode,result);
            curNode=curNode.pre;
        }
    }
    void visit_EqExp(EqExp eqExp,Value result){
        this.scope_no=eqExp.scope_no;
        System.out.println("eqExp");
        // 相等性表达式 EqExp → RelExp | EqExp ('==' | '!=') RelExp
        int len=eqExp.visited;
        if(len==0){
            // EqExp → RelExp
            curNode=curNode.next.get(0);
            visit_RelExp((RelExp)curNode,result);
            curNode=curNode.pre;
        }
    }
    void visit_RelExp(RelExp relExp,Value result){
        this.scope_no=relExp.scope_no;
        System.out.println("relExp");
        // 关系表达式 RelExp → AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp
        int len=relExp.visited;
        if(len==0){
            curNode=curNode.next.get(0);    // 1. RelExp → AddExp
            visit_AddExp((AddExp) curNode,result,false);
            curNode=curNode.pre;
        }else{
            curNode=curNode.next.get(0);    // RelExp -> AddExp
            Value value1=new Value(ValueType.VariableDataTy,result.type);
            visit_AddExp((AddExp)curNode,value1,false);
            curNode=curNode.pre;    // AddExp <- RelExp
            Value curValue1=value1;

            for(int i=1;i<=len;i++){
                // 2. RelExp -> RelExp ('<' | '>' | '<=' | '>=') AddExp
                curNode=curNode.next.get(i);    // RelExp -> AddExp
                Value value2=new Value(ValueType.VariableDataTy,result.type);
                Value subResult=new Value(ValueType.VariableDataTy,Type.BooleanTyID);
                visit_AddExp((AddExp)curNode,value2,false);

                BinaryOpType opType=((AddExp)curNode).opType;

                if(opType.equals(BinaryOpType.lss)){
                    if(curValue1.num<value2.num) subResult.num=1;
                    else subResult.num=0;
                }else if(opType.equals(BinaryOpType.gre)){
                    if(curValue1.num>value2.num) subResult.num=1;
                    else subResult.num=0;
                }else if(opType.equals(BinaryOpType.leq)){
                    if(curValue1.num<=value2.num) subResult.num=1;
                    else subResult.num=0;
                }else{
                    if(curValue1.num>=value2.num) subResult.num=1;
                    else subResult.num=0;
                }
                CompareInst compareInst=new CompareInst(opType);
                Use use_result=new Use(subResult,compareInst);
                Use use_left=new Use(curValue1,compareInst),use_right=new Use(value2,compareInst);
                llvmCtx.usesList.add(use_result);compareInst.usesList.add(use_result);
                llvmCtx.usesList.add(use_left);compareInst.usesList.add(use_left);
                llvmCtx.usesList.add(use_right);compareInst.usesList.add(use_right);

                curValue1=subResult;
                if(this.curBlock!=null) this.curBlock.instList.add(compareInst);

                if(i<len){
                    // 扩展指令：<result> = zext <ty> <value> to <ty2>
                    ZextInst zextInst=new ZextInst();
                    Value value3=new Value(ValueType.VariableDataTy,result.type);
                    Use zext_src=new Use(subResult,zextInst),zext_dst=new Use(value3,zextInst);
                    llvmCtx.usesList.add(zext_src);zextInst.usesList.add(zext_src);
                    llvmCtx.usesList.add(zext_dst);zextInst.usesList.add(zext_dst);

                    if(this.curBlock!=null) this.curBlock.instList.add(zextInst);
                    curValue1=value3;
                }

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

            BinaryOpType opType=((MulExp)curNode).opType;

            if(opType.equals(BinaryOpType.sub)){
                BinaryInst binaryInst=new BinaryInst(opType);
                Value zero=new Value(ValueType.VariableDataTy,data.type);
                zero.num=0;zero.isIntChar=true;
                Value data_final=new Value(ValueType.VariableDataTy,data.type);

                visit_MulExp((MulExp)curNode,data_final,added);

                // binaryInst 这里为求差语句
                Use binaryOp_sum=new Use(data,binaryInst);
                Use binaryOp_1=new Use(zero,binaryInst),binaryOp_2=new Use(data_final,binaryInst);
                llvmCtx.usesList.add(binaryOp_sum);binaryInst.usesList.add(binaryOp_sum);
                llvmCtx.usesList.add(binaryOp_1);binaryInst.usesList.add(binaryOp_1);
                llvmCtx.usesList.add(binaryOp_2);binaryInst.usesList.add(binaryOp_2);

                if(this.curValue instanceof GlobalVariable globalVariable){
                    globalVariable.instList.add(0,binaryInst);
                }else{
                    this.curBlock.instList.add(binaryInst);
                }
                data.num=-data_final.num;
            }else{
                visit_MulExp((MulExp)curNode,data,added);
            }
            curNode=curNode.pre;
        }
        else{
            curNode=curNode.next.get(0);    // AddExp -> MulExp

            Value value1=new Value(ValueType.VariableDataTy,data.type);
            BinaryOpType opType=((MulExp)curNode).opType;
            if(opType.equals(BinaryOpType.sub)) value1.num=-value1.num;
            data.num=value1.num;
            System.out.println(opType+" "+data.num);

            if(opType.equals(BinaryOpType.sub)){
                BinaryInst binaryInst=new BinaryInst(opType);
                Value zero=new Value(ValueType.VariableDataTy,data.type);
                zero.num=0;zero.isIntChar=true;

                Value value1_init=new Value(ValueType.VariableDataTy,data.type);

                visit_MulExp((MulExp)curNode,value1_init,added);

                // binaryInst 这里为求差语句
                Use binaryOp_sum=new Use(value1,binaryInst);
                Use binaryOp_1=new Use(zero,binaryInst),binaryOp_2=new Use(value1_init,binaryInst);
                llvmCtx.usesList.add(binaryOp_sum);binaryInst.usesList.add(binaryOp_sum);
                llvmCtx.usesList.add(binaryOp_1);binaryInst.usesList.add(binaryOp_1);
                llvmCtx.usesList.add(binaryOp_2);binaryInst.usesList.add(binaryOp_2);

                if(this.curValue instanceof GlobalVariable globalVariable){
                    globalVariable.instList.add(0,binaryInst);
                }else{
                    this.curBlock.instList.add(binaryInst);
                }
                value1.num=-value1_init.num;
            }else{
                visit_MulExp((MulExp)curNode,value1,added);
            }

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
                    if(this.curBlock!=null) this.curBlock.instList.add(binaryInst);
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
                    if(this.curBlock!=null) this.curBlock.instList.add(binaryInst);
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

            if(this.curBlock!=null) this.curBlock.instList.add(zextInst);
        }
        else{
            // 截断指令：<result> = trunc <ty> <value> to <ty2>, int转char
            TruncInst truncInst=new TruncInst();
            Use use_trunc_src=new Use(data_init,truncInst),use_trunc_dst=new Use(data,truncInst);
            llvmCtx.usesList.add(use_trunc_src);truncInst.usesList.add(use_trunc_src);
            llvmCtx.usesList.add(use_trunc_dst);truncInst.usesList.add(use_trunc_dst);

            if(this.curBlock!=null) this.curBlock.instList.add(truncInst);
            else data.num=data_init.num;
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

            if(!funcType.equals(Type.VoidTyID) && !ident.checkType(target_type)){
                type_Convert(data_init,data);
            }
        }else{
            // 3. UnaryExp → UnaryOp UnaryExp
            System.out.println("visit unaryExp");
            visit_UnaryExp((UnaryExp)curNode,data);
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
                    GetelementInst getelementInst=new GetelementInst();
                    Value pos=new Value(ValueType.VariableDataTy,Type.IntegerTyID);pos.num=0;pos.isIntChar=true;

                    Use use_getelement_dst=new Use(data,getelementInst);
                    Use use_array=new Use(value_src,getelementInst),use_pos=new Use(pos,getelementInst);
                    llvmCtx.usesList.add(use_getelement_dst);getelementInst.usesList.add(use_getelement_dst);
                    llvmCtx.usesList.add(use_array);getelementInst.usesList.add(use_array);
                    llvmCtx.usesList.add(use_pos);getelementInst.usesList.add(use_pos);

                    if(this.curBlock!=null) this.curBlock.instList.add(getelementInst);
                }
                else{
                    LoadInst loadInst=new LoadInst();
                    Use use_src=new Use(value_src,loadInst),use_dst=new Use(data,loadInst);
                    llvmCtx.usesList.add(use_src);loadInst.usesList.add(use_src);
                    llvmCtx.usesList.add(use_dst);loadInst.usesList.add(use_dst);

                    if(this.curBlock!=null) this.curBlock.instList.add(loadInst);
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

                    if(this.curBlock!=null) this.curBlock.instList.add(loadInst);
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

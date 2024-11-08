package frontend;

import java.util.ArrayList;
import frontend.Tree.*;
import frontend.Tree.Const.ConstDecl;
import frontend.Tree.Var.VarDecl;
import frontend.Tree.Const.*;
import frontend.Tree.Var.*;
import frontend.Tree.Func.*;

public class Semantics {
    public boolean defineInt;      //上一个字符串为int，表示：正在进行int标识符定义
    public boolean defineChar;
    public boolean defineConst;
    public boolean defineVoidFun;   //
    public boolean inLoop;      //是否位于循环中(用于break和continue语句的合法性判定)
    public boolean inPrint;     //是否正位于待打印的字符串（判断%表示模或格式字符串）
    public int printNum;        //待打印的格式化字符串个数
    public int printfLine;      //‘printf’所在行号（使用错误类型码为'l'）
    public boolean isAssign;    //上一个为'=',进行赋值
    public boolean defineArray;       //上一个字符为'[',表示：正在进行数组定义
    public boolean defineArrayLength;   //上一个字符为'[',表示：正在进行数组长度定义；遇到']'后结束
    public boolean referArray;          //引用数组
    public int referFunction;       //调用函数时，函数的嵌套层数
    public boolean enterLoop;           //进入for循环
    //public boolean toPrint;             //printf语句中，是否形成新的表达式(考虑printf("%d",a+b);,避免a,b重复计数)

    public IndexTable indexTable;   //分程序索引表
    public int level;       //当前嵌套层数
    public SymTable currentSymTable;    //当前子程序的符号表
    public int current_no;      //当前子程序的索引项编号（唯一）

    public SymTable.SymTab last_symTab; //上一个声明的标识符（如果标识符声明后紧跟'(',说明是函数声明；否则为int/char型变量/常量）
    public SymTable.SymTab function_symTab; //上一个声明的函数标识符（用于正在声明的函数）
    public SymTable.referred_function_symTab referred_function_symTab;
    public int last_refer_array_index;    //上一个数组元素位置（用于数组元素赋值）
    public SymTable functionParamSymTable;      //包含当前函数参数的符号表

    public ArrayList<SymTable.referred_function_symTab> functions_referred_stack;  // 函数调用栈

    public Grammar grammar;

    public void initialize(Grammar grammar) {
        this.defineInt=false;
        this.defineChar=false;
        this.defineConst=false;
        this.defineVoidFun=false;
        this.inLoop=false;
        this.inPrint=false;
        this.isAssign=false;
        this.defineArray=false;
        this.referArray=false;
        this.referFunction=0;
        this.enterLoop=false;

        this.indexTable=new IndexTable(this);
        this.indexTable.initialize();       //索引表初始化：创建全局符号表，getint(),getchar()符号表
        //this.indexTable.createNewSymTable(0,-1,false);
        IndexTable.IndexTab indexTab=this.indexTable.indexTabList.get(0);
        this.currentSymTable=indexTab.pointer;   //初始化索引表：创建全局符号表
        this.level=0;this.current_no=0;

        this.last_symTab=null;
        this.function_symTab=null;
        this.referred_function_symTab=null;
        this.printNum=-1;
        this.last_refer_array_index=-1;

        this.functions_referred_stack=new ArrayList<>();

        this.grammar=grammar;
    }

    public char processToken(String token,LexType lexType,int line){     //处理读取的单词值
        if (lexType.equals(LexType.INTTK)) {      //当前单词为int
            this.defineInt = true;this.defineChar=false;this.defineVoidFun=false;
            if(this.function_symTab!=null) {this.defineArray=false;this.defineArrayLength=false;}   //进行函数声明/函数调用，不会出现数组定义
            return 'z';        //开始int型常量/变量/函数定义
        } else if (lexType.equals(LexType.CHARTK)) {
            this.defineChar = true;this.defineInt=false;this.defineVoidFun=false;
            if(this.function_symTab!=null) {this.defineArray=false;this.defineArrayLength=false;}
            return 'z';
        }else if(lexType.equals(LexType.VOIDTK)){
            this.defineVoidFun = true;this.defineInt=false;this.defineChar=false;this.defineConst=false;
            if(this.function_symTab!=null) {this.defineArray=false;this.defineArrayLength=false;}
            this.grammar.lexer.inVoidFunDefine=true;        // 进入void函数定义
            return 'z';
        }else if(lexType.equals(LexType.CONSTTK)){  //当前单词为const
            this.defineConst = true;return 'z';
        }else if(lexType.equals(LexType.MAINTK)){
            this.indexTable.searchSymTable(token,LexType.MAINTK,this.level,this.currentSymTable,true,false);
            return 'z';
        }else if(lexType.equals(LexType.BREAKTK)||lexType.equals(LexType.CONTINUETK)){
            if(!this.inLoop) return 'm';    //错误：在非循环块中使用break和continue语句     //m
            return 'z';
        }else if(lexType.equals(LexType.FORTK)){
            this.enterLoop=true;this.inLoop=true;
            return 'z';
            // 例： for(; s[i] != '\0'; i = i + 1) ; 不创建新作用域
        }else if(lexType.equals(LexType.IDENFR)){       //标识符
            Node node=(Node)this.grammar.curNode;
            if(node instanceof ConstDef || node instanceof VarDef || node instanceof FuncDef || node instanceof FuncFParam)
            {      //声明部分:查本层符号表
                System.out.println("declare:"+token);
                boolean exists;     //查本层符号表，有无同名
                LexType idenfr_LexType;
                if(this.defineInt && this.defineConst){     //该符号声明对应int型常量（/数组）
                    //if(this.defineArray) idenfr_LexType=LexType.INT_CONST_ARRAY_IDENFR;
                    idenfr_LexType=LexType.INT_CONST_IDENFR;
                }else if(this.defineChar && this.defineConst){  //该符号声明对应char型常量（/数组）
                    //if(this.defineArray) idenfr_LexType=LexType.CHAR_CONST_ARRAY_IDENFR;
                    idenfr_LexType=LexType.CHAR_CONST_IDENFR;
                }else if(this.defineInt){  //该符号声明对应int型变量（/数组）
                    //if(this.defineArray) idenfr_LexType=LexType.INT_VAR_ARRAY_IDENFR;
                    idenfr_LexType=LexType.INT_VAR_IDENFR;
                }else{  //该符号声明对应char型变量（/数组）
                    //if(this.defineArray) idenfr_LexType=LexType.CHAR_VAR_ARRAY_IDENFR;
                    idenfr_LexType=LexType.CHAR_VAR_IDENFR;
                }
                exists=this.indexTable.searchSymTable(token,idenfr_LexType,this.level,this.currentSymTable,true,false);
                if(exists) {
                    return 'b';      //重复声明，报错    //b
                }
            }else{                                  //引用部分:查本层符号表
                /*有：即已声明。则取该名字信息（局部量）；
                无：递归地转直接外层查找，若到最外层依旧未找到该标识符的声明，报错。
                 */
                int original_no=this.current_no;          //cur_no是当前层符号表编号
                boolean found=findIdentifier(token);
                if(!found) return 'c';              //使用了未定义的标识符     //c

                //若该标识符为数组
                /*if(this.last_symTab.type==LexType.INT_VAR_ARRAY_IDENFR || this.last_symTab.type==LexType.CHAR_VAR_ARRAY_IDENFR
                ||this.last_symTab.type==LexType.INT_CONST_ARRAY_IDENFR || this.last_symTab.type==LexType.CHAR_CONST_ARRAY_IDENFR){
                    this.referArray=true;
                }*/
                if(this.referFunction>0){       //当前标识符为函数调用时传入的参数
                    System.out.println("param:"+token);
                    if(this.referred_function_symTab.arguNum>=this.function_symTab.functionIndexTab.ecount){
                        //错误类型'd'：传入参数过多
                    }else{
                        char error=checkRParamsType(false,false);
                        if(error!='z') return error;
                    }
                }
                //标识符为函数：当前是函数调用语句
                if(this.last_symTab.type==LexType.INT_FUN_IDENFR || this.last_symTab.type==LexType.CHAR_FUN_IDENFR
                        || this.last_symTab.type==LexType.VOID_FUN_IDENFR){
                    SymTable.referred_function_symTab current_referred_symTab=new SymTable.referred_function_symTab(this.last_symTab);
                    this.functions_referred_stack.add(current_referred_symTab);
                    this.referred_function_symTab=current_referred_symTab;
                    this.function_symTab=this.last_symTab;      //被调用的函数符号

                    this.referFunction++;
                    this.functionParamSymTable=this.function_symTab.functionIndexTab.pointer;   //包含该函数参数的符号表
                    this.grammar.function_parentheses_Stack.clear();
                    System.out.println("cleared");
                }
            }
            return 'z';
        }else if(lexType.equals(LexType.PRINTFTK)){
            this.inPrint=true;this.printNum=0;this.printfLine=line;
            return 'z';
        }else if(lexType.equals(LexType.GETINTTK)){
        }
        else if(lexType.equals(LexType.GETCHARTK)){
        }else if(lexType.equals(LexType.RETURNTK)){
        }
        return 'z';
    }
    public char checkRParamsType(boolean isChar,boolean isInt){
        SymTable.SymTab param=this.function_symTab.parameters.get(this.referred_function_symTab.arguNum);   //形参
        LexType lexType_argu;              //实参
        if(isChar) lexType_argu=LexType.CHAR_CONST_IDENFR;
        else if(isInt) lexType_argu=LexType.INT_CONST_IDENFR;
        else {
            SymTable.SymTab argu=this.last_symTab;
            lexType_argu=this.last_symTab.type;
            if(judgeArray(lexType_argu)){
                int curPos_test=grammar.lexer.curPos;
                while(curPos_test<grammar.lexer.source.length() && Character.isWhitespace(grammar.lexer.source.charAt(curPos_test))) curPos_test++;
                if(curPos_test<grammar.lexer.source.length() && grammar.lexer.source.charAt(curPos_test)=='['){
                    lexType_argu=arrayElementType(argu.type);
                }else if(argu.type.equals(LexType.INT_FUN_IDENFR)){ // 当前参数为int/char型函数，其类型为返回值类型
                    lexType_argu=LexType.INT_VAR_IDENFR;
                }else if(argu.type.equals(LexType.CHAR_FUN_IDENFR)){
                    lexType_argu=LexType.CHAR_VAR_IDENFR;
                }
                else lexType_argu=argu.type;
            }
        }

        if(!this.referred_function_symTab.mismatched && !this.referred_function_symTab.hasCheckedRParamsType) {
            this.referred_function_symTab.hasCheckedRParamsType=true;
            if(!this.compareParamsType(param.type,lexType_argu)){
                this.referred_function_symTab.mismatched=true;
                return 'e';     //错误类型'e'：参数类型不匹配     //e
            }
        }
        return 'z';
    }
    public boolean judgeArray(LexType lexType){
        if(lexType.equals(LexType.INT_VAR_ARRAY_IDENFR)||lexType.equals(LexType.CHAR_VAR_ARRAY_IDENFR)
        ||lexType.equals(LexType.INT_CONST_ARRAY_IDENFR)||lexType.equals(LexType.CHAR_CONST_ARRAY_IDENFR)){
            return true;
        }
        return false;
    }
    public LexType arrayElementType(LexType lexType){
        if(lexType.equals(LexType.INT_VAR_ARRAY_IDENFR)) return LexType.INT_VAR_IDENFR;
        else if(lexType.equals(LexType.CHAR_VAR_ARRAY_IDENFR)) return LexType.CHAR_VAR_IDENFR;
        else if(lexType.equals(LexType.INT_CONST_ARRAY_IDENFR)) return LexType.INT_CONST_IDENFR;
        else return LexType.CHAR_CONST_IDENFR;
    }
    public boolean compareParamsType(LexType lexType_param,LexType lexType_argu){
        System.out.println("param type:"+lexType_param+", argu type:"+lexType_argu);
        //1.传递变量给数组/传递数组给变量
        if(judgeArray(lexType_param)!=judgeArray(lexType_argu)) return false;
        //2.传递 char 型数组给 int 型数组
        else if((lexType_param==LexType.INT_VAR_ARRAY_IDENFR || lexType_param==LexType.INT_CONST_ARRAY_IDENFR)
        &&(lexType_argu==LexType.CHAR_VAR_ARRAY_IDENFR || lexType_argu==LexType.CHAR_CONST_ARRAY_IDENFR)){
            return false;
        }//3.传递 int 型数组给 char 型数组
        else if((lexType_argu==LexType.INT_VAR_ARRAY_IDENFR || lexType_argu==LexType.INT_CONST_ARRAY_IDENFR)
                &&(lexType_param==LexType.CHAR_VAR_ARRAY_IDENFR || lexType_param==LexType.CHAR_CONST_ARRAY_IDENFR)){
            return false;
        }
        return true;
    }
    public boolean findIdentifier(String token){
        boolean found=false;

        int cur_no;          //cur_no是当前层符号表编号
        SymTable cur_symtable;      //cur_symtable是当前层符号表
        boolean exists;     //查本层符号表，有无同名
        for(cur_no=this.current_no,cur_symtable=this.currentSymTable;cur_no>=0;){
            System.out.println("cur_no:"+cur_no);
            exists=this.indexTable.searchSymTable(token,LexType.IDENFR,this.level,cur_symtable,false,true);
            if(!exists){      //该层符号表中无该标识符的声明，向更外层寻找
                IndexTable.IndexTab currentTab=this.indexTable.indexTabList.get(cur_no);   //当前索引项
                int outer_no=currentTab.outer;
                if(outer_no<0) break;
                cur_no=outer_no; //更新为直接外层编号
                IndexTable.IndexTab newTab=this.indexTable.indexTabList.get(outer_no);
                cur_symtable=newTab.pointer;  //恢复为直接外层的符号表
            }else{
                found=true;break;
            }
        }
        return found;
    }

    public void returnToOuter(){    //回到直接外层
        IndexTable.IndexTab currentTab=this.indexTable.indexTabList.get(this.current_no);   //当前索引项
        int outer_no=currentTab.outer;
        this.current_no=outer_no; //更新为直接外层编号
        IndexTable.IndexTab newTab=this.indexTable.indexTabList.get(outer_no);
        this.currentSymTable=newTab.pointer;  //恢复为直接外层的符号表

        if(!this.currentSymTable.isLoop) this.inLoop=false;     //退出for循环的作用域
    }

    public void defineFunctionParameters(){
        if(this.last_symTab.redefined) return;
        if(this.last_symTab!=null){
            if(this.defineInt){             //定义返回值为int型的函数
                this.last_symTab.type=LexType.INT_FUN_IDENFR;
                grammar.lexer.IntCharFunDefine=true;
            }else if(this.defineChar){      //定义返回值为char型的函数
                this.last_symTab.type=LexType.CHAR_FUN_IDENFR;
                grammar.lexer.IntCharFunDefine=true;
            }else{                          //定义无返回值的函数
                this.last_symTab.type=LexType.VOID_FUN_IDENFR;
            }
        }
        //为函数创建新的符号表，通常层数为1，进入该作用域
        this.current_no=this.indexTable.createNewSymTable(++this.level,this.current_no,true,false);
        this.changeSymTable(this.current_no);
        //将该函数的symtab关联至：其函数参数符号表对应的索引项
        this.function_symTab.functionIndexTab=this.indexTable.indexTabList.get(current_no);
    }
    public void finishFunctionParameters(){
        if(this.last_symTab!=null){
            this.indexTable.saveSymTable(this.current_no);  //统计该符号表的登记项个数（即函数的参数个数）并保存
        }
        //当前的this.current_no指向该函数的符号表
        this.defineArray=false;       //函数中数组声明，随参数定义结束而结束
        this.defineInt=this.defineChar=this.defineConst=this.defineVoidFun=false;
    }

    public void changeSymTable(int no){     //将currentSymTable更换为编号为no的符号表,即：进入新的作用域
        IndexTable.IndexTab newTab=this.indexTable.indexTabList.get(no);
        this.currentSymTable=newTab.pointer;
    }

    public char processInt(int number){
        SymTable.SymTab symTab=this.last_symTab;

        //1.数组声明
        if(this.defineArray && (this.last_symTab.type==LexType.INT_VAR_ARRAY_IDENFR || this.last_symTab.type==LexType.INT_CONST_ARRAY_IDENFR
        || this.last_symTab.type==LexType.CHAR_VAR_ARRAY_IDENFR || this.last_symTab.type==LexType.CHAR_CONST_ARRAY_IDENFR)) {
            //symTab是数组对应的标识符
            if (this.defineArrayLength) {   //这里的数字为数组长度
                if(number<=0) System.out.println("Array length should be greater than 0.");
                symTab.arrayLength = number;
                symTab.initializeArray(number, this.defineInt);    //初始化数组
            } else {          //这里的数字为int型数组内部值
                SymTable.SymTab curArray_symTab = this.last_symTab;     //当前数组的符号
                //curArray_symTab.storeArrayElementValue(number, ++curArray_symTab.declaredIndex);     //将值存入数组
            }
        }
        //2.数组引用
        else if(this.referArray){          //引用部分：取数组元素(这里number是索引)
            this.last_refer_array_index=number;
        }
        //3.int型变量/常量声明处赋值
        else if((this.defineInt || this.defineChar)&&this.isAssign){
            //symTab.storeValue(number);
        }
        //4.int型变量/常量引用处赋值
        else if(this.isAssign){
            //if(symTab.isConst) return 'h';      //错误类型'h':改变常量的值
            //symTab.storeValue(number);
        }
        //5.单纯的数值，用于计算
        else{

        }
        if(this.referFunction>0) {       //当前数字为函数调用时传入的参数
            if (this.referred_function_symTab.arguNum >= this.function_symTab.functionIndexTab.ecount) {
                //错误类型'd'：传入参数过多
            } else {
                char error=checkRParamsType(false,true);
                if(error!='z') return error;
            }
        }
        return 'z';
    }
    public char storeString(String token){
        if(this.inPrint) return 'z';
        /*if(this.last_symTab.type!=LexType.CHAR_VAR_ARRAY_IDENFR && this.last_symTab.type!=LexType.CHAR_CONST_ARRAY_IDENFR){
            System.out.print("Not an array consisting of characters.");
            return;
        }*/
        SymTable.SymTab symTab=this.last_symTab;
        if(!this.defineArray &&  this.isAssign){    //只有声明时可初始化为字符串；引用时不能赋值为字符串，即：str="abc";非法
            //if(symTab.isConst) return 'h';          //错误类型'h':改变常量的值
            //System.out.print("Invalid assignment for arrays of characters.");
        }
        //char型数组声明
        //symTab.storeStringValue(token);
        return 'z';
    }
    public char storeChar(char c){
        /*if(this.last_symTab.type!=LexType.CHAR_VAR_IDENFR && this.last_symTab.type!=LexType.CHAR_CONST_IDENFR){
            System.out.print("Not a character.");return;
        }*/
        SymTable.SymTab symTab=this.last_symTab;
        if(!defineChar && symTab.isConst && this.isAssign) {     //引用处对常量赋值，非法
            //return 'h';
        }
        System.out.println(symTab.type);
        //1.给char变量赋值
        if(symTab.type==LexType.CHAR_VAR_IDENFR || this.last_symTab.type==LexType.CHAR_CONST_IDENFR){
            symTab.storeValue(c);
        }
        //2.给char数组中元素赋值
        else{
            symTab.storeArrayElementValue(c,this.last_refer_array_index);
        }
        return 'z';
    }

    public LexType reserve(String token){
        if(token.equals("main")) return LexType.MAINTK;
        else if(token.equals("const")) {
            this.defineConst=true;
            return LexType.CONSTTK;
        }
        else if(token.equals("int")) {
            this.defineInt=true;
            return LexType.INTTK;
        }
        else if(token.equals("char")) {
            this.defineChar=true;
            return LexType.CHARTK;
        }
        else if(token.equals("break")) return LexType.BREAKTK;
        else if(token.equals("continue")) return LexType.CONTINUETK;
        else if(token.equals("if")) return LexType.IFTK;
        else if(token.equals("else")) return LexType.ELSETK;
        else if(token.equals("for")) return LexType.FORTK;
        else if(token.equals("getint")) return LexType.GETINTTK;
        else if(token.equals("getchar")) return LexType.GETCHARTK;
        else if(token.equals("printf")) return LexType.PRINTFTK;
        else if(token.equals("return")) return LexType.RETURNTK;
        else if(token.equals("void")) return LexType.VOIDTK;
        return LexType.IDENFR;
    }

    public String symbolType(SymTable.SymTab symTab){
        if(symTab.type.equals(LexType.INT_FUN_IDENFR)) return "IntFunc";
        else if(symTab.type.equals(LexType.CHAR_FUN_IDENFR)) return "CharFunc";
        else if(symTab.type.equals(LexType.VOID_FUN_IDENFR)) return "VoidFunc";

        else if(symTab.type.equals(LexType.INT_VAR_IDENFR)) return "Int";
        else if(symTab.type.equals(LexType.CHAR_VAR_IDENFR)) return "Char";
        else if(symTab.type.equals(LexType.INT_VAR_ARRAY_IDENFR)) return "IntArray";
        else if(symTab.type.equals(LexType.CHAR_VAR_ARRAY_IDENFR)) return "CharArray";

        else if(symTab.type.equals(LexType.INT_CONST_IDENFR)) return "ConstInt";
        else if(symTab.type.equals(LexType.CHAR_CONST_IDENFR)) return "ConstChar";
        else if(symTab.type.equals(LexType.INT_CONST_ARRAY_IDENFR)) return "ConstIntArray";
        else return "ConstCharArray";
    }
}

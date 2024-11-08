package frontend;
import frontend.Tree.*;
import frontend.Tree.Const.*;
import frontend.Tree.Exp.*;
import frontend.Tree.Func.*;
import frontend.Tree.Stmt.*;
import frontend.Tree.Var.*;

import java.util.*;

//词法分析器类（单例模式）
public class Lexer {
    public String source;       //源程序字符串
    public int curPos;          //当前字符串位置指针
    public String token;
    public String lexType;
    public int lineNum;           // 当前行号
    public int number;            // 解析数值
    public int printNum;     //待打印的参数个数

    public Grammar grammar;     //语法分析器
    public Semantics semantics; //语义分析器

    public boolean in_Single_Notation;      //是否位于单行注释内
    public boolean in_Multiple_Notation;    //是否位于多行注释内
    public boolean inString;        //是否位于字符串内（遇到首个双引号后进入；遇到第二个退出）
    public boolean isMinus;             //'-'号之后是数字时：构成负整数(false)，还是作为表达式中减号(true)

    public boolean right_Brackets_before_Assign;    // 检查ConstDef中的错误k
    public boolean right_Parentheses;
    public boolean inCond;      // 辅助判断if语句的 (' Cond ')'是否缺失右括号:从读到if开始，至读到Cond后右括号结束

    public boolean inVoidFunDefine;     // 是否处于void函数定义中，判断'f'型错误
    public boolean IntCharFunDefine;    // 是否处于int/char型函数或主函数定义中，判断'g'型错误
    public boolean checkReturn;

    public boolean checkSemicolon;  /*
        对以下情况：Stmt ->  'break' ';' | 'continue' ';' | LVal '=' 'getint''('')'';' | LVal '=' 'getchar''('')'';'
        有可能存在分号换行情况
    */

    public ArrayList<String> statements;    //待输出至lexer.txt中的内容
    public ArrayList<String> errors;
    public ArrayList<String> info;

    public void initialize(){
        this.curPos=0;
        this.token=null;
        this.lexType=null;

        this.grammar=new Grammar(this);
        this.semantics=new Semantics();
        this.grammar.initialize();
        this.semantics.initialize(this.grammar);
        this.in_Single_Notation=false;
        this.in_Multiple_Notation=false;
        this.lineNum=0;
        this.printNum=0;
        this.isMinus=false;

        this.right_Brackets_before_Assign=false;
        this.right_Parentheses=false;
        this.inCond=false;

        this.inVoidFunDefine=false;
        this.IntCharFunDefine=false;
        this.checkReturn=false;

        this.checkSemicolon=false;

        statements=new ArrayList<>();
        errors=new ArrayList<>();
        info=new ArrayList<>();
    }
    public boolean Digit_or_Letter_or_Underline(char c){
        return (Character.isDigit(c)||Character.isLetter(c)||c=='_');
    }
    public char next(int lineNum) {     //处理当前行，返回错误类型（无错误返回'z'）
        int len = this.source.length();   //本行字符串的长度
        String token = "";
        LexType lexType;
        this.in_Single_Notation=false;
        while(curPos<len && Character.isWhitespace(this.source.charAt(curPos))) curPos++;
        //if(curPos<len && this.source.charAt(curPos)!='}') this.hasReturn=false;

        while (this.curPos < len){
            char c = this.source.charAt(this.curPos);
            if(this.in_Single_Notation) {curPos++;continue;}
            if(this.in_Multiple_Notation && c!='*'){curPos++;continue;}
            if (Character.isWhitespace(c)) {
                token = "";
                curPos++;
                //continue;       //跳过空格
            } else if(c=='/') {          // 第一个 /
                curPos++;
                if (curPos < this.source.length() && this.source.charAt(curPos) == '/') {
                    if(this.inCond){
                        errors.add(Integer.toString(lineNum)+" j");
                        this.source=this.source.substring(0,curPos-1)+")"+this.source.substring(curPos-1);len++;
                        curPos--;this.inCond=false;continue;
                    }if(!(grammar.curNode instanceof CompUnit) && !(grammar.curNode instanceof Block)
                            && !(grammar.curNode instanceof Stmt) && !(grammar.curNode instanceof FuncDef)
                            && !(grammar.curNode instanceof MainFuncDef)){
                        errors.add(Integer.toString(lineNum)+" i");       //i
                        this.source=this.source.substring(0,curPos-1)+";"+this.source.substring(curPos-1);len++;
                        curPos--;continue;
                    }
                    this.in_Single_Notation=true;
                    // 第二个 / : 单行注释，略过本行
                    curPos++;
                    //return 'z';
                }else if(curPos < this.source.length() && this.source.charAt(curPos) == '*'){
                    int curPos_test=curPos;
                    // 第二个 * : 跨行注释，用状态机判断
                    this.in_Multiple_Notation=true;curPos++;
                    while(curPos < this.source.length() && this.source.charAt(curPos) != '*') curPos++; //非*字符
                    while(curPos < this.source.length() && this.source.charAt(curPos) == '*') curPos++; //*字符
                    if(curPos<this.source.length() && this.source.charAt(curPos)=='/') {
                        c=this.source.charAt(curPos++);
                        this.in_Multiple_Notation=false;        //多行注释结束
                    }
                }else{      //非*非/字符，视作除号
                    statements.add("DIV /");token+=c;
                    System.out.println("DIV /");

                    if(this.grammar.curNode instanceof MulExp mulExp){
                        // 乘除模表达式 MulExp → UnaryExp | MulExp ('*' | '/' | '%') UnaryExp
                        //除号'/'后的式子
                        mulExp.create_UnaryExp(this.grammar,this.lineNum);
                    }
                }
            }else if(c=='*'){
                curPos++;
                if(this.in_Multiple_Notation && curPos<this.source.length() && this.source.charAt(curPos)=='/') {
                    curPos++;
                    this.in_Multiple_Notation=false;        //多行注释结束
                }else{      //视作乘号
                    statements.add("MULT *");token+=c;
                    System.out.println("MULT *");

                    if(this.grammar.curNode instanceof MulExp mulExp){
                        // 乘除模表达式 MulExp → UnaryExp | MulExp ('*' | '/' | '%') UnaryExp
                        //乘号'*'后的式子
                        mulExp.create_UnaryExp(this.grammar,this.lineNum);
                    }
                }
            }else if(c=='\"'){      //遇到双引号
                curPos++;token="";
                this.printNum=0;     //待打印的参数个数
                while(curPos < this.source.length()) {
                    if(this.source.charAt(curPos-1)!='\\' && this.source.charAt(curPos)=='\"') break;
                    System.out.print(this.source.charAt(curPos));
                    if(this.semantics.inPrint && this.source.charAt(curPos)=='%' && curPos+1<this.source.length()){     //待打印的内容
                        //%d,%c为格式化字符
                        if(this.source.charAt(curPos+1)=='d'||this.source.charAt(curPos+1)=='c'){
                            this.printNum++;token+=this.source.charAt(curPos);token+=this.source.charAt(curPos+1);curPos+=2;
                            //System.out.print('*');System.out.print(this.printNum);
                        }else{
                            token+=this.source.charAt(curPos);curPos++;
                        }
                    }else{
                        token+=this.source.charAt(curPos);curPos++;
                    }
                }System.out.print("\n");
                //Note 1
                if(this.semantics.referFunction>0) {       //当前数值为函数调用时传入的参数
                    if (this.semantics.referred_function_symTab.arguNum >= this.semantics.function_symTab.functionIndexTab.ecount) {
                        System.out.println("------");
                        this.semantics.referred_function_symTab.arguNum = 0;
                        errors.add(Integer.toString(lineNum)+" d"); //错误类型'd'：传入参数过多      //d
                    } else {
                        SymTable.SymTab param = this.semantics.function_symTab.parameters.get(this.semantics.referred_function_symTab.arguNum);   //形参
                        if(!this.semantics.referred_function_symTab.mismatched && !this.semantics.referred_function_symTab.hasCheckedRParamsType) {
                            this.semantics.referred_function_symTab.hasCheckedRParamsType=true;
                            if(param.type!=LexType.CHAR_CONST_ARRAY_IDENFR && param.type!=LexType.CHAR_VAR_ARRAY_IDENFR){
                                this.semantics.referred_function_symTab.mismatched=true;
                                errors.add(Integer.toString(lineNum)+" e");     //错误类型'e'：参数类型不匹配     //e
                            }
                        }
                        param.storeStringValue(token);         //注意：忽略了char c='a'+1;的情况，这里直接视作char c='a';
                    }
                }
                char error=this.semantics.storeString(token);      //字符数组声明时赋值
                //if(error!='z') return error;
                //return 'z';

                curPos++;       //跳过'\"'
                statements.add("STRCON \""+token+"\"");
                System.out.println("STRCON \""+token+"\"");
                if(this.semantics.defineArray){
                    // 例：const char ca3[5]="abcd",c4='a';
                    this.semantics.defineArray=false;this.semantics.isAssign=false;
                }
            }else if(c=='\''){      //单引号：char型变量/常量的值
                curPos++;token="";
                while(curPos < this.source.length()){
                    if(this.source.charAt(curPos-1)=='\\'&& this.source.charAt(curPos)=='\\') {
                        token+=this.source.charAt(curPos);curPos++;break;      // 字符为'\\'
                    }
                    if(this.source.charAt(curPos-1)!='\\'&& this.source.charAt(curPos)=='\'') break;
                    token+=this.source.charAt(curPos);++curPos;
                }
                // Note 2
                if(this.semantics.referFunction>0) {       //当前数值为函数调用时传入的参数
                    if (this.semantics.referred_function_symTab.arguNum >= this.semantics.function_symTab.functionIndexTab.ecount) {
                        System.out.println("------");
                        this.semantics.referred_function_symTab.arguNum = 0;
                        errors.add(Integer.toString(lineNum)+" d"); //错误类型'd'：传入参数过多  //d
                    } else {
                        SymTable.SymTab param = this.semantics.function_symTab.parameters.get(this.semantics.referred_function_symTab.arguNum);   //形参
                        if(!this.semantics.referred_function_symTab.mismatched && !this.semantics.referred_function_symTab.hasCheckedRParamsType) {
                            this.semantics.referred_function_symTab.hasCheckedRParamsType=true;
                            if(param.type!=LexType.CHAR_CONST_IDENFR && param.type!=LexType.CHAR_VAR_IDENFR){
                                this.semantics.referred_function_symTab.mismatched=true;
                                errors.add(Integer.toString(lineNum)+" e");     //错误类型'e'：参数类型不匹配     //e
                            }
                        }
                        param.storeValue(this.source.charAt(curPos-1));         //注意：忽略了char c='a'+1;的情况，这里直接视作char c='a';
                    }
                }
                curPos++;     //跳过'\''
                statements.add("CHRCON \'"+token+"\'");
                System.out.println("CHRCON \'"+token+"\'");

                if(this.grammar.curNode instanceof Block block){
                    Stmt stmt=new Stmt(this.grammar,this.lineNum);
                    block.next.add(stmt);stmt.pre=block;block.visited++;    // Block -> Stmt
                    this.grammar.curNode=stmt;
                    stmt.create_Exp(this.grammar,this.lineNum);          // Stmt -> Exp ->...->UnaryExp
                    this.grammar.curNode.match(token,LexType.CHRCON);          // UnaryExp -> PrimaryExp -> Number/Character
                }
                this.grammar.curNode.match(token,LexType.CHRCON);
            }else if(c=='+'){
                if(this.grammar.curNode instanceof MulExp mulExp){
                    // 加减表达式 AddExp → MulExp | AddExp ('+' | '−') MulExp
                    //加号'+'后的式子
                    this.grammar.curNode=mulExp.pre;
                    this.grammar.curNode.return_to_upper();
                }
                curPos++;
                while(curPos<this.source.length() && Character.isWhitespace(this.source.charAt(curPos))) curPos++;
                statements.add("PLUS +");
                System.out.println("PLUS +");
                if(this.grammar.curNode instanceof Block block){
                    // 例子：{ +a; }
                    Stmt stmt=new Stmt(this.grammar,this.lineNum);
                    block.next.add(stmt);stmt.pre=block;block.visited++;
                    this.grammar.curNode=stmt;
                    stmt.create_Exp(this.grammar,this.lineNum); // Block -> Stmt -> Exp -> ... -> UnaryExp;
                }
                if(this.grammar.curNode instanceof UnaryExp unaryExp) {
                    // UnaryExp -> UnaryOp UnaryExp
                    unaryExp.create_UnaryExp(this.grammar,this.lineNum);
                }
                if(this.grammar.curNode instanceof AddExp addExp){
                    addExp.create_MulExp(this.grammar,this.lineNum);
                }
            }else if(c=='-'){   //词法分析时，只能将"-"识别成运算符，但无法确定其到底是用作负号还是减号。 这个工作将在语法分析阶段完成
                /*
                char pre;
                if(curPos>0) pre=this.source.charAt(curPos-1);
                else pre='#';
                c=this.source.charAt(++curPos);token="";

                if((Character.isDigit(c) && this.semantics.isAssign
                        &&(pre=='#'||pre=='='||pre==','||pre=='('||pre=='{'))){        //1.负整数
                    token += c;
                    curPos++;
                    while (curPos < this.source.length() && Character.isDigit(this.source.charAt(curPos))) {
                        // 下一个符号是数字
                        c = this.source.charAt(curPos++);
                        token += c;
                    }
                    lexType = LexType.INTCON;
                    number = Integer.parseInt("-"+token); // 转化为数值
                    char error=this.semantics.processInt(number);
                    //if(error=='h') return 'h';
                    statements.add("INTCON "+number);
                    System.out.println("INTCON "+number);
                }else{                  //2.计算式中的'-'
                    statements.add("MINU -");
                    System.out.println("MINU -");
                }*/

                if(this.grammar.curNode instanceof MulExp mulExp){
                    // 加减表达式 AddExp → MulExp | AddExp ('+' | '−') MulExp
                    //减号'-'后的式子
                    this.grammar.curNode=mulExp.pre;
                    this.grammar.curNode.return_to_upper();
                }
                curPos++;
                while(curPos<this.source.length() && Character.isWhitespace(this.source.charAt(curPos))) curPos++;
                statements.add("MINU -");
                System.out.println("MINU -");

                if(this.grammar.curNode instanceof Block block){
                    // 例子：{ -a; }
                    Stmt stmt=new Stmt(this.grammar,this.lineNum);
                    block.next.add(stmt);stmt.pre=block;block.visited++;
                    this.grammar.curNode=stmt;
                    stmt.create_Exp(this.grammar,this.lineNum); // Block -> Stmt -> Exp -> ... -> UnaryExp;
                }
                if(this.grammar.curNode instanceof UnaryExp unaryExp) {
                    unaryExp.create_UnaryExp(this.grammar,this.lineNum);
                }
                if(this.grammar.curNode instanceof AddExp addExp){
                    addExp.create_MulExp(this.grammar,this.lineNum);
                }

            }
            else if(c=='%'){
                curPos++;
                while(curPos<this.source.length() && Character.isWhitespace(this.source.charAt(curPos))) curPos++;
                statements.add("MOD %");
                System.out.println("MOD %");

                if(this.grammar.curNode instanceof MulExp mulExp){
                    // 乘除模表达式 MulExp → UnaryExp | MulExp ('*' | '/' | '%') UnaryExp
                    //模'%'后的式子
                    mulExp.create_UnaryExp(this.grammar,this.lineNum);
                }
            } else if (c == '=') {
                curPos++;
                System.out.println("hi");
                if (curPos < this.source.length() && this.source.charAt(curPos) == '='){    //判断相等：==
                    if(this.grammar.curNode instanceof MulExp mulExp) {
                        this.grammar.curNode = mulExp.pre;    // MulExp <- AddExp
                    }
                    if(this.grammar.curNode instanceof AddExp addExp) {
                        addExp.return_to_upper();     //"<AddExp>"
                        this.grammar.curNode=addExp.pre;  // AddExp <- RelExp
                    }
                    if(this.grammar.curNode instanceof RelExp relExp) {
                        relExp.return_to_upper();   //"<RelExp>"
                        this.grammar.curNode=relExp.pre;    // RelExp <- EqExp
                        this.grammar.curNode.return_to_upper();     //"<EqExp>"
                    }

                    curPos++;
                    while(curPos<this.source.length() && Character.isWhitespace(this.source.charAt(curPos))) curPos++;
                    statements.add("EQL ==");
                    System.out.println("EQL ==");

                    if(this.grammar.curNode instanceof EqExp eqExp) {
                        eqExp.create_RelExp(this.grammar,this.lineNum); // EqExp -> RelExp
                    }
                }else{                          //赋值：=
                    if(this.right_Brackets_before_Assign) {      //source.charAt(curPos-1)处是'='
                        this.source = this.source.substring(0, curPos - 1) + ']' + this.source.substring(curPos - 1);
                        len += 1;
                        errors.add(Integer.toString(lineNum) + " k");
                        this.right_Brackets_before_Assign = false;
                        curPos--;
                        continue;
                    }
                    while(curPos<this.source.length() && Character.isWhitespace(this.source.charAt(curPos))) curPos++;
                    if(this.grammar.curNode instanceof ConstDef constDef){
                        //this.statements.add("***");
                        // 常量定义 ConstDef → Ident [ '[' ConstExp ']' ] '=' ConstInitVal
                        ConstInitVal constInitVal=new ConstInitVal(this.grammar,this.lineNum);
                        constDef.next.add(constInitVal);constInitVal.pre=constDef;constDef.visited++;
                        this.grammar.curNode=constInitVal;
                        //常量初值 ConstInitVal → ConstExp | '{' [ ConstExp { ',' ConstExp } ] '}' | StringConst
                        if(this.source.charAt(curPos)!='{' && this.source.charAt(curPos)!='\"'){
                            // 1.  ConstInitVal → ConstExp
                            constInitVal.create_ConstExp(this.grammar,this.lineNum);
                        }else if(this.source.charAt(curPos)=='{' && this.source.charAt(curPos+1)!='}'){
                            // 2.  ConstInitVal -> '{' [ ConstExp { ',' ConstExp } ] '}'
                            // 注： ConstInitVal -> {}不创建ConstExp
                            //constInitVal.create_ConstExp(this.grammar,this.lineNum);    // ConstInitVal -> ConstExp -> ... -> UnaryExp
                            //constInitVal.multipleInitVal=true;
                        }else if(this.source.charAt(curPos)=='\"'){
                            // 3. StringConst
                        }
                    }else if(this.grammar.curNode instanceof VarDef varDef){
                        //变量定义 VarDef → Ident [ '[' ConstExp ']' ] | Ident [ '[' ConstExp ']' ] '=' InitVal
                        InitVal initVal=new InitVal(this.grammar,this.lineNum);
                        varDef.next.add(initVal);initVal.pre=varDef;initVal.visited++;
                        this.grammar.curNode=initVal;
                        // 变量初值 InitVal → Exp | '{' [ Exp { ',' Exp } ] '}' | StringConst
                        if(this.source.charAt(curPos)!='{' && this.source.charAt(curPos)!='\"'){
                            // 1. InitVal → Exp
                            initVal.create_Exp(this.grammar,this.lineNum);
                        }else if(this.source.charAt(curPos)=='{' && this.source.charAt(curPos+1)!='}'){
                            // 2. InitVal → '{' [ Exp { ',' Exp } ] '}'
                            // 注：InitVal -> {}不创建Exp
                            //initVal.create_Exp(this.grammar,this.lineNum);
                            //initVal.multipleInitVal=true;
                        }// 3. StringConst
                    }
                    if(this.grammar.curNode instanceof LVal lVal){
                        lVal.return_to_upper();
                    }
                    statements.add("ASSIGN =");
                    this.semantics.isAssign=true;
                    System.out.println("ASSIGN =");
                    if(this.grammar.curNode instanceof ForStmt forStmt){
                        // 语句 ForStmt → LVal '=' Exp
                        forStmt.create_Exp(this.grammar,this.lineNum);
                    }
                    if(this.grammar.curNode instanceof Stmt stmt){
                        // 语句 Stmt → LVal '=' Exp ';'
                        // 后续读之Ident处要排除：Stmt -> LVal '=' 'getint''('')'';'| LVal '=' 'getchar''('')'';'
                        stmt.create_Exp(this.grammar,this.lineNum);
                    }
                }
            }else if(c=='!'){
                curPos++;
                while(curPos<this.source.length() && Character.isWhitespace(this.source.charAt(curPos))) curPos++;
                if(curPos<this.source.length() && this.source.charAt(curPos)=='='){
                    if(this.grammar.curNode instanceof MulExp mulExp) {
                        this.grammar.curNode = mulExp.pre;    // MulExp <- AddExp
                    }
                    if(this.grammar.curNode instanceof AddExp addExp) {
                        addExp.return_to_upper();     //"<AddExp>"
                        this.grammar.curNode=addExp.pre;  // AddExp <- RelExp
                    }
                    if(this.grammar.curNode instanceof RelExp relExp) {
                        relExp.return_to_upper();   //"<RelExp>"
                        this.grammar.curNode=relExp.pre;    // RelExp <- EqExp
                        this.grammar.curNode.return_to_upper();       //"<EqExp>"
                    }

                    curPos++;statements.add("NEQ !=");
                    System.out.println("NEQ !=");

                    if(this.grammar.curNode instanceof EqExp eqExp) {
                        eqExp.create_RelExp(this.grammar,this.lineNum); // EqExp -> RelExp
                    }
                }else{
                    statements.add("NOT !");
                    System.out.println("NOT !");
                    if(this.grammar.curNode instanceof UnaryExp unaryExp) {
                        unaryExp.create_UnaryExp(this.grammar,this.lineNum);
                    }
                }
            }else if(c=='<' || c=='>'){
                if(this.grammar.curNode instanceof MulExp mulExp) {
                    this.grammar.curNode = mulExp.pre;    // MulExp <- AddExp
                }
                if(this.grammar.curNode instanceof AddExp addExp) {
                    addExp.return_to_upper();     //"<AddExp>"
                    this.grammar.curNode=addExp.pre;  // AddExp <- RelExp
                    this.grammar.curNode.return_to_upper();     //"<RelExp>"
                }

                curPos++;
                while(curPos<this.source.length() && Character.isWhitespace(this.source.charAt(curPos))) curPos++;
                if(curPos<this.source.length() && this.source.charAt(curPos)=='='){
                    if(c=='<'){
                        curPos++;statements.add("LEQ <=");
                        System.out.println("LEQ <=");
                    }else{
                        curPos++;statements.add("GEQ >=");
                        System.out.println("GEQ >=");
                    }
                }else{
                    if(c=='<'){
                        statements.add("LSS <");
                        System.out.println("LSS <");
                    }else{
                        statements.add("GRE >");
                        System.out.println("GRE >");
                    }
                }

                if(this.grammar.curNode instanceof RelExp relExp){
                    relExp.create_AddExp(this.grammar,this.lineNum);    // RelExp -> AddExp
                }
            }
            else if(c=='&'){
                curPos++;
                if(curPos<this.source.length() && this.source.charAt(curPos)!='&') {
                    errors.add(Integer.toString(lineNum)+" a");
                    //return 'a';
                }
                if(this.grammar.curNode instanceof MulExp mulExp) {
                    this.grammar.curNode = mulExp.pre;    // MulExp <- AddExp
                }
                if(this.grammar.curNode instanceof AddExp addExp) {
                    addExp.return_to_upper();     //"<AddExp>"
                    this.grammar.curNode=addExp.pre;  // AddExp <- RelExp
                }
                if(this.grammar.curNode instanceof RelExp relExp) {
                    relExp.return_to_upper();   //"<RelExp>"
                    this.grammar.curNode=relExp.pre;    // RelExp <- EqExp
                }
                if(this.grammar.curNode instanceof EqExp eqExp) {
                    eqExp.return_to_upper();    //"<EqExp>"
                    this.grammar.curNode=eqExp.pre;     // EqExp <- LAndExp
                    this.grammar.curNode.return_to_upper();     //"<LAndExp>"
                }

                curPos++;statements.add("AND &&");
                System.out.println("AND &&");

                if(this.grammar.curNode instanceof LAndExp lAndExp) {
                    lAndExp.create_EqExp(this.grammar,this.lineNum); // lAndExp -> EqExp
                }
            }else if(c=='|'){
                curPos++;
                if(curPos<this.source.length() && this.source.charAt(curPos)!='|') {
                    errors.add(Integer.toString(lineNum)+" a");
                    //return 'a';
                }
                if(this.grammar.curNode instanceof MulExp mulExp) {
                    this.grammar.curNode = mulExp.pre;    // MulExp <- AddExp
                }
                if(this.grammar.curNode instanceof AddExp addExp) {
                    addExp.return_to_upper();     //"<AddExp>"
                    this.grammar.curNode=addExp.pre;  // AddExp <- RelExp
                }
                if(this.grammar.curNode instanceof RelExp relExp) {
                    relExp.return_to_upper();   //"<RelExp>"
                    this.grammar.curNode=relExp.pre;    // RelExp <- EqExp
                }
                if(this.grammar.curNode instanceof EqExp eqExp) {
                    eqExp.return_to_upper();    //"<EqExp>"
                    this.grammar.curNode=eqExp.pre;     // EqExp <- LAndExp
                }
                if(this.grammar.curNode instanceof LAndExp lAndExp) {
                    lAndExp.return_to_upper();    //"<lAndExp>"
                    this.grammar.curNode=lAndExp.pre;     // lAnd <- LOrExp
                    this.grammar.curNode.return_to_upper();    // "<LOrExp>"
                }

                curPos++;statements.add("OR ||");
                System.out.println("OR ||");

                if(this.grammar.curNode instanceof LOrExp lOrExp) {
                    lOrExp.create_LAndExp(this.grammar,this.lineNum); // LOrExp -> LAndExp
                }
            }
            else if (c == '(' || c == ')') {
                boolean match = this.grammar.process_Parentheses(c,curPos);
                if (!match) System.out.println("Redundant right parenthesis");
                curPos++;
                while(curPos<this.source.length() && Character.isWhitespace(this.source.charAt(curPos))) curPos++;

                /*if(c=='(' && this.grammar.curNode instanceof MainFuncDef){
                    if(curPos>=this.source.length() || this.source.charAt(curPos)!=')') errors.add(Integer.toString(lineNum)+" j");
                }*/
                if(c=='(' && this.grammar.curNode instanceof MainFuncDef) checkRightParentheses();
                if(c=='(' && this.grammar.curNode instanceof Block block){
                    // 例子：{ (1); }
                    Stmt stmt=new Stmt(this.grammar,this.lineNum);
                    block.next.add(stmt);stmt.pre=block;block.visited++;
                    this.grammar.curNode=stmt;      // Bloc -> Stmt
                    stmt.create_Exp(this.grammar,this.lineNum);     // Stmt -> Exp -> ... -> UnaryExp
                    //这里：UnaryExp -> PrimaryExp -> '(' Exp ')', 进入下一个处理UnaryExp的if循环
                }
                if(c=='(' && (this.grammar.curNode instanceof Exp || this.grammar.curNode instanceof AddExp
                || this.grammar.curNode instanceof UnaryExp unaryExp)){
                    if(this.grammar.curNode instanceof Exp exp){
                        exp.create_AddExp(this.grammar,this.lineNum);
                    }else if(this.grammar.curNode instanceof AddExp addExp){
                        addExp.create_MulExp(this.grammar,this.lineNum);
                    }
                    //一元表达式 UnaryExp → PrimaryExp | Ident '(' [FuncRParams] ')' | UnaryOp UnaryExp
                    //基本表达式 PrimaryExp → '(' Exp ')' | LVal | Number | Character
                    //此处为：UnaryExp->PrimaryExp->'(' Exp ')';
                    // UnaryExp->PrimaryExp->Ident'('[FuncRParams]')'在读入Ident时处理
                    else{
                        UnaryExp unaryExp=(UnaryExp)this.grammar.curNode;
                        if(!unaryExp.containsFuncRParams){
                            unaryExp.PrimaryExp_in_parentheses(this.grammar,this.lineNum);
                            if(checkPrimaryExpParentheses()) len++;
                        }
                    }
                }
                else if(c=='(' && this.grammar.curNode instanceof Cond cond){
                    // Cond -> LOrExp -> LAndExp -> EqExp -> RelExp -> AddExp -> MulExp -> UnaryExp
                    cond.create_LOrExp(this.grammar,this.lineNum);
                }else if(c=='(' && this.grammar.curNode instanceof Stmt stmt){
                    if(stmt.isFor){
                        // Stmt ->  'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt
                        if(this.source.charAt(curPos)!=';'){
                            ForStmt forStmt=new ForStmt(this.grammar,this.lineNum);
                            stmt.next.add(forStmt);forStmt.pre=stmt;stmt.visited++;
                            stmt.with_first_forStmt=true;
                            // 语句 ForStmt → LVal '=' Exp
                            LVal lval=new LVal(this.grammar,this.lineNum);
                            forStmt.next.add(lval);lval.pre=forStmt;forStmt.visited++;
                            this.grammar.curNode=lval;
                        }
                    }else if(stmt.isPrintf){
                        // Stmt -> 'printf''('StringConst {','Exp}')'';'
                        char ch=checkPrintf();
                        if(ch!='z') len++;
                    }
                }
                if(c==')' && this.grammar.curNode instanceof FuncFParam funcFParam){
                    funcFParam.return_to_outer();    //FuncParam <- FuncParams
                    this.grammar.curNode.return_to_outer(); // FuncParams <- FuncDef
                }
                if(c==')' && this.grammar.curNode instanceof UnaryExp unaryExp){
                    if(!unaryExp.containsFuncRParams) unaryExp.return_to_upper();
                    else{
                        // UnaryExp ->  Ident '(' ')',FuncRParams不存在
                        // 先输出')'，再从UnaryExp返回，放在最后处理
                    }
                }
                if(c==')' && this.grammar.curNode instanceof MulExp mulExp){
                    /* 返回路径：
                        1. 基本表达式 PrimaryExp → '(' Exp ')'
                            MulExp <- AddExp <- Exp
                        2. 语句 Stmt -> 'if' (' Cond ')' Stmt [ 'else' Stmt ]
                            MulExp <- AddExp <- RelExp <- EqExp <- LAndExp <- LOrExp <- Cond
                        3. 语句 Stmt -> 'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt
                           语句 ForStmt → LVal '=' Exp
                            MulExp <- AddExp <- Exp <- ForStmt
                        4. 语句 Stmt ->  'printf''('StringConst {','Exp}')'';'
                            MulExp <- AddExp <- Exp <- Stmt
                        5. 一元表达式 UnaryExp -> Ident '(' [FuncRParams] ')'
                           函数实参表 FuncRParams → Exp { ',' Exp }
                            MulExp <- AddExp <- Exp <- FuncParams
                     */
                    mulExp.return_to_outer();
                }
                if(c==')' && this.grammar.curNode instanceof ForStmt forStmt){
                    // Stmt -> 'for' '(' [ForStmt] ';' [Cond] ';' ForStmt ')' Stmt
                    // 第二个ForStmt存在的情况
                    this.grammar.curNode.return_to_upper();     //"<ForStmt>"
                    Stmt stmt=(Stmt)this.grammar.curNode;
                    Stmt stmt1=new Stmt(this.grammar,this.lineNum);
                    stmt.next.add(stmt1);stmt1.pre=stmt;stmt.visited++;
                    this.grammar.curNode=stmt1;

                    this.semantics.isAssign=false;
                }
                if(c==')' && this.grammar.curNode instanceof Stmt stmt && stmt.isFor){
                    // Stmt -> 'for' '(' [ForStmt] ';' [Cond] ';)' Stmt
                    // 第二个ForStmt不存在的情况
                    Stmt stmt1=new Stmt(this.grammar,this.lineNum);
                    stmt.next.add(stmt1);stmt1.pre=stmt;stmt.visited++;
                    this.grammar.curNode=stmt1;

                    this.semantics.isAssign=false;
                }
                if(c==')' && this.grammar.curNode instanceof Stmt stmt && stmt.isPrintf){
                    // Stmt -> 'printf''('StringConst {','Exp}')'';'
                    if(this.semantics.inPrint){
                        this.semantics.inPrint=false;   // 结束打印，判断'l'型错误
                        //printf中格式字符与表达式个数不匹配:
                        //this.printNum:格式字符串个数；this.semantics.printNum:表达式个数

                        if(this.printNum!=this.semantics.printNum){
                            System.out.println("here格式化字符串个数："+this.printNum+";"+"表达式个数："+this.semantics.printNum);
                            this.printNum=0;this.semantics.printNum=0;
                            errors.add(Integer.toString(lineNum)+" l");   //l
                        }
                        this.printNum=0;this.semantics.printNum=0;
                    }
                }
                if(c==')' && this.grammar.curNode instanceof Cond cond){
                    // 语句 Stmt -> 'if' (' Cond ')' Stmt [ 'else' Stmt ]
                    this.grammar.curNode=cond.pre;
                    Stmt stmt=new Stmt(this.grammar,this.lineNum);
                    this.grammar.curNode.next.add(stmt);stmt.pre=this.grammar.curNode;this.grammar.curNode.visited++;
                    this.grammar.curNode=stmt;
                    this.inCond=false;
                }
                if (c == '(') {statements.add("LPARENT (");System.out.println("LPARENT (");}
                else {
                    if(this.grammar.curNode instanceof FuncRParams funcRParams){
                        // UnaryExp → Ident '(' [FuncRParams] ')'
                        funcRParams.return_to_upper();      // FuncRParams <- UnaryExp <- MulExp
                        //函数调用
                        if(this.semantics.referFunction>0){
                            //当前函数的括号栈
                            boolean match_1=this.grammar.process_Function_Parentheses(c,curPos);
                            System.out.println(grammar.function_parentheses_Stack.size());
                            if(!match_1) System.out.println("Redundant right parenthesis for current function.");

                            if(this.source.charAt(curPos-2)!='(') this.semantics.referred_function_symTab.arguNum++; //实参个数
                            this.semantics.referred_function_symTab.hasCheckedRParamsType=false;
                            this.semantics.referred_function_symTab.mismatched=false;
                            endReferFunction();     //判断参数不匹配的错误；重置实参个数
                        }
                    }else if(this.grammar.curNode instanceof UnaryExp unaryExp && unaryExp.containsFuncRParams){
                        // UnaryExp → Ident '('  ')', FuncRParams不存在
                        //函数调用
                        if(this.semantics.referFunction>0){
                            //当前函数的括号栈
                            boolean match_1=this.grammar.process_Function_Parentheses(c,curPos);
                            System.out.println(grammar.function_parentheses_Stack.size());
                            if(!match_1) System.out.println("Redundant right parenthesis for current function.");

                            this.semantics.referred_function_symTab.hasCheckedRParamsType=false;
                            this.semantics.referred_function_symTab.mismatched=false;
                            endReferFunction();     //判断参数不匹配的错误；重置实参个数
                        }
                        statements.add("RPARENT )");
                        System.out.println("RPARENT )");
                        unaryExp.return_to_upper();             // UnaryExp <- MulExp
                    }else if(this.grammar.curNode instanceof Exp exp){
                        if(this.grammar.curNode.pre instanceof PrimaryExp primaryExp){
                            this.grammar.curNode=this.grammar.curNode.pre;
                            statements.add("RPARENT )");
                            System.out.println("RPARENT )");
                            this.grammar.curNode.return_to_upper();     //从PrimaryExp返回
                        }
                    }else if(this.grammar.curNode instanceof PrimaryExp primaryExp){
                        // PrimaryExp → '(' Exp ')'
                        statements.add("RPARENT )");
                        System.out.println("RPARENT )");
                        this.grammar.curNode.return_to_upper();     //从PrimaryExp返回
                    }
                    else{
                        statements.add("RPARENT )");
                        System.out.println("RPARENT )");
                    }
                }

                // Note 3
                //进入/退出函数的参数定义
                if(!this.semantics.isAssign && (this.semantics.defineInt || this.semantics.defineChar || this.semantics.defineVoidFun)){
                    if(c=='(') this.semantics.defineFunctionParameters();
                    else this.semantics.finishFunctionParameters();
                }
            } else if (c == '[' || c == ']') {
                boolean match = this.grammar.process_Brackets(c,curPos);
                if (!match) System.out.println("Redundant right bracket");
                curPos++;
                while(curPos<this.source.length() && Character.isWhitespace(this.source.charAt(curPos))) curPos++;

                Node node=(Node)this.grammar.curNode;
                if(node instanceof ConstDef||node instanceof VarDef||node instanceof FuncFParam)
                //if((this.semantics.defineInt || this.semantics.defineChar) && !this.semantics.isAssign)
                {  //声明部分：开始数组定义
                    this.semantics.defineArray=true;
                    this.semantics.defineArrayLength=true;
                    //将当前符号设置为对应的数组类型
                    if(this.semantics.defineInt && this.semantics.defineConst) this.semantics.last_symTab.type=LexType.INT_CONST_ARRAY_IDENFR;
                    else if(this.semantics.defineInt) this.semantics.last_symTab.type=LexType.INT_VAR_ARRAY_IDENFR;
                    else if(this.semantics.defineConst) this.semantics.last_symTab.type=LexType.CHAR_CONST_ARRAY_IDENFR;
                    else this.semantics.last_symTab.type=LexType.CHAR_VAR_ARRAY_IDENFR;
                    this.semantics.last_symTab.arrayLength=0;
                }else{this.semantics.referArray=true;}                  //引用部分：取数组元素值

                if(c=='[' && this.grammar.curNode instanceof ConstDef def){
                    //常量定义 ConstDef → Ident [ '[' ConstExp ']' ] '=' ConstInitVal
                    checkConstDefBrackets();
                    def.create_ConstExp(this.grammar,this.lineNum);
                }else if(c=='[' && this.grammar.curNode instanceof VarDef def){
                    // 变量定义 VarDef → Ident [ '[' ConstExp ']' ] | Ident [ '[' ConstExp ']' ] '=' InitVal
                    if(checkLValVarDef()) len++;
                    def.create_ConstExp(this.grammar,this.lineNum);
                }
                else if(c=='[' && this.grammar.curNode instanceof LVal){
                    // 左值表达式 LVal -> Ident ['[' Exp ']']
                    if(checkLValVarDef()) len++;
                    Exp exp=new Exp(this.grammar,this.lineNum);
                    this.grammar.curNode.next.add(exp);exp.pre=this.grammar.curNode;this.grammar.curNode.visited++;
                    this.grammar.curNode=exp;
                    exp.create_AddExp(this.grammar,this.lineNum);
                }else if(c=='[' && this.grammar.curNode instanceof UnaryExp unaryExp){
                    if(unaryExp.withIdent){
                        // UnaryExp -> PrimaryExp -> LVal -> Ident ['[' Exp ']']
                        unaryExp.PrimaryExp_as_LVal(this.grammar,this.lineNum);
                    }
                }else if(c=='[' && this.grammar.curNode instanceof FuncFParam funcParam){
                    // 函数形参 FuncFParam → BType Ident ['[' ']']
                    /* 错误类型k：
                        函数形参 FuncFParam → BType Ident ['[' ']'] // k
                     */
                    if(this.source.charAt(curPos)!=']') errors.add(Integer.toString(lineNum)+" k");
                }
                if(c=='[') {
                    statements.add("LBRACK [");
                    System.out.println("LBRACK [");
                    continue;
                }
                if(c==']' && this.grammar.curNode instanceof MulExp mulExp){
                    /* 返回路径：
                        1. 左值表达式 LVal → Ident ['[' Exp ']']
                         MulExp <- AddExp <-  Exp <- LVal
                         2. 返回路径：MulExp <- AddExp <- ConstExp <- ConstDef
                     */
                    mulExp.return_to_outer();
                }
                statements.add("RBRACK ]");
                System.out.println("RBRACK ]");
                if(c==']' && this.grammar.curNode instanceof Exp exp){
                    if(this.grammar.curNode.pre instanceof LVal lval){
                        this.grammar.curNode=this.grammar.curNode.pre;  //回到LVal
                        // 1. 基本表达式 PrimaryExp →  LVal
                        if(this.grammar.curNode.pre instanceof PrimaryExp){
                            this.grammar.curNode.return_to_upper();     //从LVal返回，回到PrimaryExp
                        }
                        // 2. 语句 ForStmt → LVal '=' Exp
                    }
                }
                else if(c==']' && this.grammar.curNode instanceof ConstExp) {
                    // 常量定义 ConstDef → Ident [ '[' ConstExp ']' ] '=' ConstInitVal
                    //或：变量定义 VarDef → Ident [ '[' ConstExp ']' ] | Ident [ '[' ConstExp ']' ] '=' InitVal
                    this.grammar.curNode = this.grammar.curNode.pre;
                    //this.grammar.curNode.visited++;     // 回到ConstDef或VarDef
                }
                else if(c==']' && this.grammar.curNode instanceof LVal lval){
                    // 例子：arr[c]=1;
                    // 返回路径1：LVal <- PrimaryExp <- UnaryExp <- MulExp
                    // 返回路径2：LVal <- Stmt
                    lval.return_to_upper();
                }

                else {                  //结束数组定义
                    if(this.semantics.defineInt || this.semantics.defineChar) {  //声明部分：结束数组长度定义
                        this.semantics.defineArrayLength=false;
                    }else {                          //引用部分：停止获取数组索引
                        this.semantics.last_refer_array_index = -1;
                        this.semantics.referArray=false;
                    }
                }
            } else if (c == '{' || c == '}') {   //进入子程序，形成新的作用域
                boolean match=this.grammar.process_Braces(c,curPos-1);
                if(!match) System.out.println("Redundant right brace");
                curPos++;
                if(c=='{') {
                    if(this.right_Parentheses){
                        this.source=this.source.substring(0,curPos-1)+")"+this.source.substring(curPos-1);len++;
                        errors.add(Integer.toString(lineNum)+" j");
                        curPos--;this.right_Parentheses=false;
                        continue;
                    }
                    if(this.inCond){
                        // Stmt → 'if' (' Cond ')' Stmt1
                        // Stmt1 -> Block
                        errors.add(Integer.toString(lineNum)+" j");
                        this.source=this.source.substring(0,curPos-1)+")"+this.source.substring(curPos-1);len++;
                        curPos--;this.inCond=false;
                        continue;
                    }
                    if(this.grammar.curNode instanceof MainFuncDef || this.grammar.curNode instanceof FuncDef){
                        // 主函数定义 MainFuncDef → 'int' 'main' '(' ')' Block
                        // 函数定义 FuncDef → FuncType Ident '(' [FuncFParams] ')' Block
                        Block block=new Block(this.grammar,this.lineNum);
                        this.grammar.curNode.next.add(block);block.pre=this.grammar.curNode;this.grammar.curNode.visited++;
                        this.grammar.curNode=block;
                    }
                    else if(this.grammar.curNode instanceof ConstInitVal constInitVal){
                        // ConstInitVal →  '{' [ ConstExp { ',' ConstExp } ] '}'
                        if(curPos<len && this.source.charAt(curPos)!='}'){
                            // 1. ConstInitVal →  '{' [ConstExp { ',' ConstExp } '}', ConstExp存在
                            constInitVal.create_ConstExp(this.grammar,this.lineNum);    // ConstInitVal -> ConstExp -> ... -> UnaryExp
                            constInitVal.multipleInitVal=true;
                        }else{
                            // 2. ConstInitVal → '{'  '}', ConstExp不存在
                            statements.add("LBRACE {");System.out.println("LBRACE {");
                            statements.add("RBRACE }");System.out.println("RBRACE }");
                            constInitVal.return_to_outer();     // ConstInitVal <- ConstDef
                            curPos++;       // 跳过'}'
                            continue;
                        }
                    }
                    else if(this.grammar.curNode instanceof InitVal initVal){
                        if(curPos<len && this.source.charAt(curPos)!='}'){
                            initVal.create_Exp(this.grammar,this.lineNum);
                            initVal.multipleInitVal=true;
                        }else{
                            statements.add("LBRACE {");System.out.println("LBRACE {");
                            statements.add("RBRACE }");System.out.println("RBRACE }");
                            initVal.return_to_outer();
                            curPos++;       // 跳过'}'
                            continue;
                        }
                    }
                    else if(curPos==1 || this.source.charAt(curPos-2)!='='){ //不是常量/变量定义中的'{'
                        // 语句块 Block → '{' { BlockItem } '}'
                        if(this.grammar.curNode instanceof Block block){
                            Stmt stmt=new Stmt(this.grammar,this.lineNum);
                            block.next.add(stmt);stmt.pre=block;block.visited++;
                            Block new_block=new Block(this.grammar,this.lineNum);
                            stmt.next.add(new_block);new_block.pre=stmt;stmt.visited++;
                            this.grammar.curNode=new_block;
                        }else{
                            Block block=new Block(this.grammar,this.lineNum);
                            this.grammar.curNode.next.add(block);block.pre=this.grammar.curNode;this.grammar.curNode.visited++;
                            this.grammar.curNode=block;
                        }
                    }
                    statements.add("LBRACE {");System.out.println("LBRACE {");

                    // Note 4
                    if(this.semantics.isAssign && this.semantics.defineArray) continue; //数组定义，不需要额外声明作用域
                    if(this.semantics.function_symTab!=null){   //处于函数定义，不需要额外声明作用域，使用该函数的作用域
                        this.semantics.function_symTab=null;
                        this.semantics.changeSymTable(this.semantics.current_no);
                    }else{
                        int new_no=this.semantics.indexTable.createNewSymTable(++this.semantics.level,this.semantics.current_no,false,false);
                        this.semantics.current_no=new_no;   //更新当前符号表对应编号
                        this.semantics.changeSymTable(new_no);  //使用新的符号表
                    }this.semantics.enterLoop=false;
                }else{
                    // Note 5
                    if(this.semantics.defineArray){         //声明部分的数组赋值结束
                        this.semantics.defineArray=false;
                    }else{                              //退出当前子程序，回到其直接外层
                        this.semantics.returnToOuter();this.semantics.level--;
                        if(this.semantics.current_no==0){     //结束函数定义
                            this.semantics.finishFunctionParameters();
                            grammar.lexer.inVoidFunDefine=false;    //void函数定义结束

                            if(this.IntCharFunDefine){
                                this.checkReturn=true;this.IntCharFunDefine=false;
                            }
                        }
                    }

                    if(this.grammar.curNode instanceof MulExp mulExp) {
                        this.grammar.curNode.return_to_outer();
                        statements.add("RBRACE }");
                        System.out.println("RBRACE }");
                        if(!(this.grammar.curNode instanceof ConstInitVal) && !(this.grammar.curNode instanceof InitVal)){
                            continue;
                        }
                    }
                    if(this.grammar.curNode instanceof ConstInitVal constInitVal){
                        constInitVal.return_to_outer();
                        continue;
                    }
                    if(this.grammar.curNode instanceof InitVal initVal){
                        initVal.return_to_outer();      // InitVal <- VarDef
                        continue;
                    }
                    statements.add("RBRACE }");System.out.println("RBRACE }");
                    this.grammar.curNode.return_to_upper();
                }
            }else if(c==';'){
                curPos++;
                while(curPos<this.source.length() && Character.isWhitespace(this.source.charAt(curPos))) curPos++;
                if(!this.semantics.enterLoop && !grammar.parentheses_Stack.isEmpty()){
                    System.out.println("Wrong!");

                }
                // Note 6
                this.semantics.defineInt=false;
                this.semantics.defineChar=false;
                this.semantics.defineVoidFun=false;
                this.semantics.defineConst=false;
                this.semantics.defineArray=false;
                this.semantics.isAssign=false;          //结束赋值

                //System.out.println("------");
                /*if(this.semantics.function_symTab!=null){
                    if(this.semantics.referFunction==0){      //1.结束函数声明
                        this.semantics.current_no=0;            //回到全局作用域
                    }else{                              //2.结束函数调用
                        endReferFunction();
                    }
                    this.semantics.function_symTab=null;
                }*/         //hi
                if(this.right_Brackets_before_Assign){      //source.charAt(curPos-1)处是';'
                    this.source=this.source.substring(0,curPos-1)+']'+this.source.substring(curPos-1);len+=1;
                    errors.add(Integer.toString(lineNum)+" k");
                    this.right_Brackets_before_Assign=false;curPos--;
                    continue;
                }
                if(this.grammar.curNode instanceof MulExp mulExp){
                    /* 回归路径：
                        1. MulExp <- AddExp <- Exp / ConstExp <- ...
                        2. MulExp <- AddExp <- RelExp <- EqExp <- LAndExp <- LOrExp <- Cond
                        3. MulExp <- AddExp <- Exp <- ForStmt
                        4. MulExp <- AddExp <- Exp <- Stmt
                     */
                    this.grammar.curNode=mulExp.pre;
                    this.grammar.curNode.return_to_outer();
                }
                int flag=0;
                if(this.grammar.curNode instanceof Block block){
                    Stmt stmt=new Stmt(this.grammar,this.lineNum);
                    block.next.add(stmt);stmt.pre=block;block.visited++;
                    this.grammar.curNode=stmt;
                    statements.add("SEMICN ;");System.out.println("SEMICN ;");
                    stmt.return_to_outer();         // Stmt <- Block
                    continue;
                }
                else if (this.grammar.curNode instanceof Stmt stmt
                        && this.grammar.curNode.pre instanceof Stmt stmt_pre) {
                    // Stmt ->  [Exp] ';' 无Exp的情况; Stmt -> return Exp ';'
                    //出现在：1. Stmt ->  'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt 末尾
                    // 2. Stmt ->  'if' (' Cond ')' Stmt [ 'else' Stmt ] 末尾

                    /* 1. stmt_pre -> 'if' (' Cond ')' stmt [ 'else' stmt ]
                                     stmt -> 'break' ';'
                                   2. stmt_pre -> 'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' stmt
                                     stmt -> 'break' ';'
                                */
                    if(stmt_pre.isFor){
                        statements.add("SEMICN ;");System.out.println("SEMICN ;");
                        stmt.return_to_outer();             // Stmt <- Stmt
                        this.grammar.curNode.return_to_outer(); // Stmt <- ...
                        continue;
                    }
                    else if(stmt_pre.isElse || stmt_pre.isIf){
                        statements.add("SEMICN ;");System.out.println("SEMICN ;");
                        stmt.return_to_outer();             // Stmt <- Stmt
                        while(this.grammar.curNode instanceof Stmt stmt1 && (stmt1.isIf || stmt1.isElse)){
                            stmt1.return_to_outer();     // stmt <- stmt_pre
                        }
                        continue;
                    }
                }

                if(this.grammar.curNode instanceof ConstExp constExp){
                    this.grammar.curNode=this.grammar.curNode.pre;
                    this.grammar.curNode.return_to_outer();
                    // ConstExp 回到 ConstDef；或 ConstExp 回到 ConstInitVal
                }else if(this.grammar.curNode instanceof Exp exp){
                    this.grammar.curNode=this.grammar.curNode.pre;
                    this.grammar.curNode.return_to_outer();
                }
                if(this.grammar.curNode instanceof ConstInitVal constInitVal){
                    // 常量定义 ConstDef → Ident [ '[' ConstExp ']' ] '=' ConstInitVal
                    constInitVal.return_to_outer();      // 回到ConstDef
                }else if(this.grammar.curNode instanceof InitVal initVal){
                    // 变量定义 VarDef → Ident [ '[' ConstExp ']' ] | Ident [ '[' ConstExp ']' ] '='InitVal
                    initVal.return_to_outer();          // 回到VarDef
                }
                if(this.grammar.curNode instanceof ConstDef constDef){
                    constDef.return_to_outer();     //回到ConstDecl
                }else if(this.grammar.curNode instanceof VarDef varDef){
                    varDef.return_to_outer();       //回到VarDecl
                }
                if(this.grammar.curNode instanceof ForStmt forStmt){
                    // Stmt -> 'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt
                    this.grammar.curNode.return_to_upper();     //"<ForStmt>", ForStmt <- Stmt
                    // Note
                    if(this.grammar.curNode instanceof Stmt stmt){
                        //Stmt stmt=(Stmt)this.grammar.curNode;
                        if(this.source.charAt(curPos)!=';'){
                            stmt.isFor_visited++;
                            stmt.with_cond=true;
                            Cond cond=new Cond(this.grammar,this.lineNum);
                            this.grammar.curNode.next.add(cond);cond.pre=this.grammar.curNode;this.grammar.curNode.visited++;
                            this.grammar.curNode=cond;
                            // 条件表达式 Cond → LOrExp
                            cond.create_LOrExp(this.grammar,this.lineNum);
                        }
                    }
                }
                if(flag==0){
                    statements.add("SEMICN ;");System.out.println("SEMICN ;");
                }
                if(this.grammar.curNode instanceof ConstDecl constDecl){
                    constDecl.return_to_outer();
                }else if(this.grammar.curNode instanceof VarDecl varDecl){
                    varDecl.return_to_outer();
                }
                if(this.grammar.curNode instanceof Cond cond){
                    // 'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt
                    this.grammar.curNode=this.grammar.curNode.pre;  //  Cond <- Stmt
                    // Note
                    if(this.grammar.curNode instanceof Stmt stmt){
                        //Stmt stmt=(Stmt)this.grammar.curNode;
                        if(this.source.charAt(curPos)!=')'){
                            stmt.isFor_visited++;
                            stmt.with_last_forStmt=true;
                            ForStmt forStmt=new ForStmt(this.grammar,this.lineNum);    // Stmt -> ForStmt
                            stmt.next.add(forStmt);forStmt.pre=stmt;stmt.visited++;
                            LVal lval=new LVal(this.grammar,this.lineNum);              // ForStmt -> LVal
                            forStmt.next.add(lval);lval.pre=forStmt;forStmt.visited++;
                            this.grammar.curNode=lval;
                        }
                    }
                }if(this.grammar.curNode instanceof Block block &&
                        (curPos==1)){   // || Character.isWhitespace(this.source.charAt(curPos-2))      //add
                    // 例子：{ ; }
                    Stmt stmt=new Stmt(this.grammar,this.lineNum);
                    block.next.add(stmt);stmt.pre=block;block.visited++;
                    this.grammar.curNode=stmt;
                    stmt.return_to_outer();
                }
                if (this.grammar.curNode instanceof Stmt stmt) {
                    //  'printf''('StringConst {','Exp}')'';'
                    if(stmt.isPrintf) stmt.return_to_outer();      // Stmt <- ...
                    //  'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt
                    // 对于ForStmt, Cond不存在的情况
                    else if(!stmt.isFor) stmt.return_to_outer();
                    else{
                        stmt.isFor_visited++;
                        /* 这里是：Stmt -> 'for' '(' [ForStmt] ' ;' [Cond] ';' [ForStmt] ')' Stmt
                            若第一个ForStmt存在，则进入this.grammar.curNode instanceof ForStmt forStmt的情况
                         */
                        // 1. 第一个';'处：第一个ForStmt不存在（补充判断Cond是否存在）；或第一个ForStmt存在，Cond不存在
                        if(stmt.isFor_visited==1){
                            if(!stmt.with_first_forStmt){
                                if(this.source.charAt(curPos)!=';'){
                                    stmt.with_cond=true;
                                    Cond cond=new Cond(this.grammar,this.lineNum);
                                    stmt.next.add(cond);cond.pre=stmt;stmt.visited++;
                                    this.grammar.curNode=cond;
                                    cond.create_LOrExp(this.grammar,this.lineNum);
                                }
                            }else{
                            }
                        }
                        // 2. 第二个';'处：Cond不存在（补充判断第二个ForStmt是否存在）；或Cond存在，第二个ForStmt不存在
                        else if(stmt.isFor_visited==2){
                            if(!stmt.with_cond){
                                if(this.source.charAt(curPos)!=')'){
                                    stmt.with_last_forStmt=true;
                                    ForStmt forStmt=new ForStmt(this.grammar,this.lineNum);
                                    stmt.next.add(forStmt);forStmt.pre=stmt;stmt.visited++;
                                    this.grammar.curNode=forStmt;
                                    // 语句 ForStmt → LVal '=' Exp
                                    LVal lval=new LVal(this.grammar,this.lineNum);
                                    forStmt.next.add(lval);lval.pre=forStmt;forStmt.visited++;
                                    this.grammar.curNode=lval;
                                }
                            }else{
                            }
                        }
                    }
                }
            }else if(c==','){
                int flag=0;
                if(this.grammar.curNode instanceof MulExp mulExp){
                    flag=1;
                    /*返回路径：
                        1. MulExp <- AddExp <- ConstExp <- ConstDef/ConstInitVal
                        2. MulExp <- AddExp <- Exp <- Stmt (printf语句)
                        3. MulExp <- AddExp <- Exp <- FuncRParams
                    */
                    this.grammar.curNode.return_to_outer();
                }
                if(this.grammar.curNode instanceof ConstExp constExp){
                    flag=1;
                    constExp.return_to_outer();
                }
                if(this.grammar.curNode instanceof ConstInitVal constInitVal){
                    flag=1;
                    if(constInitVal.multipleInitVal){
                        // 常量初值 ConstInitVal ->  '{' [ ConstExp { ',' ConstExp } ] '}'
                        // 不返回至ConstDef
                        constInitVal.create_ConstExp(this.grammar,this.lineNum);
                    }else{
                        // 常量初值 ConstInitVal ->  ConstExp | StringConst
                        // 返回至ConstDef
                        constInitVal.return_to_outer();
                        this.statements.add("<ConstDef>");
                    }
                }else if(this.grammar.curNode instanceof InitVal initVal) {
                    flag=1;
                    if (initVal.multipleInitVal) {
                        // 变量初值 InitVal → Exp | '{' [ Exp { ',' Exp } ] '}'
                        // 不返回至VarDef
                        initVal.create_Exp(this.grammar, this.lineNum);
                    } else {
                        // 变量初值 InitVal -> StringConst
                        // 返回至VarDef
                        initVal.return_to_outer();
                        this.statements.add("<VarDef>");
                    }
                }
                if(this.grammar.curNode instanceof Stmt stmt && stmt.isPrintf){
                    stmt.create_Exp(this.grammar,this.lineNum);flag=1;
                    if(this.semantics.inPrint){
                        this.semantics.printNum++;
                    }
                }
                if(this.grammar.curNode instanceof FuncFParam funcParam){
                    funcParam.return_to_outer();    //返回路径：FuncParam <- FuncParams
                    FuncFParams funcFParams=(FuncFParams)this.grammar.curNode;
                    funcFParams.add_FuncParam();flag=1;
                }if(this.grammar.curNode instanceof FuncRParams funcRParams){
                    // FuncRParams → Exp { ',' Exp }
                    funcRParams.create_Exp(this.grammar,this.lineNum);flag=1;
                }
                if(flag==0) this.grammar.curNode.return_to_outer();
                //else this.grammar.curNode.return_to_outer();

                if(!this.semantics.defineArray) {       //非数组定义，当前标识符的赋值结束
                    // 排除引用函数时，函数实参之间的逗号，此时依旧处于赋值状态
                    // 例：int result = func_with_param(intArray[0], charArray[0], intArray, charArray);
                    if(this.grammar.curNode instanceof UnaryExp unaryExp && unaryExp.isFuncRParams){}
                    else this.semantics.isAssign=false;
                }
                if(this.semantics.referFunction>0){
                    this.semantics.referred_function_symTab.arguNum++;          //逗号分隔函数的各个实参
                    this.semantics.referred_function_symTab.hasCheckedRParamsType=false;
                }
                statements.add("COMMA ,");curPos++;System.out.println("COMMA ,");
            }else if (Character.isLetter(c) || c == '_') {     //字母或下划线
                token="";token += c;curPos++;
                while (curPos < this.source.length() &&
                        (Digit_or_Letter_or_Underline(this.source.charAt(curPos)))) {
                    // 下一个符号是字母或下划线或数字
                    c = this.source.charAt(curPos++);
                    token += c;
                }
                //System.out.println(token);
                lexType = this.semantics.reserve(token);     //查关键字表
                if(!lexType.equals(LexType.IDENFR)){
                    char error=this.semantics.processToken(token, lexType,this.lineNum);
                    if(error!='z') errors.add(Integer.toString(lineNum)+" "+Character.toString(error));
                }

                /*不报错的情况：INTTK,CHARTK,CONSTTK,MAINTK,BREAKTK/CONTINUETK,FORTK,
                    可能报错：IDENFR(可能报错b/c)
                 */
                //System.out.println(lexType);

                if(this.inCond && (lexType.equals(LexType.IFTK) || lexType.equals(LexType.FORTK)
                || lexType.equals(LexType.BREAKTK) || lexType.equals(LexType.CONTINUETK)
                || lexType.equals(LexType.RETURNTK) || lexType.equals(LexType.PRINTFTK) // 提前进入的Stmt以LVal以外的开头
                )){
                    // 错误类型j：
                    // Stmt -> 'if' (' Cond ')' Stmt , ')'未出现而提前进入Stmt
                    errors.add(Integer.toString(lineNum)+" j");
                    this.source=this.source.substring(0,curPos)+')'+this.source.substring(curPos);len++;
                    this.inCond=false;continue;
                }
                /*if(this.inCond && this.grammar.curNode instanceof MulExp mulExp){
                    Node node=mulExp.pre.pre;
                    if(node instanceof RelExp){
                        errors.add(Integer.toString(lineNum)+" j");
                        this.source=this.source.substring(0,curPos)+')'+this.source.substring(curPos);len++;
                        this.inCond=false;continue;
                    }
                }*/

                if(lexType.equals(LexType.INTTK)||lexType.equals(LexType.CHARTK)||lexType.equals(LexType.VOIDTK)
                ||lexType.equals(LexType.IFTK)||lexType.equals(LexType.FORTK)
                ||lexType.equals(LexType.BREAKTK)||lexType.equals(LexType.CONTINUETK)
                ||lexType.equals(LexType.RETURNTK)||lexType.equals(LexType.PRINTFTK)){
                    /*  i型错误：
                        ConstDecl → 'const' BType ConstDef { ',' ConstDef } ';' // i
                        VarDecl → BType VarDef { ',' VarDef } ';' // i
                        Stmt →  'if' '(' Cond ')' Stmt [ 'else' Stmt ] // j
                              | 'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt
                              | 'break' ';' | 'continue' ';' // i
                              | 'return' [Exp] ';' // i
                              | 'printf''('StringConst {','Exp}')'';' // i j
                     */
                    Node node=(Node)this.grammar.curNode;
                    /*if(node instanceof ConstDecl constDecl){
                        constDecl.match(token,lexType);     // ConstDecl -> ConstDef
                    }else if(node instanceof VarDecl varDecl){
                        varDecl.match(token,lexType);       // VarDecl -> VarDef
                    }else*/ if(node instanceof ConstDef || node instanceof VarDef || node instanceof MulExp){
                        errors.add(Integer.toString(lineNum)+" i");
                        int tokenLength=token.length();
                        this.source=this.source.substring(0,curPos-tokenLength)+";"+this.source.substring(curPos-tokenLength);
                        curPos-=tokenLength;len++;
                        continue;
                    }
                }
                if(lexType.equals(LexType.IDENFR) && this.grammar.curNode instanceof MulExp){
                    errors.add(Integer.toString(lineNum)+" i");
                    int tokenLength=token.length();
                    this.source=this.source.substring(0,curPos-tokenLength)+";"+this.source.substring(curPos-tokenLength);
                    curPos-=tokenLength;len++;
                    continue;
                }

                if(lexType.equals(LexType.ELSETK)){
                    // Stmt -> 'if' (' Cond ')' Stmt [ 'else' Stmt ]
                    // if的Stmt结束后，退回到了外层的Stmt再往上一层。
                    statements.remove(statements.size()-1);
                    // ... -> Stmt(isIf=true)
                    this.grammar.curNode=this.grammar.curNode.next.get(this.grammar.curNode.visited);
                    while(this.grammar.curNode instanceof Stmt stmt && stmt.hasElse){
                        /* 当前if语句已有else部分：
                            if(...){
                                else {
                                    if(...) stmt1
                                    else
                                }
                            }
                            stmt1结束后，已退出最外层if以外；
                            当前重新进入的if若已有else语句，应向其子Stmt递归寻找可配对的if
                        */
                        statements.remove(statements.size()-1);
                        this.grammar.curNode=this.grammar.curNode.next.get(this.grammar.curNode.visited);
                    }
                    Stmt stmt=(Stmt)this.grammar.curNode;stmt.hasElse=true;
                    this.grammar.curNode.match(token,lexType);
                    String str=lexType.toString()+" "+token;
                    System.out.println(str);
                    statements.add(str);
                    continue;
                }else if(lexType.equals(LexType.BREAKTK)||lexType.equals(LexType.CONTINUETK)){
                    // Stmt -> 'break' ';' | 'continue' ';'
                    String str=lexType.toString()+" "+token;
                    System.out.println(str);
                    statements.add(str);
                    this.semantics.isAssign=false;

                    if(this.grammar.curNode instanceof Block block){
                        // Block -> Stmt -> 'break' ';'
                        Stmt stmt=new Stmt(this.grammar,this.lineNum);
                        block.next.add(stmt);stmt.pre=block;block.visited++;
                        while(curPos<this.source.length() && Character.isWhitespace(this.source.charAt(curPos))) curPos++;
                        if(curPos>=this.source.length()) this.checkSemicolon=true;
                        else if(this.source.charAt(curPos)!=';'){
                            // 错误类型i：缺少';'
                            errors.add(Integer.toString(lineNum)+" i");   //i
                        }else{
                            curPos++;       // 跳过break/continue后的';'
                            statements.add("SEMICN ;");
                        }
                        stmt.return_to_outer();     // Stmt <- Block
                    }else if(this.grammar.curNode instanceof Stmt stmt){
                        // 1. Block -> Stmt -> 'break' ';'
                        if(stmt.pre instanceof Block block){
                            while(curPos<this.source.length() && Character.isWhitespace(this.source.charAt(curPos))) curPos++;
                            if(curPos>=this.source.length()) this.checkSemicolon=true;
                            else if(this.source.charAt(curPos)!=';'){
                                // 错误类型i：缺少';'
                                errors.add(Integer.toString(lineNum)+" i");   //i
                            }else{
                                curPos++;       // 跳过break/continue后的';'
                                statements.add("SEMICN ;");
                            }
                            stmt.return_to_outer();     // Stmt <- Block
                            continue;
                        }

                        while(curPos<this.source.length() && Character.isWhitespace(this.source.charAt(curPos))) curPos++;
                        if(curPos>=this.source.length()) this.checkSemicolon=true;
                        else if(this.source.charAt(curPos)!=';'){
                            // 错误类型i：缺少';'
                            errors.add(Integer.toString(lineNum)+" i");   //i
                        }else{
                            curPos++;       // 跳过break/continue后的';'
                            statements.add("SEMICN ;");
                        }
                        while(stmt.pre instanceof Stmt stmt_pre && (stmt_pre.isIf || stmt_pre.isFor)){
                            /* 1. stmt_pre -> 'if' (' Cond ')' stmt [ 'else' stmt ]
                                     stmt -> 'break' ';'
                                   2. stmt_pre -> 'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' stmt
                                     stmt -> 'break' ';'
                                */
                            stmt.return_to_outer();     // stmt <- stmt_pre
                            stmt=(Stmt)this.grammar.curNode;
                        }
                        if(stmt.isIf || stmt.isFor || stmt.isElse){
                            stmt.return_to_outer();
                        }
                    }
                    continue;
                }else if(lexType.equals(LexType.GETINTTK) || lexType.equals(LexType.GETCHARTK)){
                    // Stmt ->  LVal '=' 'getint''('')'';' |  'getchar''('')'';'
                    String str=lexType.toString()+" "+token;
                    System.out.println(str);
                    statements.add(str);

                    statements.add("LPARENT (");
                    int flag=0;
                    if(curPos+1<this.source.length() && this.source.charAt(curPos+1)==')') {
                        statements.add("RPARENT )");curPos+=2;  // 跳过getint/getchar后的'('')'
                    }else{
                        // 错误类型j：缺少')'
                        // Stmt -> LVal '=' 'getint''('')'';' | LVal '=' 'getchar''('')'';' //  j
                        errors.add(Integer.toString(lineNum)+" j");curPos++;
                    }

                    // 在" LVal = "处已创建：Exp -> AddExp -> MulExp -> UnaryExp，此处回退
                    this.grammar.curNode=this.grammar.curNode.pre.pre.pre.pre;  // UnaryExp <- ... <- Stmt

                    while(curPos<this.source.length() && Character.isWhitespace(this.source.charAt(curPos))) curPos++;
                    if(curPos>=this.source.length()) checkSemicolon=true;
                    else if(this.source.charAt(curPos)!=';'){
                        // 错误类型i：缺少';'
                        errors.add(Integer.toString(lineNum)+" i");   //i
                    }else{
                        // 继续读取break/continue后的';'
                        continue;
                    }
                    this.grammar.curNode.return_to_outer();     // Stmt <- Block
                    continue;
                }else if(lexType.equals(LexType.RETURNTK)){
                    // Stmt ->  'return' [Exp] ';'
                    this.grammar.curNode.match(token,lexType);

                    while(curPos<this.source.length() && Character.isWhitespace(this.source.charAt(curPos))) curPos++;
                    if(curPos<this.source.length() && this.source.charAt(curPos)!=';' && this.source.charAt(curPos)!='/'){
                        // 1. 有Exp
                        Stmt stmt=(Stmt)this.grammar.curNode;
                        stmt.create_Exp(this.grammar,this.lineNum);

                        // 'f'型错误：无返回值的函数存在不匹配的return语句
                        if(this.inVoidFunDefine) errors.add(Integer.toString(lineNum)+" f");  //f
                    }else{
                        // 2. 无Exp
                        // Stmt -> 'return' ';'
                        String str=lexType.toString()+" "+token;
                        System.out.println(str);
                        statements.add(str);
                        this.semantics.isAssign=false;

                        if(this.grammar.curNode instanceof Block block){
                            // Block -> Stmt -> 'return' ';'
                            Stmt stmt=new Stmt(this.grammar,this.lineNum);
                            block.next.add(stmt);stmt.pre=block;block.visited++;
                            if(curPos>=this.source.length() || this.source.charAt(curPos)!=';'){
                                // 错误类型i：缺少';'
                                errors.add(Integer.toString(lineNum)+" i");   //i
                            }else{
                                curPos++;       // 跳过return后的';'
                                statements.add("SEMICN ;");
                            }
                            stmt.return_to_outer();     // Stmt <- Block
                        }else if(this.grammar.curNode instanceof Stmt stmt){
                            // 1. Block -> Stmt -> 'return' ';'
                            if(stmt.pre instanceof Block block){
                                if(curPos>=this.source.length() || this.source.charAt(curPos)!=';'){
                                    // 错误类型i：缺少';'
                                    errors.add(Integer.toString(lineNum)+" i");   //i
                                }else{
                                    curPos++;       // 跳过return后的';'
                                    statements.add("SEMICN ;");
                                }
                                stmt.return_to_outer();     // Stmt <- Block
                                continue;
                            }
                            if(curPos>=this.source.length() || this.source.charAt(curPos)!=';'){
                                // 错误类型i：缺少';'
                                errors.add(Integer.toString(lineNum)+" i");   //i
                            }else{
                                curPos++;       // 跳过return后的';'
                                statements.add("SEMICN ;");
                            }
                            while(stmt.pre instanceof Stmt stmt_pre && (stmt_pre.isIf || stmt_pre.isFor)){
                            /* 1. stmt_pre -> 'if' (' Cond ')' stmt [ 'else' stmt ]
                                     stmt -> 'break' ';'
                                   2. stmt_pre -> 'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' stmt
                                     stmt -> 'break' ';'
                                */
                                stmt.return_to_outer();     // stmt <- stmt_pre
                                stmt=(Stmt)this.grammar.curNode;
                            }
                            if(stmt.isIf || stmt.isFor || stmt.isElse){
                                stmt.return_to_outer();
                            }
                        }
                        continue;
                    }
                    String str=lexType.toString()+" "+token;
                    System.out.println(str);
                    statements.add(str);
                    continue;
                }

                if(lexType.equals(LexType.IDENFR) && this.grammar.curNode instanceof CompUnit compUnit
                        && curPos<len && this.source.charAt(curPos)=='('){
                    // CompUnit -> FuncDef -> FuncType Ident '(' [FuncFParams] ')' Block
                    statements.add("<FuncType>");
                    String str=lexType.toString()+" "+token;
                    System.out.println(str);
                    statements.add(str);

                    compUnit.match_FuncDef(this.grammar,this.lineNum);
                    if(this.source.charAt(curPos+1)!=')'){
                        checkRightParentheses();
                        FuncFParams funcFParams=new FuncFParams(this.grammar,this.lineNum);
                        this.grammar.curNode.next.add(funcFParams);funcFParams.pre=this.grammar.curNode;this.grammar.curNode.visited++;
                        this.grammar.curNode=funcFParams;       // FuncDef -> FuncFParams
                        funcFParams.add_FuncParam();            //FuncFParams -> FuncFParam
                    }
                    // 语义分析
                    if(lexType.equals(LexType.IDENFR)){
                        char error=this.semantics.processToken(token, lexType,this.lineNum);
                        if(error!='z') errors.add(Integer.toString(lineNum)+" "+Character.toString(error));
                    }
                    continue;
                }

                String str=lexType.toString()+" "+token;
                System.out.println(str);
                statements.add(str);

                if(lexType.equals(LexType.IDENFR) && this.grammar.curNode instanceof CompUnit compUnit){
                    if(curPos>=len || this.source.charAt(curPos)!='('){
                        // 编译单元 CompUnit -> Decl -> VarDecl
                        Decl d=new Decl(this.grammar,this.lineNum,true);
                        compUnit.next.add(d);d.pre=compUnit;compUnit.visited++;
                        VarDecl varDecl=new VarDecl(this.grammar,this.lineNum);
                        d.next.add(varDecl);varDecl.pre=d;d.visited++;
                        //VarDef varDef=new VarDef(this.grammar,this.lineNum);
                        //varDecl.next.add(varDef);varDef.pre=varDecl;varDecl.visited++;
                        this.grammar.curNode=varDecl;
                    }
                }
                else if(lexType.equals(LexType.IDENFR) &&
                        (this.grammar.curNode instanceof Block block || this.grammar.curNode instanceof Stmt stmt)){
                    /*区分：
                        1. BlockItem -> Stmt -> LVal '=' Exp ';'| LVal '=' 'getint''('')'';' | LVal '=' 'getchar''('')'';'
                        2. BlockItem -> Stmt ->  [Exp] ';'
                            Exp -> ... -> UnaryExp -> PrimaryExp -> LVal
                            或：Exp -> ... -> UnaryExp -> Ident '(' [FuncRParams] ')'
                     */
                    int flag = 0;
                    for (int i = curPos; i < this.source.length(); i++) {
                        if (this.source.charAt(i) == '=') {
                            flag = 1;
                            break;
                        }
                    }
                    if(flag==1){        // 有'=',说明是：LVal
                        this.grammar.curNode.match(token,lexType);
                    }else{              // 无'=',说明是：Exp
                        Exp exp=new Exp(this.grammar,this.lineNum);
                        if(this.grammar.curNode instanceof Block block){
                            Stmt stmt=new Stmt(this.grammar,this.lineNum);
                            block.next.add(stmt);stmt.pre=block;block.visited++;
                            this.grammar.curNode=stmt;
                        }
                        this.grammar.curNode.next.add(exp);exp.pre=this.grammar.curNode;this.grammar.curNode.visited++;
                        this.grammar.curNode=exp;
                        exp.create_AddExp(this.grammar,this.lineNum);   // Exp -> ... UnaryExp
                    }
                }
                if(lexType.equals(LexType.IDENFR) && this.grammar.curNode instanceof UnaryExp unaryExp){
                    if(curPos<this.source.length() && this.source.charAt(curPos)!='('){
                        // 一元表达式 UnaryExp -> PrimaryExp -> LVal -> Ident ['[' Exp ']']
                        PrimaryExp primaryExp=new PrimaryExp(this.grammar,this.lineNum);
                        unaryExp.next.add(primaryExp);primaryExp.pre=unaryExp;unaryExp.visited++;
                        LVal lval=new LVal(this.grammar,this.lineNum);
                        primaryExp.next.add(lval);lval.pre=primaryExp;primaryExp.visited++;
                        this.grammar.curNode=lval;

                        lval.match(token,lexType);
                        if(this.source.charAt(curPos)!='['){
                            // UnaryExp -> PrimaryExp -> LVal -> Ident, 返回至MulExp
                            lval.return_to_upper();
                        }
                    }else{
                        // UnaryExp -> Ident '(' [FuncRParams] ')'
                        unaryExp.containsFuncRParams=true;
                        if(curPos+1<this.source.length() && this.source.charAt(curPos+1)!=')'){
                            // 1. UnaryExp -> Ident '(' FuncRParams ')', 实参列表存在
                            if(checkFuncRParamsParentheses()) len++;
                            FuncRParams funcRParams=new FuncRParams(this.grammar,this.lineNum);
                            unaryExp.next.add(funcRParams);funcRParams.pre=unaryExp;unaryExp.visited++;
                            this.grammar.curNode=funcRParams;
                                // 函数实参表 FuncRParams → Exp { ',' Exp }
                            funcRParams.create_Exp(this.grammar,this.lineNum);
                            curPos++;
                            statements.add("LPARENT (");    // 跳过'(',避免在读入'('误认为：UnaryExp -> '(' Exp ')'
                        }else{
                            // 2. UnaryExp -> Ident '(' ')', 实参列表不存在
                        }
                    }
                }else{
                    this.grammar.curNode.match(token,lexType);
                }
                if(lexType.equals(LexType.IDENFR)){
                    char error=this.semantics.processToken(token, lexType,this.lineNum);
                    if(error!='z') errors.add(Integer.toString(lineNum)+" "+Character.toString(error));
                }
            } else if (Character.isDigit(c)) {   // 无符号整数
                token="";token += c;
                curPos++;
                while (curPos < this.source.length() && Character.isDigit(this.source.charAt(curPos))) {
                    // 下一个符号是数字
                    c = this.source.charAt(curPos++);
                    token += c;
                }
                lexType = LexType.INTCON;
                number = Integer.parseInt(token); // 转化为数值

                /*Node node=(Node)this.grammar.curNode;
                    if(node instanceof ConstDef || node instanceof VarDef || node instanceof MulExp){
                    errors.add(Integer.toString(lineNum)+" i");
                    int tokenLength=token.length();
                    this.source=this.source.substring(0,curPos-tokenLength)+";"+this.source.substring(curPos-tokenLength);
                    curPos-=tokenLength;len++;
                    continue;
                }*/

                SymTable.SymTab symTab=this.semantics.last_symTab;
                char error=this.semantics.processInt(number);
                if(error!='z') errors.add(Integer.toString(lineNum)+" "+Character.toString(error));
                statements.add("INTCON "+token);
                System.out.println("INTCON "+number);

                if(this.grammar.curNode instanceof Block block){
                    Stmt stmt=new Stmt(this.grammar,this.lineNum);
                    block.next.add(stmt);stmt.pre=block;block.visited++;    // Block -> Stmt
                    this.grammar.curNode=stmt;
                    stmt.create_Exp(this.grammar,this.lineNum);          // Stmt -> Exp ->...->UnaryExp
                    this.grammar.curNode.match(token,lexType);          // UnaryExp -> PrimaryExp -> Number/Character
                }

                this.grammar.curNode.match(token,LexType.INTCON);
            }//else statements.add("UNKNOWN "+c);
        }
        return 'z';
    }
    public void endReferFunction(){
        System.out.println("******");
        boolean error_d=false;
        System.out.println(Integer.toString(semantics.referred_function_symTab.arguNum)+" "+Integer.toString(semantics.function_symTab.functionIndexTab.ecount));
        if(this.semantics.referred_function_symTab.arguNum!=this.semantics.function_symTab.functionIndexTab.ecount){
            error_d=true;         //参数个数不匹配
        }
        this.semantics.referred_function_symTab.arguNum=0;      //重置已传入函数的实参个数
        this.semantics.referFunction--;
        System.out.println("endReferFunction:"+this.semantics.function_symTab.name);
        System.out.println("referFunction="+semantics.referFunction);       //hi

        int len=this.semantics.functions_referred_stack.size();
        this.semantics.functions_referred_stack.remove(len-1);
        if(len>=2){
            this.semantics.referred_function_symTab=this.semantics.functions_referred_stack.get(len-2);
            this.semantics.function_symTab=this.semantics.referred_function_symTab.symTab;
        }
        else {
            this.semantics.referred_function_symTab=null;
            this.semantics.function_symTab=null;
        }
        //System.out.println(",return to:"+symTab.name);
        if(error_d) errors.add(Integer.toString(lineNum)+" d");   //d
    }
    public String replaceCharAt(String line, int index, char newChar) {
        // 检查索引是否有效
        if (index < 0 || index >= line.length()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Length: " + line.length());
        }
        // 使用StringBuilder进行字符串修改
        StringBuilder sb = new StringBuilder(line);
        sb.setCharAt(index, newChar); // 设置新字符
        return sb.toString(); // 返回修改后的字符串
    }
    public void checkConstDefBrackets(){
        /* 错误类型k：缺少']'
            1. 常量定义 ConstDef → Ident [ '[' ConstExp ']' ] '=' ConstInitVal // k
            2. 变量定义 VarDef → Ident [ '[' ConstExp ']' ] | Ident [ '[' ConstExp ']' ] '=' InitVal // k

            检查'='前是否有']',若无,在'='前补充']'以完成语法分析
         */
        int curPos_test=curPos,found=0;
        while(curPos_test<this.source.length() && this.source.charAt(curPos_test)!='='){
            if(this.source.charAt(curPos_test)==']') found=1;
            curPos_test++;
        }
        if(found==0) this.right_Brackets_before_Assign=true;
    }
    public void checkRightParentheses(){
        /* 错误类型j：缺少')'
            1. 函数定义 FuncDef → FuncType Ident '(' [FuncFParams] ')' Block // j
         */
        int curPos_test=curPos,found=0;
        while(curPos_test<this.source.length()){
            if(this.source.charAt(curPos_test)==')') {found=1;break;}
            curPos_test++;
        }
        if(found==0) {this.right_Parentheses=true;}
        /*int curPos_test=this.source.length()-1,found=0;
        while(curPos_test>=curPos){
            if(this.source.charAt(curPos_test)==')') {found=1;break;}
            curPos_test--;
        }
        if(found==0) {this.right_Parentheses=true;}*/
    }
    public char checkPrintf(){
        int curPos_test=curPos,found_i=0;

        while(curPos_test<this.source.length()){
            if(this.source.charAt(curPos_test)=='\"'){
                while(curPos_test<this.source.length()){
                    if(this.source.charAt(curPos_test-1)!='\\' && this.source.charAt(curPos_test)=='\"') break;
                    curPos_test++;
                }
            }
            if(this.source.charAt(curPos_test)=='\''){
                while(curPos_test < this.source.length()){
                    if(this.source.charAt(curPos_test-1)=='\\'&& this.source.charAt(curPos_test)=='\\') {
                        curPos_test++;break;      // 字符为'\\'
                    }
                    if(this.source.charAt(curPos_test-1)!='\\'&& this.source.charAt(curPos_test)=='\'') break;
                    curPos_test++;
                }
            }
            if(this.source.charAt(curPos_test)=='/' && this.source.charAt(curPos_test-1)=='/') {
                //curPos_test-=2;break;     //Note
            }
            if(this.source.charAt(curPos_test)=='*' && this.source.charAt(curPos_test-1)=='/') {
                int curPos_preRead=curPos_test;boolean exitNotation=false;
                while(curPos_preRead<this.source.length()){
                    if(this.source.charAt(curPos_preRead-1)=='*' && this.source.charAt(curPos_preRead)=='/') {
                        exitNotation=true;break;
                    }
                    curPos_preRead++;
                }
                if(exitNotation) {curPos_test=curPos_preRead;}  // 多行注释"/* ... */"在本行内结束，继续遍历
                else {curPos_test-=2;break;}
            }
            if(this.source.charAt(curPos_test)==';') {found_i=1;break;}
            curPos_test++;
        }
        if(found_i==0){
            errors.add(Integer.toString(lineNum)+" i");   //i
            if(curPos_test<this.source.length()){
                this.source=this.source.substring(0,curPos_test+1)+";"+this.source.substring(curPos_test+1);
            }       //add
            else this.source=this.source+';';
            return 'i';
        }
        //while(curPos_test>=curPos && Character.isWhitespace(this.source.charAt(curPos_test))) curPos_test--;
        curPos_test--;
        if(this.source.charAt(curPos_test)!=')'){
            errors.add(Integer.toString(lineNum)+" j");
            this.source=this.source.substring(0,curPos_test+1)+")"+this.source.substring(curPos_test+1);
            return 'j';
        }
        return 'z';
    }
    public boolean checkPrimaryExpParentheses(){
        int curPos_test=curPos,found=0,cnt=1;char c;
        while(curPos_test<this.source.length()){
            c=this.source.charAt(curPos_test);
            if(c=='(') cnt++;
            if(c==')'){
                cnt--;
                if(cnt==0) {found=1;break;}
            }
            //c==','||
            if(c==';'||c=='<'||c=='>'||c=='='||c=='!'||c=='&'||c=='|') break;
            curPos_test++;
        }
        if(found==1) return false;
        else{
            errors.add(Integer.toString(lineNum)+" j");
            this.source=this.source.substring(0,curPos_test)+')'+this.source.substring(curPos_test);
            return true;
        }
    }
    public boolean checkFuncRParamsParentheses(){
        /* 错误类型j：缺少')'
            UnaryExp -> Ident '(' [FuncRParams] ')'
         */
        int curPos_test=curPos,found=0;char c;
        while(curPos_test<this.source.length()){
            c=this.source.charAt(curPos_test);
            if(c==')'){
                found=1;break;
            }
            if(c==';'||c=='<'||c=='>'||c=='='||c=='!'||c=='&'||c=='|') break;
            curPos_test++;
        }
        if(found==1) return false;
        else{
            errors.add(Integer.toString(lineNum)+" j");
            this.source=this.source.substring(0,curPos_test)+')'+this.source.substring(curPos_test);
            return true;
        }
    }
    public boolean checkLValVarDef(){
        /* 错误类型k：缺少']'
        3. 左值表达式 LVal → Ident ['[' Exp ']'] // k  LVal结束后一定是'='
     */
        int curPos_test=curPos,found=0,paren_cnt=0;
        while(curPos_test<this.source.length()){
            char c=this.source.charAt(curPos_test);
            if(this.source.charAt(curPos_test)==']') found=1;
            if(c==';'||c=='='||c=='!'||c=='&'||c=='|'||c=='}') break;
            if(c==',' && paren_cnt==0) break;
            if(c=='(') paren_cnt++;
            else if(c==')'){
                if(paren_cnt==0) break;
                else paren_cnt--;
            }
            curPos_test++;
        }
        if(found==1) return false;
        else{
            errors.add(Integer.toString(lineNum)+" k");
            this.source=this.source.substring(0,curPos_test)+']'+this.source.substring(curPos_test);
            return true;
        }
    }
}

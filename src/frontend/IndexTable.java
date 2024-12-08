package frontend;
import java.util.ArrayList;
import java.util.Stack;
import frontend.Tree.Exp.LVal;

public class IndexTable {       //分程序索引表
    public Semantics semantics;
    public int totalIndexTabCnt;        //总索引数
    public ArrayList<IndexTab> indexTabList;    //当前索引表包含的索引项

    public class IndexTab {              //每个索引项
        public int no;          //索引项编号（从0开始）
        public int outer;       //该分程序的直接外层分程序的编号
        public int ecount;      //该分程序符号表登记项的个数
        public SymTable pointer;    //指向该分程序符号表

        public IndexTab(int no,int outer,SymTable pointer) {
            this.no = no;
            this.outer = outer;
            this.pointer = pointer;
        }
    }
    public IndexTable(Semantics semantics) {
        this.semantics = semantics;
        this.totalIndexTabCnt = 0;
        this.indexTabList = new ArrayList<IndexTab>();
    }
    public void initialize(){       //初始化索引表：创建包含全局变量/常量/函数的符号表
        int new_no=this.createNewSymTable(0,-1,false,false);
        System.out.println("全局符号表编号："+new_no);

        //加入getint(),getchar()函数
        //1.getint()函数
        SymTable symTable=this.indexTabList.get(new_no).pointer;    //全局符号表
        insertSymTab(symTable.symTabList,"getint",LexType.INT_FUN_IDENFR,0);
        //为函数创建新的符号表，通常层数为1，进入该作用域
        //int new_no_1=this.createNewSymTable(1,0,true,false);
        //System.out.println("getint()符号表编号："+new_no_1);

        //2.getchar()函数
        insertSymTab(symTable.symTabList,"getchar",LexType.CHAR_FUN_IDENFR,0);
        //int new_no_2=this.createNewSymTable(1,0,true,false);
        //System.out.println("getchar()符号表编号："+new_no_2);

    }
    /*进入一层分程序：创建该分程序的符号表(level为层数,outer_no为其直接外层分程序的编号)
    返回int值：新符号表对应索引项的编号
    enterLoop:创建for循环的新作用域时为true
     */
    public int createNewSymTable(int level,int outer_no,boolean defineFunction,boolean enterLoop){
        SymTable newSymTable=new SymTable();   //创建新的符号表
        if(this.semantics.inLoop) newSymTable.isLoop=true;  //在for循环内
        IndexTab indexTab=new IndexTab(this.totalIndexTabCnt++,outer_no,newSymTable);       //创建链接至该符号表的索引项
        indexTab.pointer=newSymTable;
        indexTabList.add(indexTab);     //新的索引项加入索引表（按no递增）

        if(defineFunction){         //处于函数声明中：将funtion_symTab设为上一个标识符项（即函数对应的标识符项）
            this.semantics.function_symTab=this.semantics.last_symTab;
            if(this.totalIndexTabCnt>3) System.out.println(this.semantics.last_symTab.name+"符号表编号："+(this.totalIndexTabCnt-1)+",上一层编号："+indexTab.outer);
        }
        if(enterLoop) System.out.println("for循环符号表编号："+(this.totalIndexTabCnt-1)+",上一层编号："+indexTab.outer);
        if(!defineFunction && !enterLoop) System.out.println("作用域符号表编号："+(this.totalIndexTabCnt-1)+",上一层编号："+indexTab.outer);
        return this.totalIndexTabCnt-1;
    }
    /*查找符号表：在符号表symTable中，查找是否存在标识符token
        insert_or_not=true,标识符出现在声明中，需要插入；
        insert_or_not=false,标识符出现在引用中，只查找不插入；
    */
    public boolean searchSymTable(String token,LexType lexType,int level,SymTable symTable,boolean insert_or_not,boolean checkIfConst){
        //查询本层符号表的所有符号中，有无与token同名的
        ArrayList<SymTable.SymTab> currentSymTabList=symTable.symTabList;
        boolean exists=false;
        int len=currentSymTabList.size();
        for(int i=0;i<len;i++){
            if(currentSymTabList.get(i).name.equals(token)){
                exists=true;
                currentSymTabList.get(i).redefined=true;
                this.semantics.last_symTab=currentSymTabList.get(i);

                if(checkIfConst && currentSymTabList.get(i).isConst && (semantics.grammar.curNode instanceof LVal)){
                    /* 判断'h'型错误：是否修改常量
                        Stmt → LVal ‘=’ Exp ‘;’
                        Stmt → LVal ‘=’ ‘getint’ ‘(’ ‘)’ ‘;’
                     */
                    boolean found_equal=false;      // 是否有等号
                    int curPos_test=semantics.grammar.lexer.curPos,source_len=semantics.grammar.lexer.source.length();
                    while(curPos_test<source_len && Character.isWhitespace(semantics.grammar.lexer.source.charAt(curPos_test))) curPos_test++;

                    if(semantics.grammar.lexer.source.charAt(curPos_test)=='['){
                        // 1. LVal → Ident '[' Exp ']'
                        while(curPos_test<source_len && semantics.grammar.lexer.source.charAt(curPos_test)!=']'){
                            if(semantics.grammar.lexer.source.charAt(curPos_test)=='='){found_equal=true;break;}
                            curPos_test++;
                        }
                        if(!found_equal && curPos_test<source_len && semantics.grammar.lexer.source.charAt(curPos_test)==']'){
                            curPos_test++;      // 跳过']'
                            while(curPos_test<source_len && Character.isWhitespace(semantics.grammar.lexer.source.charAt(curPos_test))) curPos_test++;
                            if(curPos_test<source_len && semantics.grammar.lexer.source.charAt(curPos_test)=='='){
                                found_equal=true;
                            }
                        }
                    }else{
                        // 2. LVal → Ident
                        if(semantics.grammar.lexer.source.charAt(curPos_test)=='=') found_equal=true;
                    }
                    if(found_equal){
                        semantics.grammar.lexer.errors.add(Integer.toString(semantics.grammar.lexer.lineNum)+" h"); //h   //h
                    }
                }
                break;
            }
        }
        //boolean exists=currentSymTabList.stream().anyMatch(symTab -> token.equals(symTab.getName()));
        if(exists){
            return true;     //有：重复声明，报错
        }
        if(insert_or_not) {
            this.insertSymTab(currentSymTabList,token,lexType,level);    //无：填入符号表
        }

        return false;
    }
    public void insertSymTab(ArrayList<SymTable.SymTab> currentSymTabList,String token,LexType lexType,int level){
        SymTable.SymTab newSymTab=new SymTable.SymTab(token,lexType,level);
        if(newSymTab.type==LexType.INT_CONST_IDENFR || newSymTab.type==LexType.INT_CONST_ARRAY_IDENFR
        || newSymTab.type==LexType.CHAR_CONST_IDENFR || newSymTab.type==LexType.CHAR_CONST_ARRAY_IDENFR){
            newSymTab.isConst=true;     //常量
        }
        currentSymTabList.add(newSymTab);    //无：填入符号表
        this.semantics.last_symTab=newSymTab;

        if(this.semantics.function_symTab!=null){       //该符号表对应的标识符为函数声明中的参数
            this.semantics.function_symTab.addFunctionParameters(newSymTab);
        }
    }
    public void saveSymTable(int current_no){    //保存current_no对应的符号表
        IndexTab indexTab=this.indexTabList.get(current_no);
        SymTable symTable=indexTab.pointer;
        indexTab.ecount=symTable.symTabList.size();     //该符号表的登记项个数
    }
}

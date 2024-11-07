package frontend;
import java.util.ArrayList;

public class SymTable {
    public String function_name;    //函数名
    public boolean isFunction;      //是否是函数的符号表
    public ArrayList<SymTab> symTabList;   //当前符号表包含的所有符号项
    public boolean isLoop;      //是否是for循环对应的字符表(用于break和continue语句使用合法性的判定)

    public SymTable() {
        symTabList = new ArrayList<SymTab>();
        this.isLoop = false;
    }
    public static class referred_function_symTab{
        public SymTab symTab;
        public int arguNum;        //函数调用时，已传入的参数个数
        public boolean mismatched;  // 函数参数类型不匹配，只报错一次
        public boolean hasCheckedRParamsType;    // 对于每个实参数做一次检查（避免实参数为表达式时的多次检查）

        public referred_function_symTab(SymTab symTab) {
            this.symTab = symTab;
            this.arguNum = 0;
            this.mismatched = false;
            this.hasCheckedRParamsType = false;
        }
    }

    public static class SymTab {
        public int no;  //符号项序号
        public String name;     //符号名字
        /*符号类型：
            函数型：INT_FUN_IDENFR,CHAR_FUN_IDENFR,VOID_FUN_IDENFR;
            变量型：INT_VAR_IDENFR,INT_VAR_ARRAY_IDENFR,CHAR_VAR_IDENFR,CHAR_VAR_ARRAY_IDENFR;
            常量型：INT_CONST_IDENFR,INT_CONST_ARRAY_IDENFR,CHAR_CONST_IDENFR,CHAR_CONST_ARRAY_IDENFR.
         */
        public LexType type;       //符号类型：IDENFR, INTCON, STRCON, CHRCON
        public int level;
        public Object value;        //值
        public ArrayList<SymTab> parameters;        //函数的参数符号项列表
        public int arrayLength;         //数组长度
        public ArrayList<Object> arrayValues;       //数组元素值
        public int declaredIndex;     //数组在声明阶段赋值的元素最大位置

        public boolean isConst;     //当前字符为常量，不能修改

        public IndexTable.IndexTab functionIndexTab;   //当前SymTab是函数符号时，其参数符号表关联到的索引项
        public boolean redefined;   // 若该函数的标识符出现'b'类错误，则忽略重复定义中的形参情况

        public SymTab(String name, LexType type,int level){
            this.name=name;
            this.type=type;
            this.level=level;
            this.parameters=new ArrayList<SymTab>();
            this.arrayLength=-1;
            this.isConst=false;
            this.declaredIndex=-1;
            this.redefined=false;
        }
        public void initializeArray(int number,boolean defineInt){
            this.arrayLength=number;
            if(defineInt){
                this.arrayValues=new ArrayList<>(number);
                for(int i=0;i<number;i++){
                    this.arrayValues.add(0);
                }
            }else{
                this.arrayValues=new ArrayList<>(number);
                for(int i=0;i<number;i++){
                    this.arrayValues.add('\0');
                }
            }
        }
        public String getName(){
            return this.name;
        }
        public void storeValue(Object value){
            this.value=value;
            //
        }
        public void storeArrayElementValue(Object value,int i){
            arrayValues.set(i,value);
        }
        public void storeStringValue(String token){
            int len=token.length();
            if(len>this.arrayLength){
                System.out.println("Error: token length exceeds array length");return;
            }
            for(int i=0;i<len;i++){
                arrayValues.set(i,token.charAt(i));
            }
        }
        public void addFunctionParameters(SymTab symTab){
            if(this.redefined) return;
            this.parameters.add(symTab);        //该参数的符号项
        }
    }
    public static class IntFunction{

    }
    public static class IntParam{       //函数中的int型参数
        SymTab symTab;
    }
}

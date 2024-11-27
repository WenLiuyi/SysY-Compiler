package llvm.IR.Value;

import java.util.ArrayList;
import frontend.Tree.Exp.Ident;

public class Value {
    public ValueType valueType;    // 语法类型
    public Type type;  // 值类型
    public String name;  //名称

    public ArrayList<Use> userList;
    public ArrayList<Use> usesList;

    public Ident ident;

    public boolean isArray;
    public ArrayList<Object> array;     // 存储int/char型数组，没有时为null
    public int arrayLength;     // 数组长度
    public boolean zeroinitialized;     // 区分：VarDef → Ident '[' ConstExp ']' | Ident '[' ConstExp ']' '=' InitVal

    public String string;
    public int stringLength;    // 字符串长度
    public int stringID;        // 在全局常量字符串中的序号

    // 基本表达式 PrimaryExp → '(' Exp ')' | LVal | Number | Character
    public boolean isIntChar;   // 当前Value是：Number | Character
    public boolean isConst;     // 是否是常量
    public boolean isCalledFunc;    // 当前value，内容为：Ident '(' [FuncRParams] ')'

    public int num;    // int/char类型对应的值

    public void init(){
        this.userList = new ArrayList<Use>();
        this.usesList=new ArrayList<Use>();
        this.num=0;
        this.isIntChar=false;
        this.arrayLength=-1;
        this.zeroinitialized=true;
        this.isArray=false;
        this.isConst=false;
        this.isCalledFunc=false;
        this.stringLength=0;
        this.stringID=-1;
    }
    public Value(){
        init();
    }
    public Value(ValueType valueType,Type type){
        // 局部变量的名称在 LLVM 中没有意义，因为在翻译过后需要用数字对虚拟寄存器重新命名
        this.valueType = valueType;
        this.type = type;
        init();
    }
    public Value(ValueType valueType, Type type, String name) {
        // 名称记录只在全局变量（GlobalVariable, Function）中有意义
        this.valueType = valueType;
        this.type = type;
        this.name = name;
        init();
    }
}

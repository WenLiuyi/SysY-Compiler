package frontend.Tree.Exp;

import frontend.*;
import frontend.Tree.*;
import llvm.IR.Value.*;

public class Ident extends Node{
    public String name;
    public SymTable.SymTab symTab;     // 关联的符号表项
    public boolean isGlobal;        // 是否是全局变量或函数

    public Ident(Grammar grammar,int lineno,int scope_no,String name){
        super(grammar,lineno,scope_no);
        this.name = name;
        this.isGlobal = false;
    }
    public void match(String token,LexType lexType){}

    public Type getType(){
        if(symTab.type.equals(LexType.INT_CONST_IDENFR)||symTab.type.equals(LexType.INT_VAR_IDENFR)||symTab.type.equals(LexType.INT_FUN_IDENFR)){
            return Type.IntegerTyID;
        }
        else if(symTab.type.equals(LexType.INT_CONST_ARRAY_IDENFR)||symTab.type.equals(LexType.INT_VAR_ARRAY_IDENFR)){
            return Type.IntPointerTyID;
        }
        else if(symTab.type.equals(LexType.CHAR_CONST_ARRAY_IDENFR)||symTab.type.equals(LexType.CHAR_VAR_ARRAY_IDENFR)){
            return Type.CharPointerTyID;
        }
        return Type.CharTyID;
    }
    public Type getElementType(){   // 获取数组的元素类型
        Type type=this.getType();
        if(type.equals(Type.IntegerTyID)||type.equals(Type.IntArrayTyID)||type.equals(Type.IntPointerTyID)){
            return Type.IntegerTyID;
        } else return Type.CharTyID;
    }
    public boolean checkType(Type type){
        if(isIntType(type) && isIntType(this.getType())) return true;
        else if(!isIntType(type) && !isIntType(this.getType())) return true;
        else return false;
    }
    public boolean isIntType(Type type){
        if(type.equals(Type.IntegerTyID)||type.equals(Type.IntArrayTyID)||type.equals(Type.IntPointerTyID)){
            return true;
        }return false;
    }
}

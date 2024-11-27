package llvm.IR;

import frontend.Grammar;
import frontend.LexType;
import frontend.SymTable;
import llvm.IR.Value.*;
import frontend.Tree.Exp.*;

import java.util.ArrayList;
import java.util.Stack;

// 栈式符号表
public class SymTableStack {
    public Stack<Element> symTable_Stack;
    public Element curElement;

    public class Element{
        public int scope_no;    // 作用域编号
        public ArrayList<SymTabEntry> symTabEntryList;   // 符号项列表

        public Element(int scope_no) {
            this.scope_no = scope_no;
            this.symTabEntryList = new ArrayList<SymTabEntry>(); // symTabEntryList中记录已分配寄存器的全局/局部变量
        }
    }
    public class SymTabEntry{
        public SymTable.SymTab symTab;
        public Value value;

        public SymTabEntry(SymTable.SymTab symTab, Value value) {
            this.symTab = symTab;
            this.value = value;
        }
    }

    public SymTableStack() {
        this.symTable_Stack = new Stack<Element>();
    }
    public void enterScope(int scope_no){
        Element element = new Element(scope_no);
        this.symTable_Stack.push(element);
        this.curElement = element;
    }
    public void exitScope(){
        this.symTable_Stack.pop();
        int size=this.symTable_Stack.size();
        this.curElement = symTable_Stack.get(size-1);
    }
    public void addSymTab(SymTable.SymTab symTab, Value value){
        // curElement.symTable添加SymTab，与value绑定
        curElement.symTabEntryList.add(new SymTabEntry(symTab, value));
    }
}

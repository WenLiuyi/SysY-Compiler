package frontend.Tree.Const;

import frontend.*;
import frontend.Tree.*;
public class ConstInitVal extends Node{
    public boolean multipleInitVal;
        //区分常表达式和一维数组初值：ConstInitVal-> '{' [ ConstExp { ',' ConstExp } ] '}'
    public ConstInitVal(Grammar grammar,int lineno,int scope_no) {
        super(grammar,lineno,scope_no);
        this.multipleInitVal = false;
    }
    public void match(String token,LexType lexType){

    }
    public void create_ConstExp(Grammar grammar, int lineno,int scope_no) {
        ConstExp constExp = new ConstExp(grammar,lineno,scope_no);
        this.next.add(constExp);constExp.pre=this;this.visited++;
        this.grammar.curNode=constExp;
        constExp.create_AddExp(grammar,lineno,scope_no);     //常量表达式 ConstExp → AddExp
    }
    @Override
    public void return_to_outer(){
        this.grammar.lexer.statements.add("<ConstInitVal>");
        this.grammar.curNode=this.pre;
        //pre.return_to_outer();
    }
}

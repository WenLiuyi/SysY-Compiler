package frontend.Tree.Var;

import frontend.*;
import frontend.Tree.*;
import frontend.Tree.Exp.*;

public class InitVal extends Node{
    public boolean multipleInitVal;
    //区分表达式和一维数组初值：InitVal →  '{' [ Exp { ',' Exp } ] '}'
    public InitVal(Grammar grammar, int lineno,int scope_no) {
        super(grammar,lineno,scope_no);
        this.multipleInitVal = false;
    }
    public void match(String token, LexType lexType){

    }
    public void create_Exp(Grammar grammar, int lineno,int scope_no) {
        Exp exp = new Exp(grammar,lineno,scope_no);
        this.next.add(exp);exp.pre=this;this.visited++;
        this.grammar.curNode=exp;
        exp.create_AddExp(grammar,lineno,scope_no);     //变量表达式 Exp → AddExp
    }
    @Override
    public void return_to_outer(){
        this.grammar.lexer.statements.add("<InitVal>");
        this.grammar.curNode=this.pre;
        //pre.return_to_outer();
    }
}

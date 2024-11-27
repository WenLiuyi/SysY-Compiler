package frontend.Tree.Exp;

import frontend.*;
import frontend.Tree.*;

import java.util.ArrayList;

// 条件表达式 Cond → LOrExp
public class Cond extends Node {
    public Cond(Grammar grammar,int lineno,int scope_no){
        super(grammar,lineno,scope_no);
    }
    public void match(String token,LexType lexType){

    }
    public void create_LOrExp(Grammar grammar,int lineno,int scope_no){
        LOrExp lorExp = new LOrExp(grammar,lineno,scope_no);
        this.next.add(lorExp);lorExp.pre=this;this.visited++;
        this.grammar.curNode=lorExp;
        lorExp.create_LAndExp(grammar,lineno,scope_no);
    }
    @Override
    public void return_to_outer(){
        this.grammar.lexer.statements.add("<Cond>");
        //this.grammar.curNode=this.pre;
    }
}

package frontend.Tree.Exp;

import frontend.*;
import frontend.Tree.*;

import java.util.ArrayList;

// 条件表达式 Cond → LOrExp
public class Cond extends Node {
    public Cond(Grammar grammar,int lineno){
        super(grammar,lineno);
    }
    public void match(String token,LexType lexType){

    }
    public void create_LOrExp(Grammar grammar,int lineno){
        LOrExp lorExp = new LOrExp(grammar,lineno);
        this.next.add(lorExp);lorExp.pre=this;this.visited++;
        this.grammar.curNode=lorExp;
        lorExp.create_LAndExp(grammar,lineno);
    }
    @Override
    public void return_to_outer(){
        this.grammar.lexer.statements.add("<Cond>");
        //this.grammar.curNode=this.pre;
    }
}

package frontend.Tree.Exp;

import frontend.*;
import frontend.Tree.*;

// 相等性表达式 EqExp → RelExp | EqExp ('==' | '!=') RelExp
public class EqExp extends Node{
    public EqExp(Grammar grammar,int lineno){
        super(grammar,lineno);
    }
    public void match(String token,LexType lexType){

    }
    public void create_RelExp(Grammar grammar,int lineno){
        RelExp relExp = new RelExp(grammar,lineno);
        this.next.add(relExp);relExp.pre=this;this.visited++;
        this.grammar.curNode=relExp;
        relExp.create_AddExp(grammar,lineno);
    }
    @Override
    public void return_to_upper(){
        this.grammar.lexer.statements.add("<EqExp>");
        //pre.return_to_upper();
    }
    @Override
    public void return_to_outer(){
        this.grammar.lexer.statements.add("<EqExp>");
        this.grammar.curNode=this.pre;
        pre.return_to_outer();
    }
}

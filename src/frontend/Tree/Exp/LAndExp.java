package frontend.Tree.Exp;

import frontend.*;
import frontend.Tree.*;

// 逻辑与表达式 LAndExp → EqExp | LAndExp '&&' EqExp
public class LAndExp extends Node{
    public LAndExp(Grammar grammar,int lineno){
        super(grammar,lineno);
    }
    public void match(String token,LexType lexType){

    }
    public void create_EqExp(Grammar grammar,int lineno){
        EqExp eqExp = new EqExp(grammar,lineno);
        this.next.add(eqExp);eqExp.pre=this;this.visited++;
        this.grammar.curNode=eqExp;
        eqExp.create_RelExp(grammar,lineno);
    }
    @Override
    public void return_to_upper(){
        this.grammar.lexer.statements.add("<LAndExp>");
        //pre.return_to_upper();
    }
    @Override
    public void return_to_outer(){
        this.grammar.lexer.statements.add("<LAndExp>");
        this.grammar.curNode=this.pre;
        pre.return_to_outer();
    }
}

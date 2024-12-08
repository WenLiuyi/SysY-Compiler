package frontend.Tree.Exp;

import frontend.*;
import frontend.Tree.*;

// 逻辑与表达式 LAndExp → EqExp | LAndExp '&&' EqExp
public class LAndExp extends Node{
    public LAndExp(Grammar grammar,int lineno,int scope_no){
        super(grammar,lineno,scope_no);
    }
    public void match(String token,LexType lexType){

    }
    public void create_EqExp(Grammar grammar,int lineno,int scope_no){
        EqExp eqExp = new EqExp(grammar,lineno,scope_no);
        this.next.add(eqExp);eqExp.pre=this;this.visited++;
        this.grammar.curNode=eqExp;
        eqExp.create_RelExp(grammar,lineno,scope_no);
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

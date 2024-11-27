package frontend.Tree.Exp;

import frontend.*;
import frontend.Tree.*;

// 逻辑或表达式 LOrExp → LAndExp | LOrExp '||' LAndExp
public class LOrExp extends Node{
    public LOrExp(Grammar grammar,int lineno,int scope_no){
        super(grammar,lineno,scope_no);
    }
    public void match(String token,LexType lexType){}

    public void create_LAndExp(Grammar grammar,int lineno,int scope_no){
        LAndExp lAndExp = new LAndExp(grammar,lineno,scope_no);
        this.next.add(lAndExp);lAndExp.pre=this;this.visited++;
        this.grammar.curNode=lAndExp;
        lAndExp.create_EqExp(grammar,lineno,scope_no);
    }
    @Override
    public void return_to_upper(){
        this.grammar.lexer.statements.add("<LOrExp>");
        //pre.return_to_upper();
    }
    @Override
    public void return_to_outer(){
        this.grammar.lexer.statements.add("<LOrExp>");
        this.grammar.curNode=this.pre;
        pre.return_to_outer();
    }
}

package frontend.Tree.Exp;

import frontend.*;
import frontend.Tree.*;

public class MulExp extends Node {
    public MulExp(Grammar grammar, int lineno) {
        super(grammar, lineno);
    }
    public void match(String token, LexType lexType){

    }
    public void create_UnaryExp(Grammar grammar, int lineno){
        UnaryExp unaryExp=new UnaryExp(grammar, lineno);
        this.next.add(unaryExp);unaryExp.pre=this;this.visited++;
        this.grammar.curNode=unaryExp;
        //this.grammar.lexer.statements.add("create UnaryExp");
    }
    @Override
    public void return_to_upper(){
        this.grammar.lexer.statements.add("<MulExp>");
        //this.grammar.curNode=this.pre;
    }
    @Override
    public void return_to_outer(){
        //this.grammar.lexer.statements.add("<MulExp>");
        this.grammar.curNode=this.pre;
        pre.return_to_outer();
    }
}

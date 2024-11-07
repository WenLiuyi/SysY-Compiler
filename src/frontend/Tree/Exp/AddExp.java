package frontend.Tree.Exp;

import frontend.Grammar;
import frontend.LexType;
import frontend.Tree.Node;

public class AddExp extends Node {
    Exp lhs,rhs;
    public AddExp(Grammar grammar, int lineno) {
        super(grammar, lineno);
    }
    public void match(String token, LexType lexType){
    }
    public void create_MulExp(Grammar grammar, int lineno){
        MulExp mulExp=new MulExp(grammar, lineno);
        this.next.add(mulExp);mulExp.pre=this;this.visited++;
        this.grammar.curNode=mulExp;
        //this.grammar.lexer.statements.add("create MulExp");
        mulExp.create_UnaryExp(grammar,lineno);  //乘除模表达式 MulExp → UnaryExp | MulExp ('*' | '/' | '%') UnaryExp
    }
    @Override
    public void return_to_upper(){
        this.grammar.lexer.statements.add("<AddExp>");
        //pre.return_to_upper();
    }
    @Override
    public void return_to_outer(){
        this.grammar.lexer.statements.add("<AddExp>");
        this.grammar.curNode=this.pre;
        pre.return_to_outer();
    }
}

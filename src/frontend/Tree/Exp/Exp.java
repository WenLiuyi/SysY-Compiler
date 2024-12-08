package frontend.Tree.Exp;

import frontend.Grammar;
import frontend.LexType;
import frontend.Tree.Node;

public class Exp extends Node {
    public Exp(Grammar grammar,int lineno,int scope_no) {
        super(grammar,lineno,scope_no);
    }
    public void match(String token, LexType lexType){
        create_AddExp(this.grammar,this.lineno,this.scope_no);
    }
    public void create_AddExp(Grammar grammar,int lineno,int scope_no){
        AddExp addExp = new AddExp(this.grammar,this.lineno,this.scope_no);
        this.next.add(addExp);addExp.pre=this;this.visited++;
        this.grammar.curNode=addExp;
        //this.grammar.lexer.statements.add("create AddExp");
        addExp.create_MulExp(grammar,lineno,scope_no);
    }
    @Override
    public void return_to_upper(){
        this.grammar.lexer.statements.add("<Exp>");
    }
    @Override
    public void return_to_outer(){
        this.grammar.lexer.statements.add("<Exp>");
        this.grammar.curNode=this.pre;
    }
}

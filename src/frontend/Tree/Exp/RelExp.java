package frontend.Tree.Exp;

import frontend.*;
import frontend.Tree.*;

// 关系表达式 RelExp → AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp
public class RelExp extends Node{
    public RelExp(Grammar grammar,int lineno){
        super(grammar,lineno);
    }
    public void match(String token,LexType lexType){

    }
    public void create_AddExp(Grammar grammar,int lineno){
        AddExp addExp = new AddExp(this.grammar,this.lineno);
        this.next.add(addExp);addExp.pre=this;this.visited++;
        this.grammar.curNode=addExp;
        //this.grammar.lexer.statements.add("create AddExp");
        addExp.create_MulExp(grammar,lineno);
    }
    @Override
    public void return_to_upper(){
        this.grammar.lexer.statements.add("<RelExp>");
        //pre.return_to_upper();
    }
    @Override
    public void return_to_outer(){
        this.grammar.lexer.statements.add("<RelExp>");
        this.grammar.curNode=this.pre;
        pre.return_to_outer();
    }
}

package frontend.Tree.Stmt;

import frontend.*;
import frontend.Tree.*;
import frontend.Tree.Exp.Exp;

import java.util.ArrayList;

public class ForStmt extends Node{
    public ForStmt(Grammar grammar,int lineno){
        super(grammar,lineno);
    }
    public void match(String token,LexType lexType){

    }
    public void create_Exp(Grammar grammar,int lineno){
        Exp exp=new Exp(grammar,lineno);
        this.next.add(exp);exp.pre=this;this.visited++;
        this.grammar.curNode=exp;
        //this.grammar.lexer.statements.add(this.grammar.curNode.toString());
        //this.grammar.lexer.statements.add("create Exp");
        exp.create_AddExp(grammar,lineno);
    }
    public void return_to_upper(){
        this.grammar.lexer.statements.add("<ForStmt>");
        this.grammar.curNode=this.pre;  // ForStmt <- Stmt
    }
}

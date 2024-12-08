package frontend.Tree.Func;

import frontend.*;
import frontend.Tree.*;
import frontend.Tree.Const.*;
import frontend.Tree.Exp.*;
import frontend.Tree.Stmt.*;
import frontend.Tree.Var.*;

import java.util.ArrayList;

public class FuncRParams extends Node{
    public int paramNum;        // FuncRParams中的实参个数
    public FuncRParams(Grammar grammar,int lineno,int scope_no) {
        super(grammar,lineno,scope_no);
        this.paramNum=0;
    }
    public void match(String token,LexType lexType){

    }
    public void create_Exp(Grammar grammar,int lineno,int scope_no){
        Exp exp=new Exp(grammar,lineno,scope_no);
        this.next.add(exp);exp.pre=this;this.visited++;
        this.grammar.curNode=exp;
        exp.create_AddExp(grammar,lineno,scope_no);      // Exp -> AddExp -> MulExp -> UnaryExp
        UnaryExp unaryExp=(UnaryExp) this.grammar.curNode;
        unaryExp.isFuncRParams=true;
    }
    @Override
    public void return_to_upper(){
        this.grammar.lexer.statements.add("<FuncRParams>");
        this.grammar.lexer.statements.add("RPARENT )");
        this.grammar.curNode=this.pre;
        pre.return_to_upper();      // FuncRParams <- UnaryExp
    }
}

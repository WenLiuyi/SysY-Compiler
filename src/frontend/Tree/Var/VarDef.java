package frontend.Tree.Var;

import frontend.*;
import frontend.Tree.*;
import frontend.Tree.Const.ConstExp;

import java.util.ArrayList;

public class VarDef extends Node{
    LexType lexType;        //变量数据类型
    public VarDef(Grammar grammar, int lineno){
        super(grammar, lineno);
    }
    public VarDef(Grammar grammar, int lineno, LexType lexType) {
        this.grammar = grammar;
        this.lineno = lineno;
        this.lexType = lexType;
        this.next=new ArrayList<Node>();
        this.visited=-1;
    }
    public void match(String token,LexType lexType){
        if(this.isBType(token)) return;
        if(this.isIdent(lexType)) return;
    }
    public void create_ConstExp(Grammar grammar, int lineno) {
        ConstExp constExp = new ConstExp(grammar,lineno);
        this.next.add(constExp);constExp.pre=this;
        this.grammar.curNode=constExp;
        constExp.create_AddExp(grammar,lineno);     //常量表达式 ConstExp → AddExp
    }

    @Override
    public void return_to_outer(){  //返回路径：Exp <- InitVal <- VarDef
        this.grammar.lexer.statements.add("<VarDef>");
        this.grammar.curNode=this.pre;
        //pre.return_to_outer();
    }
}

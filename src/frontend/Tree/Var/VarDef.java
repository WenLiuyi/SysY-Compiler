package frontend.Tree.Var;

import frontend.*;
import frontend.Tree.*;
import frontend.Tree.Const.ConstExp;

import java.util.ArrayList;

public class VarDef extends Node{
    public LexType lexType;        //变量数据类型
    public VarDef(Grammar grammar, int lineno,int scope_no){
        super(grammar, lineno,scope_no);
    }
    public VarDef(Grammar grammar, int lineno,int scope_no, LexType lexType) {
        super(grammar, lineno,scope_no);
        this.lexType = lexType;
        this.next=new ArrayList<Node>();
    }
    public void match(String token,LexType lexType){
        if(this.isBType(token)) return;
        if(this.isIdent(lexType)) return;
    }
    public void create_ConstExp(Grammar grammar, int lineno,int scope_no) {
        ConstExp constExp = new ConstExp(grammar,lineno,scope_no);
        this.next.add(constExp);constExp.pre=this;this.visited++;
        this.grammar.curNode=constExp;
        constExp.create_AddExp(grammar,lineno,scope_no);     //常量表达式 ConstExp → AddExp
    }

    @Override
    public void return_to_outer(){  //返回路径：Exp <- InitVal <- VarDef
        this.grammar.lexer.statements.add("<VarDef>");
        this.grammar.curNode=this.pre;
        //pre.return_to_outer();
    }
}

package frontend.Tree;

import frontend.Grammar;
import frontend.LexType;
import frontend.Tree.Const.ConstDecl;
import frontend.Tree.Var.VarDecl;

import java.util.ArrayList;

public class Decl extends Node{     //ConstDecl,VarDecl的父类
    LexType lexType;        //数据类型：int或char
    boolean isConstDecl;
    public Decl(){}
    public Decl(Grammar grammar,int lineno) {
        this.grammar = grammar;
        this.lineno = lineno;
        this.next= new ArrayList<Node>();
    }
    public Decl(Grammar grammar,int lineno,boolean isConstDecl) {
        this.grammar = grammar;
        this.lineno = lineno;
        this.isConstDecl = isConstDecl;
        this.next= new ArrayList<Node>();
    }
    public void match(String token,LexType lexType) {
        if(this.isConstDecl) {
            ConstDecl d=new ConstDecl(grammar,lineno);
            this.next.add(d);d.pre=this;
            this.grammar.curNode=d;
            d.match(token,lexType);
        }else{
            VarDecl d=new VarDecl(grammar,lineno);
            this.next.add(d);d.pre=this;
            this.grammar.curNode=d;
        }
        this.lexType = lexType;
    }
}

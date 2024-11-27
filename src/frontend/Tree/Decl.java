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
    public Decl(Grammar grammar,int lineno,int scope_no) {
        super(grammar,lineno,scope_no);
        this.next= new ArrayList<Node>();
    }
    public Decl(Grammar grammar,int lineno,int scope_no,boolean isConstDecl) {
        super(grammar,lineno,scope_no);
        this.isConstDecl = isConstDecl;
        this.next= new ArrayList<Node>();
    }
    public void match(String token,LexType lexType) {
        if(this.isConstDecl) {
            // Decl -> ConstDecl
            ConstDecl d=new ConstDecl(grammar,lineno,scope_no);
            this.next.add(d);d.pre=this;this.visited++;
            this.grammar.curNode=d;
        }else{
            VarDecl d=new VarDecl(grammar,lineno,scope_no);
            this.next.add(d);d.pre=this;this.visited++;
            this.grammar.curNode=d;
        }
        this.lexType = lexType;
    }
}

package frontend.Tree.Stmt;
import frontend.*;
import frontend.Tree.Decl;
import frontend.Tree.Func.FuncDef;
import frontend.Tree.Func.MainFuncDef;
import frontend.Tree.Node;

import java.util.ArrayList;
import java.util.Map;

import frontend.Tree.Exp.*;

public class Block extends Node {
    public Block(Grammar grammar,int lineno) {
        super(grammar,lineno);
    }
    public void match(String token,LexType lexType){
        if(this.isBType(token)){                   //变量声明：Decl->VarDecl
            Decl d=new Decl(grammar,lineno,false);
            this.next.add(d);d.pre=this;this.visited++;
            this.grammar.curNode=d;
            d.match(token,lexType);
        }else if(token.equals("const")){                 //常量声明：Decl->ConstDecl
            Decl d=new Decl(grammar,lineno,true);
            this.next.add(d);d.pre=this;this.visited++;
            this.grammar.curNode=d;
            d.match(token,lexType);
        }else{                                          //语句：Stmt
            Stmt s=new Stmt(grammar,lineno);
            this.next.add(s);s.pre=this;this.visited++;
            this.grammar.curNode=s;
            s.match(token,lexType);
        }
    }
    @Override
    public void return_to_upper(){
        this.grammar.lexer.statements.add("<Block>");
        this.grammar.curNode=this.grammar.curNode.pre;
        if(this.grammar.curNode instanceof MainFuncDef mainFuncDef){
            mainFuncDef.return_to_upper();
        }else if(this.grammar.curNode instanceof FuncDef funcDef){
            funcDef.return_to_upper();
        }else if(this.grammar.curNode instanceof Stmt stmt){
            if(stmt.pre instanceof Stmt stmt_pre && (stmt_pre.isIf || stmt_pre.isElse)){
                stmt.return_to_outer();         // Stmt <- Stmt
                while(this.grammar.curNode instanceof Stmt stmt1 && (stmt1.isIf || stmt1.isElse)){
                    stmt1.return_to_outer();
                }
            }else{
                stmt.return_to_outer();
                this.grammar.curNode.return_to_outer();
            }
        }
    }
}

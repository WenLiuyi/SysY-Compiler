package frontend.Tree.Stmt;

import frontend.*;
import frontend.Tree.*;
import frontend.Tree.Exp.*;

import java.util.ArrayList;

public class Stmt extends Node {
    public boolean isIf;            // 该Stmt为： 'if' (' Cond ')' Stmt [ 'else' Stmt ]
    public boolean isElse;          // 该Stmt为：else部分的Stmt
    public boolean hasElse;         // isIf=true的Stmt，是否有else部分
    public boolean isFor;           // 该Stmt为： 'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt
    public boolean isPrintf;        // 该Stmt为：  'printf''('StringConst {','Exp}')'';'
    public boolean with_first_forStmt;
    public boolean with_cond;
    public boolean with_last_forStmt;
    public int isFor_visited;       // 该Stmt为ForStmt时，访问至第几个';'
    public boolean end_LVal;   // 对于break/continue/getint/getchar, LVal遇到'='退出后，不再创建新的Exp
    public boolean isReturn;    // 该Stmt为：'return' [Exp] ';'

    public boolean isBreak;
    public boolean isContinue;
    public boolean isGetint;
    public boolean isGetchar;

    public Stmt(Grammar grammar,int lineno,int scope_no){
        super(grammar,lineno,scope_no);

        this.isIf=false;
        this.isElse=false;
        this.hasElse=false;

        this.isFor = false;
        this.isPrintf = false;
        this.with_first_forStmt = false;
        this.with_cond = false;
        this.with_last_forStmt = false;
        this.isFor_visited=0;
        this.end_LVal = false;
        this.isReturn = false;

        this.isBreak = false;
        this.isContinue = false;
        this.isGetint = false;
        this.isGetchar = false;
    }
    public void match(String token,LexType lexType){
        //this.grammar.lexer.statements.add(token+" "+lexType);
        if(lexType.equals(LexType.IFTK)){
            //  1. Stmt -> 'if' (' Cond ')' Stmt [ 'else' Stmt ]
            Cond cond=new Cond(this.grammar,this.lineno,this.scope_no);   //Stmt -> Cond
            this.isIf=true;
            this.next.add(cond);cond.pre=this;this.visited++;
            this.grammar.curNode=cond;
            this.grammar.lexer.inCond=true;

        }else if(lexType.equals(LexType.ELSETK)){
            Stmt stmt_else=new Stmt(this.grammar,this.lineno,this.scope_no);  // 'else' Stmt
            this.next.add(stmt_else);stmt_else.pre=this;this.visited++;
            this.grammar.curNode=stmt_else;
            stmt_else.isElse=true;
        }else if(lexType.equals(LexType.BREAKTK) || lexType.equals(LexType.CONTINUETK)){
            this.end_LVal=true;
        }
        else if(lexType.equals(LexType.GETINTTK) || lexType.equals(LexType.GETCHARTK)){
            //this.end_Semicolon=true;
        }else if(lexType.equals(LexType.RETURNTK)){
            this.isReturn=true;
        }else if(lexType.equals(LexType.PRINTFTK)){
            //  'printf''('StringConst {','Exp}')'';'
            this.isPrintf=true;
        }
        else if(this.isIdent(lexType)){
            // 2. Stmt->LVal '=' Exp ';'
            LVal lval=new LVal(this.grammar,this.lineno,this.scope_no);   //左值表达式
            this.next.add(lval);lval.pre=this;this.visited++;
            this.grammar.curNode=lval;
        }else if(lexType.equals(LexType.FORTK)){
            // Stmt ->  'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt
            this.isFor=true;
        }
        else if(lexType.equals(LexType.INTCON) || lexType.equals(LexType.CHRCON)){
            // Stmt->Exp->...-> PrimaryExp->Number/Character
        }
    }
    public void create_Exp(Grammar grammar,int lineno,int scope_no){
        Exp exp=new Exp(grammar,lineno,scope_no);
        this.next.add(exp);exp.pre=this;this.visited++;
        this.grammar.curNode=exp;
        //this.grammar.lexer.statements.add("create Exp");
        exp.create_AddExp(grammar,lineno,scope_no);
    }
    @Override
    public void return_to_upper(){
        this.grammar.lexer.statements.add("<Stmt>");
    }
    @Override
    public void return_to_outer(){
        this.grammar.lexer.statements.add("<Stmt>");
        this.grammar.curNode=this.pre;
    }
}

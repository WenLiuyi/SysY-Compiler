package frontend.Tree.Exp;

import frontend.Grammar;
import frontend.LexType;
import frontend.Tree.Node;
import llvm.IR.Value.Inst.BinaryOpType;

import java.util.ArrayList;

//基本表达式 PrimaryExp → '(' Exp ')' | LVal | Number | Character
public class UnaryExp extends Node {
    public boolean withIdent;
        /*面对'('时，判断上一个是否为Ident，用于区分：
            (1) UnaryExp -> PrimaryExp → '(' Exp ')'
            (2) UnaryExp ->  Ident '(' [FuncRParams] ')'
         */
    public boolean containsFuncRParams;         // UnaryExp -> Ident '(' [FuncRParams] ')'：FuncRParams中的实参个数
    public boolean isFuncRParams;           // 是FuncRParams通过create_Exp创造的，作为实参
    public BinaryOpType opType;

    public UnaryExp(Grammar grammar, int lineno,int scope_no){
        super(grammar, lineno,scope_no);
        this.withIdent=false;
        this.containsFuncRParams=false;
        this.isFuncRParams=false;

        this.opType=BinaryOpType.mul;
    }
    public void match(String token,LexType lexType){
        // Ident有两种情况：
        // (1) UnaryExp -> Ident '(' [FuncRParams] ')'
        // (2) UnaryExp -> LVal ->  Ident ['[' Exp ']']
        if(this.isIdent(lexType)) {this.withIdent=true;return;}
        if(lexType.equals(LexType.INTCON) || lexType.equals(LexType.CHRCON)){
            // UnaryExp -> PrimaryExp -> Number / Character
            PrimaryExp primaryExp=new PrimaryExp(grammar,lineno,scope_no);
            this.next.add(primaryExp);primaryExp.pre=this;this.visited++;
            this.grammar.curNode=primaryExp;
            primaryExp.match(token,lexType);
        }
    }
    //1. UnaryExp -> PrimaryExp → '(' Exp ')'
    public void PrimaryExp_in_parentheses(Grammar grammar,int lineno,int scope_no){
        PrimaryExp primaryExp=new PrimaryExp(grammar,lineno,scope_no);
        this.next.add(primaryExp);primaryExp.pre=this;this.visited++;
        primaryExp.exp_inParentheses=true;
        this.grammar.curNode=primaryExp;
        primaryExp.create_Exp(grammar,lineno,scope_no);
    }
    public void PrimaryExp_as_LVal(Grammar grammar,int lineno,int scope_no){
        PrimaryExp primaryExp=new PrimaryExp(grammar,lineno,scope_no);
        this.next.add(primaryExp);primaryExp.pre=this;this.visited++;
        this.grammar.curNode=primaryExp;
        primaryExp.create_LVal(grammar,lineno,scope_no);
    }
    public void create_UnaryExp(Grammar grammar,int lineno,int scope_no){
        this.grammar.lexer.statements.add("<UnaryOp>");
        UnaryExp new_unaryExp=new UnaryExp(grammar,lineno,scope_no);
        this.next.add(new_unaryExp);new_unaryExp.pre=this;this.visited++;
        this.grammar.curNode=new_unaryExp;
    }
    @Override
    public void return_to_upper(){
        this.grammar.lexer.statements.add("<UnaryExp>");
        this.grammar.curNode=this.pre;
        pre.return_to_upper();
        if(this.pre instanceof UnaryExp){       // 1. 一元表达式 UnaryExp -> UnaryOp UnaryExp
        }
        // 2. 乘除模表达式 MulExp -> UnaryExp | MulExp ('*' | '/' | '%') UnaryExp

    }
    @Override
    public void return_to_outer(){
        this.grammar.lexer.statements.add("<UnaryExp>");
        this.grammar.curNode=this.pre;
        pre.return_to_outer();
    }
}

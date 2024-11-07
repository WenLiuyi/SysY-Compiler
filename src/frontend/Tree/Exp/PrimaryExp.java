package frontend.Tree.Exp;

import frontend.Grammar;
import frontend.LexType;
import frontend.Tree.Node;

import java.util.ArrayList;

//基本表达式 PrimaryExp -> '(' Exp ')' | LVal | Number | Character
public class PrimaryExp extends Node{
    public boolean exp_inParentheses;
    public PrimaryExp(Grammar grammar, int lineno) {
        super(grammar, lineno);
        this.exp_inParentheses = false;
    }
    public void match(String token,LexType lexType){
        if(this.exp_inParentheses){         // 1. PrimaryExp -> '(' Exp ')'
            Exp exp=new Exp(this.grammar,this.lineno);
            this.next.add(exp);exp.pre=this;
            this.grammar.curNode=exp;
            exp.match(token,lexType);
        }
        else if(this.isIdent(lexType)){     // 2. PrimaryExp -> LVal -> Ident ['[' Exp ']']
            LVal lval=new LVal(this.grammar,this.lineno);
            this.next.add(lval);lval.pre=this;
            this.grammar.curNode=lval;
        }
        else if(lexType.equals(LexType.INTCON)){    // 3. PrimaryExp -> Number
            this.grammar.lexer.statements.add("<Number>");
            this.return_to_upper();
        }
        else{                               // 4. PrimaryExp -> Character
            this.grammar.lexer.statements.add("<Character>");
            this.return_to_upper();
        }
    }
    public void create_Exp(Grammar grammar,int lineno){
        Exp exp=new Exp(grammar,lineno);
        this.next.add(exp);exp.pre=this;
        this.grammar.curNode=exp;
        exp.create_AddExp(grammar,lineno);
    }
    public void create_LVal(Grammar grammar,int lineno){
        //调用场景:读到了'['，UnaryExp -> PrimaryExp -> LVal -> Ident ['[' Exp ']']
        LVal lval=new LVal(grammar,lineno);
        this.next.add(lval);lval.pre=this;
        this.grammar.curNode=lval;
        this.create_Exp(grammar,lineno);
    }
    @Override
    //一元表达式 UnaryExp -> PrimaryExp
    public void return_to_upper(){
        this.grammar.lexer.statements.add("<PrimaryExp>");
        this.grammar.curNode=this.pre;
        pre.return_to_upper();
    }
}

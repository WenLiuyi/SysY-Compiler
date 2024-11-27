package frontend.Tree.Exp;

import frontend.Grammar;
import frontend.LexType;
import frontend.Tree.Node;

//基本表达式 PrimaryExp -> '(' Exp ')' | LVal | Number | Character
public class PrimaryExp extends Node{
    public boolean exp_inParentheses;
    public PrimaryExp(Grammar grammar, int lineno,int scope_no) {
        super(grammar, lineno,scope_no);
        this.exp_inParentheses = false;
    }
    public void match(String token,LexType lexType){
        if(this.exp_inParentheses){         // 1. PrimaryExp -> '(' Exp ')'
            Exp exp=new Exp(this.grammar,this.lineno,this.scope_no);
            this.next.add(exp);exp.pre=this;this.visited++;
            this.grammar.curNode=exp;
            exp.match(token,lexType);
        }
        else if(this.isIdent(lexType)){     // 2. PrimaryExp -> LVal -> Ident ['[' Exp ']']
            LVal lval=new LVal(this.grammar,this.lineno,this.scope_no);
            this.next.add(lval);lval.pre=this;this.visited++;
            this.grammar.curNode=lval;
        }
        else if(lexType.equals(LexType.INTCON)){    // 3. PrimaryExp -> Number
            _Number number=new _Number(this.grammar,this.lineno,this.scope_no,Integer.parseInt(token));
            this.next.add(number);number.pre=this;this.visited++;
            this.grammar.lexer.statements.add("<Number>");
            this.return_to_upper();
        }
        else{                               // 4. PrimaryExp -> Character
            _Character character=new _Character(this.grammar,this.lineno,this.scope_no,token.charAt(0));
            this.next.add(character);character.pre=this;this.visited++;
            this.grammar.lexer.statements.add("<Character>");
            this.return_to_upper();
        }
    }
    public void create_Exp(Grammar grammar,int lineno,int scope_no){
        Exp exp=new Exp(grammar,lineno,scope_no);
        this.next.add(exp);exp.pre=this;
        this.grammar.curNode=exp;
        exp.create_AddExp(grammar,lineno,scope_no);
    }
    public void create_LVal(Grammar grammar,int lineno,int scope_no){
        //调用场景:读到了'['，UnaryExp -> PrimaryExp -> LVal -> Ident ['[' Exp ']']
        LVal lval=new LVal(grammar,lineno,scope_no);
        this.next.add(lval);lval.pre=this;
        this.grammar.curNode=lval;
        this.create_Exp(grammar,lineno,scope_no);
    }
    @Override
    //一元表达式 UnaryExp -> PrimaryExp
    public void return_to_upper(){
        this.grammar.lexer.statements.add("<PrimaryExp>");
        this.grammar.curNode=this.pre;
        pre.return_to_upper();
    }
}

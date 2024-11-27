package frontend.Tree.Func;

import frontend.*;
import frontend.Tree.*;
public class FuncFParams extends Node{
    public FuncFParams(Grammar grammar,int lineno, int scope_no) {
        super(grammar,lineno,scope_no);
    }
    public void match(String token, LexType lexType){

    }
    public void add_FuncParam(){
        FuncFParam funcParam=new FuncFParam(this.grammar,this.lineno,this.scope_no);
        this.next.add(funcParam);funcParam.pre=this;this.visited++;
        this.grammar.curNode=funcParam;
    }
    @Override
    public void return_to_outer(){
        this.grammar.lexer.statements.add("<FuncFParams>");
        this.grammar.curNode=this.pre;      // FuncFParams <-
    }
}

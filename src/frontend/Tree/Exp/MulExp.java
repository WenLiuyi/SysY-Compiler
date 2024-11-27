package frontend.Tree.Exp;

import frontend.*;
import frontend.Tree.*;
import llvm.IR.Value.Inst.BinaryOpType;

public class MulExp extends Node {
    public BinaryOpType opType;   // 当前MulExp与上层AddExp的之间的符号："+", "-"

    public MulExp(Grammar grammar, int lineno,int scope_no) {
        super(grammar, lineno,scope_no);
        this.opType = BinaryOpType.add;
    }
    public void match(String token, LexType lexType){

    }
    public void create_UnaryExp(Grammar grammar, int lineno,int scope_no){
        UnaryExp unaryExp=new UnaryExp(grammar, lineno,scope_no);
        this.next.add(unaryExp);unaryExp.pre=this;this.visited++;
        this.grammar.curNode=unaryExp;
        //this.grammar.lexer.statements.add("create UnaryExp");
    }
    @Override
    public void return_to_upper(){
        this.grammar.lexer.statements.add("<MulExp>");
        //this.grammar.curNode=this.pre;
    }
    @Override
    public void return_to_outer(){
        //this.grammar.lexer.statements.add("<MulExp>");
        this.grammar.curNode=this.pre;
        pre.return_to_outer();
    }
}

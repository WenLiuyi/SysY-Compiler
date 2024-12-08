package frontend.Tree.Exp;

import frontend.Grammar;
import frontend.LexType;
import frontend.Tree.Node;
import llvm.IR.Value.Inst.BinaryOpType;

public class AddExp extends Node {
    Exp lhs,rhs;
    public BinaryOpType opType;   // 当前AddExp与上层RelExp的之间的符号：('<' | '>' | '<=' | '>=')

    public AddExp(Grammar grammar, int lineno,int scope_no) {
        super(grammar, lineno,scope_no);
        this.opType = BinaryOpType.lss;
    }
    public void match(String token, LexType lexType){
    }
    public void create_MulExp(Grammar grammar, int lineno,int scope_no){
        MulExp mulExp=new MulExp(grammar, lineno,scope_no);
        this.next.add(mulExp);mulExp.pre=this;this.visited++;
        this.grammar.curNode=mulExp;
        //this.grammar.lexer.statements.add("create MulExp");
        mulExp.create_UnaryExp(grammar,lineno,scope_no);  //乘除模表达式 MulExp → UnaryExp | MulExp ('*' | '/' | '%') UnaryExp
    }
    @Override
    public void return_to_upper(){
        this.grammar.lexer.statements.add("<AddExp>");
        //pre.return_to_upper();
    }
    @Override
    public void return_to_outer(){
        this.grammar.lexer.statements.add("<AddExp>");
        this.grammar.curNode=this.pre;
        pre.return_to_outer();
    }
}

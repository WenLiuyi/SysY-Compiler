package frontend.Tree.Func;

import frontend.Grammar;
import frontend.LexType;
import frontend.Tree.Node;

public class FuncDef extends Node {
    public boolean hasFuncFParams;  // 有参数
    public boolean isVoid;      // 是否是void函数

    public FuncDef(Grammar grammar, int lineno,int scope_no) {
        super(grammar, lineno,scope_no);
        this.hasFuncFParams = false;
        this.isVoid=false;
    }
    public void match(String token, LexType lexType) {

    }
    @Override
    public void return_to_upper(){
        this.grammar.lexer.statements.add("<FuncDef>");
        this.grammar.curNode=this.grammar.curNode.pre;
        this.grammar.curNode.return_to_upper();
    }
}

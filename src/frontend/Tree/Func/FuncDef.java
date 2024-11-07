package frontend.Tree.Func;

import frontend.Grammar;
import frontend.LexType;
import frontend.Tree.Node;

public class FuncDef extends Node {
    public FuncDef(Grammar grammar, int lineno) {
        super(grammar, lineno);
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

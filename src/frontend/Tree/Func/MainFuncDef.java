package frontend.Tree.Func;

import frontend.Grammar;
import frontend.LexType;
import frontend.Tree.Node;
import frontend.Tree.Exp.*;

import java.util.ArrayList;

public class MainFuncDef extends Node {
    public MainFuncDef(Grammar grammar, int lineno,int scope_no) {
        super(grammar, lineno,scope_no);
    }
    public void match(String token, LexType lexType) {
    }
    @Override
    public void return_to_upper(){
        this.grammar.lexer.statements.add("<MainFuncDef>");
        this.grammar.curNode=this.grammar.curNode.pre;
        this.grammar.curNode.return_to_upper();
    }
}

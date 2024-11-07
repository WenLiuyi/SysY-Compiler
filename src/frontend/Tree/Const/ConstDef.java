package frontend.Tree.Const;

import frontend.Grammar;
import frontend.LexType;
import frontend.Tree.Node;

import java.util.ArrayList;

//常量定义 ConstDef → Ident [ '[' ConstExp ']' ] '=' ConstInitVal
public class ConstDef extends Node {
    LexType lexType;        //常量数据类型
    public ConstDef(Grammar grammar, int lineno, LexType lexType) {
       super(grammar, lineno);
        this.lexType = lexType;
    }
    public void match(String token, LexType lexType) {
        if(this.isBType(token)) return;
        if(this.isIdent(lexType)) return;
    }
    public void create_ConstExp(Grammar grammar, int lineno) {
        ConstExp constExp = new ConstExp(grammar,lineno);
        this.next.add(constExp);constExp.pre=this;
        this.grammar.curNode=constExp;
        constExp.create_AddExp(grammar,lineno);     //常量表达式 ConstExp → AddExp
    }

    @Override
    public void return_to_outer(){      //返回路径：ConstExp <- ConstInitVal <- ConstDef
        this.grammar.lexer.statements.add("<ConstDef>");
        this.grammar.curNode=this.pre;
        //pre.return_to_outer();
    }
}

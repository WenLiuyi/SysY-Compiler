package frontend.Tree.Const;

import frontend.Grammar;
import frontend.LexType;
import frontend.Tree.Decl;
import frontend.Tree.Node;

import java.util.ArrayList;

//常量声明 ConstDecl → 'const' BType ConstDef { ',' ConstDef } ';'
public class ConstDecl extends Decl {
    public ConstDecl(Grammar grammar,int lineno){
        super(grammar,lineno);
    }
    public void match(String token, LexType lexType) {
        //if(this.isBType(token)) return;
        ConstDef def=new ConstDef(grammar,lineno,lexType);
        this.next.add(def);def.pre=this;this.visited++;
        this.grammar.curNode=def;           //添加ConstDef
    }
    @Override
    public void return_to_outer(){
        this.grammar.lexer.statements.add("<ConstDecl>");
        this.grammar.curNode=this.grammar.curNode.pre.pre;  //返回路径：ConstDecl <- Decl <- ...
    }
}

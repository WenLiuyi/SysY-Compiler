package frontend.Tree.Var;

import frontend.Grammar;
import frontend.LexType;
import frontend.Tree.*;
import frontend.Tree.Const.ConstDef;
import frontend.Tree.Exp.Ident;

// 变量声明 VarDecl → BType VarDef { ',' VarDef } ';'
public class VarDecl extends Decl {
    public VarDecl(Grammar grammar, int lineno,int scope_no){
        super(grammar, lineno,scope_no);
        this.visited=-1;
    }
    public void match(String token, LexType lexType) {
        //if(this.isBType(token)) return;
        VarDef def=new VarDef(grammar,lineno,scope_no,lexType);
        this.next.add(def);def.pre=this;this.visited++;
        this.grammar.curNode=def;           //添加VarDef
    }
    @Override
    public void return_to_outer(){
        this.grammar.lexer.statements.add("<VarDecl>");
        this.grammar.curNode=this.grammar.curNode.pre.pre;  //返回路径：VarDecl <- Decl <- ...
    }
}

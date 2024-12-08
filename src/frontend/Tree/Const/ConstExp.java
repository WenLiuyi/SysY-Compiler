package frontend.Tree.Const;

import frontend.Grammar;
import frontend.LexType;
import frontend.Tree.Exp.AddExp;
import frontend.Tree.Node;

public class ConstExp extends Node {
    public ConstExp(Grammar grammar, int lineno,int scope_no) {
        super(grammar, lineno,scope_no);
    }
    public void match(String token, LexType lexType){

    }
    public void create_AddExp(Grammar grammar,int lineno,int scope_no){
        AddExp addExp = new AddExp(this.grammar,this.lineno,this.scope_no);
        this.next.add(addExp);addExp.pre=this;this.visited++;
        this.grammar.curNode=addExp;
        addExp.create_MulExp(grammar,lineno,scope_no);   // 加减表达式 AddExp → MulExp | AddExp ('+' | '−') MulExp
    }
    @Override
    public void return_to_upper(){
        this.grammar.lexer.statements.add("<ConstExp>");

    }
    @Override
    public void return_to_outer(){
        this.grammar.lexer.statements.add("<ConstExp>");
        this.grammar.curNode=this.pre;
        if(pre instanceof ConstDef){
            // 常量定义 ConstDef -> Ident [ '[' ConstExp ']' ]
            //pre.return_to_outer();
        }else if(pre instanceof ConstInitVal){
            // 常量初值 ConstInitVal -> ConstExp
            //pre.return_to_outer();
        }
    }
}

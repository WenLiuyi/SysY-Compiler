package frontend.Tree.Func;

import frontend.*;
import frontend.Tree.*;
// 函数形参 FuncFParam → BType Ident ['[' ']']
public class FuncFParam extends Node{
    public String token;
    public LexType lexType;
    public FuncFParam(Grammar grammar, int lineno,int scope_no){
        super(grammar,lineno,scope_no);
    }
    public FuncFParam(Grammar grammar, int lineno, int scope_no, String token, LexType lexType) {
        super(grammar,lineno,scope_no);
        this.token=token;
        this.lexType = lexType;
    }
    public void match(String token,LexType lexType){
        if(this.isBType(token)) return;
        if(this.isIdent(lexType)) return;
    }
    @Override
    public void return_to_outer(){
        this.grammar.lexer.statements.add("<FuncFParam>");
        this.grammar.curNode=this.pre;      // 返回路径：FuncParam <- FuncParams
    }
}

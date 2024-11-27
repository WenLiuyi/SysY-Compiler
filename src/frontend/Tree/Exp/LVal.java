package frontend.Tree.Exp;

import frontend.*;
import frontend.Tree.*;

// 左值表达式 LVal → Ident ['[' Exp ']'] /
public class LVal extends Node{
    public LVal(Grammar grammar,int lineno,int scope_no){
        super(grammar,lineno,scope_no);
    }
    public void match(String token,LexType lexType){
        if(this.isIdent(lexType)) return;
    }

    @Override
    public void return_to_upper(){
        this.grammar.lexer.statements.add("<LVal>");
        this.grammar.curNode=this.pre;
        if(this.pre instanceof PrimaryExp){     // 1. 基本表达式 PrimaryExp -> LVal
            pre.return_to_upper();
        }
        // 2. 语句 ForStmt -> LVal '=' Exp
        // 3. 语句 Stmt → LVal '=' Exp ';'
    }
}

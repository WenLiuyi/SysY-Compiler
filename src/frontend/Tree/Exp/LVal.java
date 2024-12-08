package frontend.Tree.Exp;

import frontend.*;
import frontend.Tree.*;

// 左值表达式 LVal → Ident ['[' Exp ']'] /
public class LVal extends Node{
    public LVal(Grammar grammar,int lineno,int scope_no){
        super(grammar,lineno,scope_no);
    }
    public void match(String token,LexType lexType){
        if(this.isIdent(lexType)){
            // AST中创建并添加Ident结点
            Ident ident=new Ident(this.grammar,this.grammar.lexer.lineNum,this.grammar.lexer.semantics.current_no,token);
            this.next.add(0,ident);ident.pre=this;this.visited++;
            ident.symTab=this.grammar.lexer.semantics.last_symTab;
        }
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

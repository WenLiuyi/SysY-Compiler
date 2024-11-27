package frontend.Tree;

import java.util.ArrayList;
import frontend.*;
import frontend.Tree.Func.FuncDef;
import frontend.Tree.Func.MainFuncDef;

public class CompUnit extends Node{
    //public int curTokenNum;         //当前读入的字符串数
    public boolean isDecl;          //是否可能是声明
    public boolean isFuncDef;
    public boolean isMainFuncDef;

    public CompUnit(Grammar grammar,int lineno,int scope_no){
        super(grammar,lineno,scope_no);
        this.pre=null;
        this.isDecl=true;this.isFuncDef=true;this.isMainFuncDef=true;
        //this.curTokenNum=0;
    }
    public void match(String token,LexType lexType){
        if(lexType.equals(LexType.INTCON)||lexType.equals(LexType.CHRCON)) return;
        else if(token.equals("main")) {                  //主函数定义：MainFuncDef
            MainFuncDef f=new MainFuncDef(grammar,lineno,scope_no);
            this.next.add(f);f.pre=this;this.visited++;
            this.grammar.curNode=f;
        }
        /*else if(token.equals("void")) {                  //void函数定义：FuncDef
            FuncDef f=new FuncDef(grammar,lineno);
            this.next.add(f);f.pre=this;
            this.grammar.curNode=f;
        }*/
        else if(token.equals("const")) {           //常量声明：Decl-> ConstDecl
            Decl d=new Decl(grammar,lineno,scope_no,true);
            this.next.add(d);d.pre=this;this.visited++;
            this.grammar.curNode=d;
            d.match(token,lexType);
        }

    }
    public void match_FuncDef(Grammar grammar,int lineno,int scope_no){      //函数定义：FuncDef
        FuncDef f=new FuncDef(grammar,lineno,scope_no);
        this.next.add(f);f.pre=this;this.visited++;
        this.grammar.curNode=f;
    }
    public void return_to_upper(){
        //this.grammar.lexer.statements.add("<CompUnit>");
    }
}

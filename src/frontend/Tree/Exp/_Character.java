package frontend.Tree.Exp;

import frontend.*;
import frontend.Tree.*;

public class _Character extends Node{
    public char ch;
    public _Character(Grammar grammar,int lineno,int scope_no,char ch){
        super(grammar,lineno,scope_no);
        this.ch=ch;
    }
    public void match(String token,LexType lexType){

    }
}

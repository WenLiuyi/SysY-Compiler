package frontend.Tree.Exp;

import frontend.*;
import frontend.Tree.*;

public class _Character extends Node{
    char ch;
    public _Character(Grammar grammar,int lineno,char ch){
        this.grammar = grammar;
        this.lineno = lineno;
        this.ch=ch;
    }
    public void match(String token,LexType lexType){

    }
}

package frontend.Tree.Exp;

import frontend.*;
import frontend.Tree.*;

public class Number extends Node{
    int num;
    public Number(Grammar grammar,int lineno,int num){
        this.grammar = grammar;
        this.lineno = lineno;
        this.num = num;
    }
    public void match(String token,LexType lexType){

    }
}

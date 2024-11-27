package frontend.Tree.Exp;

import frontend.*;
import frontend.Tree.*;

public class _Number extends Node{
    public int num;
    public _Number(Grammar grammar, int lineno, int scope_no, int num){
        super(grammar,lineno,scope_no);
        this.num = num;
    }
    public void match(String token,LexType lexType){

    }
}

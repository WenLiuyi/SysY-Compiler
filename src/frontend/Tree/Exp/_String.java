package frontend.Tree.Exp;

import frontend.Grammar;
import frontend.LexType;
import frontend.Tree.*;

public class _String extends Node {
    public String string;
    public boolean isPrintedInt;    // %d
    public boolean isPrintedChar;   // %c
    public boolean isPrintedString; // %s
    public _String(Grammar grammar, int lineno, int scope_no, String string){
        super(grammar,lineno,scope_no);
        this.isPrintedInt=false;this.isPrintedChar=false;this.isPrintedString=false;
        this.string = string;
    }
    public void match(String token, LexType lexType){

    }
}

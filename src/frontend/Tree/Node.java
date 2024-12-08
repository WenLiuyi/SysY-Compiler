package frontend.Tree;

import frontend.*;

import java.util.ArrayList;

public abstract class Node {         //语法树节点
    public int lineno;         //行号
    public Node pre;           //父节点
    public ArrayList<Node> next;       //一系列子节点
    public Grammar grammar;
    public int visited;     //当前访问的非终结符序号
    public int scope_no;    // 作用域编号

    public Node(){}
    public abstract void match(String token,LexType lexType);
    public Node(Grammar grammar,int lineno,int scope_no) {
        this.grammar = grammar;
        this.lineno = lineno;
        this.next = new ArrayList<Node>();
        this.visited = -1;
        this.scope_no = scope_no;
    }
    public Node(Grammar grammar,int lineno,int scope_no,Node pre) {
        this.grammar = grammar;
        this.lineno = lineno;
        this.pre = pre;
        this.visited = -1;
        this.scope_no = scope_no;
    }
    public boolean isBType(String token){
        return (token.equals("int")||token.equals("char"));
    }
    public boolean isIdent(LexType lexType){
        return lexType.equals(LexType.IDENFR);
    }
    public void return_to_upper(){}          // 返回至上一个节点
    public void return_to_outer(){}
}

package frontend;

import java.util.Stack;
import frontend.Tree.*;

public class Grammar {
    public class Element{
        public LexType lexType;
        public int curPos;

        public Element(LexType lexType, int curPos) {
            this.lexType = lexType;
            this.curPos = curPos;
        }
    }
    public Stack<Element> parentheses_Stack;     //小括号栈
    public Stack<Element>brackets_Stack;        //中括号栈
    public Stack<Element> braces_Stack;         //大括号栈
    public Stack<Element> function_parentheses_Stack;       //当前函数的小括号栈

    public Node TreeHead;
    public Node curNode;

    public Lexer lexer;

    public Grammar(Lexer lexer) {
        this.lexer = lexer;
    }

    public void initialize(){
        this.parentheses_Stack = new Stack<>();
        this.brackets_Stack = new Stack<>();
        this.braces_Stack = new Stack<>();
        this.function_parentheses_Stack=new Stack<>();

        this.TreeHead=new CompUnit(this,1,0);
        this.curNode=this.TreeHead;
    }
    public boolean process_Function_Parentheses(char c,int curPos){
        if(c=='('){
            function_parentheses_Stack.push(new Element(LexType.LPARENT,curPos));
            return true;
        }else return function_Parentheses_Match();
    }
    public boolean function_Parentheses_Match(){
        if(this.function_parentheses_Stack.isEmpty()) return false;  //无可匹配的左括号
        function_parentheses_Stack.pop();    //弹出一个左括号和当前右括号匹配
        return true;
    }
    public boolean process_Parentheses(char c,int curPos){
        if(c=='(') {
            parentheses_Stack.push(new Element(LexType.LPARENT,curPos));  //'('压入栈
            return true;
        }
        else return parentheses_Match();        //')'匹配
    }
    public boolean parentheses_Match(){
        if(this.parentheses_Stack.isEmpty()) return false;  //无可匹配的左括号
        parentheses_Stack.pop();    //弹出一个左括号和当前右括号匹配
        return true;
    }
    public boolean process_Brackets(char c,int curPos){
        if(c=='['){
            brackets_Stack.push(new Element(LexType.LBRACK,curPos));    //'['压入栈
            return true;
        }
        else return brackets_Match();
    }
    public boolean brackets_Match(){
        if(this.brackets_Stack.isEmpty()) return false;
        brackets_Stack.pop();
        return true;
    }
    public boolean process_Braces(char c,int curPos){
        if(c=='{'){
            braces_Stack.push(new Element(LexType.LBRACE,curPos));
            return true;
        }
        else return braces_Match();
    }
    public boolean braces_Match(){
        if(this.braces_Stack.isEmpty()) return false;
        braces_Stack.pop();
        return true;
    }
}

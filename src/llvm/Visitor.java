package llvm;
import frontend.Tree.*;
import frontend.Tree.Const.ConstDecl;
import frontend.Tree.Func.FuncDef;
import frontend.Tree.Func.MainFuncDef;
import frontend.Tree.Stmt.Block;
import frontend.Tree.Stmt.Stmt;
import frontend.Tree.Var.VarDecl;

public class Visitor {
    public Node curNode;

    void visit(CompUnit compUnit) {
        // 编译单元 CompUnit → {Decl} {FuncDef} MainFuncDef
        int len=compUnit.visited;
        for(int i=0;i<=len;i++){
            curNode=compUnit.next.get(i);
            if(curNode instanceof Decl) visit_Decl((Decl)curNode);
            else if(curNode instanceof FuncDef) visit_FuncDef((FuncDef)curNode);
            else if(curNode instanceof MainFuncDef) visit_MainFuncDef((MainFuncDef)curNode);
        }
    }
    void visit_Decl(Decl decl) {}
    void visit_ConstDecl(ConstDecl constDecl) {}
    void visit_VarDecl(VarDecl varDecl) {}
    void visit_FuncDef(FuncDef funcDef) {}
    void visit_MainFuncDef(MainFuncDef mainFuncDef) {
        // 主函数定义 MainFuncDef → 'int' 'main' '(' ')' Block
        curNode=mainFuncDef.next.get(0);
        visit_Block((Block)curNode);
    }
    void visit_Block(Block block) {
        // 语句块 Block → '{' { BlockItem } '}'
        // 语句块项 BlockItem → Decl | Stmt
        int len=block.visited;
        for(int i=0;i<=len;i++){
            curNode=block.next.get(i);
            if(curNode instanceof Decl) visit_Decl((Decl)curNode);
            else if(curNode instanceof Stmt) visit_Stmt((Stmt)curNode);
        }
    }
    void visit_Stmt(Stmt stmt) {}
}

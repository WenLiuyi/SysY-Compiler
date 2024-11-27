import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import frontend.*;
import frontend.Tree.*;
import frontend.Tree.Func.FuncDef;
import frontend.Tree.Func.MainFuncDef;
import frontend.Tree.Stmt.*;
import frontend.Tree.Var.VarDecl;
import llvm.Generator;

public class Compiler {
    Analyzer analyzer;  //分析器：完成词法、语法、语义分析

    public static void main(String[] args) {
        Analyzer analyzer=new Analyzer("testfile.txt");
        analyzer.analyze();
        Generator generator=new Generator(analyzer);
        generator.generate();
    }
}
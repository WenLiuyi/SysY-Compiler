import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import frontend.*;
import frontend.Tree.*;
import frontend.Tree.Func.FuncDef;
import frontend.Tree.Func.MainFuncDef;
import frontend.Tree.Stmt.*;
import frontend.Tree.Var.VarDecl;
import llvm.Generator;

public class Compiler {
    Analyzer analyzer;  //分析器：完成词法、语法、语义分析
    boolean updateFiles=true;

    public static void main(String[] args) {
        /*try {
            // 删除文件
            deleteFiles("main.ll", "out_ans.ll", "out_res.ll");

            // 执行命令
            executeCommand("clang -O0 -S -emit-llvm main.c -o main.ll");
            executeCommand("llvm-lib main.ll lib.ll -S -o out_ans.ll");
            executeCommand("llvm-lib lib.ll llvm_ir.ll -S -o out_res.ll");

            System.out.println("命令执行完成！");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }*/

        Analyzer analyzer=new Analyzer("testfile.txt");
        analyzer.analyze();
        //if(!analyzer.lexer.errors.isEmpty()) return;
        Generator generator=new Generator(analyzer);
        generator.generate();
    }

    // 删除文件的方法
    public static void deleteFiles(String... filenames) throws IOException {
        for (String filename : filenames) {
            Path path = Paths.get(filename);
            if (Files.exists(path)) {
                Files.delete(path);
                System.out.println(filename + " 被删除");
            } else {
                System.out.println(filename + " 不存在");
            }
        }
    }

    // 执行命令的方法
    public static void executeCommand(String command) throws IOException, InterruptedException {
        Process process = Runtime.getRuntime().exec(command);

        // 输出命令执行结果
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }

        // 等待命令执行完成
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            System.err.println("命令执行失败，退出码：" + exitCode);
        }
    }
}
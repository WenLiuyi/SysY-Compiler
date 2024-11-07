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

public class Compiler {
    public String filePath;     //源程序文件路径
    public Lexer lexer;         //词法分析器

    public static void main(String[] args) {
        String filePath = "testfile.txt";
        ReadFile reader = new ReadFile();
        WriteFile writer = new WriteFile();
        reader.read(filePath);
        List<String> lines = reader.lines;

        Lexer lexer = new Lexer();
        lexer.initialize();

        //writer.write("parser.txt",lexer.statements);
        int flag = 0;
        for (int i = 0; i < lines.size(); i++) {        //逐行词法分析
            System.out.println(i + 1);
            System.out.println(lines.get(i));
            lexer.source = lines.get(i);
            lexer.lineNum = i + 1;      //行号
            lexer.curPos = 0;
            lexer.token = null;
            lexer.lexType = null;
            lexer.in_Single_Notation = false;

            char error = lexer.next(i + 1);

            // j型错误
            if (lexer.right_Parentheses) {
                lexer.errors.add(lexer.lineNum + " j");
                lexer.right_Parentheses = false;
                lexer.source = lexer.source + ")";
                lexer.next(i + 1);
            } else if (lexer.inCond) {
                lexer.errors.add(Integer.toString(lexer.lineNum) + " j");
                lexer.source = lexer.source + ")";
                lexer.inCond = false;
                lexer.next(i + 1);
            }

            // i型错误：
            if ((!(lexer.grammar.curNode instanceof CompUnit) && !(lexer.grammar.curNode instanceof Block)
                    && !(lexer.grammar.curNode instanceof Stmt) && !(lexer.grammar.curNode instanceof FuncDef)
                    && !(lexer.grammar.curNode instanceof MainFuncDef)) || lexer.checkSemicolon) {
                lexer.checkSemicolon=false;
                int j = i + 1;
                boolean inMultipleNotation = false;
                while (j < lines.size()) {
                    String line = lines.get(j);
                    int pos = 0, len = lines.get(j).length();
                    if (len == 0) {j++;continue;}
                    while (pos < len && Character.isWhitespace(line.charAt(pos))) pos++;
                    if(pos==len) {j++;continue;}
                    if(inMultipleNotation) {
                        while(pos<len){
                            if(line.charAt(pos-1)=='*' && line.charAt(pos)=='/') {inMultipleNotation=false;break;}
                            pos++;
                        }if(inMultipleNotation)  {j++;continue;}
                    }
                    if(line.charAt(pos)=='/'){
                        pos++;
                        if(pos<len){
                            if(line.charAt(pos)=='/') continue;     // 单行注释
                            else if(line.charAt(pos)=='*'){         // 多行注释
                                inMultipleNotation=true;
                                while(pos<len){
                                    if(line.charAt(pos-1)=='*' && line.charAt(pos)=='/') {
                                        inMultipleNotation=false;pos++;break;
                                    }pos++;
                                }
                            }
                        }
                    }
                    while (pos < len && Character.isWhitespace(line.charAt(pos))) pos++;
                    if(pos==len) {j++;continue;}
                    if (line.charAt(pos) != ';') {
                        // 错误类型i：缺少';'
                        lexer.errors.add(Integer.toString(lexer.lineNum) + " i");
                        lexer.source = lexer.source + ";";
                        lexer.next(i + 1);
                    }
                    break;
                }
            }
            // g型错误
            //lexer.errors.add(Integer.toString(lexer.lineNum)+" "+lexer.checkReturn);
            if (lexer.checkReturn) {  //
                String cur_line;boolean found_return=false;
                for (int lineNum = i-1; lineNum >= 0; lineNum--) {
                    cur_line = lines.get(lineNum);
                    int curPos_test = 0, len = cur_line.length();
                    while (curPos_test < len && Character.isWhitespace(cur_line.charAt(curPos_test))) {
                        curPos_test++;
                    }
                    if (curPos_test == len) continue;       // 跳过空白行

                    String token="";char c;
                    while(curPos_test < len &&
                            (lexer.Digit_or_Letter_or_Underline(cur_line.charAt(curPos_test)))){
                        // 下一个符号是字母或下划线或数字
                        c = cur_line.charAt(curPos_test++);
                        token += c;
                    }
                    if (!token.equals("return")) lexer.errors.add(Integer.toString(lexer.lineNum) + " g");  //g
                    break;
                }
                lexer.checkReturn=false;
            }
        }
        if(!lexer.errors.isEmpty()){
            writer.write("error.txt",lexer.errors);

        }else {
            /*if(lexer.statements.isEmpty()) return;
            lexer.statements.add("<CompUnit>");
            writer.write("parser.txt",lexer.statements);*/
            List<String> symbols=new ArrayList<String>();

            for(int i=0;i<lexer.semantics.indexTable.indexTabList.size();i++){
                IndexTable.IndexTab indexTab=lexer.semantics.indexTable.indexTabList.get(i);
                for(int j=0;j<indexTab.pointer.symTabList.size();j++){
                    SymTable.SymTab symTab=indexTab.pointer.symTabList.get(j);
                    if(symTab.name.equals("getint") || symTab.name.equals("getchar")||symTab.name.equals("main")) continue;
                    symbols.add((indexTab.no+1)+" "+symTab.name+" "+lexer.semantics.symbolType(symTab));
                    //lexer.errors.add((indexTab.no+1)+" "+symTab.name+" "+lexer.semantics.symbolType(symTab)+" "+Integer.toString(symTab.parameters.size()));
                }
            }
            writer.write("symbol.txt",symbols);
            //writer.write("info.txt",lexer.info);
        }
    }
}
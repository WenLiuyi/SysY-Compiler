package llvm;

import frontend.Analyzer;
import frontend.Tree.Node;

public class Generator {
    public Analyzer analyzer;
    public Node treeHead;   // 语法树

    public Generator(Analyzer analyzer) {
        this.analyzer = analyzer;
        this.treeHead=analyzer.lexer.grammar.TreeHead;
    }
}

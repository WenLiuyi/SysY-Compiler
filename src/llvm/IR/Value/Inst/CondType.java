package llvm.IR.Value.Inst;

public enum CondType {
    /* 条件的类型：
        一定为真，True；一定为假，False；待定，UnKnown
     */
    True,
    False,
    UnKnown,
}

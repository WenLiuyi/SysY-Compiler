package llvm.IR.Value.Inst;

public enum BinaryOpType {
    /* 包含的 LLVM IR 指令形式 :
     a + - *  / % b,
     a >  <  >= <= == != b
     */
    add,       //+
    sub,      //-
    mul,       //*
    div,        // /
    mod,        //%
    lss,        //<
    leq,        //<=
    gre,        //>
    geq,        //>=
    eq,        //==
    ne,        //!=
    not,        //!
}

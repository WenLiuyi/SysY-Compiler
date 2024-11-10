package llvm.IR.Value;

public enum Type {
    // Primitive types
    VoidTyID,       // 空返回值
    LabelTyID,      // 标签类型

    // Derived types
    IntegerTyID,    // 整数类型
    FloatTyID,      // 浮点数类型
    FunctionTyID,   // 函数类型
    PointerTyID     // 指针类型
}

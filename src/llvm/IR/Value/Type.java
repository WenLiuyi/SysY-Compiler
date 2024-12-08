package llvm.IR.Value;

public enum Type {
    // Primitive types
    VoidTyID,       // 空返回值
    LabelTyID,      // 标签类型

    // Derived types
    IntegerTyID,    // 整数类型
    FloatTyID,      // 浮点数类型
    CharTyID,       // 字符类型
    StringTyID,     // 字符串类型
    FunctionTyID,   // 函数类型
    IntPointerTyID,     // int型指针类型
    CharPointerTyID,    // char型指针类型
    BooleanTyID,

    IntArrayTyID,   // int型数组
    CharArrayTyID,
}

package llvm.IR.Value;

import java.util.ArrayList;

public class Value {
    private ValueType valueType;    // 语法类型
    protected Type type;  // 值类型
    protected String name;  //名称

    protected ArrayList<User> userList;
    protected ArrayList<Use> useList;
}

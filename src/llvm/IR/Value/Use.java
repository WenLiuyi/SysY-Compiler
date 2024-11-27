package llvm.IR.Value;

// 记录指令和操作数的关系：指令和每个操作数之间都是一个 Use 关系
public class Use extends Value {
    public Value usee;  // 操作数
    User user;  // 指令
    public Use(Value usee, User user) {
        this.usee = usee;
        this.user = user;
    }
}

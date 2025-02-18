package backend.Mips.Operand;

public class Imm implements MSOperand {     // 立即数类
    private int num;
    // 指向另一个 Imm 对象的引用，它用于表示立即数的组合（比如 5 + 3 形式的组合）
    private Imm anotherImm = null;
    public static Imm ZERO = new Imm(0);

    public Imm(Imm imm, int num) {
        this.num = num;anotherImm = imm;
    }
    public Imm(int num) {
        this.num = num;
    }
    public void setNum(int num) {
        this.num = num;
    }

    @Override
    public boolean needsAllocate() {
        return false;   // 立即数不需要分配寄存器
    }

    @Override
    public String toString() {
        if(anotherImm == null){
            // 1. 如果 anotherImm 为 null（即没有组合其他立即数），则直接返回 num 的值
            return String.valueOf(num);
        }
        // 2. 如果 anotherImm 不为 null，表示当前立即数是由两个部分组合成的，
        // 它返回 anotherImm 的数值加上当前的 num，即 anotherImm.getNum() + num 的值
        return String.valueOf(anotherImm.getNum() + num);
    }

    public String getNumSub(){      // 返回当前立即数的负数值的字符串表示
        if(anotherImm == null){
            // 1. 如果没有组合其他立即数（anotherImm == null），则返回 num 的负值
            return String.valueOf(-num);
        }
        // 2. 如果有组合的立即数（anotherImm != null），则返回 anotherImm 的数值加上 num 的和的负值
        return String.valueOf(-(anotherImm.getNum() + num));
    }

    public int getNum() {
        if(anotherImm == null) return num;
        return anotherImm.num + num;
    }
}


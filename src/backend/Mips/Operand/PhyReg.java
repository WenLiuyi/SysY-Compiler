package backend.Mips.Operand;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class PhyReg implements Reg {
    public String name; // 寄存器的名称，例如 "t0", "sp" 等
    public int index;   //寄存器的索引,nameMap的key

    public static final HashMap<Integer, PhyReg> globalRegsMap = new HashMap<>();  // 全局物理寄存器的映射

    public static PhyReg getGlobalReg(Integer integer) {
        return globalRegsMap.get(integer);
    }

    public static Set<Integer> getRegIndexes() {
        return globalRegsMap.keySet();  // 返回 globalRegsMap 中所有键的集合，这些键代表物理寄存器的编号
    }

    public PhyReg(String name) {
        this.name = name;
        this.index = -1;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public String toString() {
        return "$" + name;
    }

    @Override
    public boolean needsAllocate() {
        return false;
    }

    public static final PhyReg zero = new PhyReg("zero");   // 0 的常量寄存器
    public static final PhyReg v0 = new PhyReg("v0");       // 寄存器 $v0，用于存储返回值
    public static final PhyReg sp = new PhyReg("sp");       // 寄存器 $sp，栈指针
    public static final PhyReg ra = new PhyReg("ra");       // 寄存器 $ra，返回地址
    public static final PhyReg a0 = new PhyReg("a0");       // 寄存器 $a0，参数寄存器

    // $t0 - $t7, $t8 - $t9：临时寄存器，用于基本块内的变量，发生函数调用时不必保存
    public static final PhyReg t0 = new PhyReg("t0");
    public static final PhyReg t1 = new PhyReg("t1");
    public static final PhyReg t2 = new PhyReg("t2");
    public static final PhyReg t3 = new PhyReg("t3");
    public static final PhyReg t4 = new PhyReg("t4");
    public static final PhyReg t5 = new PhyReg("t5");
    public static final PhyReg t6 = new PhyReg("t6");
    public static final PhyReg t7 = new PhyReg("t7");
    public static final PhyReg t8 = new PhyReg("t8");
    public static final PhyReg t9 = new PhyReg("t9");

    // $s0 - $s7（saved）：全局寄存器，这些寄存器用于跨基本块的变量，往往需要在发生函数调用时进行保存
    public static final PhyReg s0 = new PhyReg("s0");
    public static final PhyReg s1 = new PhyReg("s1");
    public static final PhyReg s2 = new PhyReg("s2");
    public static final PhyReg s3 = new PhyReg("s3");
    public static final PhyReg s4 = new PhyReg("s4");
    public static final PhyReg s5 = new PhyReg("s5");
    public static final PhyReg s6 = new PhyReg("s6");
    public static final PhyReg s7 = new PhyReg("s7");
    // $v0 - $v1：作为函数返回值，一般返回值只使用 $v0，当返回值超过 32 位时需要同时使用 $v1
    public static final PhyReg v1 = new PhyReg("v1");
    // 参数寄存器（a1 到 a3）
    public static final PhyReg a1 = new PhyReg("a1");
    public static final PhyReg a2 = new PhyReg("a2");
    public static final PhyReg a3 = new PhyReg("a3");
    // 内核寄存器（k0 和 k1）
    public static final PhyReg k0 = new PhyReg("k0");
    public static final PhyReg k1 = new PhyReg("k1");
    // 全局指针寄存器（gp）
    public static final PhyReg gp = new PhyReg("gp");
    // 帧寄存器 (fp): 存储函数帧的指针，即进入函数时的栈底的位置
    public static final PhyReg fp = new PhyReg("fp");

    public static List<PhyReg> tempRegs = new ArrayList<PhyReg>() {{
        // 包含特定临时寄存器（k0, k1, gp）的 List, 用于在编译过程中存储临时数据
        add(t0);
        add(t1);
        add(t2);
        add(t3);
        add(t4);
        add(t5);
        add(t6);
        add(t7);
        add(t8);
        add(t9);
    }};

    public static List<PhyReg> getTempRegs() {
        return tempRegs;
    }

    static {
        // 将一组物理寄存器（如 s0 到 s7, t0 到 t9 等）与整数键（0 到 24）对应，填充到 globalRegsMap 中
        globalRegsMap.put(0, s0);
        globalRegsMap.put(1, s1);
        globalRegsMap.put(2, s2);
        globalRegsMap.put(3, s3);
        globalRegsMap.put(4, s4);
        globalRegsMap.put(5, s5);
        globalRegsMap.put(6, s6);
        globalRegsMap.put(7, s7);
        globalRegsMap.put(8, v1);
        globalRegsMap.put(9, a1);
        globalRegsMap.put(10, a2);
        globalRegsMap.put(11, a3);
//        globalRegsMap.put(12, k0);
//        globalRegsMap.put(13, k1);
//        globalRegsMap.put(14, gp);
        globalRegsMap.put(15, t0);
        globalRegsMap.put(16, t1);
        globalRegsMap.put(17, t2);
        globalRegsMap.put(18, t3);
        globalRegsMap.put(19, t4);
        globalRegsMap.put(20, t5);
        globalRegsMap.put(21, t6);
        globalRegsMap.put(22, t7);
        globalRegsMap.put(23, t8);
        globalRegsMap.put(24, t9);
    }

//    private static HashSet<Integer> regsIndexes = new HashSet<>(nameMap.keySet());

//    public static HashSet<Integer> getRegsIndexes() {
//        return regsIndexes;
//    }

//    public PhyReg(int index) {
//        this.name = nameMap.get(index);
//        this.index = index;
//    }
}


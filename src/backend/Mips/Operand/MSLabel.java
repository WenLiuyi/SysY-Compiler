package backend.Mips.Operand;

public class MSLabel implements MSOperand{
    public String label;

    public MSLabel(String label) {
        this.label = label;
    }

    @Override
    public boolean needsAllocate() {
        return false;
    }
    @Override
    public String toString() {
        return label;
    }
}


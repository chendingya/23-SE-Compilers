package symbolTable;

public class ArrayType implements Type{
    private int elementNum;
    private Type type;
    public ArrayType(Type type, int elementNum) {
        this.elementNum = elementNum;
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }
    @Override
    public String toString() {
        return "array[" + type + "]";
    }

}

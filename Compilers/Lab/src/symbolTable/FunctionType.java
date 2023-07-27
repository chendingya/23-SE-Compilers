package symbolTable;

import java.util.ArrayList;

public class FunctionType implements Type{
    public BasicTypeSymbol retType;
    public ArrayList<Type> paramsType;
    public FunctionType(BasicTypeSymbol type) {
        this.retType = type;
        this.paramsType = new ArrayList<>();
    }

    public void setParamsType(ArrayList<Type> paramsType) {
        this.paramsType = paramsType;
    }
}

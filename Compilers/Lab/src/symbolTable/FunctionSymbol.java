package symbolTable;

import java.util.ArrayList;

public class FunctionSymbol extends BaseScope implements Symbol   {
    public FunctionType functionType;
    public FunctionSymbol(String name, Scope enclosingScope) {
        super(name, enclosingScope);
    }

    public void setFunctionType(FunctionType functionType) {
        this.functionType = functionType;
    }

    @Override
    public Type getType() {
        return functionType;
    }

}

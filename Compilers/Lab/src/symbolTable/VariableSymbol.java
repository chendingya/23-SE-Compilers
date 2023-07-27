package symbolTable;

import symbolTable.BaseSymbol;
import symbolTable.Type;

public class VariableSymbol extends BaseSymbol {
    public VariableSymbol(String name, Type type) {
        super(name, type);
    }
}

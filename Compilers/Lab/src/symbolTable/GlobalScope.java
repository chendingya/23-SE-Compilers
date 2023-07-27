package symbolTable;

import symbolTable.BaseScope;
import symbolTable.BasicTypeSymbol;

public class GlobalScope extends BaseScope {
    public GlobalScope(Scope enclosingScope) {
        super("symbolTable.GlobalScope", enclosingScope);
        define(new BasicTypeSymbol("int"));
        define(new BasicTypeSymbol("void"));
    }
}
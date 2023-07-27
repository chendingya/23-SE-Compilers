package symbolTable;

import symbolTable.BaseScope;

public class LocalScope extends BaseScope {
    public LocalScope(Scope enclosingScope) {
        super("symbolTable.LocalScope", enclosingScope);
    }
}
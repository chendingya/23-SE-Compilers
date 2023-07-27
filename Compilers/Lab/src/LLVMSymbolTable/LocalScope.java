package LLVMSymbolTable;

public class LocalScope extends BaseScope {
    public LocalScope(Scope enclosingScope) {
        super("Scope.Scope.LocalScope", enclosingScope);
    }
}

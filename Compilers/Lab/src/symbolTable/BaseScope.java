package symbolTable;


import java.util.LinkedHashMap;
import java.util.Map;

public class BaseScope implements Scope {
    private final Scope enclosingScope;
    public final Map<String, Symbol> symbols = new LinkedHashMap<>();
    private String name;

    public BaseScope(String name, Scope enclosingScope) {
        this.name = name;
        this.enclosingScope = enclosingScope;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Scope getEnclosingScope() {
        return this.enclosingScope;
    }

    public Map<String, Symbol> getSymbols() {
        return this.symbols;
    }

    @Override
    public void define(Symbol symbol) {
        this.symbols.put(symbol.getName(), symbol);
        System.out.println("+ " + symbol.getName());
    }

    @Override
    public Symbol resolve(String name) {
        Symbol symbol = symbols.get(name);
        if (symbol != null) {
            System.out.println("find " + symbol.getName());
            System.out.println("    currentScope is " + this.name);
            return symbol;
        }

        if (enclosingScope != null) {
            return enclosingScope.resolve(name);
        }

        System.out.println("Cannot find " + name);
        System.out.println("    resolve now in currentScope is " + this.name);
        return null;
    }

}

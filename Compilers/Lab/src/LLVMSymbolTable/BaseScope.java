package LLVMSymbolTable;

import org.bytedeco.llvm.LLVM.LLVMTypeRef;
import org.bytedeco.llvm.LLVM.LLVMValueRef;

import java.util.LinkedHashMap;
import java.util.Map;

public class BaseScope implements Scope {
    private final Scope enclosingScope;
    public final Map<String, LLVMValueRef> values = new LinkedHashMap<>();
    private final Map<String, LLVMTypeRef> types = new LinkedHashMap<>();
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

    public Map<String, LLVMValueRef> getValue() {
        return this.values;
    }

    @Override
    public void define(String name, LLVMValueRef llvmValueRef, LLVMTypeRef llvmTypeRef) {
        //		System.out.println(this.scopeName + "+(" + name + ", " + llvmValueRef + "," + llvmTypeRef + ")");
        values.put(name, llvmValueRef);
        types.put(name, llvmTypeRef);
    }

    @Override
    public LLVMValueRef resolve(String name) {

        LLVMValueRef value = values.get(name);
        if (value != null) {
            System.out.println("find " + value.toString());
            System.out.println("    currentScope is " + this.name);
            return value;
        }

        if (enclosingScope != null) {
            return enclosingScope.resolve(name);
        }

        System.out.println("Cannot find " + name);
        System.out.println("    resolve now in currentScope is " + this.name);
        return null;
    }

    @Override
    public LLVMTypeRef getType(String name) {
        LLVMTypeRef typeRef = types.get(name);
        if (typeRef != null) {
            return typeRef;
        }

        if (enclosingScope != null) {
            return enclosingScope.getType(name);
        }

        return null;
    }

}

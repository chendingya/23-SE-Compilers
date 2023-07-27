package symbolTable;

import java.util.Map;

public interface Scope {
    public String getName();//名称

    public void setName(String name);//设置名字

    public Scope getEnclosingScope();//外部作用域

    public Map<String, Symbol> getSymbols();//拿定义符号

    public void define(Symbol symbol);
    //在作用域中定义符号
    //把新解析到的符号名put到map里
    public Symbol resolve(String name);
    //根据名称查找、解析
    //如果符号在多个作用域中有重名，需要解析到底指的是哪一个
    //先在当前作用域找，找不到再去父作用域找
    //到了全局作用域中还是找不到，就报错
}


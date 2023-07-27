import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;
import symbolTable.*;

import java.util.ArrayList;

public class Visitor extends SysYParserBaseVisitor<Void>{
    /**
     * Visitor在访问每个节点的子节点前会调用visitChildren函数，本次实验中你可以通过重写这个函数来实现语法树的打印
     * Visitor在访问每个终结符节点时会调用visitTerminal函数，打印终结符及终结符的高亮可以通过重写这个函数来实现
     * 对于每一个语法规则都存在一个对应的visit函数，如exp规则对应的函数为visitExp，在后续实验中需要访问特定节点并做相应处理，此时需要调用这种函数
     *
     * 每个语法规则都有其编号，根据在.g4文件中出现的顺序确定
     * 通过语法规则节点(node)的上下文(ruleContext)可以获得语法规则的编号
     * 通过SysYParser的静态成员ruleNames可以获得所有语法规则的名称，下标与编号对应
     */
    /**
     * //保留字
     * CONST[orange], INT[orange], VOID[orange],IF[orange], ELSE[orange], WHILE[orange],BREAK[orange], CONTINUE[orange], RETURN[orange],
     * //运算符
     * PLUS[blue], MINUS[blue], MUL[blue], DIV[blue],MOD[blue],ASSIGN[blue], EQ[blue], NEQ[blue],LT[blue],
     * GT[blue],LE[blue],GE[blue], NOT[blue], AND[blue],OR[blue],
     * //标识符
     * IDENT[red],
     * //数字与字符串
     * INTEGER_CONST[green],
     */
    public static StringBuilder output = new StringBuilder("");
    private boolean semanticError = false;
    private GlobalScope globalScope = null;
    //current指针
    private Scope currentScope = null;
    private Scope fatherScope = null;
    //局部作用域计数器
    private int localScopeCounter = 0;
    private StringBuilder printmes = new StringBuilder("");

    public String[] color = new String[] {
            null , "[orange]", "[orange]" , "[orange]", "[orange]", "[orange]", "[orange]", "[orange]", "[orange]", "[orange]",
            "[blue]", "[blue]", "[blue]", "[blue]", "[blue]", "[blue]", "[blue]", "[blue]", "[blue]",
            "[blue]", "[blue]", "[blue]", "[blue]", "[blue]", "[blue]", null, null, null,
            null, null, null, null, null, "[red]",
            "[green]", null, null, null
    };
    public boolean[] printif = new boolean[] {
            false,true, true, true, true, true, true, true, true, true,
            true, true, true, true, true, true, true, true, true,
            true, true, true, true, true, true, false, false, false,
            false, false, false, false, false, true,
            true, false, false, false
    };
    public boolean getSemanticError() {
        return semanticError;
    }
    public void printMes() {
        System.err.println(printmes);
    }
    @Override
    public Void visitChildren(RuleNode node) {
        Void result = this.defaultResult();
        int n = node.getChildCount();
        RuleContext now = node.getRuleContext();
        int bianhao = now.getRuleIndex();
        String name = SysYParser.ruleNames[bianhao];


        //name
        printmes.append(output.toString() + name.substring(0, 1).toUpperCase() + name.substring(1) + '\n');
//        System.err.println(output.toString() + name.substring(0, 1).toUpperCase() + name.substring(1));
        output.append("  ");

        for(int i = 0; i < n && this.shouldVisitNextChild(node, result); ++i) {
            ParseTree c = node.getChild(i);
            Void childResult = c.accept(this);
            result = this.aggregateResult(result, childResult);
        }
        output = new StringBuilder(output.substring(2));

        return result;
    }
    //highlight
    @Override
    public Void visitTerminal(TerminalNode node) {
        String n = (node.getParent()).toString();
        //name correct
        String name = node.getText();
        int xiabiao = node.getSymbol().getType();
        if ( xiabiao > 0 && printif[xiabiao])  {
            int num = 0;

            String a = "";
            if(xiabiao <= 9) a = "[orange]";
            if(xiabiao <= 24 && xiabiao > 9) a = "[blue]";

            if(xiabiao == 33) a = "[red]";
            if(xiabiao == 34) a = "[green]";
//            System.err.print(output);
            printmes.append(output);
            //name
            if (xiabiao == 34) {
                if (name.charAt(0) == '0' && name.length() != 1 && name.charAt(1) != 'x') {
                    printmes.append(Integer.valueOf(name.substring(1), 8) +  " "  + SysYLexer.ruleNames[xiabiao - 1]);
//                    System.err.print(Integer.valueOf(name.substring(1), 8) +  " "  + SysYLexer.ruleNames[xiabiao - 1]);
                } else if (name.length() != 1 && name.charAt(0) == '0' && name.charAt(1) == 'x') {
                    printmes.append(Integer.parseInt(name.substring(2), 16)  +  " " + SysYLexer.ruleNames[xiabiao - 1]);
//                    System.err.print(Integer.parseInt(name.substring(2), 16)  +  " " + SysYLexer.ruleNames[xiabiao - 1]);
                } else {
                    printmes.append(name + " " + SysYLexer.ruleNames[xiabiao - 1]);
//                    System.err.print(name + " " + SysYLexer.ruleNames[xiabiao - 1]);
                }

            } else {
                printmes.append(name + " " + SysYLexer.ruleNames[xiabiao - 1]);
//                System.err.print(name + " " + SysYLexer.ruleNames[xiabiao - 1]);
            }
            printmes.append(a + "\n");
//            System.err.println(a);
        }

        return this.defaultResult();
    }

    @Override
    public Void visitProgram(SysYParser.ProgramContext ctx) {
//        System.out.println("visit program");

        // 进入新的 Scope
        // todo:
        //全局作用域：根节点所以没有父节点
        globalScope = new GlobalScope(null);
        //进入即更新currentScope
        currentScope = globalScope;
        Void ret = super.visitProgram(ctx);
        // 回到上一层 Scope
        currentScope = currentScope.getEnclosingScope();
        return ret;
    }

    @Override
    public Void visitFuncDef(SysYParser.FuncDefContext ctx) {
//        System.out.println("visit funcdef");

        /**
         *报告 Error type 4
         **/
        //拿到函数名、返回类型名
        //还需要解析类型名
        String retType = ctx.funcType().getText();
        //if already have: error4
        //not have: correct
        String funcName = ctx.IDENT().getText();
        Symbol symbol = globalScope.resolve(ctx.IDENT().toString());

        Void ret = null;

        System.out.println("funcName of funcdel is "+ funcName);
        System.out.println("funcType of funcdel is "+ retType);

        if (symbol != null) {
            System.err.println("Error type 4 at Line " + getLineNo(ctx) + ":already have been defined:" + funcName);
            semanticError = true;
        } else {
            //create a new funcScope
            //当前函数是currentScope内嵌的作用域，所以父节点即为currentScope
            FunctionType functionType = new FunctionType(new BasicTypeSymbol(retType));
            FunctionSymbol func = new FunctionSymbol(funcName, currentScope);
            func.setFunctionType(functionType);

            //函数本身也是符号，放到父作用域的符号表中
            currentScope.define(func);
            //进入，更新
            currentScope = func;
            //next is functionParams
            // 进入新的 Scope，定义新的 Symbol
             ret = super.visitFuncDef(ctx);

            // 回到上一层 Scope
            currentScope = currentScope.getEnclosingScope();
        }

        return ret;
    }

    @Override
    public Void visitBlock(SysYParser.BlockContext ctx) {
//        System.out.println("visit block");
//        System.out.println("current scope " + currentScope.getName());

        //局部作用域
        LocalScope localScope = new LocalScope(currentScope);
        //名字为原来的名字加上一个counter
        String localScopeName = localScope.getName() + localScopeCounter;
        localScope.setName(localScopeName);
        localScopeCounter++;

        //进入局部作用域
        currentScope = localScope;
        // 进入新的 Scope
        Void ret = super.visitBlock(ctx);
        // 回到上一层 Scope
        currentScope = currentScope.getEnclosingScope();
        return ret;
    }

    int getLineNo(ParserRuleContext ctx) {
        return ctx.getStart().getLine();
    }
    private String toDecimalInteger(String str) {
        if (str.charAt(0) == '0' && str.length() != 1 && str.charAt(1) != 'x') {
            str = String.valueOf(Integer.parseInt(str, 8));
        } else if (str.length() != 1 && str.charAt(0) == '0' && (str.charAt(1) == 'x' || str.charAt(1) == 'X')) {
            str = String.valueOf(Integer.parseInt(str.substring(2), 16));
        }
        return str;
    }
    public boolean definedSymbol(String name, String typeName) {
        // 报告 Error type 3:变量重复声明：
        // 声明全局变量时与函数同名，
        // 声明全局变量时与已声明的全局变量同名，声明局部变量与已声明的相同作用域的局部变量同名
        if (currentScope instanceof GlobalScope) {
            Symbol symbol = ((GlobalScope) currentScope).symbols.get(name);
            if (symbol != null) {
                return true;
            }
        } else if (currentScope instanceof FunctionSymbol){
            Symbol symbol = ((FunctionSymbol) currentScope).symbols.get(name);
            if (symbol != null) {
                return true;
            }
        } else if (currentScope instanceof LocalScope) {
            //一直向上找到functionSymbol：如果找到symbol了，就停止；
            // 没找到symbol，就继续向上
            //如果没找到functionSymbol，但是已经到了GlobalScope就停止
            Symbol symbol = ((LocalScope) currentScope).symbols.get(name);
            if (symbol != null) {
                return true;
            }
            if (currentScope.getEnclosingScope() instanceof FunctionSymbol) {
                symbol = ((FunctionSymbol) currentScope.getEnclosingScope()).symbols.get(name);
                if (symbol != null) {
                    return true;
                }
            }
//            Scope scope = currentScope;
//            while (!(scope instanceof LocalScope)) {
//                System.out.println("        in definedsymbol current scope is " + scope.getName());
//                Symbol symbol = ((LocalScope) scope).symbols.get(name);
//                if (symbol != null) {
//                    return true;
//                }
//                scope = scope.getEnclosingScope();
//            }
//
//            System.out.println("        not find in local scope");
//
//            if (scope instanceof FunctionSymbol) {
//                Symbol symbol = ((FunctionSymbol) scope).symbols.get(name);
//                if (symbol != null) {
//                    return true;
//                }
//            }
        }
        return false;
//        Symbol symbol = currentScope.resolve(name);
//        if (symbol != null) {
//            return true;
//        } else {
//            return false;
//        }
    }
    @Override
    public Void visitVarDecl(SysYParser.VarDeclContext ctx) {

//        System.out.println("visit vardecl");
//        System.out.println("current scope " + currentScope.getName());


        //variable declare
        String type = ctx.bType().getText();
        for (SysYParser.VarDefContext varDefContext : ctx.varDef()) {
            //declare时varType：funcType或者arrayType已经加入globalScope

            Type varType = (Type) globalScope.resolve(type);
            // 报告 Error type 3:变量重复声明：声明全局变量时与函数同名，声明全局变量时与已声明的全局变量同名，声明局部变量与已声明的相同作用域的局部变量同名
            String name = varDefContext.IDENT().getText();
            System.out.println("name of variable is " + name);

            if (definedSymbol(name, type)) {
                semanticError = true;
                System.err.println("Error type 3 at Line " + getLineNo(varDefContext) +  ":variable name have been defined");
                continue;
            }
            //嵌套结构
            //a[2][3]:
            //father: type = array[] num = 2
            //children: type = array num = 3
            for (SysYParser.ConstExpContext constExpContext : varDefContext.constExp()) {
                int elementNum = Integer.parseInt(toDecimalInteger(constExpContext.getText()));
                System.out.println("element num is " + elementNum);
                System.out.println("now array type is " + varType.toString());
                varType = new ArrayType(varType, elementNum);
            }

            if (varDefContext.ASSIGN() != null) {
                // 报告 Error type 5:赋值号两侧类型不匹配：赋值号两侧的类型不相同
                SysYParser.ExpContext expContext = varDefContext.initVal().exp();
				if (expContext != null) {
					Type initValType = getExpType(expContext);
					if (!initValType.toString().equals("noType")
                            && !varType.toString().equals(initValType.toString())) {
                        System.err.println("Error type 5 at Line " + getLineNo(varDefContext) +  ":type.Type mismatched for assignment.");
					}
				}
            }
            VariableSymbol varSymbol = new VariableSymbol(name, varType);
			currentScope.define(varSymbol);
        }
        return super.visitVarDecl(ctx);
    }

    @Override
    public Void visitConstDecl(SysYParser.ConstDeclContext ctx) {

//        System.out.println("visit constdecl");
//        System.out.println("current scope " + currentScope.getName());


        // 结构同 visitVarDecl
        //Const declare
        String type = ctx.bType().getText();
        for (SysYParser.ConstDefContext varDefContext : ctx.constDef()) {
            //declare时varType：funcType或者arrayType已经加入globalScope
            Type varType = (Type) globalScope.resolve(type);

            // 报告 Error type 3:变量重复声明：声明全局变量时与函数同名，声明全局变量时与已声明的全局变量同名，声明局部变量与已声明的相同作用域的局部变量同名
            String name = varDefContext.IDENT().getText();
            System.out.println("name of const variable is " + name);
            if (definedSymbol(name, type)) {
                semanticError = true;
                System.err.println("Error type 3 at Line " + getLineNo(varDefContext) +  ":variable name have been defined");
                continue;
            } else {
                //嵌套结构
                //a[2][3]:
                //father: type = array[] num = 2
                //children: type = array num = 3
                for (SysYParser.ConstExpContext constExpContext : varDefContext.constExp()) {
                    int elementNum = Integer.parseInt(toDecimalInteger(constExpContext.getText()));
                    varType = new ArrayType(varType, elementNum);
                }
            }

            if (varDefContext.ASSIGN() != null) {
                // 报告 Error type 5:赋值号两侧类型不匹配：赋值号两侧的类型不相同
                SysYParser.ConstExpContext constExpContext = varDefContext.constInitVal().constExp();
                if (constExpContext != null) {
                    //get exp type
                    Type initValType = getExpType(constExpContext.exp());
                    if (!initValType.toString().equals("noType")
                            && !varType.toString().equals(initValType.toString())) {
                        System.err.println("Error type 5 at Line " + getLineNo(varDefContext) +  ":type.Type mismatched for assignment.");
                    }
                }
            }
            VariableSymbol varSymbol = new VariableSymbol(name, varType);
            currentScope.define(varSymbol);
        }

        return super.visitConstDecl(ctx);
    }

    @Override
    public Void visitFuncFParam(SysYParser.FuncFParamContext ctx) {
//        System.out.println("visit funcfparam");
//        System.out.println("current scope " + currentScope.getName());


        // 报告 Error type 3:变量重复声明：声明全局变量时与函数同名，声明全局变量时与已声明的全局变量同名，声明局部变量与已声明的相同作用域的局部变量同名
        String name = ctx.IDENT().getText();
        String type = ctx.bType().getText();
        System.out.println("params type is " + type);
        System.out.println("params name is " + name);

        Type varType = (Type) globalScope.resolve(type);
        for (TerminalNode ptr : ctx.L_BRACKT()) {
			varType = new ArrayType(varType,0);
		}
        Symbol symbol = currentScope.resolve(name);
        if (definedSymbol(name, type)) {
            System.err.println("Error type 3 at Line " + getLineNo(ctx) + ":params var have already been defined" + name);
            semanticError = true;
        } else {
            VariableSymbol variableSymbol = new VariableSymbol(name, varType);
            currentScope.define(variableSymbol);
            //保存函数参数
            ((FunctionSymbol) currentScope).functionType.paramsType.add(varType);
        }
        // 定义新的 Symbol
        Void ret = super.visitFuncFParam(ctx);
        return ret;
    }

    private Type getLValType(SysYParser.LValContext ctx) {
//        System.out.println("get lval type " + ctx.IDENT());

        String varName = ctx.IDENT().getText();
        Symbol symbol = currentScope.resolve(varName);
        if (symbol == null) {
//
            System.out.println("    lval type is noTYpe" );
//
            return new BasicTypeSymbol("noType");
        } else {
            //if symbol is int : getType return int
            Type varType = symbol.getType();
            for (SysYParser.ExpContext ptr: ctx.exp()) {
                if (varType instanceof ArrayType) {
                    varType = ((ArrayType) varType).getType();
                } else {
                    //no exception aside from int and arrayType
//
                    System.out.println("    lval type is noTYpe" );
//
                    return new BasicTypeSymbol("noType");
                }
            }
//
            System.out.println("    lval type is " + varType.toString() );
//
            return varType;
        }
    }


    @Override
    public Void visitLVal(SysYParser.LValContext ctx) {
//        System.out.println("visit lval " + ctx.IDENT().getText());
//        System.out.println("current scope " + currentScope.getName());


        String name = ctx.IDENT().getText();
        Symbol symbol = currentScope.resolve(name);

        // 报告 Error type 1:变量未声明：使用了没有声明的变量
        if (symbol == null) {
            System.err.println("Error type 1 at Line " + getLineNo(ctx) + ":Undefined variable:" + name);
            semanticError = true;
            return null;
        } else {
            Type type = symbol.getType();
            int dimensions = ctx.exp().size();
            System.out.println("dimensions is " + dimensions);
            //已经拆掉一层[]了，三层括号的第一个type是两层括号的array
            //notype代表出错了
            for (int i = 0; i < dimensions; i++) {
                // 报告 Error type 9:对非数组使用下标运算符：对int型变量或函数使用下标运算符
                if (type instanceof ArrayType) {
                    // get next type
                    type = ((ArrayType) type).getType();
                    name = ctx.exp(i).getText();
                } else {
                    System.err.println("Error type 9 at Line " + getLineNo(ctx) + ":Not an array:" + name);
                    semanticError = true;
                }
            }
        }
        return super.visitLVal(ctx);
    }

    @Override
    public Void visitStmt(SysYParser.StmtContext ctx) {
//        System.out.println();
//        System.out.println("visit stmt");
//        System.out.println("current scope " + currentScope.getName());
//        System.out.println("*****   line is " + getLineNo(ctx));


        if (ctx.ASSIGN() != null) {
            Type lType = getLValType(ctx.lVal());
            Type rType = getExpType(ctx.exp());
            if (lType instanceof FunctionType) {
                // 报告 Error type 11:赋值号左侧非变量或数组元素：对函数进行赋值操作
                System.err.println("Error type 11 at Line " + getLineNo(ctx) + ":The left-hand side of an assignment must be a variable.");
                semanticError = true;
            } else if (!lType.toString().equals("noType")
                    && !rType.toString().equals("noType")
                    && !lType.toString().equals(rType.toString())) {
                // (l == int && r == func) || (l == func && r == int)
                // 报告 Error type 5:赋值号两侧类型不匹配：赋值号两侧的类型不相同
                System.err.println("Error type 5 at Line " + getLineNo(ctx) + ":The left-hand side of an assignment must be a variable.");
                semanticError = true;
            }

        } else if (ctx.RETURN() != null) {
            // 报告 Error type 7:返回值类型不匹配：返回值类型与函数声明的返回值类型不同
            // normal return void
            Type retType = new BasicTypeSymbol("void");
            if (ctx.exp() != null) {
                retType = getExpType(ctx.exp());
            }
            Scope Scopeptr = currentScope;
            //find the functionSymbol
            while (!(Scopeptr instanceof FunctionSymbol)) {
                Scopeptr = Scopeptr.getEnclosingScope();
            }

            Type expectedType = ((FunctionType) ((FunctionSymbol) Scopeptr).getType()).retType;
            //real retType != expected Symbol retType
            if (!retType.toString().equals("noType")
                    && !expectedType.toString().equals("noType")
                    && !retType.toString().equals(expectedType.toString())) {
                System.err.println("Error type 7 at Line " + getLineNo(ctx) + ":type.Type mismatched for return.");
                semanticError = true;
            }
        }
        return super.visitStmt(ctx);
    }

    private Type getExpType(SysYParser.ExpContext ctx) {
        System.out.println("getExpType " + ctx.IDENT());
        System.out.println("current scope " + currentScope.getName());


        if (ctx.IDENT() != null) { // IDENT L_PAREN funcRParams? R_PAREN

//
            System.out.println("    exp is func: ident, params");
//
            String name = ctx.IDENT().getText();
            Symbol symbol = currentScope.resolve(name);
            if (symbol != null && symbol.getType() instanceof FunctionType) {
                FunctionType functionType = (FunctionType) symbol.getType();
                //func : name match and params match : ret resolve() type
                //else return notype
                ArrayList<Type> paramsType = functionType.paramsType;
                ArrayList<Type> expParamsType = new ArrayList<>();
                if (ctx.funcRParams() != null) {
                    for (SysYParser.ParamContext paramContext: ctx.funcRParams().param()) {
                        expParamsType.add(getExpType(paramContext.exp()));
                    }
                }
                if (expParamsType.equals(paramsType)) {
                    System.out.println("exp == func: rettype is " + functionType.retType.toString());
                    return functionType.retType;
                }
            }
        } else if (ctx.L_PAREN() != null) { // L_PAREN exp R_PAREN
//
            System.out.println("into func ()");
//
            return getExpType(ctx.exp(0));
        } else if (ctx.unaryOp() != null) { // unaryOp exp
            return getExpType(ctx.exp(0));
        } else if (ctx.lVal() != null) { // lVal
//
            System.out.println("into lval array");
//
            return getLValType(ctx.lVal());
        } else if (ctx.number() != null) { // number
            System.out.println("    exptype is int");
            return new BasicTypeSymbol("int");
        } else if (ctx.MUL() != null || ctx.DIV() != null || ctx.MOD() != null || ctx.PLUS() != null || ctx.MINUS() != null) {
            Type operate1 = getExpType(ctx.exp(0));
            Type operate2 = getExpType(ctx.exp(1));
            if (operate1.toString().equals("int") && operate2.toString().equals("int")) {
                System.out.println("exptype is " + operate2.toString());
                return operate2;
            }
        }
        System.out.println("exptype is noType");
        return new BasicTypeSymbol("noType");
    }

    @Override
    public Void visitExp(SysYParser.ExpContext ctx) {
        System.out.println("visit exp");
        System.out.println("current scope " + currentScope.getName());

        if (ctx.IDENT() != null) { // IDENT L_PAREN funcRParams? R_PAREN
            String name = ctx.IDENT().getText();
            Symbol symbol = currentScope.resolve(name);
            if (symbol == null) {
                // 报告 Error type 2:函数未定义：使用了没有声明和定义的函数
                System.err.println("Error type 2 at Line " + getLineNo(ctx) + ":Undefined function: " + name);
                semanticError = true;
            } else if (!(symbol.getType() instanceof FunctionType)) {
                // 报告 Error type 10:对变量使用函数调用：对变量使用函数调用运算符
                System.err.println("Error type 10 at Line " + getLineNo(ctx) + ":Not a function: " + name);
                semanticError = true;
            } else {
                // 报告 Error type 8:函数参数不适用：函数参数的数量或类型与函数声明的参数数量或类型不一致
                    //为了降低实验难度，我们保证测试用例中的函数参数不会为多维（二维及以上）数组。
                FunctionType type = (FunctionType) symbol.getType();
                ArrayList<Type> paramsType = type.paramsType;
                ArrayList<Type> inputParamsType = new ArrayList<>();
                if (ctx.funcRParams() != null) {
                    for (SysYParser.ParamContext paramContext : ctx.funcRParams().param()) {
                        inputParamsType.add(getExpType(paramContext.exp()));
                    }
                }
                boolean flag = true;
                int len1 = paramsType.size();
                int len2 = inputParamsType.size();
                if (len1 != len2) {
                    flag = false;
                } else {
                    //len1 == len2
                    for (int i = 0; i < len1; i++) {
                        Type paramType = paramsType.get(i);
                        Type argType = inputParamsType.get(i);
                        if (!paramType.toString().equals(argType.toString())) {
                            flag = false;
                        }
                    }
                    for (Type tmp : paramsType) {
                        if (tmp.toString().equals("noType")) {
                            flag = true;
                            break;
                        }
                    }
                    for (Type tmp : inputParamsType) {
                        if (tmp.toString().equals("noType")) {
                            flag = true;
                            break;
                        }
                    }
                }
                if (!flag) {
                    System.err.println("Error type 8 at Line " + getLineNo(ctx) + ":Function is not applicable for arguments: " + name);
                    semanticError = true;
                }
            }
        } else if (ctx.unaryOp() != null) { // unaryOp exp
            // 报告 Error type 6:运算符需求类型与提供类型不匹配：运算符需要的类型为int却提供array或function等
            Type expType = getExpType(ctx.exp(0));
            if (!expType.toString().equals("int")) {
                System.err.println("Error type 6 at Line " + getLineNo(ctx) + ":Type mismatched for operands");
                semanticError = true;
            }
        } else if (ctx.MUL() != null || ctx.DIV() != null
                || ctx.MOD() != null || ctx.PLUS() != null || ctx.MINUS() != null) {
            // 报告 Error type 6
            Type type1 = getExpType(ctx.exp(0));
            Type type2 = getExpType(ctx.exp(1));
            if (!type1.toString().equals("noType")
                    && !type2.toString().equals("noType")
                    && (!type1.toString().equals("int") || !type2.toString().equals("int"))) {
                System.err.println("Error type 6 at Line " + getLineNo(ctx) + ":Type mismatched for operands");
                semanticError = true;
            }
        }
        return super.visitExp(ctx);
    }

    @Override
    public Void visitCond(SysYParser.CondContext ctx) {
        System.out.println("visit cond");
        System.out.println("current scope " + currentScope.getName());


        // 报告 Error type 6:运算符需求类型与提供类型不匹配：运算符需要的类型为int却提供array或function等
        if (ctx.exp() == null) {
            if (!getCondType(ctx).toString().equals("int")) {
                System.err.println("Error type 6 at Line " + getLineNo(ctx) + ":Type mismatched for operands");
                semanticError = true;
            }
        }
        return super.visitCond(ctx);
    }

    public Type getCondType(SysYParser.CondContext ctx) {

        System.out.println("get cond type");

        Type type;
        if (ctx.exp() != null) {
            type = getExpType(ctx.exp());
        } else {
            Type operator1 = getCondType(ctx.cond(0));
            Type operator2 = getCondType(ctx.cond(1));
            if (operator1.toString().equals("int") && operator2.toString().equals("int")) {
                return operator1;
            } else {
                type = new BasicTypeSymbol("noType");
            }
        }
        return type;
    }
}

import LLVMSymbolTable.GlobalScope;
import LLVMSymbolTable.LocalScope;
import LLVMSymbolTable.Scope;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.llvm.LLVM.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Stack;

import static org.bytedeco.llvm.global.LLVM.*;

public class LLVMvisitor extends SysYParserBaseVisitor<LLVMValueRef> {
    //创建module
    private LLVMModuleRef module = LLVMModuleCreateWithName("module");

    //初始化IRBuilder，后续将使用这个builder去生成LLVM IR
    private LLVMBuilderRef builder = LLVMCreateBuilder();

    //考虑到我们的语言中仅存在int一个基本类型，可以通过下面的语句为LLVM的int型重命名方便以后使用
    private LLVMTypeRef i32Type = LLVMInt32Type();
    //创建另一种类型为void

    private final LLVMTypeRef voidType = LLVMVoidType();
    //创建一个常量,这里是常数0
    private LLVMValueRef zero = LLVMConstInt(i32Type, 0, /* signExtend */ 0);
    //数组 type
    private final LLVMTypeRef intPointerType = LLVMPointerType(i32Type, 0);
    private boolean arrayAddr = false;

    //建立一张表，将函数名和返回类型对应，因为没有override，所以可以不用考虑作用域
    private final Map<String, String> retTypeMap = new LinkedHashMap<>();
    //用于判断函数是否已经生成了llvm的返回ret语句
    private boolean isReturned = false;

    //currentScope
    private Scope currentScope = null;
    private GlobalScope globalScope = null;
    private int localScopeCounter = 0;
    private LLVMBasicBlockRef blockNow = null;
    //目前所处函数的引用
    private LLVMValueRef currentFunction = null;

    //while中判断层数的栈
    /**
     * nextBlockStack中存储的对应的whileCondStack的nextBlock
     */
    private final Stack<LLVMBasicBlockRef> whileCondStack = new Stack<>();
    private final Stack<LLVMBasicBlockRef> nextBlockStack = new Stack<>();
    public LLVMvisitor() {
        //初始化LLVM
        LLVMInitializeCore(LLVMGetGlobalPassRegistry());
        LLVMLinkInMCJIT();
        LLVMInitializeNativeAsmPrinter();
        LLVMInitializeNativeAsmParser();
        LLVMInitializeNativeTarget();
    }

    public LLVMBuilderRef getBuilder() {
        return builder;
    }

    public LLVMModuleRef getModule() {
        return module;
    }

    public LLVMTypeRef getI32Type() {
        return i32Type;
    }

    @Override
    public LLVMValueRef visitProgram(SysYParser.ProgramContext ctx) {
        System.out.println("in visit program ");
        globalScope = new GlobalScope(null);
        currentScope = globalScope;
        LLVMValueRef ret = super.visitProgram(ctx);
        currentScope = currentScope.getEnclosingScope();
        return ret;
    }
    @Override
    public LLVMValueRef visitBlock(SysYParser.BlockContext ctx) {
        System.out.println("in visit block ");

        LocalScope localScope = new LocalScope(currentScope);
        localScopeCounter++;
        String localScopeName = localScope.getName() + localScopeCounter;
        localScope.setName(localScopeName);
        currentScope = localScope;
        LLVMValueRef ret = super.visitBlock(ctx);
        currentScope = currentScope.getEnclosingScope();
        return ret;
    }

    public LLVMTypeRef getTypeLLVMRef(String typeName) {
        if (typeName.equals("int")) {
            return i32Type;
        } else {
            return voidType;
        }
    }
    @Override
    public LLVMValueRef visitFuncDef(SysYParser.FuncDefContext ctx) {
        /**
         * 先生成返回值类型
         * 多个参数时需先生成函数的参数类型，再生成函数类型
         * 用生成的函数类型去生成函数
         */
        System.out.println("in visit funcdef ");

        //生成返回值类型
        String functionName = ctx.IDENT().getText();
        int paramsNum = 0;
        if(ctx.funcFParams() != null) {
            //拿到参数数量
            paramsNum = ctx.funcFParams().funcFParam().size();
        }

        System.out.println("paramsnum is " + paramsNum);

        //生成函数参数类型
        PointerPointer<Pointer> argumentTypes = new PointerPointer<>(paramsNum);
        for (int i = 0; i < paramsNum; i++) {
            SysYParser.FuncFParamContext funcFParamContext = ctx.funcFParams().funcFParam(i);
            String paramsTypeName = funcFParamContext.bType().getText();
            LLVMTypeRef paramType = getTypeLLVMRef(paramsTypeName);

            //数组实现 todo done
            if (funcFParamContext.L_BRACKT().size() > 0) {
                //变量实际上是指针
                paramType = LLVMPointerType(paramType, 0);
            }
            argumentTypes.put(i, paramType);
        }

        //生成函数类型
        String retTypeName = ctx.funcType().getText();
        LLVMTypeRef retType = getTypeLLVMRef(retTypeName);
        LLVMTypeRef functionType = LLVMFunctionType(retType, argumentTypes, paramsNum, 0);

        //存储retType和funcName的对应关系
        retTypeMap.put(functionName, retTypeName);

        //生成函数，即向之前创建的module中添加函数
        LLVMValueRef function = LLVMAddFunction(module, /*functionName:String*/functionName, functionType);
        currentFunction = function;
        //通过如下语句在函数中加入基本块，一个函数可以加入多个基本块
        LLVMBasicBlockRef block = LLVMAppendBasicBlock(function, /*blockName:String*/functionName + "_entry");
        blockNow = block;
        //选择要在哪个基本块后追加指令
        //后续生成的指令将追加在block1的后面
        LLVMPositionBuilderAtEnd(builder, block);
        //修改作用域
        currentScope.define(functionName, function, functionType);
        currentScope = new LocalScope(currentScope);

        //函数参数的值的存储
        for (int i = 0; i < paramsNum; ++i) {
            SysYParser.FuncFParamContext funcFParamContext = ctx.funcFParams().funcFParam(i);
            String paramTypeName = funcFParamContext.bType().getText();
            LLVMTypeRef paramType = getTypeLLVMRef(paramTypeName);

            //数组实现 todo done
            if (funcFParamContext.L_BRACKT().size() > 0) {
                paramType = LLVMPointerType(paramType, 0);
            }

            String paramName = ctx.funcFParams().funcFParam(i).IDENT().getText();
            //创建参数变量的指针，存储在当前的作用域中
            LLVMValueRef varPointer = LLVMBuildAlloca(builder, paramType, paramName);
            currentScope.define(paramName, varPointer, paramType);
            //创建参数变量的值，存储在上面创建的函数参数的指针里
            LLVMValueRef argValue = LLVMGetParam(function, i);
            LLVMBuildStore(builder, argValue, varPointer);
        }

        isReturned = false;
        //遍历子结点
        super.visitFuncDef(ctx);
        if (!isReturned) {
            System.out.println("---------new ret ");
            if (retType.equals(voidType)) {
                LLVMBuildRet(builder, null);
            } else {
                LLVMBuildRet(builder, zero);
            }
        }
        currentScope = currentScope.getEnclosingScope();
        //重置
        isReturned = false;
        return function;
    }
    private String toDecimalInteger(String str) {
        if (str.charAt(0) == '0' && str.length() != 1 && str.charAt(1) != 'x') {
            str = String.valueOf(Integer.parseInt(str, 8));
        } else if (str.length() != 1 && str.charAt(0) == '0' && (str.charAt(1) == 'x' || str.charAt(1) == 'X')) {
            str = String.valueOf(Integer.parseInt(str.substring(2), 16));
        }
        return str;
    }

    @Override public LLVMValueRef visitExp(SysYParser.ExpContext ctx) {

        System.out.println("in visit exp ");

        if (ctx.unaryOp() != null) {
            // unaryOp exp
            String operator = ctx.unaryOp().getText();
            LLVMValueRef expValue = visitExp(ctx.exp(0));
            switch (operator) {
                case "+": {
                    return expValue;
                }
                case "-": {
                    return LLVMBuildNeg(builder, expValue, "neg_");
                }
                case "!": {
                    LLVMValueRef tmp_ = expValue;
                    // 生成icmp
                    // LLVMBuildICmp(LLVMBuilderRef arg0, int Op, LLVMValueRef LHS, LLVMValueRef RHS, BytePointer Name)
                    tmp_ = LLVMBuildICmp(builder, LLVMIntNE, LLVMConstInt(i32Type, 0, 0), tmp_, "tmp_");
                    // 生成xor
                    tmp_ = LLVMBuildXor(builder, tmp_, LLVMConstInt(LLVMInt1Type(), 1, 0), "tmp_");
                    // 生成zext
                    tmp_ = LLVMBuildZExt(builder, tmp_, i32Type, "tmp_");
                    return tmp_;
                }
                default: {
                    System.out.println("error in visitExp unaryOP");
                }
            }
        } else if (ctx.MUL() != null || ctx.DIV() != null
                || ctx.MOD() != null || ctx.PLUS() != null || ctx.MINUS() != null)
        {
            LLVMValueRef expNum1 = visitExp(ctx.exp(0));
            LLVMValueRef expNum2 = visitExp(ctx.exp(1));
            // *
            if (ctx.MUL() != null) {
                return LLVMBuildMul(builder, expNum1, expNum2, "mul_");
            }
            // /
            if (ctx.DIV() != null) {
                return LLVMBuildSDiv(builder, expNum1, expNum2, "div_");
            }
            // +
            if (ctx.PLUS() != null) {
                return LLVMBuildAdd(builder, expNum1, expNum2, "add_");
            }
            // -
            if (ctx.MINUS() != null) {
                return LLVMBuildSub(builder, expNum1, expNum2, "sub_");
            }
            // %
            if (ctx.MOD() != null) {
                return LLVMBuildSRem(builder, expNum1, expNum2, "rem_");
            }
        } else if (ctx.number() != null) {
            int tmp = Integer.parseInt(toDecimalInteger(ctx.number().getText()));
            System.out.println("ctx.exp is num " + tmp);
            return LLVMConstInt(i32Type, tmp, 0);
        } else if (ctx.L_PAREN() != null) {
            if (ctx.IDENT() != null) {
                //function call
                System.out.println("ctx.exp is functionCall");
                //函数调用，包含参数
                String funcName = ctx.IDENT().getText();
                LLVMValueRef function = currentScope.resolve(funcName);
                PointerPointer<Pointer> params = null;
                int paramsNum = 0;

                //如果参数不为空
                if (ctx.funcRParams() != null) {
                    paramsNum = ctx.funcRParams().param().size();
                    params = new PointerPointer<>(paramsNum);
                    //调用参数全部存入params
                    for (int i = 0; i < paramsNum; i++) {
                        SysYParser.ParamContext paramContext = ctx.funcRParams().param(i);
                        SysYParser.ExpContext expContext = paramContext.exp();
                        LLVMValueRef param = this.visitExp(expContext);
                        params.put(i, param);
                    }
                }
                //void类型不应该在call的时候有名称
                if (retTypeMap.get(funcName).equals("void")) {
                    funcName = "";
                }
                return LLVMBuildCall(builder, function, params, paramsNum, funcName);
            } else if (ctx.exp() != null) {
                return visitExp(ctx.exp(0));
            }

        } else if (ctx.lVal() != null) {
            System.out.println("ctx.exp is lval");
            LLVMValueRef lval = visitLVal(ctx.lVal());
            if (arrayAddr) {
                arrayAddr = false;
                return lval;
            }
            String valueName = ctx.lVal().getText();
            return LLVMBuildLoad(builder, lval, valueName);
        }
        return super.visitExp(ctx);
    }

    /**
     *
     * @param varCount variable数量
     * @param varPointer 数组所存放的内存
     * @param initArray 需要被初始化的传入数组元素
     */
    private void buildGEP(int varCount, LLVMValueRef varPointer, LLVMValueRef[] initArray) {

        LLVMValueRef[] arrayPointer = new LLVMValueRef[2];
        arrayPointer[0] = zero;
        for (int i = 0; i < varCount; i++) {
            arrayPointer[1] = LLVMConstInt(i32Type, i, 0);
            PointerPointer<LLVMValueRef> indexPointer = new PointerPointer<>(arrayPointer);
            System.out.println("********* LLVMBuildGEP");
            LLVMValueRef elementPtr = LLVMBuildGEP(builder, varPointer, indexPointer, 2, "&" + i);
            LLVMBuildStore(builder, initArray[i], elementPtr);
        }
    }
    @Override
    public LLVMValueRef visitVarDecl(SysYParser.VarDeclContext ctx) {
        System.out.println("in visit vardecl ");

        // typeName: int
        String typeName = ctx.bType().getText();
        for (SysYParser.VarDefContext varDefContext : ctx.varDef()) {
            LLVMTypeRef varType = getTypeLLVMRef(typeName);
            System.out.println("var type is " + varType);
            //varName : a
            String varName = varDefContext.IDENT().getText();
            System.out.println("var name is " + varName);

            int varCount = 0;
           //todo in arraylist:for done
            for (SysYParser.ConstExpContext constExpContext : varDefContext.constExp()) {
                varCount = Integer.parseInt(toDecimalInteger(constExpContext.getText()));
                varType = LLVMArrayType(varType, varCount);
            }

            LLVMValueRef varValues = null;
            if (currentScope == globalScope) {
                varValues = LLVMAddGlobal(module, varType, varName);
                if (varCount == 0) {
                    LLVMSetInitializer(varValues, zero);
                } else {
                    //数组
                    PointerPointer<Pointer> pointerPointer = new PointerPointer<>(varCount);
                    //PP初始值为0
                    for (int i = 0; i < varCount; i++) {
                        pointerPointer.put(i, zero);
                    }
                    LLVMValueRef initArray = LLVMConstArray(varType, pointerPointer, varCount);
                    LLVMSetInitializer(varValues, initArray);
                }
                //有赋值
                if (varDefContext.ASSIGN() != null) {
                    SysYParser.ExpContext expContext = varDefContext.initVal().exp();
                    if (expContext != null) {
                        // exp表达式
                        LLVMValueRef initVal = visitExp(expContext);
                        LLVMSetInitializer(varValues, initVal);
                    } else {
                        // { (initVal (, initVal)*)? }
                        System.out.println("{2, 4}");
                        int initValCount = varDefContext.initVal().initVal().size();
                        PointerPointer<Pointer> pointerPointer = new PointerPointer<>(varCount);
                        for (int i = 0; i < varCount; i++) {
                            //有value就赋值，没有就= 0
                            if (i < initValCount) {
                                pointerPointer.put(i, this.visit(varDefContext.initVal().initVal(i).exp()));
                            } else {
                                pointerPointer.put(i, zero);
                            }
                        }
                        LLVMValueRef initArray = LLVMConstArray(varType, pointerPointer, varCount);
                        LLVMSetInitializer(varValues, initArray);
                    }
                }
            } else {
                // 非global
                //申请一块能存放int型的内存
                varValues = LLVMBuildAlloca(builder, varType, varName);

                if (varDefContext.ASSIGN() != null) {
                    //有赋值
                    SysYParser.ExpContext expContext = varDefContext.initVal().exp();
                    if (expContext != null) {
                        // exp表达式不为空
                        LLVMValueRef initVal = visitExp(expContext);
                        // local
                        //将数值存入该内存
                        LLVMBuildStore(builder, initVal, varValues);
                    } else {
                        //数组
                        int initValCount = varDefContext.initVal().initVal().size();
                        LLVMValueRef[] initArray = new LLVMValueRef[varCount];
                        for (int i = 0; i < varCount; i++) {
                            if (i < initValCount) {
                                initArray[i] = this.visit(varDefContext.initVal().initVal(i).exp());
                            } else {
                                initArray[i] = LLVMConstInt(i32Type, 0, 0);
                            }
                        }
                        //自行构建
                        buildGEP(varCount, varValues, initArray);
                    }
                }
            }
            currentScope.define(varName, varValues, varType);
        }
        return super.visitVarDecl(ctx);
    }

    @Override
    public LLVMValueRef visitStmt(SysYParser.StmtContext ctx) {
        System.out.println("visit stmt");

        if (ctx.RETURN() != null) {
            //return
            System.out.println("ctx.stmt is return ");

            LLVMValueRef result = null;
            //RETURN (exp)? SEMICOLON
            if (ctx.exp() != null) {
                result = visitExp(ctx.exp());
            }
            //已经在stmt的return分支里生成了ret语句
            isReturned = true;
            return LLVMBuildRet(builder, result);
        } else if (ctx.ASSIGN() != null) {
            //lVal ASSIGN exp;
            //lval : IDENT ( [ exp ])*
            //todo in arraylist done
            LLVMValueRef exp = this.visitExp(ctx.exp());
            LLVMValueRef lVal = this.visitLVal(ctx.lVal());
            return LLVMBuildStore(builder, exp, lVal);
        } else if (ctx.IF() != null) {
            LLVMValueRef condVal = this.visit(ctx.cond());
            LLVMValueRef cmpResult = LLVMBuildICmp(builder, LLVMIntNE, zero, condVal, "icmp_");
            LLVMBasicBlockRef trueBlock = LLVMAppendBasicBlock(currentFunction, "trueBlock");
            LLVMBasicBlockRef falseBlock = LLVMAppendBasicBlock(currentFunction, "falseBlock");
            LLVMBasicBlockRef nextBlockIf = LLVMAppendBasicBlock(currentFunction, "nextBlock");

            //创建分支选择
            LLVMBuildCondBr(builder, cmpResult, trueBlock, falseBlock);

            //trueBlock追加
            LLVMPositionBuilderAtEnd(builder, trueBlock);
            //访问true的stmt
            this.visit(ctx.stmt(0));
                //无条件跳转
            LLVMBuildBr(builder, nextBlockIf);

            //falseBlock追加
            LLVMPositionBuilderAtEnd(builder, falseBlock);
            //如果有else：访问false的stmt
            if (ctx.ELSE() != null) {
                this.visit(ctx.stmt(1));
            }
            LLVMBuildBr(builder, nextBlockIf);
            isReturned = false;

            //剩下的内容应该追加在nextBlock的后面
            LLVMPositionBuilderAtEnd(builder, nextBlockIf);
            //无需访问子结点：已访问完全
            return null;
        } else if (ctx.WHILE() != null) {
            LLVMBasicBlockRef whileCond = LLVMAppendBasicBlock(currentFunction, "whileCondition");
            LLVMBasicBlockRef whileBlock = LLVMAppendBasicBlock(currentFunction, "whileBlock");
            LLVMBasicBlockRef nextBlock = LLVMAppendBasicBlock(currentFunction, "nextBlock");

            //创建条件块
            LLVMBuildBr(builder, whileCond);
            LLVMPositionBuilderAtEnd(builder, whileCond);
            //生成条件跳转指令
            LLVMValueRef condVal = this.visitCond(ctx.cond());
            LLVMValueRef cmpResult = LLVMBuildICmp(builder, LLVMIntNE, zero, condVal, "icmp_");
            LLVMBuildCondBr(builder, cmpResult, whileBlock, nextBlock);

            //whileBlock追加
            LLVMPositionBuilderAtEnd(builder, whileBlock);

            whileCondStack.push(whileCond);
            nextBlockStack.push(nextBlock);
            this.visitStmt(ctx.stmt(0));
            LLVMBuildBr(builder, whileCond);
            whileCondStack.pop();
            nextBlockStack.pop();
            //无条件跳转
            LLVMBuildBr(builder, nextBlock);

            //falseBlock追加
            LLVMPositionBuilderAtEnd(builder, nextBlock);
            return null;
        } else if (ctx.BREAK() != null) {
            return LLVMBuildBr(builder, nextBlockStack.peek());
        } else if (ctx.CONTINUE() != null) {
            return LLVMBuildBr(builder, whileCondStack.peek());
        }
        return super.visitStmt(ctx);
    }

    @Override
    public LLVMValueRef visitLVal(SysYParser.LValContext ctx) {
        System.out.println("in visit lval ");

        //IDENT ( [ exp ] )*
        //todo arraylist: maybe done
        String lValName = ctx.IDENT().getText();
        LLVMValueRef varPointer = currentScope.resolve(lValName);
        LLVMTypeRef varType = currentScope.getType(lValName);
        if (varType.equals(i32Type)) {
            return varPointer;
        } else if (varType.equals(intPointerType)) {
            // exp != null
            if (ctx.exp().size() > 0) {
                // visit exp and build pointer
                LLVMValueRef[] arrayPointer = new LLVMValueRef[1];
                arrayPointer[0] = this.visit(ctx.exp(0));
                PointerPointer<LLVMValueRef> indexPointer = new PointerPointer<>(arrayPointer);
                LLVMValueRef pointer = LLVMBuildLoad(builder, varPointer, lValName);
                return LLVMBuildGEP(builder, pointer, indexPointer, 1, "&" + lValName);
            } else {
                //no exp: normal intPointer
                return varPointer;
            }
        } else {
            LLVMValueRef[] arrayPointer = new LLVMValueRef[2];
            arrayPointer[0] = zero;
            if (ctx.exp().size() > 0) {
                arrayPointer[1] = this.visit(ctx.exp(0));
            } else {
                arrayAddr = true;
                arrayPointer[1] = zero;
            }
            PointerPointer<LLVMValueRef> indexPointer = new PointerPointer<>(arrayPointer);
            return LLVMBuildGEP(builder, varPointer, indexPointer, 2, "&" + lValName);
        }

    }

    @Override
    public LLVMValueRef visitConstDecl(SysYParser.ConstDeclContext ctx) {
        System.out.println("in visit const decl ");

        // typeName: int
        String typeName = ctx.bType().getText();
        for (SysYParser.ConstDefContext constDefContext : ctx.constDef()) {
            LLVMTypeRef varType = getTypeLLVMRef(typeName);
            //varName : a
            String varName = constDefContext.IDENT().getText();
            int varCount = 0;
            //todo in arraylist:for
            for (SysYParser.ConstExpContext constExpContext : constDefContext.constExp()) {
                varCount = Integer.parseInt(toDecimalInteger(constExpContext.getText()));
                varType = LLVMArrayType(varType, varCount);
            }

            LLVMValueRef varValues = null;
            if (currentScope == globalScope) {
                varValues = LLVMAddGlobal(module, varType, "global_" + varName);
                LLVMSetInitializer(varValues, zero);

                if (constDefContext.ASSIGN() != null) {
                    SysYParser.ConstExpContext constExpContext = constDefContext.constInitVal().constExp();
                    if (constExpContext != null) {
                        //有赋值,直接就是constExp
                        // exp表达式不为空
                        LLVMValueRef initVal = visitConstExp(constExpContext);
                        LLVMSetInitializer(varValues, initVal);
                    } else {
                        //有赋值，是{ (constInitVal (, constInitVal)*)? }
                        int initValCount = constDefContext.constInitVal().constInitVal().size();
                        PointerPointer<LLVMValueRef> pointerPointer = new PointerPointer<>(varCount);
                        for (int i = 0; i < varCount; i++) {
                            if (i < initValCount) {
                                pointerPointer.put(i, this.visit(constDefContext.constInitVal().constInitVal(i).constExp()));
                            } else {
                                pointerPointer.put(i, zero);
                            }
                        }
                        LLVMValueRef initArray = LLVMConstArray(varType, pointerPointer, varCount);
                        LLVMSetInitializer(varValues, initArray);
                    }
                }
            } else {
                // 非global
                //申请一块能存放int型的内存
                varValues = LLVMBuildAlloca(builder, varType, "local_" + varName);
                LLVMSetInitializer(varValues, zero);

                if (constDefContext.ASSIGN() != null) {
                    //有赋值
                    SysYParser.ConstExpContext constExpContext = constDefContext.constInitVal().constExp();
                    if (constExpContext != null) {
                        // exp表达式不为空
                        LLVMValueRef initVal = visitConstExp(constExpContext);
                        // local
                        //将数值存入该内存
                        LLVMBuildStore(builder, initVal, varValues);
                    } else {
                        int initValCount = constDefContext.constInitVal().constInitVal().size();
                        LLVMValueRef[] initArray = new LLVMValueRef[varCount];
                        for (int i = 0; i < varCount; i++) {
                            if (i < initValCount) {
                                initArray[i] = this.visit(constDefContext.constInitVal().constInitVal(i).constExp());
                            } else {
                                initArray[i] = LLVMConstInt(i32Type, 0, 0);
                            }
                        }
                        buildGEP(varCount, varValues, initArray);
                    }
                }
            }
            currentScope.define(varName, varValues, varType);
        }
        return super.visitConstDecl(ctx);
    }

    @Override
    public LLVMValueRef visitCond(SysYParser.CondContext ctx) {
        System.out.println("visit cond");
        System.out.println("current scope " + currentScope.getName());

        if (ctx.exp() != null) {
            return this.visitExp(ctx.exp());
        } else if (ctx.LT() != null || ctx.GT() != null 
                || ctx.LE() != null || ctx.GE() != null) 
        {
            LLVMValueRef leftVal = this.visitCond(ctx.cond(0));
            LLVMValueRef rightVal = this.visitCond(ctx.cond(1));
            LLVMValueRef cmpResult = null;
            if (ctx.LT() != null) {
                cmpResult = LLVMBuildICmp(builder, LLVMIntSLT, leftVal, rightVal, "lessThan_");
            } else if (ctx.GT() != null) {
                cmpResult = LLVMBuildICmp(builder, LLVMIntSGT, leftVal, rightVal, "biggerThan_");
            } else if (ctx.LE() != null) {
                cmpResult = LLVMBuildICmp(builder, LLVMIntSLE, leftVal, rightVal, "lessOrEqualThan_");
            } else {
                cmpResult = LLVMBuildICmp(builder, LLVMIntSGE, leftVal, rightVal, "biggerOrEqualThan_");
            }
            //将结果从i1转化为i32，使用zext零拓展，高位补0
            return LLVMBuildZExt(builder, cmpResult, i32Type, "zext_");
        } else if (ctx.EQ() != null || ctx.NEQ() != null) {
            // == and !=
            LLVMValueRef leftVal = this.visitCond(ctx.cond(0));
            LLVMValueRef rightVal = this.visitCond(ctx.cond(1));
            LLVMValueRef cmpResult = null;
            if (ctx.NEQ() != null) {
                cmpResult = LLVMBuildICmp(builder, LLVMIntNE, leftVal, rightVal, "notEqual_");
            } else {
                cmpResult = LLVMBuildICmp(builder, LLVMIntEQ, leftVal, rightVal, "equal_");
            }
            return LLVMBuildZExt(builder, cmpResult, i32Type, "zext_");
        } else if (ctx.AND() != null) {
            //短路求值
            System.out.println("now in and!!!!!!!!!!!!!!");

            LLVMBasicBlockRef leftBlock = LLVMAppendBasicBlock(currentFunction, "andLeftBlock");
            LLVMBasicBlockRef rightBlock = LLVMAppendBasicBlock(currentFunction, "andRightBlock");
            LLVMBasicBlockRef afterBlock = LLVMAppendBasicBlock(currentFunction, "afterBlock");
            LLVMValueRef result = LLVMBuildAlloca(builder, i32Type, "result");
            LLVMBuildBr(builder, leftBlock);

            LLVMPositionBuilderAtEnd(builder, leftBlock);
            LLVMValueRef leftVal = this.visitCond(ctx.cond(0));
            LLVMValueRef leftResult = LLVMBuildICmp(builder, LLVMIntNE, leftVal, zero, "leftResult");
            LLVMBuildStore(builder, leftVal, result);
            LLVMBuildCondBr(builder, leftResult, rightBlock, afterBlock);

            LLVMPositionBuilderAtEnd(builder, rightBlock);
            LLVMValueRef rightVal = this.visitCond(ctx.cond(1));
            LLVMValueRef rightResult = LLVMBuildICmp(builder, LLVMIntNE, rightVal, zero, "rightResult");
            LLVMBuildStore(builder, rightVal, result);
            LLVMBuildBr(builder, afterBlock);

            LLVMPositionBuilderAtEnd(builder, afterBlock);
            LLVMValueRef retResult = LLVMBuildLoad(builder, result, "loadFromResult");

            return LLVMBuildZExt(builder, retResult, i32Type, "zext_");

        } else if (ctx.OR() != null) {
            LLVMBasicBlockRef leftBlock = LLVMAppendBasicBlock(currentFunction, "orLeftBlock");
            LLVMBasicBlockRef rightBlock = LLVMAppendBasicBlock(currentFunction, "orRightBlock");
            LLVMBasicBlockRef afterBlock = LLVMAppendBasicBlock(currentFunction, "afterBlock");
            LLVMValueRef result = LLVMBuildAlloca(builder, i32Type, "result");
            LLVMBuildBr(builder, leftBlock);

            LLVMPositionBuilderAtEnd(builder, leftBlock);
            LLVMValueRef leftVal = this.visitCond(ctx.cond(0));
            LLVMValueRef leftResult = LLVMBuildICmp(builder, LLVMIntNE, leftVal, zero, "leftResult");
            LLVMBuildStore(builder, leftVal, result);
            LLVMBuildCondBr(builder, leftResult, afterBlock, rightBlock);

            LLVMPositionBuilderAtEnd(builder, rightBlock);
            LLVMValueRef rightVal = this.visitCond(ctx.cond(1));
//            LLVMValueRef rightResult = LLVMBuildICmp(builder, LLVMIntNE, rightVal, zero, "rightResult");
            LLVMBuildStore(builder, rightVal, result);
            LLVMBuildBr(builder, afterBlock);

            LLVMPositionBuilderAtEnd(builder, afterBlock);
            LLVMValueRef retResult = LLVMBuildLoad(builder, result, "loadFromResult");

            return LLVMBuildZExt(builder, retResult, i32Type, "zext_");
        }
        return super.visitCond(ctx);
    }
}

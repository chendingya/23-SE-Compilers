import java.io.IOException;
import java.lang.String;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;

import org.bytedeco.javacpp.BytePointer;

import static org.bytedeco.llvm.global.LLVM.*;


public class Main
{
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("input path is required");
        }
        String source = args[0];
        String dest = args[1];
        CharStream input = CharStreams.fromFileName(source);
        SysYLexer sysYLexer = new SysYLexer(input);

        sysYLexer.removeErrorListeners();
        myErrorListenerLexer myErrorListenerLexer = new myErrorListenerLexer();
        sysYLexer.addErrorListener(myErrorListenerLexer);

//        List<? extends Token> tokens = (List<Token>) sysYLexer.getAllTokens();
//        if (!myErrorListenerLexer.whetherErrorHappen()) {
//            for(Token tk: tokens) {
//                int tkid = tk.getType();
//                String tktype = SysYLexer.ruleNames[tkid - 1];
//
//                int line = tk.getLine();
//                String text = tk.getText();
//
//                if (tkid == 34) {
//                    if (text.charAt(0) == '0' && text.length() != 1 && text.charAt(1) != 'x') {
//                        System.err.println( tktype + " " + Integer.valueOf(text.substring(1), 8) + " at Line " + line + '.');
//                    } else if (text.length() != 1 && text.charAt(0) == '0' && text.charAt(1) == 'x') {
//                        System.err.println( tktype + " " + Integer.parseInt(text.substring(2), 16) + " at Line " + line + '.');
//                    } else {
//                        System.err.println( tktype + " " + text + " at Line " + line + '.');
//                    }
//
//                } else {
//                    System.err.println( tktype + " " + text + " at Line " + line + '.');
//                }
//            }
//        }
        CommonTokenStream tokens = new CommonTokenStream(sysYLexer);
        SysYParser sysYParser = new SysYParser(tokens);

        /**
         * 语法分析与高亮:语法错误
         */

        sysYParser.removeErrorListeners();
        myErrorListenerParser myErrorListenerParser = new myErrorListenerParser();
        sysYParser.addErrorListener(myErrorListenerParser);

        //Visitor默认将对语法树进行深度优先遍历，你可以自己实现继承自SysYParserBaseVisitor的Visitor控制对子节点的遍历顺序，可以自行定义遍历节点时以及遍历子节点前后的行为
        ParseTree tree = sysYParser.program();
        //Visitor extends SysYParserBaseVisitor<Void>
//        if (!myErrorListenerParser.whetherErrorHappen()) {
//            Visitor visitor = new Visitor();
//            visitor.visit(tree);
//            if (!visitor.getSemanticError()) {
//                visitor.printMes();
//            }
//
//        }

        /**
         * lab4
         */

        LLVMvisitor llvmirVisitor = new LLVMvisitor();
        llvmirVisitor.visit(tree);

        BytePointer error = new BytePointer();
        if (LLVMPrintModuleToFile(llvmirVisitor.getModule(), dest, error) != 0) {    // moudle 是你自定义的 LLVMModuleRef 对象
            LLVMDisposeMessage(error);
        }

    }
}
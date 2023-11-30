package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.parser.table.Production;
import cn.edu.hitsz.compiler.parser.table.Status;
import cn.edu.hitsz.compiler.parser.table.Symbol;
import cn.edu.hitsz.compiler.symtab.SourceCodeType;
import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.symtab.SymbolTableEntry;
import cn.edu.hitsz.compiler.utils.FilePathConfig;

import java.util.Stack;

// TODO: 实验三: 实现语义分析
public class SemanticAnalyzer implements ActionObserver {

    private final Stack<Symbol> symbolStack = new Stack<>();
    private SymbolTable symbolTable;

    @Override
    public void whenAccept(Status currentStatus) {
        // TODO: 该过程在遇到 Accept 时要采取的代码动作
        // do nothing
    }

    @Override
    public void whenReduce(Status currentStatus, Production production) {
        // TODO: 该过程在遇到 reduce production 时要采取的代码动作
        if (production.index() == 5) {
            symbolStack.pop();
            symbolStack.add(new Symbol(SourceCodeType.Int));
        } else if (production.index() == 4) {
            final var entry = symbolTable.get(symbolStack.pop().getToken().getText());
            entry.setType(symbolStack.pop().getSourceCodeType());
            symbolStack.add(new Symbol(production.head()));
        } else {
            int length = production.body().size();
            while (length > 0) {
                symbolStack.pop();
                length--;
            }
            symbolStack.add(new Symbol(production.head()));
        }
    }

    @Override
    public void whenShift(Status currentStatus, Token currentToken) {
        // TODO: 该过程在遇到 shift 时要采取的代码动作
        symbolStack.add(new Symbol(currentToken));
    }

    @Override
    public void setSymbolTable(SymbolTable table) {
        // TODO: 设计你可能需要的符号表存储结构
        // 如果需要使用符号表的话, 可以将它或者它的一部分信息存起来, 比如使用一个成员变量存储
        symbolTable = table;
    }
}


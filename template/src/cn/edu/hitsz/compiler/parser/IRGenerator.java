package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.ir.IRImmediate;
import cn.edu.hitsz.compiler.ir.IRValue;
import cn.edu.hitsz.compiler.ir.IRVariable;
import cn.edu.hitsz.compiler.ir.Instruction;
import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.parser.table.Production;
import cn.edu.hitsz.compiler.parser.table.Status;
import cn.edu.hitsz.compiler.parser.table.Symbol;
import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

// TODO: 实验三: 实现 IR 生成

/**
 *
 */
public class IRGenerator implements ActionObserver {

    private final Stack<Symbol> symbolStack = new Stack<>();
    private final List<Instruction> instructions = new ArrayList<>();

    @Override
    public void whenShift(Status currentStatus, Token currentToken) {
        // TODO
        symbolStack.add(new Symbol(currentToken));
    }

    @Override
    public void whenReduce(Status currentStatus, Production production) {
        // TODO
        IRValue val;
        IRVariable temp;
        switch (production.index()) {
            case 6 :
                val = symbolStack.pop().getIrValue();
                symbolStack.pop();
                instructions.add(Instruction.createMov(IRVariable.named(symbolStack.pop().getToken().getText()), val));
                symbolStack.add(new Symbol(production.head()));
                break;
            case 7 :
                val = symbolStack.pop().getIrValue();
                symbolStack.pop();
                instructions.add(Instruction.createRet(val));
                symbolStack.add(new Symbol(production.head()));
                break;
            case 8 :
                temp = IRVariable.temp();
                val = symbolStack.pop().getIrValue();
                symbolStack.pop();
                instructions.add(Instruction.createAdd(temp, symbolStack.pop().getIrValue(), val));
                symbolStack.add(new Symbol(temp));
                break;
            case 9 :
                temp = IRVariable.temp();
                val = symbolStack.pop().getIrValue();
                symbolStack.pop();
                instructions.add(Instruction.createSub(temp, symbolStack.pop().getIrValue(), val));
                symbolStack.add(new Symbol(temp));
                break;
            case 10 :
                break;
            case 11 :
                temp = IRVariable.temp();
                val = symbolStack.pop().getIrValue();
                symbolStack.pop();
                instructions.add(Instruction.createMul(temp, symbolStack.pop().getIrValue(), val));
                symbolStack.add(new Symbol(temp));
                break;
            case 12 :
                break;
            case 13 :
                symbolStack.pop();
                val = symbolStack.pop().getIrValue();
                symbolStack.pop();
                symbolStack.add(new Symbol(val));
                break;
            case 14 :
                val = IRVariable.named(symbolStack.pop().getToken().getText());
                symbolStack.add(new Symbol(val));
                break;
            case 15 :
                val = IRImmediate.of(Integer.parseInt(symbolStack.pop().getToken().getText()));
                symbolStack.add(new Symbol(val));
                break;
            default :
                int length = production.body().size();
                while (length > 0) {
                    symbolStack.pop();
                    length--;
                }
                symbolStack.add(new Symbol(production.head()));
                break;
        }
    }


    @Override
    public void whenAccept(Status currentStatus) {
        // TODO
        // do nothing
    }

    @Override
    public void setSymbolTable(SymbolTable table) {
        // TODO
        // do nothing
    }

    public List<Instruction> getIR() {
        // TODO
        return instructions;
    }

    public void dumpIR(String path) {
        FileUtils.writeLines(path, getIR().stream().map(Instruction::toString).toList());
    }
}


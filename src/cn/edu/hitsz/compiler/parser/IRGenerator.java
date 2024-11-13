package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.ir.IRImmediate;
import cn.edu.hitsz.compiler.ir.IRValue;
import cn.edu.hitsz.compiler.ir.IRVariable;
import cn.edu.hitsz.compiler.ir.Instruction;
import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.parser.table.Production;
import cn.edu.hitsz.compiler.parser.table.Status;
import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class IRGenerator implements ActionObserver {
    public SymbolTable table;
    private final Stack<Symbol> tokenStack = new Stack<>();
    private List<Instruction> IR = new ArrayList<>();


    @Override
    public void whenShift(Status currentStatus, Token currentToken) {
        Symbol cur = new Symbol(currentToken);
        if(currentToken.getText().matches("^\\d+$")) {
            cur.value = IRImmediate.of(Integer.parseInt(currentToken.getText()));
        } else {
            cur.value = IRVariable.named(currentToken.getText());
        }
        tokenStack.push(cur);
    }
    @Override
    public void whenReduce(Status currentStatus, Production production) {
        Symbol lhs, rhs, cur = new Symbol(production.head());
        IRVariable tmp;
        switch(production.index()) {
            case 6 -> {
                rhs = tokenStack.pop();
                tokenStack.pop();
                lhs = tokenStack.pop();

                tmp = (IRVariable)lhs.value;
                cur.value = null;
                IR.add(Instruction.createMov(tmp, rhs.value));
                tokenStack.push(cur);
            }
            case 7 -> {
                rhs = tokenStack.pop();
                tokenStack.pop();
                cur.value = null;
                IR.add(Instruction.createRet(rhs.value));
                tokenStack.push(cur);
            }
            case 8 -> {
                rhs = tokenStack.pop();
                tokenStack.pop();
                lhs = tokenStack.pop();


                tmp = IRVariable.temp();
                IR.add(Instruction.createAdd(tmp, lhs.value, rhs.value));
                cur.value = tmp;
                tokenStack.push(cur);
            }
            case 9 -> {
                rhs = tokenStack.pop();
                tokenStack.pop();
                lhs = tokenStack.pop();

                tmp = IRVariable.temp();
                IR.add(Instruction.createSub(tmp, lhs.value, rhs.value));
                cur.value = tmp;
                tokenStack.push(cur);
            }
            case 11 -> {
                rhs = tokenStack.pop();
                tokenStack.pop();
                lhs = tokenStack.pop();

                tmp = IRVariable.temp();
                IR.add(Instruction.createMul(tmp, lhs.value, rhs.value));
                cur.value = tmp;
                tokenStack.push(cur);
            }
            case 10,12,14 -> {
                cur.value = tokenStack.pop().value;
                tokenStack.push(cur);
            }
            case 13 -> {
                tokenStack.pop();
                rhs = tokenStack.pop();
                tokenStack.pop();
                cur.value = rhs.value;
                tokenStack.push(cur);
            }
            case 15 -> {
                rhs = tokenStack.pop();
                cur.value = rhs.value;
                tokenStack.push(cur);
            }
            default -> {
                for(int i = 0; i < production.body().size(); ++i) {
                    tokenStack.pop();
                }
                tokenStack.push(new Symbol(production.head()));
            }
        }
    }


    @Override
    public void whenAccept(Status currentStatus) {
    }

    @Override
    public void setSymbolTable(SymbolTable table) {
        this.table = table;
    }

    public List<Instruction> getIR() {
        return this.IR;
    }

    public void dumpIR(String path) {
        FileUtils.writeLines(path, getIR().stream().map(Instruction::toString).toList());
    }
}


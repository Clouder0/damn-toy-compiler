package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.lexer.TokenKind;
import cn.edu.hitsz.compiler.parser.table.Production;
import cn.edu.hitsz.compiler.parser.table.Status;
import cn.edu.hitsz.compiler.symtab.SourceCodeType;
import cn.edu.hitsz.compiler.symtab.SymbolTable;

import java.util.Objects;
import java.util.Stack;


public class SemanticAnalyzer implements ActionObserver {
    public SymbolTable table;
    private final Stack<Symbol> tokenStack = new Stack<>();

    @Override
    public void whenAccept(Status currentStatus) {
    }

    @Override
    public void whenReduce(Status currentStatus, Production production) {
        Symbol token1, token2;
        Symbol non_terminal;
        switch(production.index()) {
            case 4: // S -> D id;
                token1 = tokenStack.pop();
                token2 = tokenStack.pop();
                this.table.get(token1.token.getText()).setType(token2.type);
                non_terminal = new Symbol(production.head());
                non_terminal.type = null;
                tokenStack.push(non_terminal);
                break;
            case 5:
                token1 = tokenStack.pop();
                non_terminal = new Symbol(production.head());
                non_terminal.type = token1.type;
                tokenStack.push(non_terminal);
                break;
            default:
                for(int i = 0; i < production.body().size(); ++i) {
                    tokenStack.pop();
                }
                tokenStack.push(new Symbol(production.head()));
        }
    }

    @Override
    public void whenShift(Status currentStatus, Token currentToken) {
        Symbol cur = new Symbol(currentToken);
        if(Objects.equals(currentToken.getKindId(), "int")) {
            cur.type = SourceCodeType.Int;
        } else {
            cur.type = null;
        }
        tokenStack.push(cur);
    }

    @Override
    public void setSymbolTable(SymbolTable table) {
        this.table = table;
    }
}


package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.parser.table.Production;
import cn.edu.hitsz.compiler.parser.table.Status;
import cn.edu.hitsz.compiler.symtab.SourceCodeType;
import cn.edu.hitsz.compiler.symtab.SymbolTable;

import java.util.Objects;
import java.util.Stack;

// TODO: 实验三: 实现语义分析
public class SemanticAnalyzer implements ActionObserver {
    private SymbolTable symbolTable = null;

    private final Stack<SymbolEntry> symbolStack = new Stack<>();
    private final Stack<SourceCodeType> idTypeStack = new Stack<>();

    @Override
    public void whenAccept(Status currentStatus) {
        // TODO: 该过程在遇到 Accept 时要采取的代码动作
        // Do nothing 遇到Accept时语义分析结束，无操作
    }

    @Override
    public void whenReduce(Status currentStatus, Production production) {
        // TODO: 该过程在遇到 reduce production 时要采取的代码动作
        switch (production.index()) {
            case 4 -> { // S -> D id
                SymbolEntry id = symbolStack.pop();
                symbolStack.pop();
                idTypeStack.pop();
                symbolTable.get(id.token.getText()).setType(idTypeStack.pop());     // 将栈顶id的type设为与D对应的type
                idTypeStack.push(null);     // 压入null占位
            }
            case 5 -> { // D -> int
                idTypeStack.pop();
                String varType =  symbolStack.pop().getToken().getKindId();
                if(Objects.equals(varType, "int")){     // 如果栈顶token为int则向idTypeStack中压入int
                    idTypeStack.push(SourceCodeType.Int);
                }

            }
            default -> { //其他种类产生式，弹栈并压入null占位
                multipop(symbolStack, production.body().size());
                multipop(idTypeStack, production.body().size());
                idTypeStack.push(null);
            }
        }
        symbolStack.push(new SymbolEntry(production.head()));
    }

    @Override
    public void whenShift(Status currentStatus, Token currentToken) {
        // TODO: 该过程在遇到 shift 时要采取的代码动作
        symbolStack.push(new SymbolEntry(currentToken));
        idTypeStack.push(null);
    }

    public void multipop(Stack stack, int times){
        for(int i=0; i<times; i++){
            stack.pop();
        }
    }

    @Override
    public void setSymbolTable(SymbolTable table) {
        // TODO: 设计你可能需要的符号表存储结构
        this.symbolTable = table;
    }
}


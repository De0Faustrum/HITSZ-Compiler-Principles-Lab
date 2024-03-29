package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.ir.IRImmediate;
import cn.edu.hitsz.compiler.ir.IRValue;
import cn.edu.hitsz.compiler.ir.IRVariable;
import cn.edu.hitsz.compiler.ir.Instruction;
import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.parser.table.Production;
import cn.edu.hitsz.compiler.parser.table.Status;
import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Stack;

// TODO: 实验三: 实现 IR 生成

/**
 *
 */
public class IRGenerator implements ActionObserver {

    private SymbolTable symbolTable = null;
    private final Stack<SymbolEntry> symbolStack = new Stack<>();
    private final Stack<IRValue> IRStack = new Stack<>();
    private final List<Instruction> instructionList = new LinkedList<>();       //生成指令列表

    @Override
    public void whenShift(Status currentStatus, Token currentToken) {
        // TODO
        IRStack.push(null);
        symbolStack.push(new SymbolEntry(currentToken));
    }

    @Override
    public void whenReduce(Status currentStatus, Production production) {
        // TODO
        switch (production.index()) {
            case 6 -> { // S -> id = E;
                multipop(symbolStack, 2);
                String identifier = symbolStack.pop().token.getText();
                IRValue variableE = IRStack.pop();
                IRVariable idName = IRVariable.named(identifier);       // 获取id实体
                Instruction instruction = Instruction.createMov(idName, variableE);   // 创建指令MOV
                instructionList.add(instruction);
                multipop(IRStack, 2);
                IRStack.push(null);     // 压入null占位
            }
            case 7 -> { // S -> return E;
                multipop(symbolStack, 2);
                Instruction instruction = Instruction.createRet(IRStack.pop());     // 创建指令RET
                instructionList.add(instruction);
                IRStack.pop();
                IRStack.push(null);     // 压入null占位
            }
            case 8 -> { // E -> E + A
                multipop(symbolStack, 3);
                IRValue variableA = IRStack.pop();
                IRStack.pop();
                IRValue variableE = IRStack.pop();
                IRVariable variable$ = IRVariable.temp();
                IRStack.push(variable$);
                Instruction instruction = Instruction.createAdd(variable$, variableE, variableA);   // 创建指令ADD
                instructionList.add(instruction);
            }
            case 9 -> { // E -> E - A;
                multipop(symbolStack, 3);
                IRValue variableA = IRStack.pop();
                IRStack.pop();
                IRValue variableE = IRStack.pop();
                IRVariable variable$ = IRVariable.temp();
                IRStack.push(variable$);
                Instruction instruction = Instruction.createSub(variable$, variableE, variableA);   //创建指令SUB
                instructionList.add(instruction);
            }
            case 10, 12 -> { // E -> A, A -> B;
                symbolStack.pop();
                IRStack.push(IRStack.pop());    // 弹一压一，无新指令
            }
            case 11 -> { // A -> A * B;
                multipop(symbolStack, 3);
                IRValue variableB = IRStack.pop();
                IRStack.pop();
                IRValue variableA = IRStack.pop();
                IRVariable variable$ = IRVariable.temp();
                IRStack.push(variable$);
                Instruction instruction = Instruction.createMul(variable$, variableA, variableB);   //创建指令MUL
                instructionList.add(instruction);
            }
            case 13 -> { // B -> ( E );
                multipop(symbolStack, 3);
                IRStack.pop();
                IRValue variableE = IRStack.pop();
                IRStack.pop();
                IRStack.push(variableE);    // 无新指令
            }
            case 14 -> { // B -> id;
                String identifier = symbolStack.pop().token.getText();
                IRStack.pop();
                IRVariable idName = IRVariable.named(identifier);
                IRStack.push(idName);       // 无新指令
            }
            case 15 -> { // B -> IntConst;
                String immediateText = symbolStack.pop().token.getText();
                IRStack.pop();
                IRImmediate immediate = IRImmediate.of(Integer.parseInt(immediateText));
                IRStack.push(immediate);    // 压入立即数，无新指令
            }
            default -> { // 未定义指令，弹出
                multipop(symbolStack, production.body().size());
                multipop(IRStack, production.body().size());
                IRStack.push(null);
            }
        }
        symbolStack.push(new SymbolEntry(production.head()));
    }

    @Override
    public void whenAccept(Status currentStatus) {
        // TODO
        // Do nothing 遇到Accept时中间代码生成结束，无操作
    }

    @Override
    public void setSymbolTable(SymbolTable table) {
        // TODO
        this.symbolTable = table;
    }

    public List<Instruction> getIR() {
        // TODO
        return instructionList;
    }

    public void multipop(Stack stack, int times){
        for(int i=0; i<times; i++){
            stack.pop();
        }
    }

    public void dumpIR(String path) {
        FileUtils.writeLines(path, getIR().stream().map(Instruction::toString).toList());
    }
}


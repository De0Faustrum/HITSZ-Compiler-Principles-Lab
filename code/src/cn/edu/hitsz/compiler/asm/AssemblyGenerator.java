package cn.edu.hitsz.compiler.asm;

import cn.edu.hitsz.compiler.ir.IRImmediate;
import cn.edu.hitsz.compiler.ir.IRValue;
import cn.edu.hitsz.compiler.ir.IRVariable;
import cn.edu.hitsz.compiler.ir.Instruction;
import cn.edu.hitsz.compiler.utils.FileUtils;
import java.util.List;
import static cn.edu.hitsz.compiler.ir.InstructionKind.*;
import static java.lang.Integer.parseInt;


/**
 * TODO: 实验四: 实现汇编生成
 * <br>
 * 在编译器的整体框架中, 代码生成可以称作后端, 而前面的所有工作都可称为前端.
 * <br>
 * 在前端完成的所有工作中, 都是与目标平台无关的, 而后端的工作为将前端生成的目标平台无关信息
 * 根据目标平台生成汇编代码. 前后端的分离有利于实现编译器面向不同平台生成汇编代码. 由于前后
 * 端分离的原因, 有可能前端生成的中间代码并不符合目标平台的汇编代码特点. 具体到本项目你可以
 * 尝试加入一个方法将中间代码调整为更接近 risc-v 汇编的形式, 这样会有利于汇编代码的生成.
 * <br>
 * 为保证实现上的自由, 框架中并未对后端提供基建, 在具体实现时可自行设计相关数据结构.
 *
 * @see AssemblyGenerator#run() 代码生成与寄存器分配
 */
public class AssemblyGenerator {

    private final RegisterAllocator regAlc = new RegisterAllocator();

    /**
     * 加载前端提供的中间代码
     * <br>
     * 视具体实现而定, 在加载中或加载后会生成一些在代码生成中会用到的信息. 如变量的引用
     * 信息. 这些信息可以通过简单的映射维护, 或者自行增加记录信息的数据结构.
     *
     * @param originInstructions 前端提供的中间代码
     */
    public void loadIR(List<Instruction> originInstructions) {
        // TODO: 读入前端提供的中间代码并生成所需要的信息
        regAlc.intermediateCode =  originInstructions;
        for(int i=0; i<regAlc.intermediateCode.size(); i++){
            Instruction curInst = regAlc.intermediateCode.get(i);
            if(curInst.getKind()==ADD || curInst.getKind()==SUB || curInst.getKind()==MUL){
                IRValue leftOperand = curInst.getLHS();
                if(leftOperand.isImmediate()) {
                    IRVariable newTemp = IRVariable.temp();
                    IRVariable result = curInst.getResult();
                    IRValue rhs = curInst.getRHS();
                    Instruction newInst = switch (curInst.getKind()) {
                        case ADD -> Instruction.createAdd(result, newTemp, rhs);
                        case SUB -> Instruction.createSub(result, newTemp, rhs);
                        case MUL -> Instruction.createMul(result, newTemp, rhs);
                        default -> null;
                    };
                    regAlc.intermediateCode.set(i, Instruction.createMov(newTemp, leftOperand));
                    regAlc.intermediateCode.add(i+1, newInst);
                }
            }
        }
    }


    /**
     * 执行代码生成.
     * <br>
     * 根据理论课的做法, 在代码生成时同时完成寄存器分配的工作. 若你觉得这样的做法不好,
     * 也可以将寄存器分配和代码生成分开进行.
     * <br>
     * 提示: 寄存器分配中需要的信息较多, 关于全局的与代码生成过程无关的信息建议在代码生
     * 成前完成建立, 与代码生成的过程相关的信息可自行设计数据结构进行记录并动态维护.
     */
    public void run() {
        // TODO: 执行寄存器分配与代码生成
        for(int i=0; i<regAlc.intermediateCode.size(); i++){
            Instruction curInst = regAlc.intermediateCode.get(i);
            switch(curInst.getKind()) {
                case ADD -> {
                    generateArithInstruction(curInst, i, "add ");
                }
                case SUB -> {
                    generateArithInstruction(curInst, i, "sub ");
                }
                case MUL -> {
                    generateArithInstruction(curInst, i, "mul ");
                }
                case MOV -> {
                    IRValue initial = curInst.getFrom();
                    IRVariable terminal = curInst.getResult();
                    RegisterList destReg;
                    if(regAlc.isAssigned(terminal)) {
                        destReg= regAlc.getCorrespondReg(terminal);
                    } else {
                        destReg= regAlc.getNewReg(i);
                        regAlc.assignNewReg(destReg,terminal);
                    }
                    if(initial.isImmediate()) {
                        regAlc.assemblyCode.add("\tli " + destReg + "," + ((IRImmediate)initial).getValue());
                    } else {
                        RegisterList regFrom = regAlc.getCorrespondReg((IRVariable)initial);
                        regAlc.assemblyCode.add("\tmv " + destReg + "," + regFrom);
                    }
                    regAlc.assignNewReg(destReg, terminal);
                }
                case RET -> {
                    IRValue irv = curInst.getReturnValue();
                    RegisterList r= regAlc.getCorrespondReg((IRVariable) irv);
                    regAlc.assemblyCode.add("\tmv a0," + r);
                }
            }
        }
    }

    public void generateArithInstruction(Instruction curInst, int regPos, String operator) {
        IRValue source1 = curInst.getLHS();
        IRValue source2 = curInst.getRHS();
        IRVariable destination = curInst.getResult();
        RegisterList destReg, sourceReg = regAlc.getCorrespondReg((IRVariable) source1);
        if(regAlc.isAssigned(destination)) {
            destReg= regAlc.getCorrespondReg(destination);
        } else {
            destReg= regAlc.getNewReg(regPos);
            regAlc.assignNewReg(destReg,destination);
        }
        if(source2.isImmediate()) {
            regAlc.assemblyCode.add("\t" + operator + destReg + "," + sourceReg + "," + ((IRImmediate)source2).getValue());
        } else {
            RegisterList regR= regAlc.getCorrespondReg((IRVariable) source2);
            regAlc.assemblyCode.add("\t" + operator + destReg + "," + sourceReg + "," + regR);
        }
    }


    /**
     * 输出汇编代码到文件
     *
     * @param path 输出文件路径
     */
    public void dump(String path) {
        // TODO: 输出汇编代码到文件
        System.out.println(regAlc.assemblyCode);
        FileUtils.writeLines(path, regAlc.assemblyCode);
    }
}


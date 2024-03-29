package cn.edu.hitsz.compiler.asm;
import cn.edu.hitsz.compiler.ir.IRVariable;
import cn.edu.hitsz.compiler.ir.Instruction;
import java.util.LinkedList;
import java.util.List;
import static cn.edu.hitsz.compiler.ir.InstructionKind.*;

public class RegisterAllocator {

    public List<String> assemblyCode = new LinkedList<>();
    public List<Instruction> intermediateCode = new LinkedList<>();
    private final List<IRVariable> variableList = new LinkedList<>();
    private final List<RegisterList> existingRegisters = new LinkedList<>();

    RegisterAllocator(){
        assemblyCode.add(".text");
        for(RegisterList r: RegisterList.values()){
            variableList.add(null);
            existingRegisters.add(r);
        }
    }

    public void assignNewReg(RegisterList newReg, IRVariable newVal){
        for(int i=0; i<RegisterList.values().length; i++){
            if(existingRegisters.get(i).equals(newReg)){
                variableList.set(i, newVal);
            }
        }
    }


    public boolean isAssigned(IRVariable destVar){
        for(int i=0; i<RegisterList.values().length; i++){
            if(destVar.equals(variableList.get(i))){
                return true;
            }
        }
        return false;
    }



    private void findAvailableReg(int initInst) {
        for(int i=0; i<RegisterList.values().length; i++) {
            boolean foundFlag = true, terminateFlag = true;
            RegisterList curReg = existingRegisters.get(i);
            for(int j=initInst; j<intermediateCode.size(); j++) {
                Instruction curInst = intermediateCode.get(j);
                if(curInst.getKind()==RET || !foundFlag || !terminateFlag) { break; }
                else if (curInst.getKind() == MOV){
                    if(isAssigned(curInst.getResult())) {
                        if(curReg.equals(getCorrespondReg(curInst.getResult()))) { terminateFlag = false; }
                    }
                    if(curInst.getFrom().isImmediate()) { continue; }
                    if(isAssigned((IRVariable) curInst.getFrom())) {
                        if(curReg.equals(getCorrespondReg((IRVariable) curInst.getFrom()))) { foundFlag=false; }
                    }
                } else {
                    if(isAssigned(curInst.getResult())) {
                        if(curReg.equals(getCorrespondReg(curInst.getResult()))) { terminateFlag=false; }
                    }
                    if(curInst.getLHS().isImmediate()) { continue; }
                    if(isAssigned((IRVariable) curInst.getLHS())) {
                        if(curReg.equals(getCorrespondReg((IRVariable) curInst.getLHS()))) { foundFlag=false; }
                    }
                    if(curInst.getRHS().isImmediate()) { continue; }
                    if(isAssigned((IRVariable) curInst.getRHS())) {
                        if(curReg.equals(getCorrespondReg((IRVariable) curInst.getRHS()))) { foundFlag=false; }
                    }
                }
            }
            if(foundFlag) {
                variableList.set(i, null);
            }
        }
    }


    public RegisterList getNewReg(int curInst) {
        findAvailableReg(curInst);
        for(int i=0; i<RegisterList.values().length; i++) {
            if(variableList.get(i) == null) {
                return existingRegisters.get(i);
            }
        }
        return null;
    }


    public RegisterList getCorrespondReg(IRVariable destVar) {
        for(int i=0; i<RegisterList.values().length; i++) {
            if(destVar.equals(variableList.get(i))) {
                return existingRegisters.get(i);
            }
        }
        return null;
    }
}

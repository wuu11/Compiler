package cn.edu.hitsz.compiler.asm;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.ir.*;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;


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

    private final List<Instruction> instructions = new ArrayList<>();
    private final HashMap<Integer, IRVariable> irVariables = new HashMap<>();
    private final BMap<IRVariable, Reg> regAlloc = new BMap<>();
    private final List<String> assemblyCode = new ArrayList<>();

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
        int i = 0;
        for (final var instruction : originInstructions) {
            if (instruction.getKind().isBinary()) {
                if (instruction.getLHS().isImmediate() && instruction.getRHS().isImmediate()) {
                    IRImmediate immediate1 = (IRImmediate) instruction.getLHS();
                    IRImmediate immediate2 = (IRImmediate) instruction.getRHS();
                    int value;
                    switch (instruction.getKind()) {
                        case ADD :
                            value = immediate1.getValue() + immediate2.getValue();
                            break;
                        case SUB :
                            value = immediate1.getValue() - immediate2.getValue();
                            break;
                        default :
                            value = immediate1.getValue() * immediate2.getValue();
                            break;
                    }
                    instructions.add(Instruction.createMov(instruction.getResult(), IRImmediate.of(value)));
                    irVariables.put(i++, instruction.getResult());
                } else if (instruction.getKind() == InstructionKind.MUL && instruction.getLHS().isImmediate()){
                    IRImmediate immediate = (IRImmediate) instruction.getLHS();
                    IRVariable temp = IRVariable.temp();
                    instructions.add(Instruction.createMov(temp, immediate));
                    irVariables.put(i++, temp);
                    instructions.add(Instruction.createMul(instruction.getResult(), temp, instruction.getRHS()));
                    irVariables.put(i++, instruction.getResult());
                    irVariables.put(i++, temp);
                    irVariables.put(i++, (IRVariable) instruction.getRHS());
                } else if (instruction.getKind() == InstructionKind.MUL && instruction.getRHS().isImmediate()){
                    IRImmediate immediate = (IRImmediate) instruction.getRHS();
                    IRVariable temp = IRVariable.temp();
                    instructions.add(Instruction.createMov(temp, immediate));
                    irVariables.put(i++, temp);
                    instructions.add(Instruction.createMul(instruction.getResult(), instruction.getLHS(), temp));
                    irVariables.put(i++, instruction.getResult());
                    irVariables.put(i++, (IRVariable)  instruction.getLHS());
                    irVariables.put(i++, temp);
                } else if (instruction.getKind() == InstructionKind.SUB && instruction.getLHS().isImmediate()) {
                    IRImmediate immediate = (IRImmediate) instruction.getLHS();
                    IRVariable temp = IRVariable.temp();
                    instructions.add(Instruction.createMov(temp, immediate));
                    irVariables.put(i++, temp);
                    instructions.add(Instruction.createSub(instruction.getResult(), temp, instruction.getRHS()));
                    irVariables.put(i++, instruction.getResult());
                    irVariables.put(i++, temp);
                    irVariables.put(i++, (IRVariable) instruction.getRHS());
                } else if (instruction.getLHS().isImmediate()) {
                    //只可能是ADD指令
                    instructions.add(Instruction.createAdd(instruction.getResult(), instruction.getRHS(), instruction.getLHS()));
                    irVariables.put(i++,instruction.getResult());
                    irVariables.put(i++, (IRVariable) instruction.getRHS());
                } else {
                    instructions.add(instruction);
                    irVariables.put(i++, instruction.getResult());
                    irVariables.put(i++, (IRVariable) instruction.getLHS());
                    if (instruction.getRHS().isIRVariable()) {
                        irVariables.put(i++, (IRVariable) instruction.getRHS());
                    }
                }
            } else if (instruction.getKind().isReturn()) {
                instructions.add(instruction);
                if (instruction.getReturnValue().isIRVariable()) {
                    irVariables.put(i++, (IRVariable) instruction.getReturnValue());
                }
                break;
            } else {
                instructions.add(instruction);
                irVariables.put(i++, instruction.getResult());
                if (instruction.getFrom().isIRVariable()) {
                    irVariables.put(i++, (IRVariable) instruction.getFrom());
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
        String code;
        Reg[] regs = Reg.values();
        int flag = 0;
        int i = 0;
        int use = 0;
        assemblyCode.add(".text");
        for (final var instruction : instructions) {
            code = "    ";
            switch (instruction.getKind()) {
                case MOV :
                    if (instruction.getFrom().isImmediate()) {
                        code += "li";
                    } else {
                        code += "mv";
                    }
                    break;
                case ADD :
                    if (instruction.getRHS().isImmediate()) {
                        code += "addi";
                    } else {
                        code += "add";
                    }
                    break;
                case SUB :
                    if (instruction.getRHS().isImmediate()) {
                        code += "subi";
                    } else {
                        code += "sub";
                    }
                    break;
                case MUL :
                    code += "mul";
                    break;
                case RET :
                    code += "mv";
                    break;
                default :
                    code += " ";
                    break;
            }
            code += " ";
            if (instruction.getKind().isBinary() || instruction.getKind().isUnary()) {
                if (!regAlloc.containsKey(instruction.getResult())) {
                    flag = 0;
                    for (final var reg : regs) {
                        if (!regAlloc.containsValue(reg)) {
                            regAlloc.replace(instruction.getResult(), reg);
                            flag = 1;
                            break;
                        }
                    }
                    if (flag == 0) {
                        //没有空闲寄存器
                        for (final var reg : regs) {
                            use = 0;
                            IRVariable irVariable = regAlloc.getByValue(reg);
                            for (int j = i; j < irVariables.size(); j++) {
                                if (irVariables.get(j).equals(irVariable)) {
                                    use = 1;
                                    break;
                                }
                            }
                            if (use == 0) {
                                //该变量后续不再使用
                                regAlloc.replace(instruction.getResult(), regAlloc.getByKey(irVariable));
                                break;
                            }
                        }
                    }
                }
                code += regAlloc.getByKey(instruction.getResult()).name();
                i++;
            } else {
                code += "a0";
            }
            code += ", ";
            if (instruction.getKind().isBinary()) {
                IRVariable irVariable = (IRVariable) instruction.getLHS();
                code += regAlloc.getByKey(irVariable).name();
                i++;
                code += ", ";
                if (instruction.getRHS().isImmediate()) {
                    IRImmediate immediate = (IRImmediate) instruction.getRHS();
                    code += immediate.toString();
                } else {
                    irVariable = (IRVariable) instruction.getRHS();
                    code += regAlloc.getByKey(irVariable).name();
                    i++;
                }
            } else if (instruction.getKind().isUnary()) {
                if (instruction.getFrom().isImmediate()) {
                    IRImmediate immediate = (IRImmediate) instruction.getFrom();
                    code += immediate.toString();
                } else {
                    IRVariable irVariable = (IRVariable) instruction.getFrom();
                    code += regAlloc.getByKey(irVariable).name();
                    i++;
                }
            } else {
                if (instruction.getReturnValue().isImmediate()) {
                    IRImmediate immediate = (IRImmediate) instruction.getReturnValue();
                    code += immediate.toString();
                } else {
                    IRVariable irVariable = (IRVariable) instruction.getReturnValue();
                    code += regAlloc.getByKey(irVariable).name();
                    i++;
                }
            }
            code += "      # ";
            code += instruction.toString();
            assemblyCode.add(code);
        }
    }


    /**
     * 输出汇编代码到文件
     *
     * @param path 输出文件路径
     */
    public void dump(String path) {
        // TODO: 输出汇编代码到文件
        FileUtils.writeLines(path, assemblyCode.stream().toList());
    }
}


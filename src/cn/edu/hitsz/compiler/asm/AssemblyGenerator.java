package cn.edu.hitsz.compiler.asm;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.ir.*;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;


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
    enum REG {
        t0, t1, t2, t3, t4, t5, t6
    }
    private final List<Instruction> insts = new ArrayList<>();
    Map<IRValue, REG> v2r = new HashMap<>();
    Map<REG, IRValue> r2v = new HashMap<>();
    private final List<String> asm = new ArrayList<>(List.of(".text"));
    

    /**
     * 加载前端提供的中间代码
     * <br>
     * 视具体实现而定, 在加载中或加载后会生成一些在代码生成中会用到的信息. 如变量的引用
     * 信息. 这些信息可以通过简单的映射维护, 或者自行增加记录信息的数据结构.
     *
     * @param originInstructions 前端提供的中间代码
     */
    public void loadIR(List<Instruction> originInstructions) {
        for(var inst : originInstructions) {
            InstructionKind inst_kind = inst.getKind();
            if(inst_kind.isReturn()) {
                insts.add(inst);
                break;
            }
            if(inst_kind.isUnary()) {
                insts.add(inst);
                continue;
            } 
            if(inst_kind.isBinary()) {
                var lhs = inst.getLHS();
                var rhs = inst.getRHS();
                var result = inst.getResult();
                if(lhs.isImmediate() && rhs.isImmediate()) {
                    // comp-time calculate
                    int imm_res = 0;
                    var imm_lhs = ((IRImmediate)lhs).getValue();
                    var imm_rhs = ((IRImmediate)rhs).getValue();
                    switch(inst_kind) {
                        case ADD -> imm_res = imm_lhs + imm_rhs;
                        case SUB -> imm_res = imm_lhs - imm_rhs;
                        case MUL -> imm_res = imm_lhs * imm_rhs;
                        default -> System.out.println("error");
                    }
                    insts.add(Instruction.createMov(result, IRImmediate.of(imm_res)));
                } else if(lhs.isImmediate() && rhs.isIRVariable()) {
                    switch(inst_kind) {
                        case ADD -> insts.add(Instruction.createAdd(result, rhs, lhs));
                        case SUB -> {
                            IRVariable temp = IRVariable.temp();
                            insts.add(Instruction.createMov(temp, lhs));
                            insts.add(Instruction.createSub(result, temp, rhs));
                        }
                        case MUL -> {
                            IRVariable temp = IRVariable.temp();
                            insts.add(Instruction.createMov(temp, lhs));
                            insts.add(Instruction.createMul(result, temp, rhs));
                        }
                        default -> System.out.println("error");
                    }
                } else if(lhs.isIRVariable() && rhs.isImmediate()) {
                    switch(inst_kind) {
                        case ADD, SUB -> insts.add(inst);
                        case MUL -> {
                            IRVariable temp = IRVariable.temp();
                            insts.add(Instruction.createMov(temp, rhs));
                            insts.add(Instruction.createMul(result, lhs, temp));
                        }
                    }
                } else {
                    insts.add(inst);
                }

            }
        }
    }
    
    public void allocate(IRValue oprands, int idx) {
        if(oprands.isImmediate()) return;
        if(v2r.containsKey(oprands)) return;
        for(var reg : REG.values()) {
            if(!r2v.containsKey(reg)) {
                r2v.put(reg, oprands);
                v2r.put(oprands, reg);
                return;
            }
        }
        Set<REG> unused = Arrays.stream(REG.values()).collect(Collectors.toSet());
        for(int i = idx; i < insts.size(); ++i) {
            var inst = insts.get(i);
            for(var irv : inst.getOprands()) {
                unused.remove(v2r.get(irv));
            }
        }
        if(!unused.isEmpty()) {
            var touse = unused.iterator().next();
            r2v.put(touse, oprands);
            v2r.put(oprands, touse);
            return;
        }
        throw new RuntimeException("No enough registers");
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
        int i = 0;
        String code = null;
        for(var inst : insts) {
            var inst_kind = inst.getKind();
            switch(inst_kind) {
                case ADD -> {
                    var lhs = inst.getLHS();
                    var rhs = inst.getRHS();
                    var result = inst.getResult();
                    this.allocate(lhs, i);
                    this.allocate(rhs, i);
                    this.allocate(result, i);
                    var reg_lhs = v2r.get(lhs);
                    var reg_rhs = v2r.get(rhs);
                    var reg_result = v2r.get(result);
                    if(rhs.isImmediate()) {
                        code = String.format("\taddi %s, %s, %s", reg_result.toString(), reg_lhs.toString(), rhs.toString());
                    } else {
                        code = String.format("\tadd %s, %s, %s", reg_result.toString(), reg_lhs.toString(), reg_rhs.toString());
                    }
                }
                case SUB -> {
                    var lhs = inst.getLHS();
                    var rhs = inst.getRHS();
                    var result = inst.getResult();
                    this.allocate(lhs, i);
                    this.allocate(rhs, i);
                    this.allocate(result, i);
                    var reg_lhs = v2r.get(lhs);
                    var reg_rhs = v2r.get(rhs);
                    var reg_result = v2r.get(result);
                    if(rhs.isImmediate()) {
                        code = String.format("\tsubi %s, %s, %s", reg_result.toString(), reg_lhs.toString(), rhs.toString());
                    } else {
                        code = String.format("\tsub %s, %s, %s", reg_result.toString(), reg_lhs.toString(), reg_rhs.toString());
                    }
                }
                case MUL -> {
                    var lhs = inst.getLHS();
                    var rhs = inst.getRHS();
                    var result = inst.getResult();
                    this.allocate(lhs, i);
                    this.allocate(rhs, i);
                    this.allocate(result, i);
                    var reg_lhs = v2r.get(lhs);
                    var reg_rhs = v2r.get(rhs);
                    var reg_result = v2r.get(result);
                    code = String.format("\tmul %s, %s, %s", reg_result.toString(), reg_lhs.toString(), reg_rhs.toString());
                }
                case MOV -> {
                    var from = inst.getFrom();
                    var to = inst.getResult();
                    this.allocate(from, i);
                    this.allocate(to, i);
                    var reg_from = v2r.get(from);
                    var reg_to = v2r.get(to);
                    if(from.isImmediate()) {
                        code = String.format("\tli %s, %s", reg_to.toString(), from.toString());
                    } else {
                        code = String.format("\tmov %s, %s", reg_to.toString(), reg_from.toString());
                    }
                }
                case RET -> {
                    var ret = inst.getReturnValue();
                    var return_reg = v2r.get(ret);
                    code = String.format("\tmv a0, %s", return_reg.toString());
                }
                default -> {
                    System.out.println("wrong asm!!!");
                    System.out.println(inst);
                }
            }
            code += "\t# %s".formatted(inst.toString()); // append source
            asm.add(code);
            i++;
            if(inst_kind == InstructionKind.RET) {
                break;
            }
        }
    }


    /**
     * 输出汇编代码到文件
     *
     * @param path 输出文件路径
     */
    public void dump(String path) {
        FileUtils.writeLines(path, asm);
    }
}


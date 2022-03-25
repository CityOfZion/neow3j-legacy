package io.neow3j.compiler.converters;

import io.neow3j.compiler.CompilationUnit;
import io.neow3j.compiler.Compiler;
import io.neow3j.compiler.CompilerException;
import io.neow3j.compiler.JVMOpcode;
import io.neow3j.compiler.NeoEvent;
import io.neow3j.compiler.NeoInstruction;
import io.neow3j.compiler.NeoMethod;
import io.neow3j.compiler.SuperNeoMethod;
import io.neow3j.devpack.annotations.Instruction;
import io.neow3j.devpack.annotations.Struct;
import io.neow3j.script.InteropService;
import io.neow3j.script.OpCode;
import io.neow3j.types.StackItemType;
import io.neow3j.utils.Numeric;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import java.io.IOException;
import java.util.List;

import static io.neow3j.compiler.AsmHelper.getAsmClass;
import static io.neow3j.compiler.AsmHelper.getAsmClassForInternalName;
import static io.neow3j.compiler.AsmHelper.getMethodNode;
import static io.neow3j.compiler.AsmHelper.hasAnnotations;
import static io.neow3j.compiler.Compiler.addPushNumber;
import static io.neow3j.compiler.Compiler.addReverseArguments;
import static io.neow3j.compiler.Compiler.buildPushDataInsn;
import static io.neow3j.compiler.Compiler.handleInsn;
import static io.neow3j.compiler.Compiler.isAssertionDisabledStaticField;
import static io.neow3j.compiler.Compiler.isCallToCtor;
import static io.neow3j.compiler.Compiler.isEvent;
import static io.neow3j.compiler.Compiler.processInstructionAnnotations;
import static io.neow3j.compiler.Compiler.skipToCtorCall;
import static io.neow3j.compiler.Compiler.skipToSuperCtorCall;
import static io.neow3j.compiler.LocalVariableHelper.buildStoreOrLoadVariableInsn;
import static io.neow3j.utils.ClassUtils.getClassNameForInternalName;
import static io.neow3j.utils.ClassUtils.getFullyQualifiedNameForInternalName;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.objectweb.asm.Type.getInternalName;

public class ObjectsConverter implements Converter {

    private static final String APPEND_METHOD_NAME = "append";
    private static final String TOSTRING_METHOD_NAME = "toString";

    @Override
    public AbstractInsnNode convert(AbstractInsnNode insn, NeoMethod neoMethod,
            CompilationUnit compUnit) throws IOException {

        JVMOpcode opcode = JVMOpcode.get(insn.getOpcode());
        switch (requireNonNull(opcode)) {
            case PUTSTATIC:
                addStoreStaticField((FieldInsnNode) insn, neoMethod, compUnit);
                break;
            case GETSTATIC:
                FieldInsnNode fieldInsn = (FieldInsnNode) insn;
                if (isEvent(fieldInsn.desc)) {
                    if (neoMethod.isVerifyMethod()) {
                        throw new CompilerException(neoMethod,
                                "The verify method is not allowed to fire any event.");
                    }
                    insn = convertEvent(fieldInsn, neoMethod, compUnit);
                } else if (isAssertionDisabledStaticField(fieldInsn)) {
                    insn = fieldInsn.getNext();
                } else {
                    addLoadStaticField(fieldInsn, neoMethod, compUnit);
                }
                break;
            case CHECKCAST:
                // Check if the object on the operand stack can be cast to a given type.
                // There is no corresponding NeoVM opcode.
                break;
            case NEW:
                TypeInsnNode typeInsn = (TypeInsnNode) insn;
                if (isStruct(typeInsn.desc, compUnit)) {
                    insn = handleNewStruct(insn, neoMethod, compUnit);
                } else {
                    insn = handleNew(insn, neoMethod, compUnit);
                }
                break;
            case ARRAYLENGTH:
                neoMethod.addInstruction(new NeoInstruction(OpCode.SIZE));
                break;
            case INSTANCEOF:
                handleInstanceOf((TypeInsnNode) insn, neoMethod);
                break;
        }
        return insn;
    }

    private boolean isStruct(String desc, CompilationUnit compUnit) throws IOException {
        ClassNode asmClass = getAsmClass(getFullyQualifiedNameForInternalName(desc), compUnit.getClassLoader());
        return hasAnnotations(asmClass, Struct.class);
    }

    private void handleInstanceOf(TypeInsnNode typeInsn, NeoMethod neoMethod) {
        Type type;
        if (typeInsn.desc.contains("/") && !typeInsn.desc.startsWith("[")) {
            // In this specific descriptor ASM doesn't add the "L" in front, when it's an object.
            // Without it the "L", Type.getType() fails.
            type = Type.getType("L" + typeInsn.desc + ";");
        } else {
            type = Type.getType(typeInsn.desc);
        }
        StackItemType stackItemType = Compiler.mapTypeToStackItemType(type);
        if (stackItemType.equals(StackItemType.BOOLEAN)) {
            // The Boolean stack item almost never appears because bool values are usually
            // represented as 0 and 1 valued integer stack items.
            stackItemType = StackItemType.INTEGER;
        }
        if (stackItemType.equals(StackItemType.ANY)) {
            throw new CompilerException(neoMethod, format("The type '%s' is not supported for " +
                    "the instanceof operation.", getFullyQualifiedNameForInternalName(
                    type.getInternalName())));
        }
        neoMethod.addInstruction(
                new NeoInstruction(OpCode.ISTYPE, new byte[]{stackItemType.byteValue()}));
    }

    public static void addLoadStaticField(FieldInsnNode fieldInsn, NeoMethod neoMethod,
            CompilationUnit compUnit) throws IOException {
        int neoVmIdx = compUnit.getNeoModule().getContractVariable(fieldInsn, compUnit).getNeoIdx();
        neoMethod.addInstruction(buildStoreOrLoadVariableInsn(neoVmIdx, OpCode.LDSFLD));
    }

    public static void addStoreStaticField(FieldInsnNode fieldInsn, NeoMethod neoMethod,
            CompilationUnit compUnit) throws IOException {
        int neoVmIdx = compUnit.getNeoModule().getContractVariable(fieldInsn, compUnit).getNeoIdx();
        neoMethod.addInstruction(buildStoreOrLoadVariableInsn(neoVmIdx, OpCode.STSFLD));
    }

    public static AbstractInsnNode handleNewStruct(AbstractInsnNode insn, NeoMethod callingNeoMethod,
            CompilationUnit compUnit) throws IOException {

        TypeInsnNode typeInsn = (TypeInsnNode) insn;
        assert typeInsn.getNext().getOpcode() == JVMOpcode.DUP.getOpcode()
                : "Expected DUP after NEW but got other instructions";

        ClassNode ownerClassNode = getAsmClassForInternalName(typeInsn.desc, compUnit.getClassLoader());
        MethodInsnNode ctorMethodInsn = skipToCtorCall(typeInsn.getNext(), ownerClassNode);
        MethodNode ctorMethod = getMethodNode(ctorMethodInsn, ownerClassNode).orElseThrow(() ->
                new CompilerException(callingNeoMethod, format("Couldn't find constructor '%s' on class '%s'.",
                        ctorMethodInsn.name, getClassNameForInternalName(ownerClassNode.name))));

        // Annotations in struct constructors are ignored
        if (ctorMethod.invisibleAnnotations == null || ctorMethod.invisibleAnnotations.size() == 0) {
            // It's a generic struct constructor without any Neo-specific annotations.
            return convertStructConstructorCall(typeInsn, ctorMethod, ownerClassNode, callingNeoMethod, compUnit);
        } else {
            // The constructor has some Neo-specific annotation.
            // skip NEW and DUP.
            insn = insn.getNext().getNext();
            // Process possible arguments passed to INVOKESPECIAL.
            while (!isCallToCtor(insn, ownerClassNode.name)) {
                insn = handleInsn(insn, callingNeoMethod, compUnit);
                insn = insn.getNext();
            }
            // Now we're at the INVOKESPECIAL call and can convert the ctor method.
            if (hasAnnotations(ctorMethod, Instruction.class, Instruction.Instructions.class)) {
                processInstructionAnnotations(ctorMethod, callingNeoMethod);
            }
            return insn;
        }
    }

    public static AbstractInsnNode handleNew(AbstractInsnNode insn, NeoMethod callingNeoMethod,
            CompilationUnit compUnit) throws IOException {

        TypeInsnNode typeInsn = (TypeInsnNode) insn;
        assert typeInsn.getNext().getOpcode() == JVMOpcode.DUP.getOpcode()
                : "Expected DUP after NEW but got other instructions";

        if (isNewStringBuilder(typeInsn)) {
            // Java, in the background, performs String concatenation, like `s1 + s2`, with the
            // instantiation of a StringBuilder. This is handled here.
            return handleStringConcatenation(typeInsn, callingNeoMethod, compUnit);
        }

        if (isNewThrowable(typeInsn, compUnit)) {
            return handleNewThrowable(typeInsn, callingNeoMethod, compUnit);
        }

        ClassNode owner = getAsmClassForInternalName(typeInsn.desc, compUnit.getClassLoader());
        MethodInsnNode ctorMethodInsn = skipToCtorCall(typeInsn.getNext(), owner);
        MethodNode ctorMethod = getMethodNode(ctorMethodInsn, owner).orElseThrow(() ->
                new CompilerException(callingNeoMethod, format(
                        "Couldn't find constructor '%s' on class '%s'.",
                        ctorMethodInsn.name, getClassNameForInternalName(owner.name))));

        if (ctorMethod.invisibleAnnotations == null
                || ctorMethod.invisibleAnnotations.size() == 0) {
            // It's a generic constructor without any Neo-specific annotations.
            return convertConstructorCall(typeInsn, ctorMethod, owner, callingNeoMethod, compUnit);
        } else { // The constructor has some Neo-specific annotation.
            // skip NEW and DUP.
            insn = insn.getNext().getNext();
            // Process possible arguments passed to INVOKESPECIAL.
            while (!isCallToCtor(insn, owner.name)) {
                insn = handleInsn(insn, callingNeoMethod, compUnit);
                insn = insn.getNext();
            }
            // Now we're at the INVOKESPECIAL call and can convert the ctor method.
            if (hasAnnotations(ctorMethod, Instruction.class, Instruction.Instructions.class)) {
                processInstructionAnnotations(ctorMethod, callingNeoMethod);
            }
            return insn;
        }
    }

    private static boolean isNewStringBuilder(TypeInsnNode typeInsn) {
        return typeInsn.desc.equals(getInternalName(StringBuilder.class));
    }

    private static boolean isNewThrowable(TypeInsnNode typeInsn,
            CompilationUnit compUnit) throws IOException {

        ClassNode type = getAsmClassForInternalName(typeInsn.desc, compUnit.getClassLoader());

        if (getFullyQualifiedNameForInternalName(type.name).equals(
                Throwable.class.getCanonicalName())) {
            return true;
        }
        while (type.superName != null) {
            type = getAsmClassForInternalName(type.superName, compUnit.getClassLoader());
            if (getFullyQualifiedNameForInternalName(type.name).equals(
                    Throwable.class.getCanonicalName())) {
                return true;
            }
        }
        return false;
    }

    private static AbstractInsnNode handleNewThrowable(TypeInsnNode typeInsn,
            NeoMethod callingNeoMethod, CompilationUnit compUnit) throws IOException {

        String fullyQualifiedExceptionName = getFullyQualifiedNameForInternalName(typeInsn.desc);
        ThrowableType throwableType = getThrowableType(fullyQualifiedExceptionName);
        if (throwableType.equals(ThrowableType.OTHER)) {
            throw new CompilerException(callingNeoMethod, format("Contract uses exception of type" +
                            " %s but only %s and %s are allowed.", fullyQualifiedExceptionName,
                    Exception.class.getCanonicalName(), AssertionError.class.getCanonicalName()));
        }
        // Skip to the next instruction after DUP.
        AbstractInsnNode insn = typeInsn.getNext().getNext();
        // Process any instructions that come before the INVOKESPECIAL, e.g., a PUSHDATA insn.
        while (!isCallToCtor(insn, Type.getType(Exception.class).getInternalName()) &&
                !isCallToCtor(insn, Type.getType(AssertionError.class).getInternalName())) {
            insn = handleInsn(insn, callingNeoMethod, compUnit);
            insn = insn.getNext();
        }

        Type[] argTypes = Type.getType(((MethodInsnNode) insn).desc).getArgumentTypes();
        checkForInvalidExceptionArguments(argTypes, throwableType, callingNeoMethod);

        if (argTypes.length == 0) {
            // No exception message is given, thus a dummy message is added.
            String dummyMessage = "error";
            if (throwableType.equals(ThrowableType.ASSERTION)) {
                dummyMessage = "assertion failed";
            }
            callingNeoMethod.addInstruction(buildPushDataInsn(dummyMessage));
        }
        return insn;
    }

    private static void checkForInvalidExceptionArguments(Type[] argTypes,
            ThrowableType throwableType, NeoMethod callingNeoMethod) {

        if (argTypes.length > 1) {
            throw new CompilerException(callingNeoMethod, format("An exception thrown in a contract"
                    + " can either take no arguments or a String argument. You provided %d "
                    + "arguments.", argTypes.length));
        }

        // Only string messages are allowed in exceptions. In assert statements, this cannot
        // be checked properly. Therefore, if an assertion message is not a string, it is
        // ignored when converting it.
        if (argTypes.length == 1 && !throwableType.equals(ThrowableType.ASSERTION) &&
                !getFullyQualifiedNameForInternalName(argTypes[0].getInternalName())
                        .equals(String.class.getCanonicalName())) {
            throw new CompilerException(callingNeoMethod, "An exception thrown in a contract " +
                    "can either take no arguments or a String argument. You provided a " +
                    "non-string argument.");
        }
    }

    private static ThrowableType getThrowableType(String fullyQualifiedExceptionName) {
        if (Exception.class.getCanonicalName().equals(fullyQualifiedExceptionName)) {
            return ThrowableType.EXCEPTION;
        }
        if (AssertionError.class.getCanonicalName().equals(fullyQualifiedExceptionName)) {
            return ThrowableType.ASSERTION;
        }
        return ThrowableType.OTHER;
    }

    private enum ThrowableType {
        EXCEPTION,
        ASSERTION,
        OTHER
    }

    /**
     * Handles the concatenation of strings, as in {@code "hello" + " world"}. Java in the
     * background uses a StringBuilder for this.
     *
     * @param typeInsnNode The NEW instruction concerning the StringBuilder.
     * @return the last processed instruction.
     */
    private static AbstractInsnNode handleStringConcatenation(TypeInsnNode typeInsnNode,
            NeoMethod neoMethod, CompilationUnit compUnit) throws IOException {

        // Skip to the next instruction after DUP and INVOKESPECIAL.
        AbstractInsnNode insn = typeInsnNode.getNext().getNext().getNext();

        boolean isFirstCall = true;
        while (insn != null) {
            if (isCallToStringBuilderAppend(insn)) {
                if (!isFirstCall) {
                    neoMethod.addInstruction(new NeoInstruction(OpCode.CAT));
                }
                isFirstCall = false;
                insn = insn.getNext();
                continue;
            }
            if (isCallToStringBuilderToString(insn)) {
                neoMethod.addInstruction(new NeoInstruction(OpCode.CONVERT,
                        new byte[]{StackItemType.BYTE_STRING.byteValue()}));
                break; // End of string concatenation.
            }
            if (isCallToAnyStringBuilderMethod(insn)) {
                throw new CompilerException(neoMethod, format("Only 'append()' and 'toString()' "
                                + "are supported for StringBuilder, but '%s' was called",
                        ((MethodInsnNode) insn).name));
            }
            insn = handleInsn(insn, neoMethod, compUnit);
            insn = insn.getNext();
        }
        if (insn == null) {
            throw new CompilerException(neoMethod, "Expected to find ScriptBuilder.toString() but "
                    + "reached end of method.");
        }
        return insn;
    }

    private static boolean isCallToStringBuilderAppend(AbstractInsnNode insn) {
        return insn instanceof MethodInsnNode
                && ((MethodInsnNode) insn).owner.equals(Type.getInternalName(StringBuilder.class))
                && ((MethodInsnNode) insn).name.equals(APPEND_METHOD_NAME);
    }

    private static boolean isCallToStringBuilderToString(AbstractInsnNode insn) {
        return insn instanceof MethodInsnNode
                && ((MethodInsnNode) insn).owner.equals(Type.getInternalName(StringBuilder.class))
                && ((MethodInsnNode) insn).name.equals(TOSTRING_METHOD_NAME);
    }

    private static boolean isCallToAnyStringBuilderMethod(AbstractInsnNode insn) {
        return insn instanceof MethodInsnNode
                && ((MethodInsnNode) insn).owner.equals(Type.getInternalName(StringBuilder.class));
    }

    private static AbstractInsnNode convertStructConstructorCall(TypeInsnNode typeInsnNode, MethodNode ctorMethod,
            ClassNode structClassNode, NeoMethod callingNeoMethod, CompilationUnit compUnit) throws IOException {

        SuperNeoMethod calledNeoMethod;
        String ctorMethodId = NeoMethod.getMethodId(ctorMethod, structClassNode);
        int fieldSize = calculateFieldSize(structClassNode, compUnit);
        if (compUnit.getNeoModule().hasMethod(ctorMethodId)) {
            // If the module already contains the converted ctor.
            calledNeoMethod = (SuperNeoMethod) compUnit.getNeoModule().getMethod(ctorMethodId);
        } else {
            // Create a new NeoMethod, i.e., convert the constructor to NeoVM code.
            calledNeoMethod = new SuperNeoMethod(ctorMethod, structClassNode);
            compUnit.getNeoModule().addMethod(calledNeoMethod);
            calledNeoMethod.initialize(compUnit);
            calledNeoMethod.convert(compUnit);
        }
        return finalizeConstructorCall(fieldSize, typeInsnNode, ctorMethod, structClassNode, callingNeoMethod,
                calledNeoMethod, compUnit);
    }

    private static AbstractInsnNode finalizeConstructorCall(int fieldSize, TypeInsnNode typeInsnNode,
            MethodNode ctorMethod, ClassNode structClassNode, NeoMethod callingNeoMethod, NeoMethod calledNeoMethod,
            CompilationUnit compUnit) throws IOException {

        addPushNumber(fieldSize, callingNeoMethod);
        callingNeoMethod.addInstruction(new NeoInstruction(OpCode.NEWARRAY));
        // TODO: Determine when to use NEWSTRUCT.
        callingNeoMethod.addInstruction(new NeoInstruction(OpCode.DUP));
        // After the JVM NEW and DUP, arguments that will be given to the INVOKESPECIAL call can
        // follow. Those are handled in the following while.
        AbstractInsnNode insn = typeInsnNode.getNext().getNext();
        while (!isCallToCtor(insn, structClassNode.name)) {
            insn = handleInsn(insn, callingNeoMethod, compUnit);
            insn = insn.getNext();
        }
        // Reverse the arguments that are passed to the constructor call.
        addReverseArguments(ctorMethod, callingNeoMethod);
        // The actual address offset for the method call is set at a later point.
        callingNeoMethod.addInstruction(new NeoInstruction(OpCode.CALL_L, new byte[4]).setExtra(calledNeoMethod));
        return insn;
    }

    // Calculates the struct's field size, i.e., including its inherited fields.
    private static int calculateFieldSize(ClassNode structClassNode, CompilationUnit compUnit) throws IOException {
        int fieldSize = structClassNode.fields.size();
        ClassNode currentClassNode = structClassNode;
        while (!getFullyQualifiedNameForInternalName(currentClassNode.superName)
                .equals(Object.class.getCanonicalName())) {
            throwIfSuperIsNotStruct(currentClassNode.superName, compUnit);
            currentClassNode = getAsmClass(currentClassNode.superName, compUnit.getClassLoader());
            fieldSize += currentClassNode.fields.size();
        }
        return fieldSize;
    }

    private static void throwIfSuperIsNotStruct(String superName, CompilationUnit compUnit) throws IOException {
        ClassNode superClassNode = getAsmClass(superName, compUnit.getClassLoader());
        if (!hasAnnotations(superClassNode, Struct.class)) {
            throw new CompilerException(format("Struct classes are not allowed to inherit non-struct classes. %s was " +
                    "inherited by a struct class.", superName));
        }
    }

    private static AbstractInsnNode convertConstructorCall(TypeInsnNode typeInsn, MethodNode ctorMethod,
            ClassNode classNode, NeoMethod callingNeoMethod, CompilationUnit compUnit) throws IOException {

        NeoMethod calledNeoMethod;
        String ctorMethodId = NeoMethod.getMethodId(ctorMethod, classNode);
        if (compUnit.getNeoModule().hasMethod(ctorMethodId)) {
            // If the module already contains the converted ctor.
            calledNeoMethod = compUnit.getNeoModule().getMethod(ctorMethodId);
        } else {
            // Create a new NeoMethod, i.e., convert the constructor to NeoVM code.
            // Skip the call to the Object ctor and continue processing the rest of the ctor.
            calledNeoMethod = new NeoMethod(ctorMethod, classNode);
            compUnit.getNeoModule().addMethod(calledNeoMethod);
            calledNeoMethod.initialize(compUnit);
            AbstractInsnNode insn = skipToSuperCtorCall(ctorMethod, classNode);
            insn = insn.getNext();
            while (insn != null) {
                insn = handleInsn(insn, calledNeoMethod, compUnit);
                insn = insn.getNext();
            }
        }
        int fieldSize = classNode.fields.size();
        return finalizeConstructorCall(fieldSize, typeInsn, ctorMethod, classNode, callingNeoMethod, calledNeoMethod,
                compUnit);
    }


    private static AbstractInsnNode convertEvent(FieldInsnNode eventFieldInsn, NeoMethod neoMethod,
            CompilationUnit compUnit) throws IOException {

        String eventVariableName = eventFieldInsn.name;
        List<NeoEvent> events = compUnit.getNeoModule().getEvents();
        NeoEvent event = events.stream()
                .filter(e -> eventVariableName.equals(e.getAsmVariable().name))
                .findFirst().orElseThrow(() -> new CompilerException(neoMethod, "Couldn't find " +
                        "triggered event in list of events. Make sure to declare events only in" +
                        " the main contract class."));

        AbstractInsnNode insn = eventFieldInsn.getNext();
        while (!isMethodCallToEventSend(insn)) {
            insn = handleInsn(insn, neoMethod, compUnit);
            insn = insn.getNext();
            assert insn != null : "Expected to find call to send() method of an event but reached"
                    + " the end of the instructions.";
        }

        // The current instruction is the method call to Event.send(...). We can pack the arguments
        // and do the syscall instead of actually calling the send(...) method.
        addReverseArguments(neoMethod, event.getNumberOfParams());
        addPushNumber(event.getNumberOfParams(), neoMethod);
        neoMethod.addInstruction(new NeoInstruction(OpCode.PACK));
        neoMethod.addInstruction(buildPushDataInsn(event.getDisplayName()));
        byte[] syscallHash = Numeric.hexStringToByteArray(
                InteropService.SYSTEM_RUNTIME_NOTIFY.getHash());
        neoMethod.addInstruction(new NeoInstruction(OpCode.SYSCALL, syscallHash));
        return insn;
    }

    private static boolean isMethodCallToEventSend(AbstractInsnNode insn) {

        if (!(insn instanceof MethodInsnNode)) {
            return false;
        }
        MethodInsnNode methodInsn = (MethodInsnNode) insn;
        return isEvent(methodInsn.owner);
    }

}

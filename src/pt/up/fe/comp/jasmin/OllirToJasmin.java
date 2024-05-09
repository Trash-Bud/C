package pt.up.fe.comp.jasmin;

import java.util.*;
import java.util.stream.Collectors;

import static org.specs.comp.ollir.AccessModifiers.*;
import static org.specs.comp.ollir.OperationType.LTH;
import static org.specs.comp.ollir.OperationType.NOTB;

import jdk.swing.interop.SwingInterOpUtils;
import org.specs.comp.ollir.*;
import pt.up.fe.comp.Array;
import pt.up.fe.comp.CalledMethod;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

public class OllirToJasmin {

    private final ClassUnit classUnit;
    private final Map<String,String> names;
    private Map<String, Descriptor> varTable;
    private int label = 0;
    private List<String> nexLabel = new ArrayList<>();
    int currentStack = 0;
    int stack_max = 0;

    public OllirToJasmin(ClassUnit classUnit)
    {
        this.classUnit = classUnit;
        names = new HashMap<>();
        getAllFullyQualifiedNames();
    }

    private void getAllFullyQualifiedNames(){
        String lastName;
        for (var importString : classUnit.getImports())     //Manages all the imports declared and makes sure they are handled correctly
        {
            var splitImport = importString.split("\\.");

            if(splitImport.length == 0)
            {
                lastName = importString;
            }
            else
            {
                lastName = splitImport[splitImport.length - 1];
            }

            names.put(lastName, importString.replace('.','/'));
        }
    }


    public String getFullyQualifiedName(String className)
    {

        var fullyQualifiedName = names.get(className);
        if (fullyQualifiedName == null){
            throw new RuntimeException("Could not find import for class " + className);
        }
        else{
            return fullyQualifiedName;
        }
    }

    private String getClassCode(){
        var code = new StringBuilder();  //Var that contains all the jasmin code
        code.append(".class public ").append(classUnit.getClassName()).append("\n");
        return code.toString();
    }

    private String getSuperName(){
        var superName = classUnit.getSuperClass();
        if(superName == null){
            return "java/lang/Object";
        }
        else{
            return getFullyQualifiedName(classUnit.getSuperClass());
        }
    }

    private String getSuperClassCode(){
        var code = new StringBuilder();
        var superName = getSuperName();
        code.append(".super ").append(superName).append("\n");
        return code.toString();
    }

    private String getConstructorCode(){
        var code = new StringBuilder();
        var superName = getSuperName();
        code.append(SpecsIo.getResource("fixtures/public/jasmin/jasminConstructor.template").replace("${SUPER_NAME}", (CharSequence) superName)).append("\n");
        return code.toString();
    }

    public String getCode()
    {
        var code = new StringBuilder();

        code.append(getClassCode()); //class line
        code.append(getSuperClassCode()); //super class line
        code.append(getFieldsCode()); //fields code block
        code.append(getConstructorCode()); //constructor code block

        var methods = classUnit.getMethods();
        methods.remove(0); //first method is constructor so ignore

        for(var method : methods)   //Iterates and handles all the methods/instructions
        {
            varTable = method.getVarTable();
            code.append(getCode(method));
        }

        return code.toString();
    }

    private String getFieldsCode() {
        var code = new StringBuilder();
        var fields = classUnit.getFields();
        for (Field field: fields){
            code.append(".field ").append(field.getFieldName()).append(" ").append(getJasminType(field.getFieldType()));
            code.append("\n");
        }

        return code.toString();
    }

    public String getCode(Method method)
    {
        var code = new StringBuilder();  //Stores the method string

        code.append(".method ");

        if(method.getMethodAccessModifier() != DEFAULT) {
            //Strings together the .method call and the access modifier it has
            code.append(method.getMethodAccessModifier().name().toLowerCase()).append(" ");
        }
        if(method.isStaticMethod()){
            code.append("static").append(" ");
        }

        code.append(method.getMethodName()).append("(");

        //Gets all the parameters for the method we are on
        var methodParamTypes= method.getParams().
                stream().
                map(element -> getJasminType(element.getType())).
                collect(Collectors.joining());

        //Closes the method and gets its return type

        code.append(methodParamTypes).append(")").append(getJasminType(method.getReturnType())).append("\n");

        var limits = new StringBuilder(); //Stores the limits
        var instructions = new StringBuilder(); //Stores the instructions
        int locals_max = varTable.size();

        //code.append(".limit stack 99\n");
        //code.append(".limit locals 99\n");


        for(var inst : method.getInstructions())  //Iterates through all the instructions inside a method
        {
            instructions.append(generate(inst));
        }


        limits.append(".limit stack ");
        limits.append(stack_max);
        limits.append("\n");

        limits.append(".limit locals ");
        limits.append(locals_max);
        limits.append("\n");

        code.append(limits);
        code.append(instructions);
        code.append("return\n.end method\n");

        return code.toString();
    }


    private String getJasminTypeOnly(Type type)
    {
        if(type instanceof ClassType classType){
            return "Ljava/lang/String";
        }
        if (type instanceof ArrayType arrayType) {
            var typeString = arrayType.getArrayType();
            return "[" + getJasminType(typeString);
        }

        return getJasminType(type.getTypeOfElement());
    }


    private String getJasminType(Type type)
    {

        if(type instanceof ClassType classType){
            return "L" + classType.getName() + ";";
        }
        if (type instanceof ArrayType arrayType) {
            var typeString = arrayType.getArrayType();
            return "[" + getJasminType(typeString);
        }

        return getJasminType(type.getTypeOfElement());
    }

    private  String getJasminTypeFromString(String type){
        //needs more cases probably but god only knows which ones :(
        switch (type){
            case "String":
                return "Ljava/lang/String;";

            default: {
                throw new NotImplementedException(type);
            }
        }
    }

    private String getJasminType(ElementType type)    //Get the element type
    {
        return switch (type) {
            case VOID -> "V";
            case BOOLEAN -> "Z";
            case INT32 -> "I";
            case STRING -> "Ljava/lang/String;";
            case OBJECTREF -> "A";
            case THIS -> classUnit.getClassName();
            default -> throw new NotImplementedException(type);  //In case it isn't defined
        };
    }

    private String getReadableType(ElementType type){
        return switch (type) {
            case VOID -> "V";
            case BOOLEAN -> "Z";
            case INT32 -> "int";
            case STRING -> "Ljava/lang/String;";
            case OBJECTREF -> "A";
            case THIS -> classUnit.getClassName();
            default -> throw new NotImplementedException(type);  //In case it isn't defined
        };
    }
    private String opTypeToString(OperationType opType) {    //Get the operation type
        switch (opType) {
            case EQ : {
                return "if_icmpeq jvm_" + label;
            }
            case NEQ : {
                return "if_icmpne jvm_" + label;
            }
            case LTH : {
                return "if_icmplt jvm_" + label;
            }
            case LTE : {
                return "if_icmple jvm_" + label;
            }
            case GTH : {
                return "if_icmpgt jvm_" + label;
            }
            case GTE : {
                return "if_icmpge jvm_" + label;
            }
            case ANDB : {
                return "iand";
            }
            case ORB : {
                return "ior";
            }
            case NOTB : {
                return "ineg";
            }
            default : {
                return "i" + opType.toString().toLowerCase();
            }
        }
    }
    
    private String generate(Instruction instruction) {      //Get instruction type
        switch (instruction.getInstType()) {
            case CALL:
                return generate((CallInstruction) instruction);
            case GOTO:
                return generate((GotoInstruction) instruction);
            case ASSIGN:
                return generate((AssignInstruction) instruction);
            case UNARYOPER:
                return generate((UnaryOpInstruction) instruction);
            case NOPER:
                return generate((SingleOpInstruction) instruction);
            case BRANCH:
                return generate((CondBranchInstruction) instruction);
            case RETURN:
                return generate((ReturnInstruction) instruction);
            case GETFIELD:
                return generate((GetFieldInstruction) instruction);
            case PUTFIELD:
                return generate((PutFieldInstruction) instruction);
            case BINARYOPER:
                return generate((BinaryOpInstruction) instruction);
            default:
                throw new NotImplementedException(instruction.getInstType());
        }
    }

    public String generate(ReturnInstruction instruction){
        var code = new StringBuilder();
        var id = instruction.getOperand();
        code.append(getVar(id));
        switch (instruction.getReturnType().getTypeOfElement()) {
            case INT32, BOOLEAN -> code.append("i");
            case VOID -> code.append("");
            default -> code.append("a");
        }
        return code.toString();
    }

    public String generate(GotoInstruction instruction)
    {
        var code = new StringBuilder();
        code.append("goto "+ instruction.getLabel()+"\n");
        String next = "";
        if (!nexLabel.isEmpty()) {
            next = nexLabel.get(nexLabel.size()-1);
        }
        code.append(next).append(":\n");
        nexLabel.remove(next);
        if (!Objects.equals(next, instruction.getLabel())) {
            nexLabel.add(instruction.getLabel());
        }
        return code.toString();
    }

    public String generate(CondBranchInstruction instruction)
    {
        var code = new StringBuilder();
        var n = instruction.getLabel().split("_");
        if (Objects.equals(n[0], "Body")){
            code.append("Loop_"+n[1]+":\n");
        }

        if (instruction.getCondition() instanceof SingleOpInstruction i){
            code.append(getVar(i.getSingleOperand()));
        }
        nexLabel.add(instruction.getLabel());
        code.append("ifne "+ instruction.getLabel() +"\n");


        return code.toString();
    }

    public String generate(GetFieldInstruction instruction){
        var code = new StringBuilder();
        var firstOperand = instruction.getFirstOperand();
        Operand firstOperandOp = null;
        if(firstOperand instanceof Operand) {
            firstOperandOp = (Operand) firstOperand;
        }
        var secondOperand = instruction.getSecondOperand();
        Operand secondOperandop = null;
        if(secondOperand instanceof Operand) {
            secondOperandop = (Operand) secondOperand;
        }
        code.append(getVar(firstOperandOp));
        code.append("getfield ").append(firstOperandOp.getName().equals("this") ? classUnit.getClassName() : firstOperandOp.getName()).append("/").append(secondOperandop.getName()).append(" ").append(getJasminType(secondOperand.getType()));
        code.append("\n");

        return code.toString();
    }

    public String generate(PutFieldInstruction instruction){
        var code = new StringBuilder();
        var firstOperand = instruction.getFirstOperand();
        Operand op = null;
        if(firstOperand instanceof Operand) {
            op = (Operand) firstOperand;
        }
        var secondOperand = instruction.getSecondOperand();
        Operand fieldOp = null;
        if(secondOperand instanceof Operand) {
            fieldOp = (Operand) secondOperand;
        }
        var thirdOperand = instruction.getThirdOperand();
        code.append(getVar(op));
        code.append(getVar(thirdOperand));

        code.append("putfield ").append(op.getName().equals("this") ? classUnit.getClassName() : op.getName()).append("/").append(fieldOp.getName()).append(" ").append(getJasminType(thirdOperand.getType()));
        code.append("\n");
        return code.toString();
    }

    public String generate(BinaryOpInstruction instruction){
        var code = new StringBuilder();

        var l = instruction.getLeftOperand();
        var r = instruction.getRightOperand();
        code.append(getVar(l));
        code.append(getVar(r));
        code.append(opTypeToString(instruction.getOperation().getOpType())).append("\n");

        currentStack -=2;
        if(currentStack > stack_max) stack_max = currentStack;

        if (instruction.getOperation().getOpType() == LTH){
            currentStack ++;
            if(currentStack > stack_max) stack_max = currentStack;
            code.append("bipush 1\n").append("goto end_jvm_" + label + "\n").append("jvm_").append(label).append(":").append("\n").append("bipush 0\n").append("goto end_jvm_" + label + "\n").append("end_jvm_" + label + ":\n");
            label ++;
        }


        return code.toString();
    }

    public String getVar(Element op){
        System.out.println("current stack size: " + currentStack);
        var code = new StringBuilder();
        if(op instanceof LiteralElement l){
            if(Integer.parseInt(l.getLiteral()) <= 127 && Integer.parseInt(l.getLiteral()) >= -128){
                currentStack ++;
                if(currentStack > stack_max) stack_max = currentStack;
                code.append("bipush ").append(l.getLiteral()).append("\n");
            }else if(Integer.parseInt(l.getLiteral()) <= 32767 && Integer.parseInt(l.getLiteral()) >= -32768){
                currentStack ++;
                if(currentStack > stack_max) stack_max = currentStack;
                code.append("sipush ").append(l.getLiteral()).append("\n");
            }else{
                currentStack ++;
                if(currentStack > stack_max) stack_max = currentStack;
                code.append("ldc ").append(l.getLiteral()).append("\n");
            }
        }else if(op instanceof Operand o){
            if (Objects.equals(o.getName(), "true")){
                currentStack ++;
                if(currentStack > stack_max) stack_max = currentStack;
                code.append("bipush 1\n");
            }
            else if (Objects.equals(o.getName(), "false")){
                currentStack ++;
                if(currentStack > stack_max) stack_max = currentStack;
                code.append("bipush 0\n");
            }
            else{
                var reg = varTable.get(o.getName()).getVirtualReg();
                if(o.getType().getTypeOfElement() == ElementType.BOOLEAN ||
                        o.getType().getTypeOfElement() == ElementType.INT32) {
                    currentStack ++;
                    if(currentStack > stack_max) stack_max = currentStack;
                    code.append("iload ").append(reg).append("\n");
                }else{
                    currentStack ++;
                    if(currentStack > stack_max) stack_max = currentStack;
                    code.append("aload ").append(reg).append("\n");
                }
            }

        }

        return code.toString();
    }

    public String storeVar(Element op){
        var code = new StringBuilder();

        if(op instanceof LiteralElement l){
            if(Integer.parseInt(l.getLiteral()) < 256){
                currentStack --;
                if(currentStack > stack_max) stack_max = currentStack;
                code.append("bipush ").append(l.getLiteral()).append("\n");
            }else{
                currentStack --;
                if(currentStack > stack_max) stack_max = currentStack;
                code.append("sipush ").append(l.getLiteral()).append("\n");
            }

        }
        else if(op instanceof Operand o){
            var reg = varTable.get(o.getName()).getVirtualReg();
            if(o.getType().getTypeOfElement() == ElementType.BOOLEAN ||
                    o.getType().getTypeOfElement() == ElementType.INT32) {
                currentStack --;
                if(currentStack > stack_max) stack_max = currentStack;
                code.append("istore ").append(reg).append("\n");
            }else{
                currentStack --;
                if(currentStack > stack_max) stack_max = currentStack;
                code.append("astore ").append(reg).append("\n");
            }
        }
        return code.toString();
    }

    public String generate(SingleOpInstruction instruction)
    {
        var code = new StringBuilder();
        var op = instruction.getSingleOperand();

        if(op instanceof ArrayOperand arr){
            var array_reg = varTable.get(arr.getName()).getVirtualReg();
            currentStack ++;
            if(currentStack > stack_max) stack_max = currentStack;
            code.append("aload ").append(array_reg).append("\n");
            code.append(getVar(arr.getIndexOperands().get(0)));

            currentStack --;
            if(currentStack > stack_max) stack_max = currentStack;
            code.append("iaload \n");
        }else {
            code.append(getVar(op));

        }

        return code.toString();
    }

    public String generate(UnaryOpInstruction instruction) {
        instruction.show();
        var code = new StringBuilder();
        var op = instruction.getOperands().get(0);
        currentStack++;
        if (currentStack > stack_max) stack_max = currentStack;
        code.append(getVar(op));
        code.append("ifne jvm_" + label + "\n");
        code.append("bipush 1\n");
        code.append("goto end_jvm_" + label + "\n");
        code.append("jvm_" + label + ":\n");
        code.append("bipush 0\n");
        code.append("goto end_jvm_" + label + "\n");
        code.append("end_jvm_" + label + ":\n");
        label++;
        return code.toString();
    }

    public String generate(AssignInstruction instruction){
        var code = new StringBuilder();
        var dest = instruction.getDest();

        if(dest instanceof ArrayOperand arr){
            var array_reg = varTable.get(arr.getName()).getVirtualReg();
            currentStack ++;
            if(currentStack > stack_max) stack_max = currentStack;
            code.append("aload ").append(array_reg).append("\n");
            code.append(getVar(arr.getIndexOperands().get(0)));
            code.append(generate(instruction.getRhs()));

            currentStack -= 3;
            if(currentStack > stack_max) stack_max = currentStack;
            code.append("iastore \n");

        }
        else{
            code.append(generate(instruction.getRhs()));
            code.append(storeVar(dest));
        }


        return code.toString();
    }

    public String generate(CallInstruction method)
    {
        return switch (method.getInvocationType()) {
            case invokestatic -> getCodeInvokeStatic(method);
            case invokespecial -> getCodeInvokeSpecial(method);
            case invokevirtual -> getCodeInvokeVirtual(method); //TO DO
            case arraylength -> getCodeArrayLength(method);
            case NEW -> getNew(method);
            default -> throw new NotImplementedException(method.getInvocationType());
        };


    }

    private String getNew(CallInstruction method){

        var code = new StringBuilder();

        if (method.getFirstArg().getType() instanceof ArrayType) {
            Element otherArg = method.getListOfOperands().get(0);
            code.append(getVar(otherArg));
            code.append("newarray ").append( getReadableType(((ArrayType)method.getReturnType()).getArrayType())).append("\ndup\n");
        }else{
            code.append("new ").append( ((Operand)method.getFirstArg()).getName()).append("\ndup\n");
        }
        currentStack -= method.getNumOperands();
        if(currentStack > stack_max) stack_max = currentStack;
        if(method.getReturnType().getTypeOfElement() != ElementType.VOID) currentStack ++;
        if(currentStack > stack_max) stack_max = currentStack;
        return code.toString();
    }

    private String getCodeArrayLength(CallInstruction method){
        var code = new StringBuilder();
        code.append(getArgumentCode(method.getFirstArg()));

        code.append("arraylength\n");

        currentStack -= method.getNumOperands();
        if(currentStack > stack_max) stack_max = currentStack;
        if(method.getReturnType().getTypeOfElement() != ElementType.VOID) currentStack ++;
        if(currentStack > stack_max) stack_max = currentStack;

        return code.toString();
    }

	private String getCodeInvokeVirtual(CallInstruction method) {
        var code = new StringBuilder();
        for (var operand : method.getListOfOperands())
        {
            code.append(getArgumentCode(operand));
        }

        code.append(getVar(method.getFirstArg()));

        code.append("invokevirtual ");

        var methodClass = ((Operand) method.getFirstArg());

        var className = ((ClassType)methodClass.getType()).getName();

        code.append(className);
        code.append("/");

        var calledMethod = ((LiteralElement) method.getSecondArg()).getLiteral();
        code.append(calledMethod.substring(1, calledMethod.length() -1));

        code.append("(");
        for (var operand : method.getListOfOperands())
        {
            code.append(getArgumentType(operand));
        }

        code.append(")");
        code.append(getJasminType(method.getReturnType()));
        code.append("\n");

        currentStack -= method.getNumOperands();
        if(currentStack > stack_max) stack_max = currentStack;
        if(method.getReturnType().getTypeOfElement() != ElementType.VOID) currentStack ++;
        if(currentStack > stack_max) stack_max = currentStack;

        return code.toString();
	}

	private String getCodeInvokeSpecial(CallInstruction method) {
		
		//invokespecial (this ou path(java/io/something/etc/...))/<init>(ARGS)ARGS
        method.show();
		var code = new StringBuilder();
        var className = getJasminType(method.getFirstArg().getType().getTypeOfElement());

        code.append(getVar(method.getFirstArg()));
        code.append("invokespecial ").append(className).append("./<init>()V\n");

        currentStack -= method.getNumOperands();
        if(currentStack > stack_max) stack_max = currentStack;
        if(method.getReturnType().getTypeOfElement() != ElementType.VOID) currentStack ++;
        if(currentStack > stack_max) stack_max = currentStack;

		return code.toString();
	}

	private String getCodeInvokeStatic(CallInstruction method) 
	{
		var code = new StringBuilder();
        for (var operand : method.getListOfOperands())
        {
            code.append(getArgumentCode(operand));
        }
		
		code.append("invokestatic ");
		
		var methodClass = ((Operand) method.getFirstArg()).getName();		
		
		code.append(getFullyQualifiedName(methodClass));
		code.append("/");
		
		var calledMethod = ((LiteralElement) method.getSecondArg()).getLiteral();
		code.append(calledMethod.substring(1, calledMethod.length() -1));
		
		code.append("(");
		for (var operand : method.getListOfOperands())
		{
			code.append(getArgumentType(operand));
		}
		
		code.append(")");
		code.append(getJasminType(method.getReturnType()));
		code.append("\n");

        currentStack -= method.getNumOperands();
        if(currentStack > stack_max) stack_max = currentStack;
        if(method.getReturnType().getTypeOfElement() != ElementType.VOID) currentStack ++;
        if(currentStack > stack_max) stack_max = currentStack;

		return code.toString();
	}

	private String getArgumentCode(Element operand)
	{
        var code = new StringBuilder();

        code.append(getVar(operand));

        return code.toString();
	}

    private String getArgumentType(Element operand)
    {
        var code = new StringBuilder();

        code.append(getJasminType(operand.getType()));

        return code.toString();
    }
}

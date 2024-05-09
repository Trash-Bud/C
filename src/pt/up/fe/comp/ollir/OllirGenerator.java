package pt.up.fe.comp.ollir;

import pt.up.fe.comp.ast.AstNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class OllirGenerator extends AJmmVisitor<Integer, Integer> {

    private final StringBuilder code;
    private final SymbolTable symbolTable;
    private String functionSignature;
    private int tempVar;
    private int label = 0;
    List<Symbol> tempVars;

    public OllirGenerator(SymbolTable symbolTable) {
        this.code = new StringBuilder();
        this.symbolTable = symbolTable;

        addVisit(AstNode.START, this::programVisit);
        addVisit(AstNode.CLASS_DECLARATION, this::classDeclarationVisit);
        addVisit(AstNode.METHOD_DECLARATION, this::methodDeclarationVisit);
        addVisit(AstNode.BIN_OP, this::binOpVisit);
        addVisit(AstNode.METHOD_CALL, this::methodCallVisit);
        addVisit(AstNode.VAR, this::varVisit);
        addVisit(AstNode.INTEGER_LITERAL, this::integerLiteralVisit);
        addVisit(AstNode.BOOLEAN_VALUE, this::booleanValueVisit);
        addVisit(AstNode.NEW_EXPRESSION, this::newExpressionVisit);
        addVisit(AstNode.RETURN_EXPRESSION, this::returnExpressionVisit);
        addVisit(AstNode.VAR_DECLARATION, this::varDeclarationVisit);
        addVisit(AstNode.ARRAY, this::arrayVisit);
        addVisit(AstNode.IF_STATEMENT, this::ifStatementVisit);
        addVisit(AstNode.ELSE_STATEMENT, this::elseStatementVisit);
        addVisit(AstNode.NOT_EXPRESSION, this::notStatementVisit);
        addVisit(AstNode.WHILE_STATEMENT, this::whileStatementVisit);
    }

    private Integer notStatementVisit(JmmNode jmmNode, Integer integer) {
        int t0 = tempVar + 1, t1;
        tempVar ++;
        tempVars.add(new Symbol(new Type("bool",false),"t"+t0));

        if(isNotAndEndNode(jmmNode.getJmmChild(0))){
            t1 = tempVar + 1;
            visit(jmmNode.getJmmChild(0));

            code.append(OllirUtils.getCode(Objects.requireNonNull(getTempVar("t" + t0)))).
                    append(" :=.bool !.bool ").
                    append(OllirUtils.getCode(Objects.requireNonNull(getTempVar("t" + t1)))).
                    append(";\n");
        }
        else{

            code.append(OllirUtils.getCode(Objects.requireNonNull(getTempVar("t" + t0)))).
                    append(" :=.bool !.bool ");
            visit(jmmNode.getJmmChild(0));
            code.append(";\n");

        }
        return 0;
    }


    private Integer arrayVisit(JmmNode jmmNode, Integer integer) {
        int  t1 = 0, t2;

        // check if array is a parameter of function
        var isPar = isParameter(jmmNode.getJmmChild(0));
        // get parent
        var parent = jmmNode.getJmmParent();
        // get first child of parent
        var first = parent.getJmmChild(0);

        // if the array is the first node (array assignment)
        if(Objects.equals(parent.getKind(), "BinOp") && Objects.equals(parent.get("op"), "ASSIGN") && jmmNode == first){
            // if the index value is not an integer or a variable
            if(isNotAndEndNode(jmmNode.getJmmChild(1))){
                // visit it first and save variable
                t1 = tempVar + 1;
                visit(jmmNode.getJmmChild(1));
                // add parameter string if needed
                if( isPar != -1){
                    code.append("$").append(isPar + 1).append(".");
                }
                // adding rest of the array
                code.append(jmmNode.getJmmChild(0).get("name")).append("[").
                        append(OllirUtils.getCode(Objects.requireNonNull(getTempVar("t" + t1)))).
                        append("].i32");

            }else{ // if it is a variable or integer
                // if int then we need to save it in a variable
                if(Objects.equals(jmmNode.getJmmChild(1).getKind(), "IntegerLiteral")){
                    // getting temp var
                    t1 = tempVar + 1;
                    tempVar ++;
                    // saving temp var for later
                    tempVars.add(new Symbol(new Type("int",false),"t"+t1));

                    // adding assign for the int value
                    code.append(OllirUtils.getCode(Objects.requireNonNull(getTempVar("t" + t1)))).append(" :=.i32 ");
                    visit(jmmNode.getJmmChild(1));
                    code.append(";\n");
                }

                // adding $n if parameter
                if( isPar != -1){
                    code.append("$").append(isPar + 1).append(".");
                }

                // adding array
                code.append(jmmNode.getJmmChild(0).get("name")).append("[");

                if(Objects.equals(jmmNode.getJmmChild(1).getKind(), "IntegerLiteral")){
                    // index is temp var if int
                    code.append(OllirUtils.getCode(Objects.requireNonNull(getTempVar("t" + t1))));
                }else{
                    // else visit the var itself
                    visit(jmmNode.getJmmChild(1));
                }

                code.append("].i32");
            }
        }
        else{ // if it is the second node then we need to save it first
            // getting temp var to save the array value
            int t0 = tempVar + 1;
            tempVar ++;
            tempVars.add(new Symbol(new Type("int",false),"t"+t0));

            //if is indexing is not end node get it into a variable
            if(isNotAndEndNode(jmmNode.getJmmChild(1))){
                // temp var for index value
                t1 = tempVar + 1;

                visit(jmmNode.getJmmChild(1));

                // saving array value
                code.append(OllirUtils.getCode(Objects.requireNonNull(getTempVar("t" + t0)))).append(" :=.i32 ");

                if( isPar != -1){
                    code.append("$").append(isPar + 1).append(".");
                }

                code.append(jmmNode.getJmmChild(0).get("name")).append("[").
                        append(OllirUtils.getCode(Objects.requireNonNull(getTempVar("t" + t1)))).
                        append("].i32");

            }else{
                // if index is int we need to save it in a temp var
                if(Objects.equals(jmmNode.getJmmChild(1).getKind(), "IntegerLiteral")){
                    t1 = tempVar + 1;
                    tempVar ++;
                    tempVars.add(new Symbol(new Type("int",false),"t"+t1));

                    code.append(OllirUtils.getCode(Objects.requireNonNull(getTempVar("t" + t1)))).append(" :=.i32 ");
                    visit(jmmNode.getJmmChild(1));
                    code.append(";\n");
                }

                code.append(OllirUtils.getCode(Objects.requireNonNull(getTempVar("t" + t0)))).append(" :=.i32 ");
                if( isPar != -1){
                    code.append("$").append(isPar + 1).append(".");
                }
                code.append(jmmNode.getJmmChild(0).get("name")).append("[");
                if(Objects.equals(jmmNode.getJmmChild(1).getKind(), "IntegerLiteral")){
                    code.append(OllirUtils.getCode(Objects.requireNonNull(getTempVar("t" + t1))));
                }else{
                    visit(jmmNode.getJmmChild(1));
                }

                code.append("].i32");

            }
            code.append(";\n");
        }

        return 0;
    }


    public String getCode() {
        return code.toString();
    }

    private int isParameter(JmmNode var){
        var paramVariables = symbolTable.getParameters(functionSignature);
        if (paramVariables != null) {
            int i = 0;
            for (Symbol variable : paramVariables) {
                if (Objects.equals(variable.getName(), var.get("name"))) {
                    return i;
                }
                i++;
            }
        }
        return -1;
    }

    private void addVariable(JmmNode var){
        var localVariables = symbolTable.getLocalVariables(functionSignature);
        if (localVariables != null) {
            for (Symbol variable : localVariables) {
                if (Objects.equals(variable.getName(), var.get("name"))) {
                    code.append(OllirUtils.getCode(variable));
                    return;
                }
            }
        }
        var fieldVariables = symbolTable.getFields();
        for(Symbol variable : fieldVariables){
            if (Objects.equals(variable.getName(), var.get("name"))){
                code.append(OllirUtils.getCode(variable));
                return;
            }
        }
        var paramVariables = symbolTable.getParameters(functionSignature);
        if (paramVariables != null) {
            int i = 0;
            for (Symbol variable : paramVariables) {
                if (Objects.equals(variable.getName(), var.get("name"))) {
                    code.append("$").append(i+1).append(".").
                            append(OllirUtils.getCode(variable));
                    return;
                }
                i++;
            }
        }
        var imports = symbolTable.getImports();
        for (String variable : imports) {
            if (Objects.equals(variable, var.get("name"))) {
                code.append(variable).append(".V");
                return;
            }
        }
    }

    private void addVariableType(JmmNode var){
        if(Objects.equals(var.getKind(), "Var")) {
            var localVariables = symbolTable.getLocalVariables(functionSignature);
            if (localVariables != null) {
                for (Symbol variable : localVariables) {
                    if (Objects.equals(variable.getName(), var.get("name"))) {
                        code.append(OllirUtils.getCode(variable.getType()));
                        return;
                    }
                }
            }
            var fieldVariables = symbolTable.getFields();
            for (Symbol variable : fieldVariables) {
                if (Objects.equals(variable.getName(), var.get("name"))) {
                    code.append(OllirUtils.getCode(variable.getType()));
                    return;
                }
            }
            var paramVariables = symbolTable.getParameters(functionSignature);
            if (paramVariables != null) {
                for (Symbol variable : paramVariables) {
                    if (Objects.equals(variable.getName(), var.get("name"))) {
                        code.append(OllirUtils.getCode(variable.getType()));
                        return;
                    }
                }
            }
        }
        else if(Objects.equals(var.getKind(), "IntegerLiteral")){
            code.append(OllirUtils.getCode(new Type("int",false)));
        }
        else if(Objects.equals(var.getKind(), "BooleanValue")){
            code.append(OllirUtils.getCode(new Type("bool",false)));

        }
    }

    private Type getVariableType(JmmNode var){
        if(Objects.equals(var.getKind(), "Var")) {
            var localVariables = symbolTable.getLocalVariables(functionSignature);
            if (localVariables != null) {
                for (Symbol variable : localVariables) {
                    if (Objects.equals(variable.getName(), var.get("name"))) {
                        return variable.getType();

                    }
                }
            }
            var fieldVariables = symbolTable.getFields();
            for (Symbol variable : fieldVariables) {
                if (Objects.equals(variable.getName(), var.get("name"))) {
                    return variable.getType();
                }
            }
            var paramVariables = symbolTable.getParameters(functionSignature);
            if (paramVariables != null) {
                for (Symbol variable : paramVariables) {
                    if (Objects.equals(variable.getName(), var.get("name"))) {
                        return variable.getType();
                    }
                }
            }
        }
        else if(Objects.equals(var.getKind(), "IntegerLiteral")){
            return new Type("int",false);
        }
        else if(Objects.equals(var.getKind(), "BooleanValue")){
            return new Type("bool",false);

        }
        return null;
    }


    private Symbol getTempVar(String var){
        for (Symbol variable : tempVars) {
            if (Objects.equals(variable.getName(), var)) {
                return variable;
            }
        }
        return null;
    }

    private boolean isImport(String name){
        for (var importString : symbolTable.getImports()) {
            if(Objects.equals(importString, name)){
                return true;
            }
        }
        return false;
    }

    private Integer programVisit(JmmNode program, Integer dummy){
        for (var importString : symbolTable.getImports()) {
            code.append("import ").append(importString).append(";\n");
        }

        for(var child : program.getChildren()){
            visit(child);
        }

        return 0;
    }

    private Integer classDeclarationVisit(JmmNode classDecl, Integer dummy) {
        code.append("public ").append(symbolTable.getClassName());
        var superClass = symbolTable.getSuper();
        if (superClass != null){
            code.append(" extends ").append(superClass);
        }
        code.append(" {\n\n");

        List<JmmNode> children = new ArrayList<>();
        for(var child : classDecl.getChildren()){
            if(!Objects.equals(child.getKind(), "VarDeclaration")){
                children.add(child);
            }
            else {
                visit(child);
            }
        }

        code.append("\n.construct ").append(symbolTable.getClassName()).append("().V{\n");
        code.append("invokespecial(this, \"<init>\").V;\n}\n");

        for(var child : children){
            code.append("\n");
            visit(child);
        }

        code.append("}\n");

        return 0;
    }

    private Integer methodDeclarationVisit(JmmNode methodDecl, Integer dummy) {
        functionSignature = methodDecl.getJmmChild(1).get("name");
        var isStatic = Boolean.valueOf(methodDecl.get("isStatic"));

        tempVar = 1;
        tempVars = new ArrayList<>();

        code.append(".method public ");
        if (isStatic) code.append("static ");
        code.append(functionSignature).append("(");

        var params = symbolTable.getParameters(functionSignature);
        var paramCode = params.stream()
                .map(OllirUtils::getCode)
                .collect(Collectors.joining(", "));

        code.append(paramCode);
        code.append(").");
        code.append(OllirUtils.getCode(symbolTable.getReturnType(functionSignature)));
        code.append(" {\n");

        int lastParamIndex = 2;

        if (symbolTable.getParameters(functionSignature).size() == 0) lastParamIndex = 1;

        var statements = methodDecl.getChildren().subList(lastParamIndex + 1, methodDecl.getNumChildren());
        for(var statement : statements){
            visit(statement);

        }

        if (!Objects.equals(methodDecl.getJmmChild(methodDecl.getNumChildren() - 1).getKind(), "ReturnExpression")){
            code.append("ret.V;\n");
        }

        code.append("}\n");

        return 0;
    }

    private Integer varVisit(JmmNode var, Integer dummy) {
        if(Objects.equals(var.get("name"), "this")){
            code.append("this");
        }
        else addVariable(var);
        return 0;
    }

    private Integer binOpVisit(JmmNode binOp, Integer dummy) {
        var operationType = binOp.get("op");
        var children = binOp.getChildren();
        switch (operationType) {
            case "ADD":
                tempOrder(" +.i32 ", children,new Type("int",false));
                break;
            case "SUB":
                tempOrder(" -.i32 ", children, new Type("int",false));
                break;
            case "MUL":
                tempOrder(" *.i32 ", children, new Type("int",false));
                break;
            case "DIV":
                tempOrder(" /.i32 ", children, new Type("int",false));
                break;
            case "ASSIGN":
                assignConverter(children);
                break;
            case "AND":
                tempOrder(" &&.bool ",children,new Type("bool",false));
                break;
            case "LESS":
                tempOrder(" <.i32 ",children, new Type("bool",false));
                break;
            default:
                break;
        }
        return 0;
    }

    private boolean isField(JmmNode child){
        if(!Objects.equals(child.getKind(), "Var")) return false;
        var fields = symbolTable.getFields();
        for(var local: symbolTable.getLocalVariables(this.functionSignature)){
            if(Objects.equals(child.get("name"), local.getName())){
                return false;
            }
        }
        for (var field: fields){
            if(Objects.equals(child.get("name"), field.getName())){
                return true;
            }
        }
        return false;
    }

    private void assignConverter(List<JmmNode> children) {
        int t1;

        if (isNotAndEndNode(children.get(1)) && isAnEndNode(children.get(0))){
            t1 = tempVar + 1;
            visit(children.get(1));

            if(isField(children.get(0))){
                code.append("putfield( this, ");
                visit(children.get(0));
                code.append(", ");
                code.append(OllirUtils.getCode(getTempVar("t" + t1))).append(").V;\n");
            }
            else{
                visit(children.get(0));
                code.append(" :=.");
                addVariableType(children.get(0));
                code.append(" ");
                code.append(OllirUtils.getCode(Objects.requireNonNull(getTempVar("t" + t1))));
                code.append(";\n");
            }

        }
        else if(isNotAndEndNode(children.get(1)) && isNotAndEndNode(children.get(0))){
            t1 = tempVar + 1;
            visit(children.get(1));

            visit(children.get(0));
            code.append(" :=.");
            code.append(OllirUtils.getCode(Objects.requireNonNull(getTempVar("t" + t1)).getType()));
            code.append(" ");
            code.append(OllirUtils.getCode(Objects.requireNonNull(getTempVar("t" + t1))));
            code.append(";\n");
        }
        //the first node is only not a var when it is an array index
        else if(isAnEndNode(children.get(1)) && isNotAndEndNode(children.get(0))){

            if(isField(children.get(1))){
                visit(children.get(0));
                code.append(" :=.");
                addVariableType(children.get(1));
                code.append(" getfield(this, ");
                visit(children.get(1));
                code.append(").");
                addVariableType(children.get(0));
                code.append(";\n");
            }else{
                visit(children.get(0));
                code.append(" :=.");
                addVariableType(children.get(1));
                code.append(" ");
                visit(children.get(1));
                code.append(";\n");
            }

        }
        else{
            if(isField(children.get(1)) && !isField(children.get(0))){
                visit(children.get(0));
                code.append(" :=.");
                addVariableType(children.get(1));
                code.append(" getfield(this, ");
                visit(children.get(1));
                code.append(").");
                addVariableType(children.get(0));
                code.append(";\n");
            }
            else if(!isField(children.get(1)) && isField(children.get(0))){
                code.append("putfield(this, ");
                visit(children.get(0));
                code.append(", ");
                visit(children.get(1));
                code.append(").V;\n");
            }
            else if(isField(children.get(1)) && isField(children.get(0))){
                int tGet = tempVar + 1, tPut = tempVar + 1;
                tempVar += 2;
                tempVars.add(new Symbol(getVariableType(children.get(0)),"t"+tGet));
                tempVars.add(new Symbol(getVariableType(children.get(1)),"t"+tPut));

                code.append(OllirUtils.getCode(Objects.requireNonNull(getTempVar("t" + tGet))));
                code.append(" :=.");
                addVariableType(children.get(1));
                code.append(" getfield(this, ");
                visit(children.get(1));
                code.append(").");
                addVariableType(children.get(0));
                code.append(";\n");

                code.append(OllirUtils.getCode(Objects.requireNonNull(getTempVar("t" + tPut))));
                code.append(" :=.");
                addVariableType(children.get(0));
                code.append("putfield(this, ");
                visit(children.get(0));
                code.append(", ");
                code.append(OllirUtils.getCode(Objects.requireNonNull(getTempVar("t" + tGet))));
                code.append(").V;\n");

            }
            else{
                visit(children.get(0));
                code.append(" :=.");
                addVariableType(children.get(0));
                code.append(" ");
                visit(children.get(1));
                code.append(";\n");
            }
        }
    }

    private void tempOrder(String symbol, List<JmmNode> children, Type type){
        Integer t0, t1, t2;
        tempVar ++;
        t0 = tempVar;
        tempVars.add(new Symbol(type,"t" + t0));
        if(isNotAndEndNode(children.get(0)) && isNotAndEndNode(children.get(1))){
            t1 = tempVar + 1;
            visit(children.get(0));
            t2 = tempVar + 1;
            visit(children.get(1));

            code.append(OllirUtils.getCode(Objects.requireNonNull(getTempVar("t" + t0)))).append(" ").
                    append(":=").append(".").append(OllirUtils.getCode(Objects.requireNonNull(getTempVar("t" + t0).getType()))).append(" ");
            code.append(OllirUtils.getCode(Objects.requireNonNull(getTempVar("t" + t1))));
            code.append(symbol);
            code.append(OllirUtils.getCode(Objects.requireNonNull(getTempVar("t" + t2))));
            code.append(";\n");
        }
        else if (isNotAndEndNode(children.get(1)) && isAnEndNode(children.get(0))){
            t1 = tempVar + 1;
            visit(children.get(1));

            code.append(OllirUtils.getCode(Objects.requireNonNull(getTempVar("t" + t0)))).append(" ").
                    append(":=").append(".").append(OllirUtils.getCode(Objects.requireNonNull(getTempVar("t" + t0).getType()))).append(" ");
            visit(children.get(0));
            code.append(symbol);
            code.append(OllirUtils.getCode(Objects.requireNonNull(getTempVar("t" + t1)))).
                    append(";\n");
        }
        else if (isNotAndEndNode(children.get(0)) && isAnEndNode(children.get(1))){
            t1 = tempVar + 1;
            visit(children.get(0));

            code.append(OllirUtils.getCode(Objects.requireNonNull(getTempVar("t" + t0)))).append(" ").
                    append(":=").append(".").append(OllirUtils.getCode(Objects.requireNonNull(getTempVar("t" + t0).getType()))).append(" ");

            code.append(OllirUtils.getCode(Objects.requireNonNull(getTempVar("t" + t1))));
            code.append(symbol);
            visit(children.get(1));
            code.append(";\n");
        }else{
            code.append(OllirUtils.getCode(Objects.requireNonNull(getTempVar("t" + t0)))).append(" ").
                    append(":=").append(".").append(OllirUtils.getCode(Objects.requireNonNull(getTempVar("t" + t0).getType()))).append(" ");
            visit(children.get(0));
            code.append(symbol);
            visit(children.get(1));
            code.append(";\n");
        }
    }

    private boolean isAnEndNode(JmmNode child) {
        return Objects.equals(child.getKind(), "Var") || Objects.equals(child.getKind(), "IntegerLiteral") || Objects.equals(child.getKind(), "BooleanValue");
    }

    private Type getCalledMethodType(JmmNode methodCall, String methodName){
        if ("length".equals(methodName)) {
            return new Type("int", false);
        }
        else if (symbolTable.getMethods().contains(methodName)){
            return symbolTable.getReturnType(methodName);
        }
        else{
            if (Objects.equals(methodCall.getJmmParent().getKind(), "BinOp")){
                if(Objects.equals(methodCall.getJmmParent().get("op"), "AND") ||
                        Objects.equals(methodCall.getJmmParent().get("op"), "LESS")){
                    return new Type("bool", false);
                }
                else if (Objects.equals(methodCall.getJmmParent().get("op"), "ASSIGN")){
                    if (Objects.equals(methodCall.getJmmParent().getJmmChild(0).getKind(), "Array")){
                        return getVariableType(methodCall.getJmmParent().getJmmChild(0).getJmmChild(1));
                    }
                    else{
                        return getVariableType(methodCall.getJmmParent().getJmmChild(0));
                    }
                }
                else{
                    return new Type("int", false);
                }
            }
            else if (Objects.equals(methodCall.getJmmParent().getKind(), "ReturnExpression")){
                return symbolTable.getReturnType(functionSignature);
            }
            else{
                return new Type("void",false);
            }
        }
    }

    private Integer methodCallVisit(JmmNode methodCall, Integer dummy) {
        // Method name
        var methodName = methodCall.getJmmChild(1).get("method");
        // Used to store parameter variables
        List<Integer> parameterTemps = new ArrayList<>();
        // Used to store method type
        Type methodType = getCalledMethodType(methodCall,methodName);
        // Used to store parameter nodes
        List<JmmNode> parameters = new ArrayList<>();
        // Variable used on method call
        var calledVar = methodCall.getJmmChild(0);
        // Temp var value used if the variable called is not an end node (ex.: new expression)
        int t0 = 0, t1 = 0;

        // Parameters if they exist are stored in the node of index 1 of methodCall
        if(methodCall.getJmmChild(1).getChildren().size() != 0){
            parameters =  methodCall.getJmmChild(1).getJmmChild(0).getChildren();
        }

        // if the method is not void we need a temporary variable
        if (!Objects.equals(methodType.getName(), "void")){
            tempVar++;
            t1 = tempVar;
            tempVars.add(new Symbol(methodType, "t" + t1));
        }

        // if the variable where the method is called is for example a NewExpression it needs to create a temp variable first
        if(isNotAndEndNode(calledVar)){
            t0 = tempVar + 1;
            visit(calledVar);
        }

        // If a parameter is a variable, integer or boolean then it can be
        // used right away otherwise the compiler
        // needs to first visit those nodes to create
        // temporary variables and store them to be used later
        // otherwise their position in our array is filled with 0
        for (var parameter: parameters){
            if(isNotAndEndNode(parameter)){
                parameterTemps.add(tempVar+1);
                visit(parameter);
            }else if(isField(parameter)){
                var t4 = tempVar + 1;
                tempVar++;
                var symb = new Symbol(getVariableType(parameter), "t" + t4);
                tempVars.add(symb);
                code.append(OllirUtils.getCode(Objects.requireNonNull(getTempVar("t" + t4))));
                code.append(" :=.").append(OllirUtils.getCode(getVariableType(parameter)));
                code.append(" getfield(this, ");
                visit(parameter);
                code.append(").").append(OllirUtils.getCode(getVariableType(parameter))).append(" ;\n");
                parameterTemps.add(t4);
            }
            else{
                parameterTemps.add(0);
            }
        }

        if (!Objects.equals(methodType.getName(), "void")){
            code.append(OllirUtils.getCode(Objects.requireNonNull(getTempVar("t" + t1)))).
                    append(" :=.").
                    append(OllirUtils.getCode(methodType)).append(" ");
        }

        if (Objects.equals(methodName, "length")){
                code.append("arraylength(");

                //check if we should add the first child straight away or if we need to add a temporary variable
                if (isAnEndNode(calledVar)) {
                    visit(methodCall.getJmmChild(0));
                } else {
                    code.append(OllirUtils.getCode(Objects.requireNonNull(getTempVar("t" + t0))));
                }

                code.append(").i32");
        }
        else {
            if (isAnEndNode(calledVar)){
                if (isImport(calledVar.get("name"))) {
                    code.append("invokestatic(");
                    if (isAnEndNode(calledVar)) {
                        code.append(calledVar.get("name"));
                    } else {
                        code.append(OllirUtils.getCode(Objects.requireNonNull(getTempVar("t" + t0))));
                    }
                }else{
                    code.append("invokevirtual(");
                    if (isAnEndNode(calledVar)) {
                        visit(methodCall.getJmmChild(0));
                    } else {
                        code.append(OllirUtils.getCode(Objects.requireNonNull(getTempVar("t" + t0))));
                    }
                }
            }
            else{
                code.append("invokevirtual(");
                if (isAnEndNode(calledVar)) {
                    visit(methodCall.getJmmChild(0));
                } else {
                    code.append(OllirUtils.getCode(Objects.requireNonNull(getTempVar("t" + t0))));
                }
            }


            code.append(", \"").append(methodName).append("\"");

            getParameters(parameterTemps, methodType, parameters); //add parameters
        }
        code.append(";\n");
        return 0;
    }


    private boolean isNotAndEndNode(JmmNode parameter) {
        return !Objects.equals(parameter.getKind(), "Var") && !Objects.equals(parameter.getKind(), "IntegerLiteral") && !Objects.equals(parameter.getKind(), "BooleanValue");
    }

    private void getParameters(List<Integer> temps, Type methodType, List<JmmNode> parameters) {

        for (int i = 0; i <parameters.size(); i++) {
            if(isNotAndEndNode(parameters.get(i)) || isField(parameters.get(i))){
                code.append(", ");
                code.append(OllirUtils.getCode(Objects.requireNonNull(getTempVar("t" + temps.get(i)))));
            }
            else{
                code.append(", ");
                visit(parameters.get(i));
            }

        }

        code.append(").").append(OllirUtils.getCode(methodType));
    }

    private Integer integerLiteralVisit(JmmNode integerLiteral, Integer dummy) {

        code.append(integerLiteral.get("value")).append(".i32");
        return 0;
    }

    private Integer booleanValueVisit(JmmNode booleanValue, Integer dummy) {
        code.append(booleanValue.get("value")).append(".bool");
        return 0;
    }

    private Integer newExpressionVisit(JmmNode newExpression, Integer dummy) {
        var node = newExpression.getJmmParent();
        var child = node.getJmmChild(0).get("name");

        int t0 = tempVar + 1;
        tempVar ++;


        if(newExpression.getChildren().isEmpty()){
            tempVars.add(new Symbol(new Type(newExpression.get("name"),false),"t"+t0));

            code.append(OllirUtils.getCode(Objects.requireNonNull(getTempVar("t" + t0)))).append(" :=.");
            code.append(newExpression.get("name")).append(" new(").append(newExpression.get("name")).append(").").append(newExpression.get("name")).append(";\n");
            code.append("invokespecial(").append(
                    OllirUtils.getCode(Objects.requireNonNull(getTempVar("t" + t0)))).append(", \"<init>\").V;\n");
        }
        else{
            tempVars.add(new Symbol(new Type(newExpression.get("name"),true),"t"+t0));

            code.append(OllirUtils.getCode(Objects.requireNonNull(getTempVar("t" + t0)))).append(" :=.");

            int t2 = 0;
            if(isNotAndEndNode(newExpression.getJmmChild(0).getJmmChild(0))){
                t2 = tempVar + 1;
                visit(newExpression.getJmmChild(0).getJmmChild(0));
            }
            code.append("array.i32 new(array, ");
            if(isNotAndEndNode(newExpression.getJmmChild(0).getJmmChild(0))){
                code.append(OllirUtils.getCode(Objects.requireNonNull(getTempVar("t" + t2))));
            }
            else{
                visit(newExpression.getJmmChild(0).getJmmChild(0));
            }
            code.append(").array.i32").append(";\n");

        }

        return 0;
    }

    private Integer returnExpressionVisit(JmmNode returnExpression, Integer dummy) {
        int temp;
        if(!Objects.equals(returnExpression.getJmmChild(0).getKind(), "Var") &&
                !Objects.equals(returnExpression.getJmmChild(0).getKind(), "IntegerLiteral") &&
                !Objects.equals(returnExpression.getJmmChild(0).getKind(), "BooleanValue")){
            temp = tempVar + 1;
            visit(returnExpression.getJmmChild(0));
            code.append("ret.").
                    append(OllirUtils.getCode(symbolTable.getReturnType(functionSignature))).
                    append(" ");
            code.append(OllirUtils.getCode(Objects.requireNonNull(getTempVar("t" + temp)))).append(";\n");

        }else if(isField(returnExpression.getJmmChild(0))){
            var t4 = tempVar + 1;
            tempVar++;
            var parameter = returnExpression.getJmmChild(0);
            var symb = new Symbol(getVariableType(parameter), "t" + t4);
            tempVars.add(symb);
            code.append(OllirUtils.getCode(Objects.requireNonNull(getTempVar("t" + t4))));
            code.append(" :=.").append(OllirUtils.getCode(getVariableType(parameter)));
            code.append(" getfield(this, ");
            visit(parameter);
            code.append(").").append(OllirUtils.getCode(getVariableType(parameter))).append(" ;\n");
            code.append("ret.").
                    append(OllirUtils.getCode(symbolTable.getReturnType(functionSignature))).
                    append(" ");
            code.append(OllirUtils.getCode(Objects.requireNonNull(getTempVar("t" + t4)))).append(";\n");
        }
        else{
            code.append("ret.").
                    append(OllirUtils.getCode(symbolTable.getReturnType(functionSignature))).
                    append(" ");
            visit(returnExpression.getJmmChild(0));
            code.append(";\n");
        }
        return 0;
    }

    private Integer varDeclarationVisit(JmmNode varDeclaration, Integer dummy) {
        if (Objects.equals(varDeclaration.getJmmParent().getKind(), "ClassDeclaration")) {
            code.append(".field private ");
            addVariable(varDeclaration);
            code.append(";\n");
        }
        return 0;
    }

    private Integer ifStatementVisit(JmmNode ifStatement, Integer dummy) {
        var condition = ifStatement.getJmmChild(0).getJmmChild(0);
        var ifContent = ifStatement.getChildren();
        ifContent.remove(0);

        int t0 = tempVar + 1;

        if(isNotAndEndNode(condition)){
            visit(condition);
        }

        int t1 = tempVar + 1;
        tempVar ++;
        tempVars.add(new Symbol(new Type("bool",false),"t"+ t1));

        if(isAnEndNode(condition)){
            code.append(OllirUtils.getCode(Objects.requireNonNull(getTempVar("t" + t1)))).append(" :=.bool !.bool ");
            visit(condition);
            code.append(";\n");
        }else{
            code.append(OllirUtils.getCode(Objects.requireNonNull(getTempVar("t" + t1)))).append(" :=.bool !.bool ");
            code.append(OllirUtils.getCode(Objects.requireNonNull(getTempVar("t" + t0)))).append(";\n");
        }

        label++;
        int myLabel = label;


        code.append("if (");
        code.append(OllirUtils.getCode(Objects.requireNonNull(getTempVar("t" + t1))));
        code.append(") goto else_"+myLabel+";\n");

        List<JmmNode> elseNodes = new ArrayList<>();

        for (var child: ifContent) {
            if(Objects.equals(child.getKind(), "ElseStatement")){
                elseNodes.add(child);
            }
            else visit(child);

        }
        code.append("goto endif_"+myLabel+";\n");

        for(var elseNode: elseNodes){
            visit(elseNode);
        }
        label --;
        code.append("endif_"+myLabel+":\n");

        return 0;
    }

    private Integer elseStatementVisit(JmmNode elseStatement, Integer dummy) {
        List<JmmNode> children = elseStatement.getChildren();

        code.append("else_"+label+":\n");

        for (var child : children) {
            visit(child);
        }

        code.append("goto endif_"+label+";\n");

        return 0;
    }



    private Integer whileStatementVisit(JmmNode whileStatement, Integer dummy){
        var condition = whileStatement.getJmmChild(0).getJmmChild(0);
        var whileContent = whileStatement.getChildren();
        whileContent.remove(0);

        label++;
        int myLabel = label;

        code.append("Loop_"+ myLabel+":\n");
        int t0 = tempVar + 1;

        visit(condition);

        int t1 = tempVar + 1;
        tempVar ++;

        tempVars.add(new Symbol(new Type("bool",false),"t"+ t1));
        code.append(OllirUtils.getCode(Objects.requireNonNull(getTempVar("t" + t1)))).append(" :=.bool !.bool ");
        code.append(OllirUtils.getCode(Objects.requireNonNull(getTempVar("t" + t0)))).append(";\n");


        code.append("if (");
        code.append(OllirUtils.getCode(Objects.requireNonNull(getTempVar("t" + t1))));
        code.append(") goto Body_"+myLabel+";\n");
        code.append("goto EndLoop_"+myLabel+";\n");

        code.append("Body_"+myLabel+":\n");
        for (var child: whileContent) {
            visit(child);
        }
        code.append("goto Loop_"+myLabel+";\n");

        code.append("EndLoop_"+myLabel+":\n");


        return 0;
    }
}

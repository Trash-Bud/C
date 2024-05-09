package pt.up.fe.comp.ollir;

import pt.up.fe.comp.ast.AstNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ConstantFoldingAndPropagating extends AJmmVisitor<Integer, Integer> {

    Map<Symbol,String> variables;
    SymbolTable symbolTable;
    String methodName;

    public ConstantFoldingAndPropagating(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        variables = new HashMap<>();
        addVisit(AstNode.BIN_OP, this::binOpVisit);
        addVisit(AstNode.METHOD_DECLARATION, this::methodDecVisit);
        addVisit(AstNode.VAR, this::varVisit);
        addVisit(AstNode.NOT_EXPRESSION, this::notStatementVisit);
        setDefaultVisit(this::defaultVisit);
    }

    private Integer notStatementVisit(JmmNode jmmNode, Integer integer) {
        visit(jmmNode.getJmmChild(0));
        if (Objects.equals(jmmNode.getJmmChild(0).getKind(), "BooleanValue")){
            var value = jmmNode.getJmmChild(0).get("value");
            var parent = jmmNode.getJmmParent();
            int i = 0;
            for (;i < parent.getChildren().size(); i++){
                if (parent.getJmmChild(i) == jmmNode){
                    break;
                }
            }
            parent.removeJmmChild(jmmNode);
            var newNode = new JmmNodeImpl("BooleanValue");
            newNode.put("value", String.valueOf(!Boolean.parseBoolean(value)));
            parent.add(newNode,i);

        }
        return 0;
    }

    private Integer methodDecVisit(JmmNode jmmNode, Integer integer) {
        methodName = jmmNode.getJmmChild(1).get("name");
        for( var child: jmmNode.getChildren()){
            visit(child);
        }
        return 0;
    }

    private Symbol getVar(String name){
        var parameters = symbolTable.getParameters(methodName);
        var localVars = symbolTable.getLocalVariables(methodName);
        var imports = symbolTable.getImports();
        var fields = symbolTable.getFields();

        if(parameters != null) {
            for (var par : parameters) {
                if (Objects.equals(par.getName(), name)) {
                    return par;
                }
            }
        }
        if(localVars != null) {
            for (var par : localVars) {
                if (Objects.equals(par.getName(), name)) {
                    return par;
                }
            }
        }
        for (var par: fields){
            if(Objects.equals(par.getName(), name)){
                return par;
            }
        }
        for (var par: imports){
            if(Objects.equals(par, name)){
                return new Symbol(new Type(par,false),par);
            }
        }
        return null;
    }


    private Integer defaultVisit(JmmNode node, Integer integer) {
        for( var child: node.getChildren()){
            visit(child);
        }
        return 0;
    }

    private Integer varVisit(JmmNode jmmNode, Integer integer) {
        var variable = getVar(jmmNode.get("name"));
        var value = variables.get(variable);
        if (value != null ){
            var parent = jmmNode.getJmmParent();
            int i = 0;
            for (; i < parent.getChildren().size(); i++){
                if (parent.getJmmChild(i) == jmmNode){
                    break;
                }
            }
            parent.removeJmmChild(jmmNode);
            String nodeType;
            if (Objects.equals(variable.getType().getName(), "int")){
                nodeType = "IntegerLiteral";
            }else{
                nodeType = "BooleanValue";
            }
            var newJmmNode = new JmmNodeImpl(nodeType);
            newJmmNode.put("value", value);
            parent.add(newJmmNode,i);
        }
        return 0;
    }

    private Integer binOpVisit(JmmNode jmmNode, Integer integer) {
        JmmNodeImpl newJmmNode = null;
        if(Objects.equals(jmmNode.get("op"), "ASSIGN")){
            visit(jmmNode.getJmmChild(1));
            if (Objects.equals(jmmNode.getJmmChild(0).getKind(), "Var") ){
                if (Objects.equals(jmmNode.getJmmChild(1).getKind(), "IntegerLiteral") || Objects.equals(jmmNode.getJmmChild(1).getKind(), "BooleanValue")){
                    variables.put(getVar(jmmNode.getJmmChild(0).get("name")), jmmNode.getJmmChild(1).get("value"));
                }
            }
            return 0;
        }

        for(var child: jmmNode.getChildren()){
            visit(child);
        }

        if (Objects.equals(jmmNode.getJmmChild(0).getKind(), "IntegerLiteral") && Objects.equals(jmmNode.getJmmChild(1).getKind(), "IntegerLiteral")){
            int result = 0;
            boolean resultB = true;
            switch (jmmNode.get("op")) {
                case "ADD":
                    result = Integer.parseInt(jmmNode.getJmmChild(0).get("value")) + Integer.parseInt(jmmNode.getJmmChild(1).get("value"));
                    newJmmNode = new JmmNodeImpl("IntegerLiteral");
                    newJmmNode.put("value", String.valueOf(result));
                    break;
                case "SUB":
                    result = Integer.parseInt(jmmNode.getJmmChild(0).get("value")) - Integer.parseInt(jmmNode.getJmmChild(1).get("value"));
                    newJmmNode = new JmmNodeImpl("IntegerLiteral");
                    newJmmNode.put("value", String.valueOf(result));
                    break;
                case "MUL":
                    result = Integer.parseInt(jmmNode.getJmmChild(0).get("value")) * Integer.parseInt(jmmNode.getJmmChild(1).get("value"));
                    newJmmNode = new JmmNodeImpl("IntegerLiteral");
                    newJmmNode.put("value", String.valueOf(result));
                    break;
                case "DIV":
                    result = Integer.parseInt(jmmNode.getJmmChild(0).get("value")) / Integer.parseInt(jmmNode.getJmmChild(1).get("value"));
                    newJmmNode = new JmmNodeImpl("IntegerLiteral");
                    newJmmNode.put("value", String.valueOf(result));
                    break;
                case "LESS":
                    resultB = Integer.parseInt(jmmNode.getJmmChild(0).get("value")) < Integer.parseInt(jmmNode.getJmmChild(1).get("value"));
                    newJmmNode = new JmmNodeImpl("BooleanValue");
                    newJmmNode.put("value", String.valueOf(resultB));
                    break;
            };



        }else if (Objects.equals(jmmNode.getJmmChild(0).getKind(), "BooleanValue") && Objects.equals(jmmNode.getJmmChild(1).getKind(), "BooleanValue")){
            boolean result = true;
            if ("AND".equals(jmmNode.get("op"))) {
                result = Boolean.parseBoolean(jmmNode.getJmmChild(0).get("value")) && Boolean.parseBoolean(jmmNode.getJmmChild(1).get("value"));
            }
            else{
                return 0;
            }

            newJmmNode = new JmmNodeImpl("BooleanValue");
            newJmmNode.put("value", String.valueOf(result));
        }else{
            return 0;
        }

        var parent = jmmNode.getJmmParent();
        int i = 0;
        for (; i < parent.getChildren().size(); i++){
            if (parent.getJmmChild(i) == jmmNode){
                break;
            }
        }
        parent.removeJmmChild(jmmNode);
        parent.add(newJmmNode,i);

        return 0;
    }

}

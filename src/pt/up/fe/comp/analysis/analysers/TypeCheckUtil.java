package pt.up.fe.comp.analysis.analysers;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

import javax.lang.model.element.TypeElement;
import java.util.List;
import java.util.Objects;

public class TypeCheckUtil {
    private SymbolTable symbolTable;
    private String funcName;
    public TypeCheckUtil(SymbolTable symbolTable, String funcName) {
        this.symbolTable = symbolTable;
        this.funcName = funcName;
    }

    public Type getVarType(String varName){
        if(varName.equals("this")){
            return new Type(symbolTable.getClassName(),false);
        }

        List<Symbol> localVars = symbolTable.getLocalVariables(funcName);
        if (localVars != null) {
            for (Symbol var : localVars) {
                if (var.getName().equals(varName)) {

                    return var.getType();
                }
            }
        }
        List<Symbol> fieldVars = symbolTable.getFields();
        if(fieldVars != null) {
            for (Symbol var : fieldVars) {
                if (var.getName().equals(varName)) {
                    return var.getType();
                }
            }
        }
        List<Symbol> paramVars = symbolTable.getParameters(funcName);
        if(paramVars != null) {
            for (Symbol var : paramVars) {
                if (var.getName().equals(varName)) {
                    return var.getType();
                }
            }
        }
        List<String> imports = symbolTable.getImports();
        for(String importName : imports){
            if(varName.equals(importName)){
                return new Type(importName, false);
            }
        }

        return null;
    }
    public Type getType(JmmNode Op){

        if (Op.getKind().equals("Var")) {
            return this.getVarType(Op.get("name"));
        }
        if(Op.getKind().equals("MethodCall")){
            var called_method = Op.getJmmChild(1).get("method");
            //assume that the imported/extended method has the desired return type
            return symbolTable.getReturnType(called_method) == null ? new Type("outside_type", false) : symbolTable.getReturnType(called_method);
        }
        if(Op.getKind().equals("NewExpression")){

            return new Type(Op.get("name"), !Op.getChildren().isEmpty());
        }
        if(Op.getKind().equals("Array")){
            return new Type(getType(Op.getJmmChild(0)).getName(), false );
        }
        if(Op.getKind().equals("NotExpression")){
            return new Type("boolean", false);
        }
        if(Op.getKind().equals("BinOp")){
            if(Op.get("op").equals("LESS") || Op.get("op").equals("AND")){
                return new Type("boolean", false);
            }
            else if (
                    Objects.equals(Op.get("op"), "SUB") ||
                            Objects.equals(Op.get("op"), "ADD") ||
                            Objects.equals(Op.get("op"), "MUL") ||
                            Objects.equals(Op.get("op"), "DIV")){
                return new Type("int", false);
            }
            else if(Objects.equals(Op.get("op"), "ASSIGN")){
                return getType(Op.getJmmChild(0));
            }
        }
        if (Op.getKind().equals("NewExpression")){
            return new Type(Op.get("name"), false);
        }
        if(Op.getKind().equals("IntegerLiteral")){
            return new Type("int", false);
        }
        if(Op.getKind().equals("BooleanValue")){
            return new Type("boolean", false);
        }
        return new Type("lol", false);
    }
}

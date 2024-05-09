package pt.up.fe.comp.analysis.analysers;

import pt.up.fe.comp.analysis.PreorderSemanticAnalyzer;
import pt.up.fe.comp.ast.AstNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.List;
import java.util.Objects;

public class OperationTypeCheck extends PreorderSemanticAnalyzer {
    private final SymbolTable symbolTable;
    String funcName = "";
    private TypeCheckUtil typeChecker;

    public OperationTypeCheck(SymbolTable symbolTable) {
        super();
        this.symbolTable = symbolTable;
        addVisit(AstNode.METHOD_DECLARATION,this::methodDecVisit);
    }

    public Integer methodDecVisit(JmmNode methodDecl,  Integer dummy){
        funcName = methodDecl.getJmmChild(1).get("name");
        addVisit(AstNode.BIN_OP,this::binOpDecVisit);
        addVisit(AstNode.ARRAY, this::varArrayAccessVisit);
        this.typeChecker = new TypeCheckUtil(symbolTable, funcName);
        return 0;
    }

    public Integer binOpDecVisit(JmmNode binOp, Integer dummy){
        var binOpChildren = binOp.getChildren();

        var firstOp = binOpChildren.get(0);
        var secondOp = binOpChildren.get(1);


        if(typeChecker.getType(secondOp) == null){
            return 0;
        }

        if (typeChecker.getType(secondOp).getName().equals("outside_type")){
            return 0;
        }
        else if (Objects.equals(binOp.get("op"), "LESS") ||
                Objects.equals(binOp.get("op"), "AND") ||
                 Objects.equals(binOp.get("op"), "SUB") ||
                 Objects.equals(binOp.get("op"), "ADD") ||
                 Objects.equals(binOp.get("op"), "MUL") ||
                 Objects.equals(binOp.get("op"), "DIV"))
        {
            if(!checkIfTypesCompatible(firstOp,secondOp )){
                this.addReport(new Report(ReportType.ERROR, Stage.SEMANTIC,Integer.parseInt(binOp.get("line")),
                        Integer.parseInt(binOp.get("column")),"Incompatible types for operation " + binOp.get("op")
                       + " " + typeChecker.getType(firstOp) + " and " + typeChecker.getType(secondOp)));
                return -1;
            }
            return 0;


        }
        else if (Objects.equals(binOp.get("op"), "ASSIGN")){
            Type firstOpType = typeChecker.getType(firstOp);
            Type secondOpType = typeChecker.getType(secondOp);
            if(symbolTable.getImports().contains(firstOpType.getName()) && symbolTable.getImports().contains(secondOpType.getName())){
                return 0;
            }

            if( (firstOpType.getName().equals(secondOpType.getName()) || checkIfAextendsB(firstOpType, secondOpType) ) && firstOpType.isArray() == secondOpType.isArray()){
                return 0;
            }
            this.addReport(new Report(ReportType.ERROR, Stage.SEMANTIC,Integer.parseInt(binOp.get("line")),
                    Integer.parseInt(binOp.get("column")),"Tried to assign incompatible variable of type " + secondOpType +
                    " to variable of type " + firstOpType));
            return -1;

        }
        return 0;

    }

    private boolean checkIfAextendsB(Type firstOpType, Type secondOpType){
        String firstTypeName = firstOpType.getName();
        String secondTypeName = secondOpType.getName();

        if(symbolTable.getSuper() == null){
            return false;
        }

        if(secondTypeName.equals(symbolTable.getClassName()) && firstTypeName.equals(symbolTable.getSuper())){
            return true;
        }
        return false;
    }

    //possible types: int, bool , string, Object
    private boolean checkIfTypesCompatible(JmmNode firstOp, JmmNode secondOp) {
        Type firstType = typeChecker.getType(firstOp);
        Type secondType = typeChecker.getType(secondOp);
        String firstTypeName = firstType.getName();
        String secondTypeName = secondType.getName();

        if (firstType.isArray() && !secondType.isArray() || !firstType.isArray() && secondType.isArray()){
            return false;
        }
        boolean isFirstTypeObj = !firstTypeName.equals("int") && !firstTypeName.equals("boolean") && !firstTypeName.equals("string");
        boolean isSecondTypeObj = !secondTypeName.equals("int") && !secondTypeName.equals("boolean") && !secondTypeName.equals("string");

        if(!isFirstTypeObj && isSecondTypeObj || isFirstTypeObj && !isSecondTypeObj){
            return false;
        }

        if(firstTypeName.equals("int") && secondTypeName.equals("boolean") || firstTypeName.equals("boolean") && secondTypeName.equals("int")){
            return false;
        }
        return true;

    }


    private Integer varArrayAccessVisit(JmmNode arrayNode, Integer integer) {
        var binOpChildren = arrayNode.getChildren();
        var firstOp = binOpChildren.get(0);
        var secondOp = binOpChildren.get(1);
        Type firstOpType = typeChecker.getType(firstOp);
        if(!firstOpType.isArray()){
            this.addReport(new Report(ReportType.ERROR, Stage.SEMANTIC,Integer.parseInt(arrayNode.get("line")),
                    Integer.parseInt(arrayNode.get("column")),"Invalid Array access on a varaible of type " + firstOpType.getName() ));
            return -1;
        }
        Type arrayAcessor = typeChecker.getType(secondOp);
        if(!arrayAcessor.getName().equals("int")){
            this.addReport(new Report(ReportType.ERROR, Stage.SEMANTIC,Integer.parseInt(arrayNode.get("line")),
                    Integer.parseInt(arrayNode.get("column")),"Invalid Array access: arrays must be indexed with ints" ));
            return -1;
        }

        return 0;
    }


}
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

public class MethodIsDeclaredCheck extends PreorderSemanticAnalyzer {
    private final SymbolTable symbolTable;
    String funcName = "";
    private TypeCheckUtil typeChecker;
    public MethodIsDeclaredCheck(SymbolTable symbolTable) {
        super();
        this.symbolTable = symbolTable;

        addVisit(AstNode.METHOD_DECLARATION,this::methodDecVisit);
    }

    public Integer methodDecVisit(JmmNode methodDecl, Integer dummy){
        funcName = methodDecl.getJmmChild(1).get("name");
        addVisit(AstNode.METHOD_CALL,this::methodCallVisit);
        this.typeChecker = new TypeCheckUtil(symbolTable,funcName);
        return 0;
    }


    private Integer methodCallVisit(JmmNode methodCall, Integer integer) {
        var caller = methodCall.getJmmChild(0);
        var method = methodCall.getJmmChild(1);
        var caller_type = typeChecker.getType(caller);
        if(symbolTable.getSuper() != null){
            return 0;
        }
        //we're probably dealing with a class that does not exist
        if(caller_type == null){
            return -1;
        }
        if(caller_type.getName().equals(symbolTable.getClassName())){
            if(!symbolTable.getMethods().contains(method.get("method"))){
                this.addReport(new Report(ReportType.ERROR, Stage.SEMANTIC,Integer.parseInt(methodCall.get("line")),
                        Integer.parseInt(methodCall.get("column")),"Method " + method.get("method") + " is not declared in class " + symbolTable.getClassName() ));
                return -1;
            }
        }
        return 0;
    }
}

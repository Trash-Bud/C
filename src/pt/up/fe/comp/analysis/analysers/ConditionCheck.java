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

public class ConditionCheck extends PreorderSemanticAnalyzer {
    private final SymbolTable symbolTable;
    String funcName = "";
    private TypeCheckUtil typeChecker;
    public ConditionCheck(SymbolTable symbolTable) {
        super();
        this.symbolTable = symbolTable;

        addVisit(AstNode.METHOD_DECLARATION,this::methodDecVisit);
    }

    public Integer methodDecVisit(JmmNode methodDecl, Integer dummy){
        funcName = methodDecl.getJmmChild(1).get("name");
        addVisit(AstNode.CONDITION,this::conditionVisit);
        this.typeChecker = new TypeCheckUtil(symbolTable,funcName);
        return 0;
    }

    public Integer conditionVisit(JmmNode conditionNode, Integer dummy){
        var child_node = conditionNode.getJmmChild(0);
        if (typeChecker.getType(child_node) != null && typeChecker.getType(child_node).getName().equals("boolean")){
            return 0;
        }

        this.addReport(new Report(ReportType.ERROR, Stage.SEMANTIC,Integer.parseInt(conditionNode.get("line")),
                Integer.parseInt(conditionNode.get("column")),"This condition could not be evaluated as a boolean value!"));
        return -1;
    }
}

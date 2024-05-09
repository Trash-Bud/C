package pt.up.fe.comp.analysis.analysers;

import pt.up.fe.comp.analysis.PreorderSemanticAnalyzer;
import pt.up.fe.comp.ast.AstNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.List;
import java.util.Objects;

public class VarDeclarationCheck extends PreorderSemanticAnalyzer {
    private final SymbolTable symbolTable;
    String funcName = "";
    boolean isStaticMethod = false;
    private  TypeCheckUtil typeChecker;

    public VarDeclarationCheck(SymbolTable symbolTable) {
        super();
        this.symbolTable = symbolTable;
        addVisit(AstNode.METHOD_DECLARATION,this::methodDecVisit);
    }

    public Integer methodDecVisit(JmmNode methodDecl,  Integer dummy){
        funcName = methodDecl.getJmmChild(1).get("name");
        isStaticMethod = Boolean.parseBoolean(methodDecl.get("isStatic"));
        addVisit(AstNode.VAR,this::varDecVisit);
        this.typeChecker = new TypeCheckUtil(symbolTable,funcName);
        return 0;
    }

    private Integer varDecVisit(JmmNode varDecl, Integer integer) {

        if (Objects.equals(varDecl.get("name"), "this")){
            if( isStaticMethod){
                //cant use field parameters in static method
                this.addReport(new Report(ReportType.ERROR, Stage.SEMANTIC,Integer.parseInt(varDecl.get("line")),
                        Integer.parseInt(varDecl.get("column")),"Can't use field parameters in static function" +
                        " "+ funcName ));
                return -1;
            }
            return 0;
        }
        List<Symbol> localVars = symbolTable.getLocalVariables(funcName);
        if (localVars != null) {
            for (Symbol var : localVars) {
                if (var.getName().equals(varDecl.get("name"))) {
                    return 0;
                }
            }
        }
        List<Symbol> fieldVars = symbolTable.getFields();
        if(fieldVars != null) {
            for (Symbol var : fieldVars) {
                if (var.getName().equals(varDecl.get("name"))) {
                    if( isStaticMethod){
                        //cant use field parameters in static method
                        this.addReport(new Report(ReportType.ERROR, Stage.SEMANTIC,Integer.parseInt(varDecl.get("line")),
                                Integer.parseInt(varDecl.get("column")),"Can't use field parameters in static function" +
                                " "+ funcName ));
                        return -1;
                    }
                    return 0;
                }
            }
        }
        List<Symbol> paramVars = symbolTable.getParameters(funcName);
        if(paramVars != null) {
            for (Symbol var : paramVars) {
                if (var.getName().equals(varDecl.get("name"))) {
                    return 0;
                }
            }
        }

        List<String> imports = symbolTable.getImports();
        if(imports.contains(varDecl.get("name"))) return 0;

        this.addReport(new Report(ReportType.ERROR, Stage.SEMANTIC,Integer.parseInt(varDecl.get("line")),
                Integer.parseInt(varDecl.get("column")),"Variable " + varDecl.get("name") + " is not initialized in method "+ funcName + ". Perhaps you meant to import it?"));
        return -1;
    }
}

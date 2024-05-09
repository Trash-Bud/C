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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FunctionArgsCheck extends PreorderSemanticAnalyzer {
    private final SymbolTable symbolTable;
    String funcName = "";
    private TypeCheckUtil typeChecker;
    public FunctionArgsCheck(SymbolTable symbolTable) {
        super();
        this.symbolTable = symbolTable;

        addVisit(AstNode.METHOD_DECLARATION,this::methodDecVisit);
    }


    public Integer methodDecVisit(JmmNode methodDecl, Integer dummy){
        funcName = methodDecl.getJmmChild(1).get("name");
        addVisit(AstNode.METHOD_CALL,this::methodCallVisit);
        this.typeChecker = new TypeCheckUtil(symbolTable,funcName);
        addVisit(AstNode.RETURN_EXPRESSION, this::returnExpressionVisit);
        return 0;
    }


    public Integer returnExpressionVisit(JmmNode returnExp, Integer dummy){
        var method_declaration = returnExp.getJmmParent();
        boolean is_array = method_declaration.getJmmChild(0).getOptional("isArray").isPresent();
        Type method_declaration_type = new Type(method_declaration.getJmmChild(0).get("name"), is_array);
        var returnExpressionType = typeChecker.getType(returnExp.getJmmChild(0));
        //this type does not exist (we're probably dealing with a unitialized variable)

        //this function is imported so we'll assume its type matches the declared type of the method
        if(returnExpressionType == null){
            this.addReport(new Report(ReportType.ERROR, Stage.SEMANTIC,Integer.parseInt(returnExp.get("line")),
                    Integer.parseInt(returnExp.get("column")),"Return expression with type null can't be the return type"
            ));
            return -1;
        }
        if(returnExpressionType.equals(new Type("outside_type", false))){
            return 0;
        }
        if(!returnExpressionType.equals(method_declaration_type)){
            this.addReport(new Report(ReportType.ERROR, Stage.SEMANTIC,Integer.parseInt(returnExp.get("line")),
                    Integer.parseInt(returnExp.get("column")),"Return expression with type " + returnExpressionType + "can't be the return of a method with declared type of "
                    + method_declaration_type
            ));
            return -1;
        }
        return 0;
    }
    private Integer methodCallVisit(JmmNode methodCallVisit, Integer integer) {
        var caller = methodCallVisit.getJmmChild(0);

        var imports = symbolTable.getImports();
        if(typeChecker.getType(caller) == null){
            this.addReport(new Report(ReportType.ERROR, Stage.SEMANTIC,Integer.parseInt(methodCallVisit.get("line")),
                    Integer.parseInt(methodCallVisit.get("column")),"Unknown type of var " + caller.get("name")
            + "Perhaps it's unimported?"));
            return -1;
        }
        if(imports.contains(typeChecker.getType(caller).getName()) ){
            return 0;
        }
        var called_method = methodCallVisit.getJmmChild(1);
        var called_method_name = called_method.get("method");
        List<JmmNode> called_with_params = new ArrayList<>();
        if(!called_method.getChildren().isEmpty()){
            called_with_params = called_method.getJmmChild(0).getChildren();
        }
        
        var method_params = symbolTable.getParameters(called_method_name);
        //we are dealing with an imported method, so we'll assume the parameters are okay
        if(method_params == null){
            return 0;
        }
        if(method_params.size() != called_with_params.size()){
            this.addReport(new Report(ReportType.ERROR,
                    Stage.SEMANTIC,Integer.parseInt(methodCallVisit.get("line")),
                    Integer.parseInt(methodCallVisit.get("column")),"Method " +
                    called_method_name + " was called with " + called_with_params.size() + " " +
                    " arguments but needs " + method_params.size() + " arguments! "));
            return -1;
        }
        for(int i = 0; i < called_with_params.size(); i++){
            var param_node = called_with_params.get(i);
            Type paramType = typeChecker.getType(param_node);
            if(!paramType.equals(method_params.get(i).getType())){
                this.addReport(new Report(ReportType.ERROR, Stage.SEMANTIC,Integer.parseInt(methodCallVisit.get("line")),
                        Integer.parseInt(methodCallVisit.get("column")),"Method " + called_method_name + " was called with invalid type arguments! "));
                return -1;
            }

        }
        return 0;

    }
}

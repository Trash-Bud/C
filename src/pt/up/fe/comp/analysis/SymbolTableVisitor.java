package pt.up.fe.comp.analysis;
import pt.up.fe.comp.ast.AstNode;
import pt.up.fe.comp.ast.AstUtils;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class SymbolTableVisitor extends PreorderJmmVisitor<SymbolTableBuilder, Integer> {

    private final List<Report> reports;

    public SymbolTableVisitor(){
        reports = new ArrayList<>();
        addVisit(AstNode.IMPORT_DECLARATION,this::importDecVisit);
        addVisit(AstNode.CLASS_DECLARATION,this::classDecVisit);
        addVisit(AstNode.METHOD_DECLARATION,this::methodDecVisit);
    }

    public List<Report>  getReports(){
        return reports;
    }

    private Integer importDecVisit(JmmNode importDecl, SymbolTableBuilder symbolTableBuilder){
        var imp = importDecl.getChildren().stream()
                .map(id -> id.get("name"))
                .collect(Collectors.joining("."));
        symbolTableBuilder.addImports(imp);

        return 0;
    }

    private Integer classDecVisit(JmmNode classDecl, SymbolTableBuilder symbolTableBuilder){

        getClassInfo(classDecl,symbolTableBuilder);
        return getFieldInfo(classDecl,symbolTableBuilder);
    }

    private void getClassInfo(JmmNode classDecl, SymbolTableBuilder symbolTableBuilder){
        var name = classDecl.get("name");
        symbolTableBuilder.setClassName(name);
        classDecl.getOptional("extends").ifPresent(symbolTableBuilder::setSuper);
    }

    private Integer getFieldInfo(JmmNode classDecl, SymbolTableBuilder symbolTableBuilder){
        var fields = classDecl.getChildren().stream()
                .filter(node -> node.getKind().equals("VarDeclaration")).toList();

        if(!fields.isEmpty()){
            for (JmmNode field : fields) {
                var name1 = field.get("name");
                if (symbolTableBuilder.hasField(name1)) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(field.get("line")),
                            Integer.parseInt(field.get("column")), "Found field with duplicate name."));
                    return -1;
                }
                var type1 = field.getJmmChild(0);
                symbolTableBuilder.addFields(new Symbol(AstUtils.buildType(type1), name1));
            }
        }
        return 0;
    }

    private Integer methodDecVisit(JmmNode methodDecl, SymbolTableBuilder symbolTableBuilder){
        if(getMethodInfo(methodDecl,symbolTableBuilder) == -1) return -1;
        String name = methodDecl.getJmmChild(1).get("name");


        return getVariables(methodDecl,name,symbolTableBuilder);
    }

    private Integer getVariables(JmmNode methodDecl, String name, SymbolTableBuilder symbolTableBuilder){

        var variables = methodDecl.getChildren().stream()
                .filter(node -> node.getKind().equals("VarDeclaration")).toList();
        List<Symbol> vars = new ArrayList<>();
        if(!variables.isEmpty()){
            for (JmmNode variable : variables) {
                var name1 = variable.get("name");
                /*
                if (symbolTableBuilder.hasField(name1)) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(variable.get("line")),
                            Integer.parseInt(variable.get("column")), "Found variable with duplicate name "+ name1 +"."));
                    symbolTableBuilder.addLocalVariables(name, vars);
                    return -1;
                }
                */

                var type1 = variable.getJmmChild(0);
                vars.add(new Symbol(AstUtils.buildType(type1), name1));
            }
        }

        symbolTableBuilder.addLocalVariables(name, vars);

        return 0;
    }


    private Integer getMethodInfo(JmmNode methodDecl, SymbolTableBuilder symbolTableBuilder){

        var methodName = methodDecl.getJmmChild(1).get("name");

        if (symbolTableBuilder.hasMethod(methodName)){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,Integer.parseInt(methodDecl.get("line")),
                    Integer.parseInt(methodDecl.get("column")),"Found duplicate method with signature " + methodName + "."));

            return -1;
        }
        var returnTypeNode = methodDecl.getJmmChild(0);
        var returnType = AstUtils.buildType(returnTypeNode);
        List<Symbol> symbols = new ArrayList<>();

        try {
            if (Objects.equals(methodDecl.getJmmChild(2).getKind(), "Param")) {

                var param = methodDecl.getJmmChild(2);
                var params = param.getChildren();

                for (int i = 0; i < params.size(); i+=2) {

                    symbols.add(new Symbol(AstUtils.buildType(params.get(i)), params.get(i+1).get("name")));
                }

            }
        }catch(Throwable e){
            System.out.println(e.getMessage());
        }

        symbolTableBuilder.addMethod(methodName,returnType,symbols);
        return 0;
    }


}
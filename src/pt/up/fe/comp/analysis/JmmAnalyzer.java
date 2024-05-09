package pt.up.fe.comp.analysis;

import pt.up.fe.comp.analysis.analysers.*;
import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.jmm.report.Report;

import java.util.ArrayList;
import java.util.List;

public class JmmAnalyzer implements JmmAnalysis {

    @Override
    public JmmSemanticsResult semanticAnalysis(JmmParserResult parserResult) {
        var symbolTable = new SymbolTableBuilder();
        var symbolTableVisitor = new SymbolTableVisitor();
        symbolTableVisitor.visit(parserResult.getRootNode(),symbolTable);
        List<Report> reports = new ArrayList<>(symbolTableVisitor.getReports());
        List<PreorderSemanticAnalyzer> analyzers = List.of(new VarDeclarationCheck(symbolTable), new OperationTypeCheck(symbolTable)
        , new ConditionCheck(symbolTable), new MethodIsDeclaredCheck(symbolTable), new FunctionArgsCheck(symbolTable));

        for(var analyzer : analyzers){
            analyzer.visit(parserResult.getRootNode());
            reports.addAll(analyzer.getReports());
        }

        return new JmmSemanticsResult(parserResult,symbolTable, reports);
    }
}

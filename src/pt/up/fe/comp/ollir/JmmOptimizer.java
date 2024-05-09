package pt.up.fe.comp.ollir;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;

import java.util.Collections;

public class JmmOptimizer implements JmmOptimization {

    @Override
    public JmmSemanticsResult optimize(JmmSemanticsResult semanticsResult) {
        /*
        var ConstantFolder = new ConstantFoldingAndPropagating(semanticsResult.getSymbolTable());
        ConstantFolder.visit(semanticsResult.getRootNode());

        System.out.println("After Constant Folding:\n");
        System.out.println(semanticsResult.getRootNode().sanitize().toTree());*/
        return semanticsResult;
    }

    @Override
    public OllirResult toOllir(JmmSemanticsResult semanticsResult) {
        var ollirGenerator = new OllirGenerator(semanticsResult.getSymbolTable());
        ollirGenerator.visit(semanticsResult.getRootNode());
        var ollirCode = ollirGenerator.getCode();

        System.out.println("OLLIR CODE:\n" + ollirCode);

        return new OllirResult(semanticsResult, ollirCode, Collections.emptyList());
    }
}

package pt.up.fe.comp;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import pt.up.fe.comp.analysis.JmmAnalyzer;
import pt.up.fe.comp.jasmin.JasminEmitter;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.ollir.JmmOptimizer;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.SpecsSystem;

public class Launcher {

    public static void main(String[] args) {
        SpecsSystem.programStandardInit();

        SpecsLogs.info("Executing with args: " + Arrays.toString(args));

        // read the input code
        if (args.length != 1) {
            throw new RuntimeException("Expected a single argument, a path to an existing input file.");
        }
        File inputFile = new File(args[0]);
        if (!inputFile.isFile()) {
            throw new RuntimeException("Expected a path to an existing input file, got '" + args[0] + "'.");
        }
        String input = SpecsIo.read(inputFile);

        // Create config
        Map<String, String> config = new HashMap<>();
        config.put("inputFile", args[0]);
        config.put("optimize", "false");
        config.put("registerAllocation", "-1");
        config.put("debug", "false");

        // Instantiate JmmParser
        SimpleParser parser = new SimpleParser();

        JmmParserResult parserResult = parser.parse(input, config);

        // Check if there are parsing errors
        TestUtils.noErrors(parserResult.getReports());

        var analyser = new JmmAnalyzer();

        var semanticResult = analyser.semanticAnalysis(parserResult);

        TestUtils.noErrors(semanticResult);


        var optimizer = new JmmOptimizer();

        var optimizationResultStep1 = optimizer.optimize(semanticResult);
        TestUtils.noErrors(optimizationResultStep1);
        var optimizationResultStep2 = optimizer.toOllir(optimizationResultStep1);
        TestUtils.noErrors(optimizationResultStep2);
        var optimizationResultStep3 = optimizer.optimize(optimizationResultStep2);
        TestUtils.noErrors(optimizationResultStep3);

        //Instantiate JasminBackend
        var jasminEmitter = new JasminEmitter();

        //Jasmin stage
        var jasminResult = jasminEmitter.toJasmin(optimizationResultStep3);
    }

}

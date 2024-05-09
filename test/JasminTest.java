import org.junit.Test;
import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.specs.util.SpecsIo;

import java.util.Collections;

public class JasminTest {

    @Test
    public void testFindMaximum() {
        var result = TestUtils.backend(SpecsIo.getResource("fixtures/public/Simple.jmm"));
        TestUtils.noErrors(result.getReports());

        result.compile();
        System.out.println(result.run());
    }

    @Test
    public void testWithOllir(){
        var ollirResult = new OllirResult(SpecsIo.getResource("fixtures/public/cp2/OllirToJasminArithmetics.ollir"), Collections.emptyMap());
        var jasminResult = TestUtils.backend(ollirResult);
        System.out.println("RUN RESULT: ");
        jasminResult.run();
    }

    @Test
    public void testTicTacToe() {
        var result = TestUtils.backend(SpecsIo.getResource("fixtures/public/WhileAndIf.jmm"));
        TestUtils.noErrors(result.getReports());

        result.compile();
        System.out.println(result.run());
    }

}

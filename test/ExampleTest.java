import org.junit.Test;

import pt.up.fe.comp.TestUtils;
import pt.up.fe.specs.util.SpecsIo;

public class ExampleTest {

    @Test
    public void testExpression() {

        var parserResult = TestUtils.parse("2+3\n10+20\n");
        parserResult.getReports().get(0).getException().get().printStackTrace();
        System.out.println();
        var analysisResult = TestUtils.analyse(parserResult);
    }

    @Test
    public void testFile(){
        var result = TestUtils.parse(SpecsIo.getResource("fixtures/public/Life.jmm"));
        TestUtils.noErrors(result.getReports());
        
    }

}

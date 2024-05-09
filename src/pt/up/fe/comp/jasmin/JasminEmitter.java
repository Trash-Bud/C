package pt.up.fe.comp.jasmin;

import java.util.Collections;

import pt.up.fe.comp.jmm.jasmin.JasminBackend;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jasmin.OllirToJasmin;
import pt.up.fe.comp.jmm.ollir.OllirResult; 

public class JasminEmitter implements JasminBackend 
{

    @Override
    public JasminResult toJasmin(OllirResult ollirResult) 
    {

        String jasminCode = new OllirToJasmin(ollirResult.getOllirClass()).getCode();

        System.out.println("JASMIN CODE:\n" + jasminCode);

        return new JasminResult(ollirResult, jasminCode, Collections.emptyList());
    }
}
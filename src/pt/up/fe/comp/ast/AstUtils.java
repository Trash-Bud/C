package pt.up.fe.comp.ast;

import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.specs.util.SpecsCheck;

public class AstUtils {

    public static Type buildType(JmmNode type){
        SpecsCheck.checkArgument(type.getKind().equals("Type"),() -> "Expexted node of type Type got " + type.getKind());

        var typeName = type.get("name");
        var isArray = type.getOptional("isArray").isPresent();

        return new Type(typeName,isArray);
    }
}

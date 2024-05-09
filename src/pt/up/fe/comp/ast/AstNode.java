package pt.up.fe.comp.ast;

import pt.up.fe.specs.util.SpecsStrings;

public enum AstNode {
    IMPORT_DECLARATION,
    CLASS_DECLARATION,
    METHOD_DECLARATION,
    START,
    VAR,
    BIN_OP,
    METHOD_CALL,
    INTEGER_LITERAL,
    BOOLEAN_VALUE,
    NEW_EXPRESSION,
    RETURN_EXPRESSION,
    VAR_DECLARATION,
    IF_STATEMENT,
    ELSE_STATEMENT,
    CONDITION,
    WHILE_STATEMENT, ARRAY, NEW_ARRAY, NOT_EXPRESSION;

    private final String name;

    AstNode(){
        this.name = SpecsStrings.toCamelCase(name(),"_",true);
    }

    @Override
    public String toString() {
        return name;
    }
}

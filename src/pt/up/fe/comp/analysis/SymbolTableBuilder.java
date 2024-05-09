package pt.up.fe.comp.analysis;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.*;


public class SymbolTableBuilder implements SymbolTable {

    private final List<String> imports;
    private String className;
    private String superName;
    private final List<Symbol> fields;
    private final List<String> methods;
    private final Map<String,Type> returnType;
    private final Map<String,List<Symbol>> parameters;
    private final Map<String,List<Symbol>>  localVariables;

    public SymbolTableBuilder() {
        this.imports = new ArrayList<>();
        this.className = null;
        this.superName = null;
        this.methods = new ArrayList<>();
        this.returnType = new HashMap<>();
        this.returnType.put("length",new Type("int", false));
        this.parameters = new HashMap<>();
        this.fields = new ArrayList<>();
        this.localVariables = new HashMap<>();
    }

    @Override
    public List<String> getImports() {
        return imports;
    }

    public void addImports(String importString) {
        imports.add(importString);
    }

    @Override
    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    @Override
    public String getSuper() {
        return superName;
    }
    public void setSuper(String superName) {
        this.superName = superName;
    }

    @Override
    public List<Symbol> getFields() {
        return fields;
    }

    public void addFields(Symbol field) {
        this.fields.add(field);
    }

    public boolean hasField(String name){
        var filtered = this.fields.stream().filter(
                (field) -> Objects.equals(field.getName(), name)).toList();
        return (filtered.size() != 0);
    }

    @Override
    public List<String> getMethods() {
        return methods;
    }

    public boolean hasMethod(String method){
        return methods.contains(method);
    }

    public void addMethod(String method, Type returnType, List<Symbol> parameters) {
        methods.add(method);
        this.parameters.put(method, parameters);
        this.returnType.put(method, returnType);
    }

    @Override
    public Type getReturnType(String methodSignature) {
        return returnType.get(methodSignature);
    }

    @Override
    public List<Symbol> getParameters(String methodSignature) {
        return parameters.get(methodSignature);
    }

    @Override
    public List<Symbol> getLocalVariables(String methodSignature) {
        return localVariables.get(methodSignature);
    }

    public void addLocalVariables(String methodSignature, List<Symbol> variable) {
        this.localVariables.put(methodSignature, variable);
    }
}

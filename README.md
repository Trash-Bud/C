
|Name | UP Number   | Grade | Contribution
|----|-------------|-------|-----------|
Joana Teixeira Mesquita | up201907878 | 19    | 25%
Miguel Norberto Costa Freitas | up201906159 | 19    | 25%
Diogo Miguel Chaves dos Santos Antunes Pereira | up201906422 | 19    | 25%
Rogério Filipe dos Santos Rocha | up201805123 | 19    | 25%

GLOBAL Grade of the project: 19

## Summary

This project's main goal was to apply the theoretical principals of the course Compilers. This was achieved by building
a compiler for programs written in the Java-- language. The main parts of the project are Syntactic error controller,
Semantic analysis, and Code generation.

## Semantic Analysis

### Type Verification
- Operations must be between elements of the same type (e.g. int + boolean is not allowed)
- Doesn't allow operations between arrays (e.g. array1 + array2)
- Array access is only allowed to arrays (e.g. 1[10] is not allowed)
- Only int values to index accessing array (e.g. a[true] is not allowed)
- Only int values to initialize an array
- Assignment between the same type (a_int = b_boolean is not allowed)
- Boolean operation (&&, < ou !) only with booleans
- Conditional expressions only accepts variables, operations or function calls that return boolean
- Raises an error if variable has not been initialized
- Assumes parameters are initialized

### Method Verification
- Check if the method "target" exists and if it contains the corresponding method (e.g. a.foo, check if 'a' exists and
  if it has a method 'foo')
- Check if method exists in the declared class (e.g. a using this), else: error if no extends is declared, if extends
  assume that the method is from the super class
- If the method is not from the declared class (meaning its from an imported class), assume that the method exists and
  assume the expected types (e.g. a = Foo.b(), if 'a' is an int, and 'Foo' an imported class, assume 'b' method as
  static, that has no arguments and returns an int)
- Verify that the number of arguments in an invoke is the same as the number of parameters of the declaration (method
  overload)
- Verify that the type of the parameters matches the type of the arguments (method overload)

### Fields

- Checks if the variable is initialized, throwing an error if the variable is used without being initialized;
- The `initialized` flag is saved on the symbol table.

## Code Generation

Starting with the Java-- code file, we developed a parser for this language, taking into account the grammar.
With the code exempt of lexical and syntactic errors, we perform the generation of the syntax tree while annotating some
nodes and leafs with extra information. With this AST we can create the Symbol Table and perform a Semantic Analysis.
The next step needed is to generate OLLIR code derived from the AST and Symbol table and the final step is to generate
the set of JVM instructions to be accepted by jasmin.

All the necessary code is generated in the Main and the AST, SymbolTable, OLLIR code, Jasmin code and class files are
saved inside a folder in the root of the project with the name of the jmm file. The class file is also saved under
test/fixtures/libs/compiled/ so that other files that extend those classes can use it. For example, running the
HelloWorld.jmm will generate a HelloWorld folder in the root of the projct, and within that folder the HelloWorld.json,
HelloWorld.symbols.txt, HelloWorld.ollir, HelloWorld.j, HelloWorld.class, and we went for the extra mile adding an
"enhanced" symbol table, which features the method overloading, and every field regarding each method (fields will also
be marked as initialized or not).

## PROS

### Optimizations: 

- Use of more efficient instructions of Jvm
- Constant Folding
- Constant Propagation

## CONS

- no cons

# Compilers Project

For this project, you need to install [Java](https://jdk.java.net/), [Gradle](https://gradle.org/install/), and [Git](https://git-scm.com/downloads/) (and optionally, a [Git GUI client](https://git-scm.com/downloads/guis), such as TortoiseGit or GitHub Desktop). Please check the [compatibility matrix](https://docs.gradle.org/current/userguide/compatibility.html) for Java and Gradle versions.

## Project setup

There are three important subfolders inside the main folder. First, inside the subfolder named ``javacc`` you will find the initial grammar definition. Then, inside the subfolder named ``src`` you will find the entry point of the application. Finally, the subfolder named ``tutorial`` contains code solutions for each step of the tutorial. JavaCC21 will generate code inside the subfolder ``generated``.

## Compile and Running

To compile and install the program, run ``gradle installDist``. This will compile your classes and create a launcher script in the folder ``./build/install/comp2022-00/bin``. For convenience, there are two script files, one for Windows (``comp2022-00.bat``) and another for Linux (``comp2022-00``), in the root folder, that call tihs launcher script.

After compilation, a series of tests will be automatically executed. The build will stop if any test fails. Whenever you want to ignore the tests and build the program anyway, you can call Gradle with the flag ``-x test``.

## Test

To test the program, run ``gradle test``. This will execute the build, and run the JUnit tests in the ``test`` folder. If you want to see output printed during the tests, use the flag ``-i`` (i.e., ``gradle test -i``).
You can also see a test report by opening ``./build/reports/tests/test/index.html``.

## Checkpoint 1
For the first checkpoint the following is required:

1. Convert the provided e-BNF grammar into JavaCC grammar format in a .jj file
2. Resolve grammar conflicts, preferably with lookaheads no greater than 2
3. Include missing information in nodes (i.e. tree annotation). E.g. include the operation type in the operation node.
4. Generate a JSON from the AST

### JavaCC to JSON
To help converting the JavaCC nodes into a JSON format, we included in this project the JmmNode interface, which can be seen in ``src-lib/pt/up/fe/comp/jmm/ast/JmmNode.java``. The idea is for you to use this interface along with the Node class that is automatically generated by JavaCC (which can be seen in ``generated``). Then, one can easily convert the JmmNode into a JSON string by invoking the method JmmNode.toJson().

Please check the JavaCC tutorial to see an example of how the interface can be implemented.

### Reports
We also included in this project the class ``src-lib/pt/up/fe/comp/jmm/report/Report.java``. This class is used to generate important reports, including error and warning messages, but also can be used to include debugging and logging information. E.g. When you want to generate an error, create a new Report with the ``Error`` type and provide the stage in which the error occurred.


### Parser Interface

We have included the interface ``src-lib/pt/up/fe/comp/jmm/parser/JmmParser.java``, which you should implement in a class that has a constructor with no parameters (please check ``src/pt/up/fe/comp/CalculatorParser.java`` for an example). This class will be used to test your parser. The interface has a single method, ``parse``, which receives a String with the code to parse, and returns a JmmParserResult instance. This instance contains the root node of your AST, as well as a List of Report instances that you collected during parsing.

To configure the name of the class that implements the JmmParser interface, use the file ``config.properties``.

### Compilation Stages 

The project is divided in four compilation stages, that you will be developing during the semester. The stages are Parser, Analysis, Optimization and Backend, and for each of these stages there is a corresponding Java interface that you will have to implement (e.g. for the Parser stage, you have to implement the interface JmmParser).


### config.properties

The testing framework, which uses the class TestUtils located in ``src-lib/pt/up/fe/comp``, has methods to test each of the four compilation stages (e.g., ``TestUtils.parse()`` for testing the Parser stage). 

In order for the test class to find your implementations for the stages, it uses the file ``config.properties`` that is in root of your repository. It has four fields, one for each stage (i.e. ``ParserClass``, ``AnalysisClass``, ``OptimizationClass``, ``BackendClass``), and initially it only has one value, ``pt.up.fe.comp.SimpleParser``, associated with the first stage.

During the development of your compiler you will update this file in order to setup the classes that implement each of the compilation stages.

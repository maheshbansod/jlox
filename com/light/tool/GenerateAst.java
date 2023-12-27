package com.light.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

public class GenerateAst {
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: generate_ast <output_directory>");
            System.exit(64);
        }
        String outputDir = args[0];

        defineAst(outputDir, "Expr", Arrays.asList(
            "Assign : Token name, Expr value",
            "Binary : Expr left, Token operator, Expr right",
            "Call : Expr callee, Token paren, List<Expr> arguments",
            "Grouping : Expr expression",
            "Get : Expr object, Token name",
            "Literal : Object value",
            "Logical : Expr left, Token operator, Expr right",
            "Set : Expr object, Token name, Expr value",
            "This : Token keyword",
            "Unary : Token operator, Expr right",
            "Variable : Token name"
        ));

        defineAst(outputDir, "Stmt", Arrays.asList(
            "Block : List<Stmt> statements",
            "Break : Token keyword",
            "Class : Token name, List<Stmt.Function> methods",
            "Expression : Expr expression",
            "Function : Token name, List<Token> params, List<Stmt> body",
            "If : Expr condition, Stmt thenBranch, Stmt elseBranch",
            "Print : Expr expression",
            "Return : Token keyword, Expr value",
            "Var : Token name, Expr initializer",
            "While : Expr condition, Stmt body"
        ));
    }

    private static void defineAst(
        String outputDir, String baseName, List<String> types
    ) throws IOException {
        String path = outputDir + "/" + baseName + ".java";
        PrintWriter writer = new PrintWriter(path, "UTF-8");

        writer.println("package com.light.jlox;");
        writer.println();
        writer.println("import java.util.List;");
        writer.println();
        writer.println("abstract class " + baseName + " {");

        defineVisitor(writer, baseName, types);

        writer.println("    abstract <R> R accept(Visitor<R> visitor);");

        // AST classes
        for (String type: types) {
            String className = type.split(":")[0].trim();
            String fields = type.split(":")[1].trim();
            defineType(writer, baseName, className, fields);
        }
        writer.println("}");
        writer.close();
    }

    private static void defineType(
        PrintWriter writer, String baseName, String className, String fieldList
    ) {
        writer.println("    static class " + className + " extends " + baseName + " {");
        String[] fields;
        if (fieldList.isEmpty()) {
            fields = new String[0];
        } else {
            fields = fieldList.split(",");
        }
        // declare members
        for (String field: fields) {
            writer.println("        final " + field.trim() + ";");
        }

        // constructor
        writer.println("        " + className + "("+ fieldList + ") {");
        for (String field: fields) {
            String name = field.trim().split(" ")[1];
            writer.println("            this." + name + " = " + name + ";");
        }
        writer.println("        }");

        // visitor pattern
        writer.println();
        writer.println("        @Override");
        writer.println("        <R> R accept(Visitor<R> visitor) {");
        writer.println("            return visitor.visit" + className + baseName + "(this);");
        writer.println("        }");


        writer.println("    }");
    }

    private static void defineVisitor(PrintWriter writer, String baseName, List<String> types) {
        writer.println("    interface Visitor<R> {");
        for (String type: types) {
            String typeName = type.split(":")[0].trim();
            writer.println("        R visit" + typeName + baseName + "(" + typeName + " " + baseName.toLowerCase() + ");");
        }
        writer.println("    }");
    }
}

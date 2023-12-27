package com.light.jlox;

import com.light.jlox.Expr.Assign;
import com.light.jlox.Expr.Binary;
import com.light.jlox.Expr.Call;
import com.light.jlox.Expr.Get;
import com.light.jlox.Expr.Grouping;
import com.light.jlox.Expr.Literal;
import com.light.jlox.Expr.Logical;
import com.light.jlox.Expr.Set;
import com.light.jlox.Expr.This;
import com.light.jlox.Expr.Unary;
import com.light.jlox.Expr.Variable;

class AstPrinter implements Expr.Visitor<String> {
    private Environment environment;
    String print(Expr expr, Environment environment) {
        this.environment = environment;
        return expr.accept(this);
    }

    @Override
    public String visitBinaryExpr(Binary expr) {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right);
    }

    @Override
    public String visitGroupingExpr(Grouping expr) {
        return parenthesize("group", expr.expression);
    }

    @Override
    public String visitLiteralExpr(Literal expr) {
        if (expr.value == null) return "nil";
        return expr.value.toString();
    }

    @Override
    public String visitUnaryExpr(Unary expr) {
        return parenthesize(expr.operator.lexeme, expr.right);
    }

    private String parenthesize(String name, Expr ...exprs) {
        StringBuilder builder = new StringBuilder();

        builder.append("(").append(name);
        for (Expr expr: exprs) {
            builder.append(" ");
            builder.append(expr.accept(this));
        }
        builder.append(")");

        return builder.toString();
    }

    @Override
    public String visitVariableExpr(Variable expr) {
        Object value = environment.get(expr.name);
        if (value == null) return null;
        return value.toString();
    }

    @Override
    public String visitAssignExpr(Assign expr) {
        String lhs = expr.name.lexeme;
        return parenthesize(lhs + " =", expr.value);
    }

    @Override
    public String visitLogicalExpr(Logical expr) {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right);
    }

    @Override
    public String visitCallExpr(Call expr) {
        return parenthesize("fn(" + expr.callee.accept(this) + ")", expr.arguments.stream().toArray(Expr[]::new));
    }

    @Override
    public String visitGetExpr(Get expr) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitGetExpr'");
    }

    @Override
    public String visitSetExpr(Set expr) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitSetExpr'");
    }

    @Override
    public String visitThisExpr(This expr) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitThisExpr'");
    }
}

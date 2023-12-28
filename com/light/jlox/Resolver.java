package com.light.jlox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import com.light.jlox.Expr.Assign;
import com.light.jlox.Expr.Binary;
import com.light.jlox.Expr.Call;
import com.light.jlox.Expr.Get;
import com.light.jlox.Expr.Grouping;
import com.light.jlox.Expr.Literal;
import com.light.jlox.Expr.Logical;
import com.light.jlox.Expr.Set;
import com.light.jlox.Expr.Super;
import com.light.jlox.Expr.This;
import com.light.jlox.Expr.Unary;
import com.light.jlox.Expr.Variable;
import com.light.jlox.Stmt.Block;
import com.light.jlox.Stmt.Break;
import com.light.jlox.Stmt.Class;
import com.light.jlox.Stmt.Expression;
import com.light.jlox.Stmt.Function;
import com.light.jlox.Stmt.If;
import com.light.jlox.Stmt.Print;
import com.light.jlox.Stmt.Return;
import com.light.jlox.Stmt.Var;
import com.light.jlox.Stmt.While;

class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {

    private final Interpreter interpreter;
    private final Stack<Map<String, VariableStaticState>> scopes = new Stack<>();
    private FunctionType currentFunction = FunctionType.NONE;
    private boolean isInLoop = false;
    private ClassType currentClass = ClassType.NONE;

    private class VariableStaticState {

        private enum VariableState {
            DECLARED,
            INITIALIZED,
            USED
        }

        private VariableState state;
        Token token;

        VariableStaticState(Token token) {
            this.token = token;
            state = VariableState.DECLARED;
        }

        void markInitialized() {
            state = VariableState.INITIALIZED;
        }

        void markUsed() {
            state = VariableState.USED;
        }

        boolean isUsed() {
            return state == VariableState.USED;
        }

        boolean isUninitialized() {
            return state == VariableState.DECLARED;
        }
    }

    private enum FunctionType {
        NONE,
        FUNCTION,
        METHOD,
        INITIALIZER
    }

    private enum ClassType {
        NONE,
        CLASS,
        SUBCLASS
    }

    Resolver(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    @Override
    public Void visitBlockStmt(Block stmt) {
        beginScope();
        resolve(stmt.statements);
        endScope();
        return null;
    }

    private void beginScope() {
        scopes.push(new HashMap<String, VariableStaticState>());
    }

    private void endScope() {
        var scope = scopes.pop();
        for (var entry : scope.entrySet()) {
            if (!entry.getValue().isUsed() && !entry.getKey().startsWith("_")) {
                Lox.error(entry.getValue().token, "Unused local variable.\n(Try prefixing the variable with '_' if this is intentional)");
            }
        }
    }

    void resolve(List<Stmt> statements) {
        for (Stmt statement : statements) {
            resolve(statement);
        }
    }

    void resolve(Stmt statement) {
        statement.accept(this);
    }

    void resolve(Expr expr) {
        expr.accept(this);
    }

    @Override
    public Void visitBreakStmt(Break stmt) {
        if (!isInLoop) {
            Lox.error(stmt.keyword, "'break' must be within a loop.");
        }
        return null;
    }

    @Override
    public Void visitExpressionStmt(Expression stmt) {
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitFunctionStmt(Function stmt) {
        declare(stmt.name);
        define(stmt.name);

        resolveFunction(stmt, FunctionType.FUNCTION);
        return null;
    }

    private void resolveFunction(Stmt.Function function, FunctionType type) {
        FunctionType enclosingFunction = currentFunction;
        currentFunction = type;
        beginScope();
        for (Token param: function.params) {
            declare(param);
            define(param);
        }
        resolve(function.body);
        endScope();
        currentFunction = enclosingFunction;
    }

    @Override
    public Void visitIfStmt(If stmt) {
        resolve(stmt.condition);
        resolve(stmt.thenBranch);
        if (stmt.elseBranch != null){
            resolve(stmt.elseBranch);
        }
        return null;
    }

    @Override
    public Void visitPrintStmt(Print stmt) {
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitReturnStmt(Return stmt) {
        if (currentFunction == FunctionType.NONE) {
            Lox.error(stmt.keyword, "Can't return from top-level code.");
        }
        if (stmt.value != null) {
            if (currentFunction == FunctionType.INITIALIZER) {
                Lox.error(stmt.keyword, "Can't return a value from an initializer");
            }
            resolve(stmt.value);
        }
        return null;
    }

    @Override
    public Void visitVarStmt(Var stmt) {
        declare(stmt.name);
        if (stmt.initializer != null) {
            resolve(stmt.initializer);
        }
        define(stmt.name);
        return null;
    }

    private void declare(Token name) {
        if (scopes.isEmpty()) return;

        var scope = scopes.peek();
        if (scope.containsKey(name.lexeme)) {
            Lox.error(name, "A variable with the same name already exists in this scope.");
        }
        scope.put(name.lexeme, new VariableStaticState(name));
    }

    private void define(Token name) {
        if (scopes.isEmpty()) return;

        scopes.peek().get(name.lexeme).markInitialized();
    }

    @Override
    public Void visitWhileStmt(While stmt) {
        isInLoop = true;
        resolve(stmt.condition);
        resolve(stmt.body);
        isInLoop = false;
        return null;
    }

    @Override
    public Void visitAssignExpr(Assign expr) {
        resolve(expr.value);
        resolveLocal(expr, expr.name);
        return null;
    }

    @Override
    public Void visitBinaryExpr(Binary expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitCallExpr(Call expr) {
        resolve(expr.callee);
        for (Expr argument: expr.arguments) {
            resolve(argument);
        }

        return null;
    }

    @Override
    public Void visitGroupingExpr(Grouping expr) {
        resolve(expr.expression);
        return null;
    }

    @Override
    public Void visitLiteralExpr(Literal expr) {
        return null;
    }

    @Override
    public Void visitLogicalExpr(Logical expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitUnaryExpr(Unary expr) {
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitVariableExpr(Variable expr) {
        if (!scopes.isEmpty()) {
            var scope = scopes.peek();
            if (scope.containsKey(expr.name.lexeme) && scope.get(expr.name.lexeme).isUninitialized()) {
                Lox.error(expr.name, "Can't read local variable in it's own initializer");
            }
        }
        resolveLocal(expr, expr.name);
        return null;
    }

    private void resolveLocal(Expr expr, Token name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            var scope = scopes.get(i);
            if (scope.containsKey(name.lexeme)) {
                scope.get(name.lexeme).markUsed();
                interpreter.resolve(expr, scopes.size() - 1 - i);
                return;
            }
        }
    }

    @Override
    public Void visitClassStmt(Class stmt) {
        ClassType enclosingClassType = currentClass;
        currentClass = ClassType.CLASS;
        declare(stmt.name);
        define(stmt.name);

        if (stmt.superclass != null && stmt.superclass.name.lexeme.equals(stmt.name.lexeme)) {
            Lox.error(stmt.superclass.name, "A class can't inherit from itself.");
        }

        if (stmt.superclass != null) {
            currentClass = ClassType.SUBCLASS;
            resolve(stmt.superclass);
            beginScope();
            var superclassState = new VariableStaticState(stmt.name);
            superclassState.markUsed(); // to get rid of any unused variable errors
            scopes.peek().put("super", superclassState);
        }

        beginScope();
        var thisState = new VariableStaticState(stmt.name);
        thisState.markUsed(); // to get rid of any unused variable errors
        scopes.peek().put("this", thisState);

        for (Stmt.Function method : stmt.methods) {
            
            FunctionType declaration = method.name.lexeme.equals("init") ? FunctionType.INITIALIZER : FunctionType.METHOD;
            resolveFunction(method, declaration);
        }

        endScope();

        if (stmt.superclass != null) endScope();

        currentClass = enclosingClassType;

        return null;
    }

    @Override
    public Void visitGetExpr(Get expr) {
        resolve(expr.object);
        return null;
    }

    @Override
    public Void visitSetExpr(Set expr) {
        resolve(expr.object);
        resolve(expr.value);
        return null;
    }

    @Override
    public Void visitThisExpr(This expr) {
        if (currentClass == ClassType.NONE) {
            Lox.error(expr.keyword, "Can't use 'this' outside of a class.");
            return null;
        }
        resolveLocal(expr, expr.keyword);
        return null;
    }

    @Override
    public Void visitSuperExpr(Super expr) {
        if (currentClass == ClassType.NONE) {
            Lox.error(expr.keyword, "Can't use super aoutside a class.");
        }
        if (currentClass != ClassType.SUBCLASS) {
            Lox.error(expr.keyword, "Can only use super inside a subclass.");
        }
        resolveLocal(expr, expr.keyword);
        return null;
    }
    
}

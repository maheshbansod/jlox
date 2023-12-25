package com.light.jlox;

import static com.light.jlox.TokenType.*;

import java.util.ArrayList;
import java.util.List;

class Parser {
    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }

        return statements;
    }

    private Stmt declaration() {
        try {
            if (match(VAR)) return varDeclaration();

            return statement();
        } catch (ParseError e) {
            // synchronize();
            return null;
        }
    }

    private Stmt varDeclaration() {
        Token name = consume(IDENTIFIER, "Expect variable name.");

        Expr initializer = null;
        if (match(EQUAL)) {
            initializer = expression();
        }

        consume(SEMICOLON, "Expect ; after variable declaration.");
        return new Stmt.Var(name, initializer);
    }

    private Stmt statement() {
        if (match(LEFT_BRACE)) return new Stmt.Block(block());
        if (match(PRINT)) return printStatement();

        return expressionStatement();
    }

    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();
        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }

        consume(RIGHT_BRACE, "Expect '}' after block.");

        return statements;
    }

    private Stmt printStatement() {
        Expr value = expression();
        consume(SEMICOLON, "Expect ';' after value.");

        return new Stmt.Print(value);
    }

    private Stmt expressionStatement() {
        Expr value = expression();
        consume(SEMICOLON, "Expect ';' after value.");

        return new Stmt.Expression(value);
    }

    private Expr expression() {
        return assignment();
    }

    private Expr assignment() {
        Expr expr = commaExpr();

        if (match(EQUAL)) {
            Token equals = previous();
            Expr value = assignment();

            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable)expr).name;
                return new Expr.Assign(name, value);
            }

            error(equals, "Invalid assignment target.");
        }
        
        return expr;
    }

    private Expr commaExpr() {
        return matchBinExp(() -> equality(), COMMA);
    }

    private Expr equality() {
        return matchBinExp(() -> comparison(), BANG_EQUAL, EQUAL_EQUAL);
    }

    private Expr comparison() {
        return matchBinExp(() -> term(), GREATER, GREATER_EQUAL, LESS, LESS_EQUAL);
    }

    private Expr term() {
        return matchBinExp(() -> factor(), PLUS, MINUS);
    }

    private Expr factor() {
        return matchBinExp(() -> unary(), SLASH, STAR);
    }

    private Expr unary() {
        if (match(BANG, MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }
        return primary();
    }

    private Expr primary() {
        if (match(TRUE)) return new Expr.Literal(true);
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(NIL)) return new Expr.Literal(null);

        if (match(NUMBER, STRING)) {
            return new Expr.Literal(previous().literal);
        }

        if (match(IDENTIFIER)) return new Expr.Variable(previous());

        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }

        if (match(IDENTIFIER, STRING, NUMBER)) {
            return new Expr.Literal(peek().literal);
        }
        
        throw error(peek(), "Expect expression.");
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) {
            return advance();
        }

        throw error(peek(), message);
    }

    private ParseError error(Token token, String message) {
        Lox.error(current, message);
        return new ParseError();
    }

    private Expr matchBinExp(BinaryExprMaker binexp, TokenType... types) {
        Expr expr = binexp.operation();
        
        while(match(types)) {
            Token operator = previous();
            Expr right = binexp.operation();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private boolean match(TokenType... types) {
        for (TokenType type: types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type == EOF;
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private Token peek(){
        return tokens.get(current);
    }

    interface BinaryExprMaker {
        Expr operation();
    }

    private static class ParseError extends RuntimeException {

    }
}

# JLox

Java implementation of [the lox language](https://craftinginterpreters.com/the-lox-language.html) interpreter

## Compile
```sh
javac com/light/jlox/Lox.java
```

## Running
```sh
java com.light.jlox.Lox [LOX_FILE]
```
Without arguments, it starts the lox interpreter interactively.

To run an example lox code:
```sh
java com.light.jlox.Lox lox_examples/<example name>.lox
```
package com.timing.p36;


public class Compiler {

    public static void main(String[] args) {
        Lexer lexer = new Lexer();

        ParseTableBuilder tableBuilder = new ParseTableBuilder();
        tableBuilder.runFirstSets();
        tableBuilder.printAllFirstSet();
    }
}

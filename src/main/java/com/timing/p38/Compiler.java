package com.timing.p38;


public class Compiler {

    public static void main(String[] args) {
        Lexer lexer = new Lexer();

        ParseTableBuilder tableBuilder = new ParseTableBuilder();
        tableBuilder.runFollowSets();

    }
}

package com.timing.p38;


import java.util.Stack;

import static com.timing.p38.AttributedPDAParser.Grammar.*;


public class AttributedPDAParser {
    enum Grammar {
        STMT,
        EXPR,
        EXPR_PRIME,
        TERM,
        TERM_PRIME,
        FACTOR,
        NUM_OR_ID,
        PLUS,
        SEMI,
        MULTIPLE,
        LEFT_PARENT,
        RIGHT_PARENT,
        ACTION_0,   //{$1=$2=getname();}
        ACTION_1,  //{freename($0);}
        ACTION_2, //{System.out.println($$ + " += " + $0); freename($0);}
        ACTION_3, //{System.out.println($$ + " *= " + $0); freename($0);}
        ACTION_4  //{System.out.println($$ + " = " + lexer.yytext);}
    }

	private final Lexer lexer;
    private final Stack<Grammar> pdaStack = new Stack<Grammar>();
    private final Stack<Attribute> valueStack = new Stack<Attribute>();
    private Attribute parentAttribute = null;

    private String[] names = null;
    private int nameIdx = 0;

    private String getname() {
        return names[nameIdx++];
    }

    private void freename(String name) {
        nameIdx--;
        if (nameIdx >= 0) {
            names[nameIdx] = name;
        }
    }


    public AttributedPDAParser(Lexer lexer) {
        this.lexer = lexer;
        names = new String[]{"t0", "t1", "t2", "t3", "t4", "t5", "t6"};
        parentAttribute = Attribute.getAttribute(null);
        pushGrammarSymbol(STMT);
    }


    public void parse() {
        while (pdaStack.empty() == false) {
            Grammar action = pdaStack.peek();

            switch (action) {
                case STMT:
                    if (lexer.match(Lexer.EOI)) {
                        popStacks();
                    } else {
                        popStacks();
                        pushGrammarSymbol(STMT);
                        pushGrammarSymbol(SEMI);
                        pushGrammarSymbol(ACTION_1);
                        pushGrammarSymbol(EXPR);
                        pushGrammarSymbol(ACTION_0);
                    }

                    break;
                case EXPR:
                    popStacks();
                    pushGrammarSymbol(EXPR_PRIME);
                    pushGrammarSymbol(TERM);
                    break;
                case TERM:
                    popStacks();
                    pushGrammarSymbol(TERM_PRIME);
                    pushGrammarSymbol(FACTOR);
                    break;
                case TERM_PRIME:
                    popStacks();
                    if (lexer.match(Lexer.TIMES)) {
                        pushGrammarSymbol(TERM_PRIME);
                        pushGrammarSymbol(ACTION_3);
                        pushGrammarSymbol(FACTOR);
                        pushGrammarSymbol(ACTION_0);
                        pushGrammarSymbol(MULTIPLE);
                    }
                    break;
                case FACTOR:
                    popStacks();
                    if (lexer.match(Lexer.NUM_OR_ID)) {
                        pushGrammarSymbol(ACTION_4);
                        pushGrammarSymbol(NUM_OR_ID);
                    } else if (lexer.match(Lexer.LP)) {
                        pushGrammarSymbol(RIGHT_PARENT);
                        pushGrammarSymbol(EXPR);
                        pushGrammarSymbol(LEFT_PARENT);
                    } else {
                        parseError();
                    }
                    break;
                case EXPR_PRIME:
                    popStacks();
                    if (lexer.match(Lexer.PLUS)) {
                        pushGrammarSymbol(EXPR_PRIME);
                        pushGrammarSymbol(ACTION_2);
                        pushGrammarSymbol(TERM);
                        pushGrammarSymbol(ACTION_0);
                        pushGrammarSymbol(PLUS);
                    }
                    break;
                case NUM_OR_ID:
                    popStacks();
                    if (lexer.match(Lexer.NUM_OR_ID) == false) {
                        parseError();
                    }

                    break;
                case PLUS:
                    popStacks();
                    if (lexer.match(Lexer.PLUS) == false) {
                        parseError();
                    }
                    lexer.advance();
                    break;
                case MULTIPLE:
                    popStacks();
                    if (lexer.match(Lexer.TIMES) == false) {
                        parseError();
                    }
                    lexer.advance();
                    break;
                case LEFT_PARENT:
                    popStacks();
                    if (lexer.match(Lexer.LP) == false) {
                        parseError();
                    }
                    lexer.advance();
                    break;
                case RIGHT_PARENT:
                    //pdaStack.pop(); 1+(2*3)
                    popStacks();
                    if (lexer.match(Lexer.RP) == false) {
                        parseError();
                    }
                    lexer.advance();
                    break;
                case SEMI:
                    popStacks();
                    if (lexer.match(Lexer.SEMI) == false) {
                        parseError();
                    }
                    lexer.advance();
                    break;

                case ACTION_0:
                    pdaStack.pop();
                    String t = getname();
                    int curPos = valueStack.size() - 1;
                    System.out.println("value stack grammar: " + valueStack.get(curPos - 1).getGrammar());
                    valueStack.get(curPos - 1).right = "";
                    valueStack.get(curPos - 1).right = t;

                    System.out.println("value stack grammar: " + valueStack.get(curPos - 2).getGrammar());
                    valueStack.get(curPos - 2).right = "";
                    valueStack.get(curPos - 2).right = t;
                    valueStack.pop();
                    break;
                case ACTION_1:
                    pdaStack.pop();
                    String attribute = (String) valueStack.pop().right;
                    freename(attribute);
                    break;
                case ACTION_2:
                    pdaStack.pop();
                    Attribute curAttribute = valueStack.pop();
                    String parentAttribute = (String) curAttribute.left;
                    String childAttribute = (String) curAttribute.right;
                    System.out.println(parentAttribute + " += " + childAttribute);
                    freename(childAttribute);
                    break;
                case ACTION_3:
                    pdaStack.pop();
                    curAttribute = valueStack.pop();
                    System.out.println(curAttribute.left + " *= " + curAttribute.right);
                    break;
                case ACTION_4:
                    pdaStack.pop();
                    curAttribute = valueStack.pop();
                    System.out.println(curAttribute.left + " = " + lexer.yytext);
                    lexer.advance();
                    break;
            }

        }

    }

    private void popStacks() {
        pdaStack.pop();
        parentAttribute = valueStack.pop();
    }

    private void pushGrammarSymbol(Grammar grammar) {
        pdaStack.push(grammar);
        Attribute attr = Attribute.getAttribute(parentAttribute.right);
        attr.setGrammar(grammar.toString());
        valueStack.push(attr);
    }

    private void parseError() {
        System.err.println("PDA parse error");
        System.exit(1);
    }
}

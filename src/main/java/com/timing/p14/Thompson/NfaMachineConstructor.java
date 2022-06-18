package com.timing.p14.Thompson;

import java.util.Set;

public class NfaMachineConstructor {
    private final Lexer lexer;
    private NfaManager nfaManager = null;

    public NfaMachineConstructor(Lexer lexer) throws Exception {
        this.lexer = lexer;
        nfaManager = new NfaManager();

        while (lexer.MatchToken(Lexer.Token.EOS)) {
            lexer.advance();
        }
    }

    public void cat_expr(NfaPair pairOut) throws Exception {
        /*
         * cat_expr -> factor factor .....
         * 由于多个factor 前后结合就是一个cat_expr所以
         * cat_expr-> factor cat_expr
         */
        Nfa e2_start, e2_end;
        if (first_in_cat(lexer.getCurrentToken())) {// 判断一下头的正则有没有放错位置
            factor(pairOut);
        }

        while (first_in_cat(lexer.getCurrentToken())) {
            NfaPair pairLocal = new NfaPair();
            factor(pairLocal);

            pairOut.endNode.next = pairLocal.startNode;

            pairOut.endNode = pairLocal.endNode;
        }
    }

    private boolean first_in_cat(Lexer.Token tok) throws Exception {
        switch (tok) {
            //正确的表达式不会以 ) $ 开头,如果遇到EOS表示正则表达式解析完毕，那么就不应该执行该函数
            case CLOSE_PAREN:
            case AT_EOL:
            case EOS:
                return false;
            case CLOSURE:
            case PLUS_CLOSE:
            case OPTIONAL:
                //*, +, ? 这几个符号应该放在表达式的末尾
                ErrorHandler.parseErr(ErrorHandler.Error.E_CLOSE);
                return false;
            case CCL_END:
                //表达式不应该以]开头
                ErrorHandler.parseErr(ErrorHandler.Error.E_BRACKET);
                return false;
            case AT_BOL:
                //^必须在表达式的最开始
                ErrorHandler.parseErr(ErrorHandler.Error.E_BOL);
                return false;
        }

        return true;
    }

    public void factor(NfaPair pairOut) throws Exception {
        boolean handled = false;
        handled = constructStarClosure(pairOut);
        if (handled == false) {
            handled = constructPlusClosure(pairOut);
        }

        if (handled == false) {
            handled = constructOptionsClosure(pairOut);
        }


    }

    public boolean constructStarClosure(NfaPair pairOut) throws Exception {
        /*
         * term*
         */
        Nfa start, end;
        term(pairOut);

        if (lexer.MatchToken(Lexer.Token.CLOSURE) == false) {
            return false;
        }

        start = nfaManager.newNfa();
        end = nfaManager.newNfa();

        start.next = pairOut.startNode;
        pairOut.endNode.next = pairOut.startNode;

        start.next2 = end;
        pairOut.endNode.next2 = end;

        pairOut.startNode = start;
        pairOut.endNode = end;

        lexer.advance();

        return true;
    }

    public boolean constructPlusClosure(NfaPair pairOut) throws Exception {
        /*
         * term+
         */
        Nfa start, end;
        term(pairOut);

        if (lexer.MatchToken(Lexer.Token.PLUS_CLOSE) == false) {
            return false;
        }

        start = nfaManager.newNfa();
        end = nfaManager.newNfa();

        start.next = pairOut.startNode;
        pairOut.endNode.next2 = end;
        pairOut.endNode.next = pairOut.startNode;


        pairOut.startNode = start;
        pairOut.endNode = end;

        lexer.advance();
        return true;
    }

    public boolean constructOptionsClosure(NfaPair pairOut) throws Exception {
        /*
         * term?
         */
        Nfa start, end;
        term(pairOut);

        if (lexer.MatchToken(Lexer.Token.OPTIONAL) == false) {
            return false;
        }

        start = nfaManager.newNfa();
        end = nfaManager.newNfa();

        start.next = pairOut.startNode;
        pairOut.endNode.next = end;

        start.next2 = end;

        pairOut.startNode = start;
        pairOut.endNode = end;

        lexer.advance();

        return true;
    }

    public void term(NfaPair pairOut) throws Exception {
        /*
         * term ->  character | [...] | [^...] | [character-charcter] | .
         *
         */

        boolean handled = constructNfaForSingleCharacter(pairOut);
        if (handled == false) {
            handled = constructNfaForDot(pairOut);
        }

        if (handled == false) {
            constructNfaForCharacterSet(pairOut);
        }
    }


    public boolean constructNfaForSingleCharacter(NfaPair pairOut) throws Exception {
        if (lexer.MatchToken(Lexer.Token.L) == false) {
            return false;
        }

        Nfa start = null;
        start = pairOut.startNode = nfaManager.newNfa();
        pairOut.endNode = pairOut.startNode.next = nfaManager.newNfa();

        start.setEdge(lexer.getLexeme());

        lexer.advance();

        return true;
    }

    public boolean constructNfaForDot(NfaPair pairOut) throws Exception {
        if (lexer.MatchToken(Lexer.Token.ANY) == false) {
            return false;
        }

        Nfa start = null;
        start = pairOut.startNode = nfaManager.newNfa();
        pairOut.endNode = pairOut.startNode.next = nfaManager.newNfa();

        start.setEdge(Nfa.CCL);
        start.addToSet((byte) '\n');
        start.addToSet((byte) '\r');
        start.setComplement(); // 取反操作  通配符，需要取反

        lexer.advance();

        return true;
    }

    public boolean constructNfaForCharacterSetWithoutNegative(NfaPair pairOut) throws Exception {

        if (lexer.MatchToken(Lexer.Token.CCL_START) == false) {
            return false;
        }

        lexer.advance();

        Nfa start = null;
        start = pairOut.startNode = nfaManager.newNfa();
        pairOut.endNode = pairOut.startNode.next = nfaManager.newNfa();
        start.setEdge(Nfa.CCL);

        if (lexer.MatchToken(Lexer.Token.CCL_END) == false) {
            dodash(start.inputSet);
        }

        if (lexer.MatchToken(Lexer.Token.CCL_END) == false) {
            ErrorHandler.parseErr(ErrorHandler.Error.E_BADEXPR);
        }
        lexer.advance();

        return true;
    }

    public boolean constructNfaForCharacterSet(NfaPair pairOut) throws Exception {
        if (lexer.MatchToken(Lexer.Token.CCL_START) == false) {
            return false;
        }

        lexer.advance();
        boolean negative = lexer.MatchToken(Lexer.Token.AT_BOL);

        Nfa start = null;
        start = pairOut.startNode = nfaManager.newNfa();
        pairOut.endNode = pairOut.startNode.next = nfaManager.newNfa();
        start.setEdge(Nfa.CCL);

        if (lexer.MatchToken(Lexer.Token.CCL_END) == false) {
            dodash(start.inputSet);
        }

        if (lexer.MatchToken(Lexer.Token.CCL_END) == false) {
            ErrorHandler.parseErr(ErrorHandler.Error.E_BADEXPR);
        }

        if (negative) {
            start.setComplement();
        }

        lexer.advance();

        return true;
    }

    private void dodash(Set<Byte> set) {
        int first = 0;

        while (lexer.MatchToken(Lexer.Token.EOS) == false &&
                lexer.MatchToken(Lexer.Token.CCL_END) == false) {

            if (lexer.MatchToken(Lexer.Token.DASH) == false) {
                first = lexer.getLexeme();
                set.add((byte) first);
            } else {
                lexer.advance(); //越过 -
                for (; first <= lexer.getLexeme(); first++) {
                    set.add((byte) first);
                }
            }

            lexer.advance();
        }


    }
}

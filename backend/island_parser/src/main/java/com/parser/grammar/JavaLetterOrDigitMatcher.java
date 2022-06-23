package com.parser.grammar;

public class JavaLetterOrDigitMatcher extends AbstractJavaCharacterMatcher {

    public JavaLetterOrDigitMatcher() {
        super("LetterOrDigit");
    }

    @Override
    protected boolean acceptChar(char c) {
        return Character.isJavaIdentifierPart(c);
    }
}

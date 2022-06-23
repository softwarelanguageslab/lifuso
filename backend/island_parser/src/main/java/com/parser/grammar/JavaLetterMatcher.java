package com.parser.grammar;

public class JavaLetterMatcher extends AbstractJavaCharacterMatcher {

    public JavaLetterMatcher() {
        super("Letter");
    }

    @Override
    protected boolean acceptChar(char c) {
        return Character.isJavaIdentifierStart(c);
    }
}

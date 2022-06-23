package com.parser;

import com.parser.extractor.Extractor;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class Main {
    public static void main(String[] args) throws IOException {
        System.out.println("Parsing code ...");

        String code = fileToString("snippets/snippet4.java");
        Extractor extractor = new Extractor(code);

        ArrayList<String> methodInvocations = extractor.extractMethodCalls();

        // Check phase
        for (String matchedMethod : methodInvocations) {
            System.out.println(matchedMethod);
        }

        System.out.println("Done!");
    }

    public static String fileToString(String filePath) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(filePath), Charset.defaultCharset());

        StringBuilder text = new StringBuilder();
        for (String line : lines) {
            text.append(line).append("\n");
        }
        return text.toString();
    }
}

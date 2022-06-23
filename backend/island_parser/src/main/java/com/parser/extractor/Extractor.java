package com.parser.extractor;

import com.parser.grammar.CompleteCodeSnippetParser;
import org.parboiled.Parboiled;
import org.parboiled.parserunners.AbstractParseRunner;
import org.parboiled.parserunners.ReportingParseRunner;
import org.parboiled.support.ParsingResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Extractor {
    String code;

    public Extractor(String code) {
        this.code = code;
    }

    public ArrayList<String> extractMethodCalls() {
        CompleteCodeSnippetParser parser = Parboiled.createParser(CompleteCodeSnippetParser.class);
        AbstractParseRunner<?> runnerVariable = new ReportingParseRunner(parser.InputLine());

        try {
            ParsingResult<?> result = runnerVariable.run(code);

            ArrayList<String> methodCalls = new ArrayList<>();
            HashMap<String, String> variables = new HashMap<>();

            // Extraction phase
            if (result.matched) {
                if (!result.valueStack.isEmpty()) {
                    for (Object o : result.valueStack) {
                        ArrayList<Object[]> stackValues = (ArrayList<Object[]>) o;
                        ArrayList<String> keys = new ArrayList<>();
                        ArrayList<String> values = new ArrayList<>();

                        for (Object[] array : stackValues) {
                            keys.add(array[0].toString());
                            values.add(array[1].toString());
                        }

                        if (keys.get(0).equals("RECEIVER_NAME") && keys.contains("DOT")) {
                            ArrayList<String> selectedValues = new ArrayList<>();
                            String receiverName = values.get(0);

                            if (receiverName.contains("<")) {
                                int indexOpenPoint = receiverName.indexOf('<');
                                receiverName = receiverName.substring(0, indexOpenPoint);
                            }

                            selectedValues.add(receiverName);

                            for (int i = 1; i < keys.size(); i++) {
                                if (keys.get(i).equals("METHOD_NAME")) {
                                    selectedValues.add(values.get(i));
                                }
                            }

                            methodCalls.add(String.join(".", selectedValues));
                        } else {
                            if (values.size() > 2) {
                                String variableType = values.get(2).trim();

                                if (variableType.contains("<")) {
                                    int indexOpenPoint = variableType.indexOf('<');
                                    variableType = variableType.substring(0, indexOpenPoint);
                                }

                                variables.put(variableType, values.get(1).trim());
                            } else if (values.size() == 2) {
                                String variableType = values.get(0).trim();

                                if (variableType.contains("<")) {
                                    int indexOpenPoint = variableType.indexOf('<');
                                    variableType = variableType.substring(0, indexOpenPoint);
                                }

                                variables.put(variableType, values.get(1).trim());
                            }
                        }
                    }
                }
            } else {
                System.out.println("Not matched");
            }
            // Match phase
            ArrayList<String> matchedMethodInvocations = new ArrayList<>();
            for (String methodCall : methodCalls) {
                String[] methodCallDivided = methodCall.split("\\.");

                String receiverName = receiverVariableNameIndex(methodCallDivided[0], variables);
                StringBuilder newMethod = new StringBuilder();
                newMethod.append(receiverName);

                for (int i = 1; i < methodCallDivided.length; i++) {
                    newMethod.append(".").append(methodCallDivided[i]);
                }

                matchedMethodInvocations.add(newMethod.toString());
            }

            return matchedMethodInvocations;
        } catch (Exception ignored) {
            return new ArrayList<>();
        }
    }

    /** Matches the receiver names with variable names.
     *
     * @param receiver The name of the receiver
     * @param variables A map containing types and names of variables
     * @return In case of a possible match, returns the type of the matching; otherwise, returns the receiver argument
     */
    private String receiverVariableNameIndex(String receiver, HashMap<String, String> variables) {
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            if (receiver.equals(entry.getValue())) {
                return entry.getKey();
            }
        }

        return receiver;
    }
}

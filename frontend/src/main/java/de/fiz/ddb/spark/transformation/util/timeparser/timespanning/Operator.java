package de.fiz.ddb.spark.transformation.util.timeparser.timespanning;

class Operator {

    private final String parsedInputString;
    private final OperatorType type;

    enum OperatorType {
        OR, BETWEEN
    }

    public Operator(String parsedInputString, OperatorType type) {
        this.parsedInputString = parsedInputString;
        this.type = type;
    }

    public String getParsedInputString() {
        return this.parsedInputString;
    }

    public OperatorType getType() {
        return this.type;
    }
}

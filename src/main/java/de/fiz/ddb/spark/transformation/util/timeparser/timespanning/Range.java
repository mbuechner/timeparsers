package de.fiz.ddb.spark.transformation.util.timeparser.timespanning;

class Range {

    private final String parsedInputString;
    private final RangeType type;

    enum RangeType {
        FROM, BEFORE, UNTIL, AFTER, AROUND, PRESUMABLY
    }

    public Range(String parsedInputString, RangeType type) {
        this.parsedInputString = parsedInputString;
        this.type = type;
    }

    public String getParsedInputString() {
        return this.parsedInputString;
    }

    public RangeType getType() {
        return this.type;
    }
}

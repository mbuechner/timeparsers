package de.fiz.ddb.spark.transformation.util.timeparser.timespanning;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tries to read desired strings from an input string. The input string is a normalized string given to {@link TimeSpanParser}. The {@code tryTo*()} methods are
 * used to attempt to read certain strings ({@code char}s, {@link String}s or {@link Pattern}s) from a given position within the input string. The success of
 * each attempt is indicated by an {@link AcceptResult}. If the attempt is successful, the position within the input string is immediately moved to the position
 * after the string just read.
 */
class InputStringReader {

    private final String inputString;

    public InputStringReader(String inputString) {
        this.inputString = inputString;
    }

    public AcceptResult tryToAccept(Position p, char c) {
        boolean isAccepted = p.get() < this.inputString.length() && this.inputString.charAt(p.get()) == c;
        String parsedInputString = null;
        if ( isAccepted ) {
            parsedInputString = String.valueOf(c);
            p.move(parsedInputString.length());
        }

        return new AcceptResult(isAccepted, parsedInputString, null);
    }

    public AcceptResult tryToAccept(Position p, String string) {
        int startIndex = p.get();
        int endIndex = p.get() + string.length();
        boolean isAccepted = endIndex <= this.inputString.length() && this.inputString.substring(startIndex, endIndex).equals(string);
        String parsedInputString = null;
        if ( isAccepted ) {
            parsedInputString = string;
            p.move(parsedInputString.length());
        }

        return new AcceptResult(isAccepted, parsedInputString, null);
    }

    public AcceptResult tryToAccept(Position p, Pattern pattern) {
        Matcher m = pattern.matcher(this.inputString);
        m.region(p.get(), this.inputString.length());
        boolean isAccepted = m.lookingAt();
        String parsedInputString = null;
        if ( isAccepted ) {
            parsedInputString = m.group(0);
            p.move(parsedInputString.length());
        }

        return new AcceptResult(isAccepted, parsedInputString, m);
    }

    @Override
    public String toString() {
        return this.inputString;
    }

}

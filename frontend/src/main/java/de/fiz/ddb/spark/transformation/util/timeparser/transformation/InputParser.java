package de.fiz.ddb.spark.transformation.util.timeparser.transformation;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.fiz.ddb.spark.transformation.util.timeparser.transformation.Token.Type;

/**
 * Parses an input string using a list of {@link Token}s, created from a rule's input specification by
 * {@link PatternParser}, yielding a list of {@link TokenWithValue}s.
 */
public class InputParser
{

    private static final Pattern PATTERN_DIGITS = Pattern.compile("\\d+");
    private static final String LOGEXPR_WITH = "\" with ";

    private final List<Token> patternTokens = new ArrayList<>();
    private final List<Replacement> monthReplacements;
    private final List<Replacement> weekdayReplacements;

    public InputParser(
        List<Token> pattern,
        List<Replacement> monthReplacements,
        List<Replacement> weekdayReplacements) {
        this.patternTokens.addAll(pattern);
        this.monthReplacements = monthReplacements;
        this.weekdayReplacements = weekdayReplacements;
    }

    public List<TokenWithValue> parseInputString(String inputString) throws TransformationException {
        List<TokenWithValue> tokens = new ArrayList<>();

        for ( Replacement replacement : this.monthReplacements ) {
            inputString = inputString.replace(replacement.from, replacement.to);
        }

        for ( Replacement replacement : this.weekdayReplacements ) {
            inputString = inputString.replace(replacement.from, replacement.to);
        }

        int currentInputStringPosition = 0;
        for ( Token patternToken : this.patternTokens ) {
            int patternValueLength = patternToken.getPatternValue().length();
            if ( currentInputStringPosition + patternValueLength > inputString.length() ) {
                throw new TransformationException("Input string too short; cannot parse \""
                    + inputString
                    + LOGEXPR_WITH
                    + this.patternTokens.toString());
            }
            String tokenInputValue =
                inputString.substring(currentInputStringPosition, currentInputStringPosition
                    + patternValueLength);

            if ( patternToken.getType() == Type.TEXT
                && !patternToken.getPatternValue().equals(tokenInputValue) ) {
                throw new TransformationException(
                    "Input string's text does not match pattern's text at position "
                        + currentInputStringPosition
                        + "; cannot parse \""
                        + inputString
                        + LOGEXPR_WITH
                        + this.patternTokens.toString());
            } else if ( patternToken.getType() == Type.GENERIC_VARIABLE ) {
                Matcher matcher = PATTERN_DIGITS.matcher(tokenInputValue);
                if ( !matcher.lookingAt() ) {
                    throw new TransformationException("Number expected in input string at position "
                        + currentInputStringPosition
                        + "; cannot parse \""
                        + inputString
                        + LOGEXPR_WITH
                        + this.patternTokens.toString());
                }
            }

            TokenWithValue token =
                new TokenWithValue(patternToken.getType(), patternToken.getPatternValue(), tokenInputValue);
            tokens.add(token);
            currentInputStringPosition += patternValueLength;
        }
        if ( currentInputStringPosition < inputString.length() ) {
            throw new TransformationException("Input string too long; cannot parse \""
                + inputString
                + LOGEXPR_WITH
                + this.patternTokens.toString());
        }

        return tokens;
    }
}

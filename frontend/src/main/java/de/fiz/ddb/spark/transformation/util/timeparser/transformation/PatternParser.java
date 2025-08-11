package de.fiz.ddb.spark.transformation.util.timeparser.transformation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Parses a rule's input or output specification, yielding a list of {@link Token}s.
 */
public class PatternParser {

    public List<Token> parse(String mask, String pattern) throws TransformationException {
        return this.parse(false, mask, pattern);
    }

    public List<Token> parse(boolean allowDuplicateVariableNames, String mask, String pattern) throws TransformationException {
        if ( mask.length() != pattern.length() ) {
            throw new TransformationException("Mask and pattern length not equal");
        }

        List<Token> tokens = new ArrayList<>();

        for ( int i = 0; i < mask.length(); i++ ) {
            Token token = readGenericVariableToken(mask, pattern, i);
            if ( token == null ) {
                token = readMonthReplacementToken(mask, pattern, i);
            }
            if ( token == null ) {
                token = readWeekdayReplacementToken(mask, pattern, i);
            }
            if ( token == null ) {
                token = readTextToken(mask, pattern, i);
            }

            if (token != null) {
                i += token.getPatternValue().length() - 1;
                tokens.add(token);
            }

        }

        if ( !allowDuplicateVariableNames ) {
            removeDuplicates(tokens, pattern);
        }

        return tokens;
    }
    
    private void removeDuplicates(List<Token> tokens, String pattern) throws TransformationException {
        Set<Character> variableNamesInitials = new HashSet<>();
        for ( Token token : tokens ) {
            if ( token.getType() == Token.Type.GENERIC_VARIABLE
                || token.getType() == Token.Type.MONTH_REPLACEMENT_VARIABLE
                || token.getType() == Token.Type.WEEKDAY_REPLACEMENT_VARIABLE ) {
                Character currentInitial = token.getPatternValue().charAt(0);
                if ( variableNamesInitials.contains(currentInitial) ) {
                    throw new TransformationException("Duplicate variable names for " + currentInitial + " in pattern \"" + pattern + "\"");
                } else {
                    variableNamesInitials.add(currentInitial);
                }
            }
        }
    }

    private Token readGenericVariableToken(String mask, String pattern, int start) {
        Token returnToken = null;
        StringBuilder patternValue = getPatternValue(mask, pattern, start, '#');
        if ( patternValue.length() > 0 ) {
            returnToken = new Token(Token.Type.GENERIC_VARIABLE, patternValue.toString());
        }

        return returnToken;
    }

    private Token readMonthReplacementToken(String mask, String pattern, int start) {
        Token returnToken = null;
        StringBuilder patternValue = getPatternValue(mask, pattern, start, 'M');
        if ( patternValue.length() > 0 ) {
            if ( patternValue.length() == 2 ) {
                returnToken = new Token(Token.Type.MONTH_REPLACEMENT_VARIABLE, patternValue.toString());
            } else if ( patternValue.length() >= 2 ) {
                returnToken = new Token(Token.Type.MONTH_REPLACEMENT_VARIABLE, patternValue.substring(0, 2));
            }
        }

        return returnToken;
    }

    private Token readWeekdayReplacementToken(String mask, String pattern, int start) {
        Token returnToken = null;
        StringBuilder patternValue = getPatternValue(mask, pattern, start, 'G');
        if ( patternValue.length() > 0 ) {
            if ( patternValue.length() == 2 ) {
                returnToken = new Token(Token.Type.WEEKDAY_REPLACEMENT_VARIABLE, patternValue.toString());
            } else if ( patternValue.length() >= 2 ) {
                returnToken = new Token(Token.Type.WEEKDAY_REPLACEMENT_VARIABLE, patternValue.substring(0, 2));
            }
        }

        return returnToken;
    }

    private Token readTextToken(String mask, String pattern, int start) throws TransformationException {
        Token returnToken = null;

        StringBuilder patternValue = new StringBuilder(pattern.length());
        for ( int i = start; i < mask.length(); i++ ) {
            char currentMaskChar = mask.charAt(i);
            char currentPatternChar = pattern.charAt(i);

            if ( currentMaskChar == '#' ) {
                break;
            }
            if ( i < mask.length() - 1 ) {
                char nextMaskChar = mask.charAt(i + 1);
                if ( currentMaskChar == 'M' && nextMaskChar == 'M' || currentMaskChar == 'G' && nextMaskChar == 'G' ) {
                    break;
                }
            }
            if ( currentMaskChar != currentPatternChar ) {
                throw new TransformationException("Characters do not match at position " + i + " of mask (\"" + mask + "\") and pattern (\"" + pattern + "\")");
            }
            patternValue.append(currentPatternChar);
        }

        if ( patternValue.length() > 0 ) {
            returnToken = new Token(Token.Type.TEXT, patternValue.toString());
        }

        return returnToken;
    }

    private StringBuilder getPatternValue(String mask, String pattern, int start, char maskChar) {
        char firstPatternChar = pattern.charAt(start);
        StringBuilder patternValue = new StringBuilder(pattern.length());
        for ( int i = start; i < mask.length(); i++ ) {
            char currentMaskChar = mask.charAt(i);
            char currentPatternChar = pattern.charAt(i);

            if ( currentMaskChar != maskChar || currentPatternChar != firstPatternChar ) {
                break;
            } else {
                patternValue.append(currentPatternChar);
            }
        }
        return patternValue;
    }
}

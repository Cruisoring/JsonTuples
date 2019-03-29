package JsonTuples;

import org.apache.commons.text.StringEscapeUtils;

import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * http://www.json.org/
 * A string is a sequence of zero or more Unicode characters, wrapped in double quotes, using backslash escapes.
 * A character is represented as a single character string. A string is very much like a C or Java string.
 */
public class JSONString extends JSONValue<String> {
    //Regex to match potential String value wrapped by Quotes
    public static final Pattern JSON_STRING_PATTERN = Pattern.compile("^\\\".*?\\\"$", Pattern.MULTILINE);

    /**
     * Parse given string enclosed by quotes(") as a JSONString instance by un-escape special characters.
     * @param valueString   String to represent a JSON String value.
     * @return              An JSONString instance that keeps the un-escaped string as a Tuple.
     */
    public static JSONString parseString(String valueString) {
        valueString = valueString.trim();
        int length = valueString.length();

        if(QUOTE != valueString.charAt(0) || QUOTE != valueString.charAt(valueString.length()-1)) {
            throw new IllegalArgumentException("The given valueString is not enclosed by quotes:" + valueString);
        }

        String jsonString = valueString.substring(1, length-1);
        String unescaped = _unescapeJson(jsonString);
        return new JSONString(unescaped);
    }

    /**
     * Unescape special characters of JSON to normal JAVA string.
     * @param jsonContext   Encoded JSON String.
     * @return              Un-escaped string of the original JSON context.
     */
    public static String unescapeJson(String jsonContext) {
        checkNotNull(jsonContext);
        return _unescapeJson(jsonContext);
    }

    protected static String _unescapeJson(String jsonContext) {
        return _unescapeJson(jsonContext, 0, jsonContext.length());
    }

    protected static String _unescapeJson(String jsonContext, Range range) {
        return _unescapeJson(jsonContext, range.getStartInclusive(), range.getEndExclusive());
    }

    protected static String _unescapeJson(String jsonContext, int start, int end) {
        StringBuilder sb = new StringBuilder();

        int copyFrom = start;
        int copyTo = -1;
        int unicode = -1;
        for (int i = start; i < end; i++) {
            char current = jsonContext.charAt(i);
            if(current != BACK_SLASH) {
                continue;
            }

            copyTo = i;
            if(copyTo > copyFrom) {
                sb.append(jsonContext.substring(copyFrom, copyTo));
            }

            //Assume the valid BACK_SLASH is not the last char of the range
            char next = jsonContext.charAt(i++);
            switch (next){
                case '"':
                case '\\':
                case '/':
                    sb.append(next);
                    break;
                case 'b':
                    sb.append('\b');
                    break;
                case 'n':
                    sb.append('\n');
                    break;
                case 't':
                    sb.append('\t');
                    break;
                case 'r':
                    sb.append('\r');
                    break;
                case 'u':
                    int charCode = 0;
                    for (int j = 1; j < 5; j++) {
                        charCode *= 16;
                        switch (jsonContext.charAt(j)){
                            case '0': break;
                            case '1': charCode+=1; break;
                            case '2': charCode+=2; break;
                            case '3': charCode+=3; break;
                            case '4': charCode+=4; break;
                            case '5': charCode+=5; break;
                            case '6': charCode+=6; break;
                            case '7': charCode+=7; break;
                            case '8': charCode+=8; break;
                            case '9': charCode+=9; break;
                            case 'a': charCode+=10; break;
                            case 'b': charCode+=11; break;
                            case 'c': charCode+=12; break;
                            case 'd': charCode+=13; break;
                            case 'e': charCode+=14; break;
                            case 'f': charCode+=15; break;
                            default: throw new IllegalArgumentException("Illegal escaped unicode char: " + jsonContext.substring(next-1, next+4));
                        }
                    }
                    sb.append(Character.toChars(charCode));
                    i+=4;
                    break;
            }
            copyFrom = i+1;
        }
        if(copyFrom < end){
            sb.append(jsonContext.substring(copyFrom, end));
        }
        return sb.toString();
    }

    protected JSONString(String s) {
        super(checkNotNull(s));
    }

    @Override
    public String toString() {
        if(_toString==null){
            _toString = String.format("\"%s\"", StringEscapeUtils.escapeJson(getFirst()));
        }
        return _toString;
    }
}

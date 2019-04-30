package JsonTuples;

import io.github.cruisoring.Range;
import org.apache.commons.text.StringEscapeUtils;

import java.util.regex.Pattern;

import static io.github.cruisoring.Asserts.checkWithoutNull;

/**
 * A string is a sequence of zero or more Unicode characters, wrapped in double quotes, using backslash escapes.
 * A character is represented as a single character string. A string is very much like a C or Java string.
 * @see <a href="http://www.json.org">http://www.json.org</a>
 */
public class JSONString extends JSONValue<String> {
    //A string is a sequence of Unicode code points wrapped with quotation marks (U+0022). All code points may
    //be placed within the quotation marks except for the code points that must be escaped: quotation mark
    //(U+0022), reverse solidus (U+005C), and the control characters U+0000 to U+001F. There are two-character
    //escape sequence representations of some characters.

    //Regex to match potential String value wrapped by Quotes
    public static final Pattern JSON_STRING_PATTERN = Pattern.compile("^\\\".*?\\\"$", Pattern.MULTILINE);
    //The forbidUnescapedControls would screen out controls that have not been escaped '\r' or '\n'
    public static boolean forbidUnescapedControls = false;
    static Pattern illegalCharsPattern = Pattern.compile("\\r|\\n|\\t|[\b]", Pattern.MULTILINE);

    protected JSONString(String value) {
        super(value);
    }

    /**
     * Parse given string enclosed by quotes(") as a JSONString instance by un-escape special characters.
     *
     * @param wrappedValueString String to represent a JSON String value.
     * @return An JSONString instance that keeps the un-escaped string as a Tuple.
     */
    public static JSONString parseString(String wrappedValueString) {

        wrappedValueString = forbidUnescapedControls ? JSONString.trimJSONString(wrappedValueString) : wrappedValueString.trim();
        int length = wrappedValueString.length();

        if (QUOTE != wrappedValueString.charAt(0) || QUOTE != wrappedValueString.charAt(wrappedValueString.length() - 1)) {
            throw new IllegalArgumentException("The given valueString is not enclosed by quotes:" + wrappedValueString);
        }

        String unescaped = StringEscapeUtils.unescapeJson(wrappedValueString.substring(1, length - 1));
        return new JSONString(unescaped);
    }

    //
    protected static JSONString _parseString(String valueStringOnly) {
        String unescaped = StringEscapeUtils.unescapeJson(forbidUnescapedControls ?
                illegalCharsPattern.matcher(valueStringOnly).replaceAll("") : valueStringOnly);
        return new JSONString(unescaped);
    }

    /**
     * Trim the leading and ending spaces and replace '\b', '\n', '\r', and '\t' with "".
     *
     * @param rawString JSON String including leading and ending Quotes to be trimmed.
     * @return Trimmed String also get illegal chars removed.
     */
    public static String trimJSONString(String rawString) {
        String trimmed = illegalCharsPattern.matcher(rawString.trim()).replaceAll(""); //get r of all illegal chars
        return trimmed;
    }

    /**
     * Unescape special characters of JSON to normal JAVA string.
     *
     * @param jsonContext Encoded JSON String.
     * @return Un-escaped string of the original JSON context.
     */
    public static String unescapeJson(String jsonContext) {
        checkWithoutNull(jsonContext);
        return _unescapeJson(jsonContext);
    }

    protected static String _unescapeJson(String jsonContext) {
        return _unescapeJson(jsonContext, 0, jsonContext.length());
    }

    protected static String _unescapeJson(String jsonContext, Range range) {
        return _unescapeJson(jsonContext, range.getStartInclusive(), range.getEndExclusive());
    }

    protected static String _unescapeJson(CharSequence jsonContext, int start, int end) {
        StringBuilder sb = new StringBuilder();

        int copyFrom = start;
        int copyTo = -1;
        int unicode = -1;
        for (int i = start; i < end; i++) {
            char current = jsonContext.charAt(i);
            if (current != BACK_SLASH) {
                continue;
            }

            copyTo = i;
            if (copyTo > copyFrom) {
                sb.append(jsonContext.subSequence(copyFrom, copyTo));
            }

            //Assume the valid BACK_SLASH is not the last char of the range
            char next = jsonContext.charAt(++i);
            switch (next) {
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
                        switch (jsonContext.charAt(j)) {
                            case '0':
                                break;
                            case '1':
                                charCode += 1;
                                break;
                            case '2':
                                charCode += 2;
                                break;
                            case '3':
                                charCode += 3;
                                break;
                            case '4':
                                charCode += 4;
                                break;
                            case '5':
                                charCode += 5;
                                break;
                            case '6':
                                charCode += 6;
                                break;
                            case '7':
                                charCode += 7;
                                break;
                            case '8':
                                charCode += 8;
                                break;
                            case '9':
                                charCode += 9;
                                break;
                            case 'a':
                                charCode += 10;
                                break;
                            case 'b':
                                charCode += 11;
                                break;
                            case 'c':
                                charCode += 12;
                                break;
                            case 'd':
                                charCode += 13;
                                break;
                            case 'e':
                                charCode += 14;
                                break;
                            case 'f':
                                charCode += 15;
                                break;
                            default:
                                throw new IllegalArgumentException("Illegal escaped unicode char: " + jsonContext.subSequence(next - 1, next + 4));
                        }
                    }
                    sb.append(Character.toChars(charCode));
                    i += 4;
                    break;
            }
            copyFrom = i + 1;
        }
        if (copyFrom < end) {
            sb.append(jsonContext.subSequence(copyFrom, end));
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        if (_toString == null) {
            _toString = String.format("\"%s\"", StringEscapeUtils.escapeJson(getFirst()));
        }
        return _toString;
    }
}

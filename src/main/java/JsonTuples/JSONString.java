package JsonTuples;

import org.apache.commons.text.StringEscapeUtils;

import java.util.regex.Pattern;

import static io.github.cruisoring.Asserts.fail;

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

    //The forbidUnescapedControls would screen out controls that have not been escaped '\r' or '\n'
    public static boolean forbidUnescapedControls = true;

    //Regex to match potential String value wrapped by Quotes
    public static final Pattern JSON_STRING_PATTERN = Pattern.compile("^\\\".*?\\\"$", Pattern.MULTILINE);

    static Pattern illegalCharsPattern = Pattern.compile("\\r|\\n|\\t|[\b]", Pattern.MULTILINE);

    protected JSONString(String value) {
        super(value);
    }

    /**
     * Parse given string enclosed by quotes(") as a JSONString instance by un-escape special characters.
     *
     * @param valueString String to represent a JSON String value.
     * @return An JSONString instance that keeps the un-escaped string as a Tuple.
     */
    public static JSONString parseString(String valueString) {
        String wrappedValueString = forbidUnescapedControls ? JSONString.trimJSONString(valueString) : valueString.trim();
        int length = wrappedValueString.length();

        if(length < 2 || QUOTE != wrappedValueString.charAt(0) || QUOTE != wrappedValueString.charAt(length - 1)) {
            fail("the valueString \"%s\" is not enclosed by quotation marks.", valueString);
        }

        String unescaped = StringEscapeUtils.unescapeJson(wrappedValueString.substring(1, length - 1));
        return new JSONString(unescaped);
    }

    /**
     * Trim the leading and ending spaces and replace '\b', '\n', '\r', and '\t' with "".
     *
     * @param rawString JSON String including leading and ending Quotes to be trimmed.
     * @return Trimmed String also get illegal chars removed.
     */
    public static String trimJSONString(String rawString) {
        String trimmed = illegalCharsPattern.matcher(rawString.trim()).replaceAll(""); //get trid of all illegal chars
        return trimmed;
    }

    @Override
    public String toString() {
        if (_toString == null) {
            String content = getFirst();
            _toString = content==null ? JSON_NULL : String.format("\"%s\"", StringEscapeUtils.escapeJson(content));
        }
        return _toString;
    }
}

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

    public static JSONString parseString(String valueString) {
        valueString = valueString.trim();
        int length = valueString.length();

        if(QUOTE != valueString.charAt(0) || QUOTE != valueString.charAt(valueString.length()-1)) {
            throw new IllegalArgumentException("The given valueString is not enclosed by quotes:" + valueString);
        }

        String jsonString = valueString.substring(1, length-1);
        String unescaped = StringEscapeUtils.unescapeJson(jsonString);
        return new JSONString(unescaped);
    }

    protected static String unescapeJson(String jsonContext, Range range) {
        return unescapeJson(jsonContext, range.getStartInclusive(), range.getEndExclusive());
    }

    protected static String unescapeJson(String jsonContext, int start, int end) {
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
                case 'b':
                    sb.append(next);
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
                    int charCode = Integer.parseInt(jsonContext.substring(i+1, i+5));
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
    public String toJSONString(String indent) {
        String string = getFirst();

        return "\"" + StringEscapeUtils.escapeJson(string) + "\"";
    }

    @Override
    public String toString() {
        return String.format("\"%s\"", getFirst());
    }
}

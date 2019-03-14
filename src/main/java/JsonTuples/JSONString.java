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

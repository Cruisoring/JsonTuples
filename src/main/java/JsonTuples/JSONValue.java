package JsonTuples;

import io.github.cruisoring.tuple.Tuple1;

import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <a href="http://www.json.org">http://www.json.org</a>
 * <br>
 * Represent the value of JSON strings of following types:
 * <ul>
 *     <li>true</li>
 *     <li>false</li>
 *     <li>null</li>
 *     <li>string</li>
 *     <li>number</li>
 * </ul>
 * @param <T>   JAVA type used to represent the type of the JSON value strings.
 */
public class JSONValue<T> extends Tuple1<T> implements IJSONValue {

    //Regex to match legal values of Boolean, null, String or Number with optional leading/ending spaces
    public static final Pattern BASIC_VALUE_PATTERN = Pattern.compile("\\s*(true|false|null|\\\".*?\\\"|-?(?:0|[1-9]\\d*)(?:\\.\\d+)?(?:[eE][+-]?\\d+)?)\\s*");

    // Represent the values of either 'true'
    public static final JSONValue True = new JSONValue(Boolean.TRUE);

    //Represent the values of either 'false'
    public static final JSONValue False = new JSONValue(Boolean.FALSE);

    //Represent the values of either 'null'
    public static final JSONValue Null = new JSONValue(null);

    public static IJSONValue parse(String jsonContext, Range range) {
        checkNotNull(jsonContext);
        checkNotNull(range);

        String valueString = jsonContext.substring(range.getStartInclusive(), range.getEndExclusive()).trim();
        return parse(valueString);
    }

    public static IJSONValue parse(String valueString) {
        final String trimmed = valueString.trim();
        switch (trimmed) {
            case JSON_TRUE:
                return JSONValue.True;
            case JSON_FALSE:
                return JSONValue.False;
            case JSON_NULL:
                return JSONValue.Null;
            default:
                //Switch based on the first character, leave the corresponding methods to validate and parse
                switch(trimmed.charAt(0)) {
                    case QUOTE:
                        return JSONString.parseString(trimmed);
                    case LEFT_BRACE:
                        return JSONObject.parse(trimmed);
                    case LEFT_BRACKET:
                        return JSONArray.parseArray(trimmed);
                    default:
                        //The valueString can only stand for a number or get Exception thrown there
                        return JSONNumber.parseNumber(trimmed);
                }
        }
    }

    protected JSONValue(T t) {
        super(t);
    }

    @Override
    public Object getObject() {
        return getFirst();
    }

    @Override
    public String toString() {
        Object obj = getFirst();
        return (obj == null) ? JSON_NULL : obj.toString();
    }

    @Override
    public String toJSONString(String indent) {
        return toString();
    }
}

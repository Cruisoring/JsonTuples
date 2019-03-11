package JsonTuples;

import io.github.cruisoring.tuple.Tuple1;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * <a href="http://www.json.org">http://www.json.org</a>
 * <br>
 * Represent the value of JSON strings of 5 basic values, that are:
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
    public static final String JSON_NULL = "null";
    public static final String JSON_TRUE = "true";
    public static final String JSON_FALSE = "false";

    public static final String JSON_STRING_INDICATOR = "\"";
    public static final String JSON_OBJECT_INDICATOR = "{";
    public static final String JSON_ARRAY_INDICATOR = "[";

    //Regex to match potential String value wrapped by Quotes
    public static final Pattern JSON_STRING_PATTERN = Pattern.compile("^\\\".*?\\\"$", Pattern.MULTILINE);

    //Regex to match legal values of Boolean, null, String or Number with optional leading/ending spaces
    public static final Pattern BASIC_VALUE_PATTERN = Pattern.compile("\\s*(true|false|null|\\\".*?\\\"|-?(?:0|[1-9]\\d*)(?:\\.\\d+)?(?:[eE][+-]?\\d+)?)\\s*");

    // Represent the values of either 'true'
    public static final JSONValue True = new JSONValue(Boolean.TRUE);

    //Represent the values of either 'false'
    public static final JSONValue False = new JSONValue(Boolean.FALSE);

    //Represent the values of either 'null'
    public static final JSONValue Null = new JSONValue(null);

    public static IJSONValue parse(String jsonContext, IntRange range) {
        checkNotNull(jsonContext);
        checkNotNull(range);

        String valueString = jsonContext.substring(range.getStartInclusive(), range.getEndExclusive()).trim();
        return parse(valueString);
    }

    public static IJSONValue parse(String valueString) {
        checkState(StringUtils.isNotBlank(valueString));

        valueString = valueString.trim();

        switch (valueString) {
            case JSON_TRUE:
                return True;
            case JSON_FALSE:
                return False;
            case JSON_NULL:
                return Null;
            default:
                //Switch based on the first character, leave the corresponding methods to validate and parse
                switch(valueString.substring(0, 1)) {
                    case JSON_STRING_INDICATOR:
                        return JSONString.parseString(valueString);
                    case JSON_OBJECT_INDICATOR:
                        return JSONObject.parse(valueString);
                    case JSON_ARRAY_INDICATOR:
                        return JSONArray.parseArray(valueString);
                    default:
                        //The valueString can only stand for a number or get Exception thrown there
                        return JSONNumber.parseNumber(valueString);
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
    public String toJSONString(int indentFactor) {
        Object obj = getFirst();
        if(obj == null)
            return JSON_NULL;

        return getObject().toString();
    }
}

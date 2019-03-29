package JsonTuples;

import io.github.cruisoring.tuple.Tuple1;

import java.util.Objects;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <a href="http://www.json.org">http://www.json.org</a>
 * <br>
 * Represent the value of JSON strings of following types:
 * <ul>
 * <li>true</li>
 * <li>false</li>
 * <li>null</li>
 * <li>string</li>
 * <li>number</li>
 * </ul>
 *
 * @param <T> JAVA type used to represent the type of the JSON value strings.
 */
public class JSONValue<T> extends Tuple1<T> implements IJSONValue {

    //region Static constants
    //Regex to match legal values of Boolean, null, String or Number with optional leading/ending spaces
    public static final Pattern BASIC_VALUE_PATTERN = Pattern.compile("\\s*(true|false|null|\\\".*?\\\"|-?(?:0|[1-9]\\d*)(?:\\.\\d+)?(?:[eE][+-]?\\d+)?)\\s*");

    // Represent the values of either 'true'
    public static final JSONValue True = new JSONValue(Boolean.TRUE);

    //Represent the values of either 'false'
    public static final JSONValue False = new JSONValue(Boolean.FALSE);

    //Represent the values of either 'null'
    public static final JSONValue Null = new JSONValue(null);
    //endregion

    public static IJSONValue parse(String jsonContext, Range range) {
        checkNotNull(jsonContext);
        checkNotNull(range);

        String valueString = jsonContext.substring(range.getStartInclusive(), range.getEndExclusive()).trim();
        return Parser.asJSONValue(valueString);
    }

    protected JSONValue(T t) {
        super(t);
    }

    @Override
    public Object getObject() {
        return getFirst();
    }

    @Override
    public String toJSONString(String indent) {
        return toString();
    }

    @Override
    public String toString() {
        if (_toString == null) {
            T value = getFirst();
            _toString = value == null ? JSON_NULL : value.toString();
        }
        return _toString;
    }

    @Override
    public IJSONValue deltaWith(IJSONValue other) {
        checkNotNull(other);

        if (other instanceof JSONValue && Objects.equals(getObject(), other.getObject())) {
            return JSONArray.EMPTY;
        }
        return new JSONArray(this, other);
    }

}

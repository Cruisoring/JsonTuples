package JsonTuples;

import io.github.cruisoring.Range;
import io.github.cruisoring.tuple.Tuple1;

import java.util.*;
import java.util.regex.Pattern;

import static io.github.cruisoring.Asserts.checkWithoutNull;

/**
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
 * @see <a href="http://www.json.org">http://www.json.org</a>
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

    public static final String SPACE = "  ";
    public static final String NEW_LINE = "\n";
    public static final String COMMA_NEWLINE = COMMA + NEW_LINE;
    public static final String COMMA_NEWLINE_SPACE = COMMA + NEW_LINE + SPACE;

    //endregion

    protected JSONValue(T t) {
        super(t);
    }

    /**
     * By assuming the concerned text represent simple JSON values, thus parse it as corresponding {@code JSONValue}
     * @param jsonContext   {@code CharSequence} to be parsed, it denotes the valueString along with the given {@code Range}
     * @param range         {@code Range} to denote the valueString within the given context of {@code CharSequence}
     * @return      parsed {@code JSONValue} of the concerned valueString, could be true, false, null, JSON string or number.
     */
    public static JSONValue parse(CharSequence jsonContext, Range range) {
        checkWithoutNull(jsonContext);
        checkWithoutNull(range);

        final String trimmed = Range.subString(jsonContext, range).trim();
        switch (trimmed) {
            case JSON_TRUE:
                return JSONValue.True;
            case JSON_FALSE:
                return JSONValue.False;
            case JSON_NULL:
                return JSONValue.Null;
            default:
                return trimmed.charAt(0) == QUOTE ? JSONString.parseString(trimmed) : JSONNumber.parseNumber(trimmed);
        }
    }

    @Override
    public Object getObject() {
        return getFirst();
    }

    @Override
    public Object asMutableObject() {
        return getFirst();
    }

    @Override
    public JSONValue getSorted(Comparator comparator) {
        return this;
    }

    @Override
    public Set<Integer> getSignatures() {
        return new HashSet<Integer>(Arrays.asList(hashCode()));
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
    public int hashCode() {
        if (_hashCode == null) {
            _hashCode = toString().hashCode();
        }
        return _hashCode;

    }

    @Override
    public boolean canEqual(Object obj) {
        return obj instanceof JSONValue;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof JSONValue)) {
            return false;
        } else if (obj == this) {
            return true;
        }

        //*///JSONValues must have identical toString() and thus identical hashCode()?
        if (getFirst() == null) {
            return ((JSONValue) obj).getFirst() == null;
        } else {
            return (hashCode() == obj.hashCode() && toString().equals(obj.toString()));
        }
        /*/
        //Fallback if comparing with hashCode() and toString() only get problems
        JSONValue other = (JSONValue) obj;

        if (!other.canEqual(this) || hashCode() != other.hashCode()) {
            return false;
        }

        Object thisValue = getFirst();
        Object otherValue = other.getFirst();
        if (thisValue == otherValue || (thisValue != null && thisValue.equals(otherValue))) {
            return true;
        } else {
            return toString().equals(other.toString());
        }
        //*/
    }

    @Override
    public IJSONValue deltaWith(IJSONValue other, boolean orderMatters) {
        checkWithoutNull(other);

        if (equals(other)) {
            return JSONArray.EMPTY;
        }
        return new JSONArray(this, other);
    }
}

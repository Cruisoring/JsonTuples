package JsonTuples;

import io.github.cruisoring.tuple.Tuple1;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
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
        if(_hashCode == null){
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
        if(getFirst()==null){
            return ((JSONValue)obj).getFirst()==null;
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
    public IJSONValue deltaWith(IJSONValue other, Comparator<String> comparator) {
        checkNotNull(other);

        if (equals(other)) {
            return JSONArray.EMPTY;
        }
        return new JSONArray(this, other);
    }
}

package jsonTuples;

import io.github.cruisoring.TypeHelper;
import io.github.cruisoring.throwables.FunctionThrowable;
import io.github.cruisoring.tuple.Tuple2;

import java.util.*;

import static io.github.cruisoring.Asserts.assertAllNotNull;
import static io.github.cruisoring.Asserts.assertAllTrue;

/**
 * Utility methods to convert JAVA Objects to/from {@code ISONValue}s, or compare any two JAVA objects.
 */
public class Utilities {

    private Utilities(){}

    public static final Map<Class, Tuple2<FunctionThrowable<Object, IJSONValue>, FunctionThrowable<IJSONValue, Object>>> classConverters =
            new HashMap<>();

    /**
     * Convert any Object to an IJSONValue instance.
     *
     * @param object Object to be converted.
     * @return IJSONValue instance to represent the object to be converted.
     */
    public static IJSONValue jsonify(Object object) {
        if (object == null) {
            return JSONValue.Null;
        } else if (object instanceof IJSONValue) {
            return (IJSONValue) object;
        } else if (Objects.equals(object, true)) {
            return JSONValue.True;
        } else if (Objects.equals(object, false)) {
            return JSONValue.False;
        } else if (object instanceof String) {
            return asJSONString(object);
        } else if (object instanceof Number) {
            return asJSONNumber(object);
        } else if (object instanceof Map) {
            return asJSONObject(object);
        } else if (object instanceof Collection) {
            return asJSONArrayFromCollection(object);
        } else if (object.getClass().isArray()) {
            return asJSONArrayFromArray(object);
        }

        Class clazz = object.getClass();
        if (classConverters.containsKey(clazz)) {
            return classConverters.get(clazz).getFirst().orElse(null).apply(object);
        }

        return asJSONStringFromOthers(object);
    }

    //region default Converters of Object to IJSONValue

    /**
     * Convert the String object to JSONString.
     *
     * @param object String instance that usually shall not be null.
     * @return JSONString to represent the String object to be converted.
     */
    protected static JSONString asJSONString(Object object) {
        return new JSONString((String) object);
    }

    /**
     * Convert the Number object to JSONNumber.
     *
     * @param object Number instance that usually shall not be null.
     * @return JSONNumber to represent the Number object to be converted.
     */
    protected static JSONNumber asJSONNumber(Object object) {
        return new JSONNumber((Number) object);
    }

    /**
     * Convert a Map object to JSONObject with no nameComparator.
     *
     * @param object Map instance that usually shall not be null.
     * @return JSONObject to represent the Map object to be converted.
     */
    protected static JSONObject asJSONObject(Object object) {
        return asJSONObject(null, object);
    }

    /**
     * Convert a Map object to JSONObject with a given nameComparator.
     *
     * @param comparator    the {@code Comparator<String>} used to determine the orders of the sorted NamedValues.
     * @param object Map instance that usually shall not be null.
     * @return JSONObject to represent the Map object to be converted.
     */
    protected static JSONObject asJSONObject(Comparator<String> comparator, Object object) {
        if (object instanceof JSONObject) {
            JSONObject other = (JSONObject) object;
            return other.getSorted(comparator);
        }

        //Let the casting throw exception if the object is not a map of String keys
        Map<Object, Object> map = (Map) object;

        //Assume the entry.getKey().toString() can get unique string keys
        NamedValue[] namedValues = map.entrySet().stream()
                .map(entry -> new NamedValue(entry.getKey().toString(), jsonify(entry.getValue())))
                .toArray(size -> new NamedValue[size]);
        return new JSONObject(comparator, namedValues);
    }

    /**
     * Convert an array object to JSONArray.
     *
     * @param array Array instance that usually shall not be null.
     * @return JSONArray to represent the Array object to be converted.
     */
    protected static JSONArray asJSONArrayFromArray(Object array) {
        assertAllTrue(array != null, array.getClass().isArray());

        Object[] objects = TypeHelper.convert(array, Object[].class);
        IJSONValue[] values = Arrays.stream(objects)
                .map(Utilities::jsonify)
                .toArray(size -> new IJSONValue[size]);
        return new JSONArray(values);
    }

    /**
     * Convert an Collection object to JSONArray.
     *
     * @param collection Collction instance that usually shall not be null.
     * @return JSONArray to represent the Collection object to be converted.
     */
    protected static JSONArray asJSONArrayFromCollection(Object collection) {
        assertAllTrue(collection != null, collection instanceof Collection);

        Object[] array = ((Collection) collection).toArray();
        return asJSONArrayFromArray(array);
    }

    /**
     * Convert the unknown type of object to its String form.
     *
     * @param object Object that has no definition of how to be converted.
     * @return JSONString to represent the unknown type of object.
     */
    protected static JSONString asJSONStringFromOthers(Object object) {
        assertAllNotNull(object);

        String string = object.toString();
        return new JSONString(string);
    }
    //endregion

    /**
     * Compare any two JAVA objects and return their differences as an {@code IJSONValue}.
     *
     * @param obj1      the first Object to be compared.
     * @param obj2      the second Object to be compared.
     * @param indexKey      indicating how delta shall be composed:
     *                      <tt>null</tt> means elements indexes shall always be considered;
     *                      <tt>String.Empty</tt> means orders matter but no index pair included;
     *                      names including special character '+' like "+index" would always include index pairs of two matched elements if they are different with only their positions;
     *                      otherwise the index pair of two elements would be displayed if they have different values.
     * @return  the differences of the above two Objects.
     */
    public static IJSONValue deltaWith(Object obj1, Object obj2, String indexKey){
        IJSONValue json1 = jsonify(obj1);
        IJSONValue json2 = jsonify(obj2);

        return json1.deltaWith(json2, indexKey);
    }

    /**
     * Compare any two JAVA objects and return their differences as an {@code IJSONValue} with default value of JSONArray.defaultElementOrderMatters.
     * @param obj1      the first Object to be compared.
     * @param obj2      the second Object to be compared.
     * @return  the differences of the above two Objects.
     */
    public static IJSONValue deltaWith(Object obj1, Object obj2){
        IJSONValue json1 = jsonify(obj1);
        IJSONValue json2 = jsonify(obj2);

        return json1.deltaWith(json2);
    }

}

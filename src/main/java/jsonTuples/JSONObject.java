package jsonTuples;

import io.github.cruisoring.tuple.Tuple;
import io.github.cruisoring.utility.ArrayHelper;
import io.github.cruisoring.utility.SetHelper;
import io.github.cruisoring.utility.SimpleTypedList;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.TextStringBuilder;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static io.github.cruisoring.Asserts.assertAllNotNull;
import static io.github.cruisoring.Asserts.assertAllTrue;
import static jsonTuples.Parser.*;

/**
 * An object is an unordered set of name/value pairs. An object begins with { (left brace) and ends with } (right brace). Each name is followed by : (colon) and the name/value pairs are separated by , (comma).
 * @see <a href="http://www.json.org">http://www.json.org</a>
 */
public class JSONObject extends Tuple<NamedValue> implements IJSONValue<NamedValue>, Map<String, Object> {

    static final String JSONObject_UNMODIFIABLE = "JSONObject instance is unmodifiable, asMutableObject() would return a modifiable LinkedHashMap containing underlying values that can be modified and then convert back to another JSONObject instance.";

    public static final JSONObject EMPTY = new JSONObject();
    //Pattern of string to represent a solid JSON Object
    public static final Pattern JSON_OBJECT_PATTERN = Pattern.compile("^\\{[\\s\\S]*?\\}$", Pattern.MULTILINE);
    final Comparator<String> nameComparator;
    Map<String, NamedValue> jsonMap = null;
    Map<String, Object> objectMap = null;

    protected JSONObject(NamedValue... namedValues) {
        super(NamedValue.class, namedValues);
        nameComparator = null;
    }

    protected JSONObject(Comparator<String> comparator, NamedValue... namedValues) {
        super(NamedValue.class, comparator == null ? namedValues : sorted(comparator, namedValues));
        nameComparator = comparator;
    }

    protected JSONObject(Comparator<String> comparator, List<IJSONable> namedValues) {
        super(NamedValue.class, sorted(comparator, namedValues));
        nameComparator = comparator;
    }

    //region Parse given text as a JSONObject
    /**
     * Assuming the concerned valueString is of an JSON Object, parse it as a {@code JSONObject} with given nameComparator.
     *
     * @param isStrictly    indicate if strict rules shall be applied for parsing
     * @param comparator    the name comparator used to sort the {@code IJSONValue} with fixed orders.
     * @param valueString   text to be parsed that shall begins with {(left brace) and ends with }(right brace).
     * @return      a {@code JSONObject} instance from the given text.
     */
    public static JSONObject parse(boolean isStrictly, Comparator<String> comparator, String valueString) {
        return (JSONObject) Parser.parse(isStrictly, comparator, valueString);
    }

    /**
     * Assuming the concerned valueString is of an JSON Object, parse it as a {@code JSONObject} with given nameComparator.
     *
     * @param comparator    the name comparator used to sort the {@code IJSONValue} with fixed orders.
     * @param valueString   text to be parsed that shall begins with {(left brace) and ends with }(right brace).
     * @return      a {@code JSONObject} instance from the given text.
     */
    public static JSONObject parse(Comparator<String> comparator, String valueString) {
        return (JSONObject) Parser.parse(comparator, valueString);
    }

    /**
     * Assuming the concerned valueString is of an JSON Object, parse it as a {@code JSONObject} with default nameComparator.
     * @param valueString   text to be parsed that shall begins with {(left brace) and ends with }(right brace).
     * @return      a {@code JSONObject} instance from the given text.
     */
    public static JSONObject parse(String valueString) {
        return (JSONObject) Parser.parse(valueString);
    }

    //region Use the given nameComparator to sort a NamedValue array by their names
    protected static NamedValue[] sorted(Comparator<String> comparator, NamedValue[] namedValues) {
        assertAllTrue(namedValues != null);

        if (comparator != null) {
            Arrays.sort(namedValues, (nv1, nv2)
                    -> comparator.compare(nv1.getName(), nv2.getName()));
        }
        return namedValues;
    }

    protected static NamedValue[] sorted(Comparator<String> comparator, List<IJSONable> namedValueList) {
        assertAllTrue(namedValueList != null);

        NamedValue[] namedValues = (NamedValue[]) ArrayHelper.create(NamedValue.class, namedValueList.size(), namedValueList::get);
        return comparator == null ? namedValues : sorted(comparator, namedValues);
    }

    private Map<String, NamedValue> getJsonMap() {
        if(jsonMap == null) {
            Map<String, NamedValue> map = new LinkedHashMap<>();
            for (NamedValue nv : values) {
                map.put(nv.getName(), nv);
            }
            jsonMap = Collections.unmodifiableMap(map);
        }
        return jsonMap;
    }

    @Override
    public JSONObject getSorted(Comparator<String> comparator) {
        if (comparator == null || comparator == nameComparator)
            return this;

        Map<String, NamedValue> map = getJsonMap();
        List<String> keyList = map.keySet().stream().sorted(comparator).collect(Collectors.toList());

        NamedValue[] sortedNamedValues = keyList.stream()
                .map(key -> map.get(key).getSorted(comparator))
                .toArray(size -> new NamedValue[size]);

        return new JSONObject(comparator, sortedNamedValues);
    }

    @Override
    public Object getObject() {
        if(objectMap==null) {
            Map<String, Object> map = new LinkedHashMap<>();
            for (NamedValue nv : values) {
                map.put(nv.getName(), nv.getValue());
            }
            objectMap = Collections.unmodifiableMap(map);
        }
        return objectMap;
    }

    @Override
    public Object asMutableObject(){
        Map<String, Object> map = new LinkedHashMap<>();
        for (NamedValue nv : values) {
            map.put(nv.getName(), nv.getSecond().asMutableObject());
        }
        return map;
    }

    @Override
    public String toJSONString(String indent) {
        assertAllTrue(StringUtils.isBlank(indent));

        int length = values.length;
        if (length == 0) {
            return "{}";
        } else if(indent == null) {
            String[] elementStrings = Arrays.stream(values).parallel().map(v -> v.toJSONString(null)).toArray(size -> new String[size]);
            return "{" + String.join(",", elementStrings) + "}";
        } else {
            TextStringBuilder sb = new TextStringBuilder();
            sb.append(LEFT_BRACE + NEW_LINE);
            String elementIndent = SPACE+indent;
            for (int i = 0; i < length - 1; i++) {
                sb.append(values[i].toJSONString(elementIndent) + COMMA_NEWLINE);
            }
            sb.append(values[length-1].toJSONString(elementIndent) + NEW_LINE + indent + RIGHT_BRACE);
            return sb.toString();
        }
    }

    @Override
    public String toString() {
        return toJSONString("");
    }

    @Override
    public int getLeafCount() {
        int count = 0;
        for (NamedValue element : values) {
            count += element.getLeafCount();
        }
        return count;
    }

    public JSONObject withDelta(Map<String, Object> delta) {
        assertAllNotNull(delta);
        Map<String, Object> thisMap = (Map<String, Object>) asMutableObject();
        thisMap.putAll(delta);

        JSONObject newObject = Utilities.asJSONObject(nameComparator, thisMap);
        return newObject;
    }

    public JSONObject withDelta(String jsonDeltas){
        JSONObject delta = JSONObject.parse(jsonDeltas);
        return withDelta(delta);
    }

    @Override
    public int size() {
        return getLength();
    }

    @Override
    public boolean containsKey(Object key) {
        return getJsonMap().containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        getObject();
        return objectMap.containsValue(value);
    }

    @Override
    public Object get(Object key) {
        getObject();
        return objectMap.get(key);
    }

    @Override
    public Object put(String key, Object value) {
        throw new UnsupportedOperationException(JSONObject_UNMODIFIABLE);
    }

    @Override
    public Object remove(Object key) {
        throw new UnsupportedOperationException(JSONObject_UNMODIFIABLE);
    }

    @Override
    public void putAll(Map<? extends String, ?> m) {
        throw new UnsupportedOperationException(JSONObject_UNMODIFIABLE);
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException(JSONObject_UNMODIFIABLE);
    }

    @Override
    public Set<String> keySet() {
        return getJsonMap().keySet();
    }

    @Override
    public Collection<Object> values() {
        getObject();
        return objectMap.values();
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        getObject();
        return objectMap.entrySet();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof JSONObject) || !(obj instanceof Map)) {
            return false;
        } else if (obj == this) {
            return true;
        }

        final JSONObject other = Utilities.asJSONObject(nameComparator, obj);
        if (this.isEmpty() && other.isEmpty()) {
            return true;
        } else if (!other.canEqual(this)) {
            return false;
        } else if (getLength() == other.getLength() && toString().equals(other.toString())) {
            return true;
        }

        return deltaWith(other).getLength() == 0;
    }

    @Override
    public boolean canEqual(Object other) {
        return other instanceof JSONObject || other instanceof Map;
    }

    @Override
    public IJSONValue deltaWith(IJSONValue other, String indexName) {
        if (other == null) {
            return new JSONArray(this, JSONValue.MISSING);
        } else if (other == this) {
            return JSONArray.EMPTY;
        } else if (!(other instanceof JSONObject)) {
            return new JSONArray(this, other);
        }

        JSONObject otherObject = (JSONObject) other;
        //Sort both JSONObjects with the same Comparator
        if(this.nameComparator == null && otherObject.nameComparator == null){
            Comparator<String> comparator = new OrdinalComparator<>();
            return getSorted(comparator).deltaWith(otherObject.getSorted(comparator), indexName);
        } else if (this.nameComparator == null) {
            return getSorted(otherObject.nameComparator).deltaWith(otherObject, indexName);
        } else if (this.nameComparator != ((JSONObject) other).nameComparator){
            return deltaWith(otherObject.getSorted(this.nameComparator), indexName);
        }

        if (otherObject.hashCode() == this.hashCode() && other.toString().equals(toString())) {
            return JSONArray.EMPTY;
        }

        Set<String> allKeys = SetHelper.union(keySet(), otherObject.keySet());

        if (allKeys.isEmpty()) {
            return JSONArray.EMPTY;
        }

        List<NamedValue> differences = new SimpleTypedList<>();
        Map<String, NamedValue> thisValues = getJsonMap();
        Map<String, NamedValue> otherValues = otherObject.getJsonMap();

        for (String key : allKeys) {
            IJSONValue thisValue = thisValues.containsKey(key) ? thisValues.get(key).getSecond() : JSONValue.MISSING;
            IJSONValue otherValue = otherObject.containsKey(key) ? otherValues.get(key).getSecond() : JSONValue.MISSING;
            IJSONValue valueDelta = thisValue.deltaWith(otherValue, indexName);
            if (valueDelta.getLength() != 0) {
                NamedValue newDif = new NamedValue(key, valueDelta);
                differences.add(newDif);
            }
        }

        int differenceCount = differences.size();
        if (differenceCount == 0) {
            return JSONArray.EMPTY;
        }

        JSONObject delta = new JSONObject(differences.toArray(new NamedValue[differenceCount]));
        return delta;
    }

    @Override
    public boolean isEmpty() {
        return values.length == 0;
    }
}
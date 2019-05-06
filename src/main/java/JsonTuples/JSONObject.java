package JsonTuples;

import io.github.cruisoring.Range;
import io.github.cruisoring.tuple.Tuple;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.TextStringBuilder;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static io.github.cruisoring.Asserts.checkStates;
import static io.github.cruisoring.Asserts.checkWithoutNull;

/**
 * An object is an unordered set of name/value pairs. An object begins with { (left brace) and ends with } (right brace). Each name is followed by : (colon) and the name/value pairs are separated by , (comma).
 * @see <a href="http://www.json.org">http://www.json.org</a>
 */
public class JSONObject extends Tuple<NamedValue> implements IJSONValue<NamedValue>, Map<String, Object> {

    public static final JSONObject EMPTY = new JSONObject();
    //Pattern of string to represent a solid JSON Object
    public static final Pattern JSON_OBJECT_PATTERN = Pattern.compile("^\\{[\\s\\S]*?\\}$", Pattern.MULTILINE);
    private static final Pattern LINE_STARTS = Pattern.compile("^" + SPACE, Pattern.MULTILINE);
    public static IJSONValue MISSING = JSONValue.Null;
    final Comparator<String> nameComparator;
    Map<String, NamedValue> jsonMap = null;
    Map<String, Object> objectMap = null;

    protected JSONObject(NamedValue... namedValues) {
        this(null, namedValues);
    }

    protected JSONObject(Comparator<String> comparator, NamedValue... namedValues) {
        super(NamedValue.class, sorted(comparator, namedValues));
        nameComparator = comparator;
    }

    //region Parse given text as a JSONObject
    public static IJSONValue parse(CharSequence jsonContext, Range range) {
        checkWithoutNull(jsonContext, range);

        String valueString = jsonContext.subSequence(range.getStartInclusive(), range.getEndExclusive()).toString().trim();
        return parse(valueString);
    }

    public static JSONObject parse(String valueString) {
        return (JSONObject) Parser.parse(valueString);
    }

    //region Use the given nameComparator to sort a NamedValue array by their names
    protected static NamedValue[] sorted(Comparator<String> comparator, NamedValue[] namedValues) {
        checkStates(namedValues != null);

        if (comparator != null) {
            Arrays.sort(namedValues, (nv1, nv2)
                    -> comparator.compare(nv1.getName(), nv2.getName()));
        }
        return namedValues;
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

    private Map<String, Object> getObjectMap() {
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
        return getObjectMap();
    }

    @Override
    public String toJSONString(String indent) {
        checkStates(StringUtils.isBlank(indent));

        String noIndent = toString();
        if (values.length == 0 || "".equals(indent)) {
            return noIndent;
        }

        String indented = indent == null ?
                noIndent.replaceAll("(?m)\\n\\s*", "")
                : noIndent.replaceAll("(?m)\\n", NEW_LINE + indent);
        return indented;
    }

    @Override
    public String toString() {
        if (_toString == null) {
            if (values.length == 0) {
                _toString = "{}";
            } else {
                TextStringBuilder sb = new TextStringBuilder();
                sb.append(LEFT_BRACE + NEW_LINE);
                List<String> valueStrings = Arrays.stream(values)
                        .map(nv -> nv.toJSONString(SPACE))
                        .collect(Collectors.toList());
                sb.appendWithSeparators(valueStrings, COMMA + NEW_LINE);
                sb.append(NEW_LINE + RIGHT_BRACE);
                _toString = sb.toString();
            }
        }
        return _toString;
    }

    public JSONObject withDelta(Map<String, Object> delta) {
        Map<String, Object> thisMap = new LinkedHashMap(getObjectMap());
        thisMap.putAll(delta);

        JSONObject newObject = Utilities.asJSONObject(nameComparator, thisMap);
        return newObject;
    }

    @Override
    public int hashCode() {
        if (_hashCode == null) {
            _hashCode = toString().hashCode();
        }
        return _hashCode;
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
        return getObjectMap().containsValue(value);
    }

    @Override
    public Object get(Object key) {
        return getObjectMap().get(key);
    }

    @Override
    public Object put(String key, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object remove(Object key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Map<? extends String, ?> m) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> keySet() {
        return getJsonMap().keySet();
    }

    @Override
    public Collection<Object> values() {
        return getObjectMap().values();
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return getObjectMap().entrySet();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof JSONObject) || !(obj instanceof Map)) {
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
    public IJSONValue deltaWith(IJSONValue other, boolean orderMatters) {
        if (other == null) {
            return new JSONArray(this, MISSING);
        } else if (other == this) {
            return EMPTY;
        } else if (!(other instanceof JSONObject)) {
            return new JSONArray(this, other);
        }

        JSONObject otherObject = (JSONObject) other;
        if (otherObject.hashCode() == this.hashCode() && other.toString().equals(toString())) {
            return EMPTY;
        }

        Set<String> allKeys = new HashSet<String>() {{
            addAll(keySet());
            addAll(otherObject.keySet());
        }};

        if (allKeys.isEmpty()) {
            return EMPTY;
        }

        List<NamedValue> differences = new ArrayList<>();
        Map<String, NamedValue> thisValues = getJsonMap();
        Map<String, NamedValue> otherValues = otherObject.getJsonMap();

        for (String key : allKeys) {
            IJSONValue thisValue = thisValues.containsKey(key) ? thisValues.get(key).getSecond() : MISSING;
            IJSONValue otherValue = otherObject.containsKey(key) ? otherValues.get(key).getSecond() : MISSING;
            IJSONValue valueDelta = thisValue.deltaWith(otherValue, orderMatters);
            if (valueDelta.getLength() != 0) {
                NamedValue newDif = new NamedValue(key, valueDelta);
                differences.add(newDif);
            }
        }

        int differenceCount = differences.size();
        if (differenceCount == 0) {
            return EMPTY;
        }

        JSONObject delta = new JSONObject(differences.toArray(new NamedValue[differenceCount]));
        return delta;
    }

    @Override
    public boolean isEmpty() {
        return values.length == 0;
    }
}
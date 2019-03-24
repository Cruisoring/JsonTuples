package JsonTuples;

import io.github.cruisoring.tuple.Set;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * http://www.json.org
 * An object is an unordered set of name/value pairs. An object begins with { (left brace) and ends with } (right brace). Each name is followed by : (colon) and the name/value pairs are separated by , (comma).
 */
public class JSONObject extends Set<NamedValue> implements IJSONValue, Map<String, IJSONValue> {

    public static final JSONObject EMPTY = new JSONObject();
    public static IJSONValue MISSING = JSONValue.Null;

    //Pattern of string to represent a solid JSON Object
    public static final Pattern JSON_OBJECT_PATTERN = Pattern.compile("^\\{[\\s\\S]*?\\}$", Pattern.MULTILINE);

    private static final Pattern LINE_STARTS = Pattern.compile("^" + SPACE, Pattern.MULTILINE);

    public static class OrdinalComparator<T> implements Comparator<T> {

        private Map<T, Integer> orders = new HashMap<>();

        public OrdinalComparator(Collection<T> options){
            for (T option :
                    options) {
                orders.putIfAbsent(option, orders.size() + 1);
            }
        }

        @Override
        public int compare(T o1, T o2) {
            return orders.get(o1).compareTo(orders.get(o2));
        }
    }

    public static IJSONValue parse(String jsonContext, Range range) {
        checkNotNull(jsonContext);
        checkNotNull(range);

        String valueString = jsonContext.substring(range.getStartInclusive(), range.getEndExclusive()).trim();
        return parse(valueString);
    }


    public static JSONObject parse(String valueString) {
        return (JSONObject) Parser.parse(valueString);
    }

    protected static NamedValue[] sorted(NamedValue[] namedValues){
        List<String> names = Arrays.stream(namedValues)
                .map(NamedValue::getName).collect(Collectors.toList());
        return sorted(new OrdinalComparator<String>(names), namedValues);
    }

    protected static NamedValue[] sorted(Comparator<String> comparator, NamedValue[] namedValues){
        checkNotNull(comparator);
        checkNotNull(namedValues);

        Arrays.sort(namedValues, (nv1, nv2)
                -> comparator.compare(nv1.getName(), nv2.getName()));
        return namedValues;
    }

    final Map<String, IJSONValue> map = new HashMap<>();

    final java.util.Set<String> names = new LinkedHashSet<String>();

    final Comparator<String> nameComparator;

//    final Lazy<Set<Tuple2<String, IJSONValue>>> tuples = new Lazy<>(map::asTupleSet);

    protected JSONObject(NamedValue... namedValues) {
        this(new OrdinalComparator<String>(Arrays.stream(namedValues).map(NamedValue::getName).collect(Collectors.toList())), namedValues);
    }

    protected JSONObject(Comparator<String> comparator, NamedValue... namedValues) {
        super(sorted(comparator, namedValues));
        nameComparator = comparator;

        for (Object value : values) {
            NamedValue namedValue = (NamedValue)value;
            String name = namedValue.getName();
            map.put(name, namedValue.getValue());
            names.add(name);
        }
    }

    public JSONObject deltaWith(JSONObject other) {
        checkNotNull(other);

        java.util.Set<String> thisKeys = names;
        java.util.Set<String> otherKeys = other.names;
        java.util.Set<String> allKeys = new LinkedHashSet<>(thisKeys);
        allKeys.addAll(otherKeys);

        List<NamedValue> delta = new ArrayList<>();
        for(String key : allKeys){
            NamedValue nv = null;
            IJSONValue thisValue = thisKeys.contains(key) ? get(key) : MISSING;
            IJSONValue otherValue = otherKeys.contains(key) ? other.get(key) : MISSING;
            if(thisValue.equals(otherValue)){
                continue;
            } else if (thisValue instanceof JSONObject && otherValue instanceof JSONObject) {
                JSONObject childDelta = ((JSONObject) thisValue).deltaWith((JSONObject) otherValue);
                nv = new NamedValue(key, childDelta);
            } else {
                nv = new NamedValue(key, new JSONArray(thisValue, otherValue));
            }
            delta.add(nv);
        }

        return new JSONObject(delta.toArray(new NamedValue[0]));
    }

    @Override
    public int size() {
        return values.length;
    }

    /**
     * Returns <tt>true</tt> if this map contains no key-value mappings.
     *
     * @return <tt>true</tt> if this map contains no key-value mappings
     */
    @Override
    public boolean isEmpty() {
        return names.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return names.contains(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return false;
    }

    @Override
    public IJSONValue get(Object key) {
        return map.get(key);
    }

    @Override
    public IJSONValue put(String key, IJSONValue value) {
        return null;
    }

    @Override
    public IJSONValue remove(Object key) {
        return null;
    }

    @Override
    public void putAll(Map<? extends String, ? extends IJSONValue> m) {

    }

    @Override
    public void clear() {

    }

    @Override
    public java.util.Set<String> keySet() {
        return names;
    }

    @Override
    public Collection<IJSONValue> values() {
        return null;
    }

    @Override
    public java.util.Set<Entry<String, IJSONValue>> entrySet() {
        return null;
    }

    @Override
    public String toJSONString(String indent) {

        final int length = values.length;
        if(length == 0) {
            return "{}";
        }

        List<String> lines = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            NamedValue nameValue = (NamedValue) values[i];
            if(nameValue == null) {
                throw new IllegalStateException();
            }
            String valueLines = nameValue.getValue().toJSONString();
            valueLines = valueLines.replaceAll(NEW_LINE, NEW_LINE+SPACE);
            String line = String.format(indent + "%s\"%s\": %s%s",
                    SPACE, nameValue.getName(), valueLines, i==length-1?"":",");
            lines.add(line);
        }
        lines.add(0, "{");
        lines.add("}");

        String string = String.join(IJSONValue.NEW_LINE, lines);
        string = string.replaceAll(NEW_LINE, NEW_LINE+indent);
        return string;
    }

    @Override
    public Object getObject() {
        return map;
    }

    @Override
    public String toString() {
        return toJSONString("");
    }
}
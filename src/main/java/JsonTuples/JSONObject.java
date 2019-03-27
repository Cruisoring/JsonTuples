package JsonTuples;

import io.github.cruisoring.Lazy;
import io.github.cruisoring.tuple.TupleSet;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * http://www.json.org
 * An object is an unordered set of name/value pairs. An object begins with { (left brace) and ends with } (right brace). Each name is followed by : (colon) and the name/value pairs are separated by , (comma).
 */
public class JSONObject extends TupleSet<NamedValue> implements IJSONValue, Map<String, Object> {

    public static final JSONObject EMPTY = new JSONObject();
    public static IJSONValue MISSING = JSONValue.Null;

    //Pattern of string to represent a solid JSON Object
    public static final Pattern JSON_OBJECT_PATTERN = Pattern.compile("^\\{[\\s\\S]*?\\}$", Pattern.MULTILINE);

    private static final Pattern LINE_STARTS = Pattern.compile("^" + SPACE, Pattern.MULTILINE);

    //region OrdinalComparator Definition

    /**
     * Comparator with given list of values, when new key is evaluated, it would be assigned a larger integer value.
     * @param <T>   Type of the keys to be compared.
     */
    public static class OrdinalComparator<T> implements Comparator<T> {

        private Map<T, Integer> orders = new HashMap<>();

        public OrdinalComparator(Collection<T> options){
            for (T option : checkNotNull(options)) {
                orders.putIfAbsent(option, orders.size() + 1);
            }
        }

        @Override
        public int compare(T o1, T o2) {
            orders.putIfAbsent(o1, orders.size() + 1);
            orders.putIfAbsent(o2, orders.size() + 1);
            return orders.get(o1).compareTo(orders.get(o2));
        }
    }
    //endregion

    public static IJSONValue parse(String jsonContext, Range range) {
        checkNotNull(jsonContext);
        checkNotNull(range);

        String valueString = jsonContext.substring(range.getStartInclusive(), range.getEndExclusive()).trim();
        return parse(valueString);
    }


    public static JSONObject parse(String valueString) {
        return (JSONObject) Parser.parse(valueString);
    }

    protected static NamedValue[] sorted(Comparator<String> comparator, NamedValue[] namedValues){
        checkNotNull(namedValues);

        if(comparator != null) {
            Arrays.sort(namedValues, (nv1, nv2)
                    -> comparator.compare(nv1.getName(), nv2.getName()));
        }
        return namedValues;
    }

    private Map<String, Object> getMap(){
        Map<String, Object> m = new HashMap<>();

        for (Object v : values) {
            NamedValue nv = (NamedValue)v;
            m.put(nv.getName(), nv.getValue().getObject());
        }
        return m;
    }

    final Lazy<Map<String, Object>> lazyMap = new Lazy<>(this::getMap);

    private Map<String, IJSONValue> getValueMap() {
        return Arrays.stream(values)
                .map(obj -> (NamedValue)obj)
                .collect(Collectors.toMap(
                        entry -> entry.getName(),
                        entry -> entry.getValue()
                ));
    }

    final Lazy<Map<String, IJSONValue>> lazyValueMap = new Lazy<>(this::getValueMap);

    final Comparator<String> nameComparator;

    protected JSONObject(NamedValue... namedValues) {
        this(null, namedValues);
    }

    protected JSONObject(Comparator<String> comparator, NamedValue... namedValues) {
        super(NamedValue.class, sorted(comparator, namedValues));
        nameComparator = comparator;
    }

    public JSONObject getSorted(Comparator<String> comparator){
        if(comparator == null || comparator == nameComparator)
            return this;

        NamedValue[] copy = Arrays.stream(values).map(v -> (NamedValue)v).toArray(size->new NamedValue[size]);
        return new JSONObject(comparator, copy);
    }

    public JSONObject getSorted(Collection<String> orderedNames){
        Comparator<String> comparator = new OrdinalComparator<>(orderedNames);
        NamedValue[] copy = Arrays.stream(values).map(v -> (NamedValue)v).toArray(size->new NamedValue[size]);
        return new JSONObject(comparator, copy);
    }

    public JSONObject getSortedDeep(Collection<String> orderedNames){
        checkNotNull(orderedNames);
        Comparator<String> comparator = new OrdinalComparator<>(orderedNames);
        return getSortedDeep(comparator);
    }

    public JSONObject getSortedDeep(Comparator<String> comparator){
        if(comparator == null || comparator == nameComparator)
            return this;

        NamedValue[] copy = new NamedValue[values.length];

        for (int i = 0; i < values.length; i++) {
            NamedValue original = (NamedValue)values[i];
            if(original.getValue() instanceof JSONObject) {
                copy[i] = new NamedValue(original.getName(), ((JSONObject)original.getValue()).getSortedDeep(comparator));
            } else {
                copy[i] = original;
            }
        }

        return new JSONObject(comparator, copy);
    }

    public JSONObject deltaWith(JSONObject other) {
        checkNotNull(other);

        java.util.Set<String> thisKeys = keySet();
        java.util.Set<String> otherKeys = other.keySet();
        java.util.Set<String> allKeys = new LinkedHashSet<>(thisKeys);
        allKeys.addAll(otherKeys);

        List<NamedValue> delta = new ArrayList<>();
        for(String key : allKeys){
            NamedValue nv = null;
            IJSONValue thisValue = thisKeys.contains(key) ? lazyValueMap.getValue().get(key) : MISSING;
            IJSONValue otherValue = otherKeys.contains(key) ? other.lazyValueMap.getValue().get(key) : MISSING;
            if(Objects.equals(thisValue, otherValue)){
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

    @Override
    public boolean isEmpty() {
        return values.length == 0;
    }

    @Override
    public boolean containsKey(Object key) {
        if(key == null || !(key instanceof String)) {
            return false;
        }

        return lazyMap.getValue().containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return false;
    }

    @Override
    public Object get(Object key) {
        return lazyMap.getValue().get(key);
    }

    @Override
    public Object put(String key, Object value) {
        return null;
    }

    @Override
    public Object remove(Object key) {
        return null;
    }

    @Override
    public void putAll(Map<? extends String, ? extends Object> m) {}

    @Override
    public void clear() {}

    //No order of keyset guaranteed?
    @Override
    public java.util.Set<String> keySet() {
        return lazyMap.getValue().keySet();
    }

    //No order of values guaranteed?
    @Override
    public Collection<Object> values() {
        return lazyMap.getValue().values();
    }

    //No order of entryset guaranteed?
    @Override
    public java.util.Set<Entry<String, Object>> entrySet() {
        return lazyMap.getValue().entrySet();
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
        return lazyMap.getValue();
    }

    @Override
    public String toString() {
        return toJSONString("");
    }

    public JSONObject withDelta(Map<String, Object> delta){
        Map<String, Object> thisMap = lazyMap.getValue();
//        lazyMap.closing();
        thisMap.putAll(delta);

        JSONObject newObject = Converter.asJSONObject(nameComparator, thisMap);
        return newObject;
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public boolean canEqual(Object other) {
        return super.canEqual(other);
    }
}
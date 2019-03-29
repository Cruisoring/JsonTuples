package JsonTuples;

import io.github.cruisoring.Lazy;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.TextStringBuilder;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * http://www.json.org
 * An object is an unordered set of name/value pairs. An object begins with { (left brace) and ends with } (right brace). Each name is followed by : (colon) and the name/value pairs are separated by , (comma).
 */
public class JSONObject extends TupleMap<String> implements IJSONValue {

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

    final Comparator<String> nameComparator;
    final Lazy<Map<String, IJSONValue>> lazyRawMap = new Lazy<>(() -> getRawMap());

    protected JSONObject(NamedValue... namedValues) {
        this(null, namedValues);
    }

    protected JSONObject(Comparator<String> comparator, NamedValue... namedValues) {
        super(NamedValue.class, sorted(comparator, namedValues));
        nameComparator = comparator;
    }

    /**
     * Convert the NamedValues as a conventional mutable Map that can be treated as a copy of this JSONObject.
     * @return  MutableMap with names as the keys, Object the corresponding IJSONValue from as the values.
     */
    public Map<String, Object> asMap(){
        return new HashMap<>(lazyMap.getValue());
    }

    private Map<String, IJSONValue> getRawMap(){
        Map<String, IJSONValue> valueMap = new HashMap<>();
        NamedValue[] nodes = (NamedValue[])values;
        for (NamedValue node :
                nodes) {
            valueMap.put(node.getName(), node.getValue());
        }
        return valueMap;
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

        Set<String> thisKeys = keySet();
        Set<String> otherKeys = other.keySet();
        Set<String> allKeys = new HashSet<>(thisKeys);
        allKeys.addAll(otherKeys);

        List<NamedValue> delta = new ArrayList<>();
        NamedValue nv = null;
        for(String key : allKeys){
            IJSONValue thisValue = thisKeys.contains(key) ? this.lazyRawMap.getValue().get(key) : MISSING;
            IJSONValue otherValue = otherKeys.contains(key) ? other.lazyRawMap.getValue().get(key) : MISSING;
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
    public Object getObject() {
        return lazyMap.getValue();
    }

    @Override
    public String toJSONString(String indent) {
        checkState(StringUtils.isBlank(indent));

        String noIndent = toString();
        if(values.length == 0 || "".equals(indent)){
            return noIndent;
        }

        String indented = indent==null ?
                noIndent.replaceAll("(?m)\\n\\s*", "")
                : noIndent.replaceAll("(?m)\\n", NEW_LINE+indent);
        return indented;
    }

    @Override
    public String toString() {
        if(_toString == null){
            if(values.length == 0) {
                _toString = "{}";
            } else {
                TextStringBuilder sb = new TextStringBuilder();
                sb.append(LEFT_BRACE + NEW_LINE+SPACE);
                NamedValue[] namedValues = (NamedValue[])values;
                List<String> valueStrings = Arrays.stream(namedValues)
                        .map(nv -> String.format("\"%s\": %s", nv.getName(), nv.getValue().toJSONString(SPACE)))
                        .collect(Collectors.toList());
                sb.appendWithSeparators(valueStrings, COMMA+NEW_LINE+SPACE);
                sb.append(NEW_LINE+RIGHT_BRACE);
                _toString = sb.toString();
            }
        }
        return _toString;
    }

    public JSONObject withDelta(Map<String, Object> delta){
        Map<String, Object> thisMap = asMap();
        thisMap.putAll(delta);

        JSONObject newObject = Converter.asJSONObject(nameComparator, thisMap);
        return newObject;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null || !(obj instanceof JSONObject) || !(obj instanceof Map))
            return false;
        if (obj == this)
            return true;

        final JSONObject other;
        if(obj instanceof JSONObject){
            other = (JSONObject)obj;
        } else {
            other = Converter.asJSONObject(obj);
        }

        if(!other.canEqual(this) || getLength() != other.getLength())
            return false;

        return this.deltaWith(other).getLength() == 0;
    }

    @Override
    public boolean canEqual(Object other) {
        return other instanceof JSONObject;
    }

    @Override
    public IJSONValue deltaWith(IJSONValue other) {
        return null;
    }

}
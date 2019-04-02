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

        Map<T, Integer> orders = new HashMap<>();
        List<T> orderedKeys = new ArrayList<>();

        Integer putIfAbsent(T key, Integer order){
            if(!orders.containsKey(key)){
                orders.put(key, order);
                orderedKeys.add(key);
                return order;
            }else{
                return orders.get(key);
            }
        }

        public OrdinalComparator(Collection<T> options){
            for (T option : checkNotNull(options)) {
                putIfAbsent(option, orders.size() + 1);
            }
        }

        @Override
        public int compare(T o1, T o2) {
            Integer order1 = putIfAbsent(o1, orders.size() + 1);
            Integer order2 = putIfAbsent(o2, orders.size() + 1);
            return order1.compareTo(order2);
        }

        @Override
        public String toString() {
            String string = orderedKeys.stream()
                    .map(k -> k.toString())
                    .collect(Collectors.joining(","));
            return string;
        }
    }
    //endregion

    //region Parse given text as a JSONObject
    public static IJSONValue parse(String jsonContext, Range range) {
        checkNotNull(jsonContext);
        checkNotNull(range);

        String valueString = jsonContext.substring(range.getStartInclusive(), range.getEndExclusive()).trim();
        return parse(valueString);
    }


    public static JSONObject parse(String valueString) {
        return (JSONObject) Parser.parse(valueString);
    }
    //endregion

    //region Use the given nameComparator to sort a NamedValue array by their names
    protected static NamedValue[] sorted(Comparator<String> comparator, NamedValue[] namedValues){
        checkNotNull(namedValues);

        if(comparator != null) {
            Arrays.sort(namedValues, (nv1, nv2)
                    -> comparator.compare(nv1.getName(), nv2.getName()));
        }
        return namedValues;
    }
    //endregion

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

    @Override
    public JSONObject getSorted(Comparator<String> comparator){
        if(comparator == null || comparator == nameComparator)
            return this;

        NamedValue[] copy = new NamedValue[values.length];

        for (int i = 0; i < values.length; i++) {
            NamedValue original = (NamedValue)values[i];
            if(original.getValue() instanceof JSONObject) {
                copy[i] = new NamedValue(original.getName(), ((JSONObject)original.getValue()).getSorted(comparator));
            } else {
                copy[i] = original;
            }
        }

        return new JSONObject(comparator, copy);
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
    public int hashCode() {
        if(_hashCode == null){
            _hashCode = toString().hashCode();
        }
        return _hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null || !(obj instanceof JSONObject) || !(obj instanceof Map)) {
            return false;
        } else if (obj == this) {
            return true;
        }

        final JSONObject other = Converter.asJSONObject(nameComparator, obj);
        if(this.isEmpty() && other.isEmpty()){
            return true;
        }else if(!other.canEqual(this)) {
            return false;
        }else if(getLength()==other.getLength() && toString().equals(other.toString())){
            return true;
        }

        return deltaWith(other, null).getLength() == 0;
    }

    @Override
    public boolean canEqual(Object other) {
        return other instanceof JSONObject || other instanceof Map;
    }

//    public IJSONValue deltaWith(IJSONValue other) {
//        return deltaWith(other, nameComparator);
//    }

    @Override
    public IJSONValue deltaWith(IJSONValue other, Comparator<String> comparator) {
        if(other == null){
            return new JSONArray(this, MISSING);
        }else if(other == this){
            return EMPTY;
        }else if(!(other instanceof JSONObject)){
            return new JSONArray(this, other);
        }

        JSONObject otherObject = (JSONObject)other;
        if(otherObject.hashCode()==this.hashCode() && other.toString() == toString()){
            return EMPTY;
        }

        Set<String> allKeys = new HashSet<String>(){{
            addAll(keySet());
            addAll(otherObject.keySet());
        }};

        if(allKeys.isEmpty()){
            return EMPTY;
        }

        List<NamedValue> differences = new ArrayList<>();
        Map<String, IJSONValue> thisValues = lazyRawMap.getValue();
        Map<String, IJSONValue> otherValues = otherObject.lazyRawMap.getValue();

        for (String key : allKeys) {
            IJSONValue thisValue = thisValues.containsKey(key) ? thisValues.get(key) : MISSING;
            IJSONValue otherValue = otherObject.containsKey(key) ? otherValues.get(key) : MISSING;
            IJSONValue valueDelta = thisValue.deltaWith(otherValue, comparator);
            if(valueDelta.getLength() != 0){
                NamedValue newDif = new NamedValue(key, valueDelta);
                differences.add(newDif);
            }
        };

        if(differences.isEmpty()){
            return EMPTY;
        }

        JSONObject delta = new JSONObject(comparator, differences.toArray(new NamedValue[differences.size()]));
        return delta;
    }
}
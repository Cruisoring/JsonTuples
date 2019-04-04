package JsonTuples;

import io.github.cruisoring.Lazy;
import io.github.cruisoring.tuple.Tuple;
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
public class JSONObject extends Tuple<NamedValue> implements IJSONValue<NamedValue>, Map<String, Object> {

    public static final JSONObject EMPTY = new JSONObject();
    public static IJSONValue MISSING = JSONValue.Null;

    //Pattern of string to represent a solid JSON Object
    public static final Pattern JSON_OBJECT_PATTERN = Pattern.compile("^\\{[\\s\\S]*?\\}$", Pattern.MULTILINE);

    private static final Pattern LINE_STARTS = Pattern.compile("^" + SPACE, Pattern.MULTILINE);

    //region Parse given text as a JSONObject
    public static IJSONValue parse(CharSequence jsonContext, Range range) {
        checkNotNull(jsonContext);
        checkNotNull(range);

        String valueString = jsonContext.subSequence(range.getStartInclusive(), range.getEndExclusive()).toString().trim();
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
    final Lazy<Map<String, NamedValue>> lazyJSONMap = new Lazy<>(this::getJsonMap);

    private Map<String, NamedValue> getJsonMap(){
        Map<String, NamedValue> map = new HashMap<>();
        for (NamedValue nv : values) {
            map.put(nv.getName(), nv);
        };
        return map;
    }

    final Lazy<Map<String, Object>> lazyMap = new Lazy<>(this::getObjectMap);
    private Map<String, Object> getObjectMap() {
        Map<String, Object> map = new HashMap<>();
        for (NamedValue nv : values) {
            map.put(nv.getName(), nv.getValue());
        }
        return map;
    }

    Set<String> _nameSet = null;
    Collection<Object> _valueSet = null;
    Set<Entry<String, Object>> _entrySet = null;

    protected JSONObject(NamedValue... namedValues) {
        this(null, namedValues);
    }

    protected JSONObject(Comparator<String> comparator, NamedValue... namedValues) {
        super(NamedValue.class, sorted(comparator, namedValues));
        nameComparator = comparator;
    }

    @Override
    public JSONObject getSorted(Comparator<String> comparator){
        if(comparator == null || comparator == nameComparator)
            return this;

        Map<String, NamedValue> map = lazyJSONMap.getValue();
        List<String> keyList = map.keySet().stream().sorted((Comparator<String>) comparator).collect(Collectors.toList());

        NamedValue[] sortedNamedValues = keyList.stream()
                .map(key -> map.get(key).getSorted(comparator))
                .toArray(size -> new NamedValue[size]);

        return new JSONObject(comparator, sortedNamedValues);
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
                sb.append(LEFT_BRACE + NEW_LINE);
                List<String> valueStrings = Arrays.stream(values)
                        .map(nv -> nv.toJSONString(SPACE))
                        .collect(Collectors.toList());
                sb.appendWithSeparators(valueStrings, COMMA+NEW_LINE);
                sb.append(NEW_LINE+RIGHT_BRACE);
                _toString = sb.toString();
            }
        }
        return _toString;
    }

    public JSONObject withDelta(Map<String, Object> delta){
        Map<String, Object> thisMap = lazyMap.getValue();
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
    public int size() {
        return lazyJSONMap.getValue().size();
    }

    @Override
    public boolean containsKey(Object key) {
        if(_nameSet == null){
            _nameSet = lazyJSONMap.getValue().keySet();
        }
        return _nameSet.contains(key);
    }

    @Override
    public boolean containsValue(Object value) {
        if(_valueSet == null){
            _valueSet = lazyMap.getValue().values();
        }
        return _valueSet.contains(value);
    }

    @Override
    public Object get(Object key) {
        return lazyMap.getValue().get(key);
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
        if(_nameSet == null){
            _nameSet = lazyJSONMap.getValue().keySet();
        }
        return _nameSet;
    }

    @Override
    public Collection<Object> values() {
        if(_valueSet == null){
            _valueSet = lazyMap.getValue().values();
        }
        return _valueSet;
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        if(_entrySet == null){
            _entrySet = lazyMap.getValue().entrySet();
        }
        return _entrySet;
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
        Map<String, NamedValue> thisValues = lazyJSONMap.getValue();
        Map<String, NamedValue> otherValues = otherObject.lazyJSONMap.getValue();

        for (String key : allKeys) {
            IJSONValue thisValue = thisValues.containsKey(key) ? thisValues.get(key).getSecond() : MISSING;
            IJSONValue otherValue = otherObject.containsKey(key) ? otherValues.get(key).getSecond() : MISSING;
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

    @Override
    public boolean isEmpty() {
        return false;
    }
}
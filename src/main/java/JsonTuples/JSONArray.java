package JsonTuples;

import io.github.cruisoring.Lazy;
import io.github.cruisoring.tuple.Tuple;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.TextStringBuilder;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.google.common.base.Preconditions.checkState;

/**
 * http://www.json.org
 * An array is an ordered collection of values. An array begins with [ (left bracket) and ends with ] (right bracket). Values are separated by , (comma).
 */
public class JSONArray extends Tuple<IJSONValue> implements IJSONValue {

    public static boolean isElementOrderMatter = true;

    public static final JSONArray EMPTY = new JSONArray();

    //Pattern of string to represent a solid JSON Array
    public static final Pattern JSON_ARRAY_PATTERN = Pattern.compile("^\\[[\\s\\S]*?\\]$", Pattern.MULTILINE);

    public static JSONArray parseArray(String valueString) {
        return (JSONArray) Parser.parse(valueString);
    }

    final Comparator<String> nameComparator;
    protected JSONArray(IJSONValue... values) {
        this(null, values);
    }

    protected JSONArray(Comparator<String> comparator, IJSONValue... values){
        super(values);
        this.nameComparator = comparator;
    }

    protected Lazy<Object[]> arrayLazy = new Lazy<>(() -> Arrays.stream(asArray())
            .map(IJSONValue::getObject)
            .toArray());

    @Override
    public Object getObject() {
        return arrayLazy.getValue();
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
                _toString = "[]";
            } else {
                TextStringBuilder sb = new TextStringBuilder();
                sb.append(LEFT_BRACKET + NEW_LINE+SPACE);
                List<String> valueStrings = Arrays.stream(values)
                        .map(v -> v.toJSONString(SPACE))
                        .collect(Collectors.toList());
                sb.appendWithSeparators(valueStrings, COMMA+NEW_LINE+SPACE);
                sb.append(NEW_LINE+RIGHT_BRACKET);
                _toString = sb.toString();
            }
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
    public boolean equals(Object obj) {
        if(obj == null || !(obj instanceof JSONArray || obj instanceof Collection || obj.getClass().isArray())) {
            return false;
        } else if (obj == this) {
            return true;
        }

        final JSONArray other = (JSONArray) Converter.jsonify(obj);
        if(this.isEmpty() && other.isEmpty()){
            return true;
        } else if(!other.canEqual(this)) {
            return false;
        }else if(getLength()==other.getLength() && toString().equals(other.toString())){
            return true;
        }

        return deltaWith(other, nameComparator).getLength() == 0;
    }

    @Override
    public boolean canEqual(Object other) {
        return other != null &&
                (other instanceof JSONArray || other instanceof Collection || other.getClass().isArray());
    }

    public JSONObject asIndexedObject(Comparator<String> comparator){
        NamedValue[] namedValues = IntStream.range(0, getLength()).boxed()
            .map(i -> new NamedValue(i.toString(), get(i)))
            .toArray(size -> new NamedValue[size]);
        return new JSONObject(comparator, namedValues);
    }

    protected Map<String, List<Integer>> getValueIndexes(Comparator<String> comparator){
        Map<String, List<Integer>> indexes = new HashMap<>();
        for (int i = 0; i < getLength(); i++) {
            IJSONValue value = get(i).getSorted(comparator);
            String valueString = value.toString();
            if(indexes.containsKey(valueString)){
                indexes.get(valueString).add(i);
            }else{
                indexes.put(valueString, new ArrayList<>(Arrays.asList(i)));
            }
        }
        return indexes;
    }

    @Override
    public IJSONValue getSorted(Comparator<String> comparator) {
        if(nameComparator == comparator){
            return this;
        }
        IJSONValue[] sorted = Arrays.stream(values)
                .map(v -> v.getSorted(comparator))
                .toArray(size -> new IJSONValue[size]);

        return new JSONArray(comparator, sorted);
    }

    @Override
    public IJSONValue deltaWith(IJSONValue other, Comparator<String> comparator) {
        checkState(nameComparator == comparator);

        if(other == null){
            return new JSONArray(this, JSONObject.MISSING);
        }else if(other == this){
            return EMPTY;
        }else if(!(other instanceof JSONArray)){
            return new JSONArray(this, other);
        }

        final JSONArray otherArray = (JSONArray) other;

        if(isElementOrderMatter){
            return asIndexedObject(comparator).deltaWith(otherArray.asIndexedObject(comparator), comparator);
        }
        boolean thisAllObject = Arrays.stream(values).allMatch(v -> v instanceof JSONObject || v.equals(JSONValue.Null));
        boolean otherAllObject = Arrays.stream(otherArray.values).allMatch(v -> v instanceof JSONObject || v.equals(JSONValue.Null));
        if(!thisAllObject || !otherAllObject){
            return asIndexedObject(comparator).deltaWith(otherArray.asIndexedObject(comparator), comparator);
        }

        //Now both arrays are composed of either null or Objects, then their order is not concerned
        Map<String, List<Integer>> thisValueIndexes = getValueIndexes(comparator);
        Map<String, List<Integer>> otherValueIndexes = otherArray.getValueIndexes(comparator);
        Set<String> allKeys = new HashSet<String>(){{
            addAll(thisValueIndexes.keySet());
            addAll(otherValueIndexes.keySet());
        }};

        //Get indexes of unequal JSONObjects of both array
        List<Integer> thisUncertained = new ArrayList<>();
        List<Integer> otherUncertained = new ArrayList<>();
        for (String key : allKeys) {
            if(!thisValueIndexes.containsKey(key)){
                otherUncertained.addAll(otherValueIndexes.get(key));
            }else if(!otherValueIndexes.containsKey(key)){
                thisUncertained.addAll(thisValueIndexes.get(key));
            }else{
                int countDif = thisValueIndexes.get(key).size() - otherValueIndexes.get(key).size();
                if(countDif<0){
                    otherUncertained.addAll(otherValueIndexes.get(key).subList(0, -countDif));
                }else if(countDif>0){
                    thisUncertained.addAll(thisValueIndexes.get(key).subList(0, countDif));
                }
            }
        };

        Map<Integer, Set<Integer>> thisValueTokens = thisUncertained.stream()
                .collect(Collectors.toMap(
                        i -> i,
                        i -> values[i].getSignatures()
                ));
        Map<Integer, Set<Integer>> otherValueTokens = otherUncertained.stream()
                .collect(Collectors.toMap(
                        i -> i,
                        i -> values[i].getSignatures()
                ));



        return null;
    }

}

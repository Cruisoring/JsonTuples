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

    final Lazy<String> toStringLazy = new Lazy<>(() -> toJSONString(""));

    protected JSONArray(IJSONValue... values) {
        super(values);
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

        return deltaWith(other).getLength() == 0;
    }

    @Override
    public boolean canEqual(Object other) {
        return other != null &&
                (other instanceof JSONArray || other instanceof Collection || other.getClass().isArray());
    }

    public JSONObject asIndexedObject(){
        NamedValue[] namedValues = IntStream.range(0, getLength()).boxed()
            .map(i -> new NamedValue(i.toString(), get(i)))
            .toArray(size -> new NamedValue[size]);
        return new JSONObject(namedValues);
    }

    public Map<String, List<Integer>> getValueIndexes(){
        Map<String, List<Integer>> indexes = new HashMap<>();
        for (int i = 0; i < getLength(); i++) {
            IJSONValue value = get(i);
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
    public IJSONValue deltaWith(IJSONValue other) {
        if(other == null){
            return new JSONArray(this, JSONObject.MISSING);
        }else if(other == this){
            return EMPTY;
        }else if(!(other instanceof JSONArray)){
            return new JSONArray(this, other);
        }

        final JSONArray otherArray = (JSONArray)other;

        if(isElementOrderMatter){
            return asIndexedObject().deltaWith(otherArray.asIndexedObject());
        }

        Map<String, List<Integer>> thisValueIndexes = getValueIndexes();
        Map<String, List<Integer>> otherValueIndexes = otherArray.getValueIndexes();


        return null;
    }

    @Override
    public IJSONValue deltaWith(Comparator<String> comparator, IJSONValue other) {
        return null;
    }
}

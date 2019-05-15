package JsonTuples;

import io.github.cruisoring.tuple.Tuple;
import io.github.cruisoring.tuple.Tuple3;
import io.github.cruisoring.utility.ArrayHelper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.TextStringBuilder;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static io.github.cruisoring.Asserts.checkStates;

/**
 * An array is an ordered collection of {@code IJSONValue}. An array begins with [ (left bracket) and ends with ]
 * (right bracket). Values are separated by , (comma).
 * @see <a href="http://www.json.org">http://www.json.org</a>
 */
public class JSONArray extends Tuple<IJSONValue> implements IJSONValue<IJSONValue> {

    /**
     * Assuming the concerned valueString is of array, parse it as a {@code JSONArray}
     * @param valueString   text to be parsed that shall begins with [(left bracket) and ends with ](right bracket).
     * @return      a {@code JSONArray} instance from the given text.
     */
    public static JSONArray parseArray(String valueString) {
        return (JSONArray) Parser.parse(valueString);
    }

    public static final JSONArray EMPTY = new JSONArray();

    //Pattern of string to represent a solid JSON Array
    public static final Pattern JSON_ARRAY_PATTERN = Pattern.compile("^\\[[\\s\\S]*?\\]$", Pattern.MULTILINE);

    //Indicates if the order of the elements composing this JSONArray is matter, which has impact on how deltaWith() works
    public static boolean defaultElementOrderMatters = false;

    // the Comparator<String> used by this {@code JSONArray} to sort its children JSONObjects.
    final Comparator<String> nameComparator;
    // the buffered Java array of {@code Object} represented by this {@code IJSONValue}
    protected Object[] array = null;

    protected JSONArray(IJSONValue... values) {
        this(null, values);
    }

    protected JSONArray(Comparator<String> comparator, IJSONValue... values) {
        super(values);
        this.nameComparator = comparator;
    }

    /**
     * Get the JAVA array represented by this {@code IJSONValue}
     * @return  A Java array of {@code Object} represented by this {@code IJSONValue}
     */
    @Override
    public Object getObject() {
        if(array == null){
            array = Arrays.stream(values)
                    .map(IJSONValue::getObject)
                    .toArray();
        }
        return array;
    }

    @Override
    public Object asMutableObject() {
        List<Object> list = Arrays.stream(values)
                .map(IJSONValue::asMutableObject)
                .collect(Collectors.toList());
        return list;
    }

    @Override
    public String toJSONString(String indent) {
        checkStates(StringUtils.isBlank(indent));

        if (values.length == 0 || "".equals(indent)) {
            return toString();
        }

        //*/
        TextStringBuilder sb = new TextStringBuilder();
        String[] lines = toString().split(NEW_LINE);
        int length = lines.length;
        sb.append(LEFT_BRACKET);
        if(indent == null) {
            for (int i = 1; i < length; i++) {
                sb.append(lines[i].trim());
            }
        } else {
            for (int i = 1; i < length; i++) {
                sb.append(NEW_LINE + indent + lines[i]);
            }
        }
        return sb.toString();
        /*/
        if(indent == null){
            String indented = toString().replaceAll("(?m)\\n\\s*", "");
            return indented;
        } else {
            String indented = toString().replaceAll("(?m)\\n", NEW_LINE + indent);
            return indented;
        }
        //*/
    }

    @Override
    public String toString() {
        if (_toString == null) {
            int length = values.length;
            if (length == 0) {
                _toString = "[]";
            } else {
                TextStringBuilder sb = new TextStringBuilder();
                sb.append(LEFT_BRACKET + NEW_LINE + SPACE);
                for (int i = 0; i < length - 1; i++) {
                    sb.append(values[i].toJSONString(SPACE) + COMMA_NEWLINE_SPACE);
                }
                sb.append(values[length-1].toJSONString(SPACE) + NEW_LINE + RIGHT_BRACKET);
                _toString = sb.toString();
            }
        }
        return _toString;
    }

    @Override
    public int hashCode() {
        if (_hashCode == null) {
            _hashCode = toString().hashCode();
        }
        return _hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof JSONArray || obj instanceof Collection || obj.getClass().isArray())) {
            return false;
        } else if (obj == this) {
            return true;
        }

        final JSONArray other = (JSONArray) Utilities.jsonify(obj);
        if (this.isEmpty() && other.isEmpty()) {
            return true;
        } else if (!other.canEqual(this)) {
            return false;
        } else if (getLength() == other.getLength() && toString().equals(other.toString())) {
            return true;
        }

        return deltaWith(other, true).getLength() == 0;
    }

    @Override
    public boolean canEqual(Object other) {
        return other != null &&
                (other instanceof JSONArray || other instanceof Collection || other.getClass().isArray());
    }

    /**
     * Converted this {@code JSONArray} into a {@code JSONObject} by using indexes of its elements as keys, the elements as values.
     * @return      A {@code JSONObject} with integer keys to indicating the sequences of the array elements.
     */
    public JSONObject asIndexedObject() {
        NamedValue[] namedValues = IntStream.range(0, getLength()).boxed()
                .map(i -> new NamedValue(i.toString(), getValue(i)))
                .toArray(size -> new NamedValue[size]);
        return new JSONObject(namedValues);
    }

    protected Map<String, List<Integer>> getValueIndexes(Comparator<String> comparator) {
        Map<String, List<Integer>> indexes = new HashMap<>();
        for (int i = 0; i < getLength(); i++) {
            IJSONValue value = getValue(i).getSorted(comparator);
            String valueString = value.toString();
            if (indexes.containsKey(valueString)) {
                indexes.get(valueString).add(i);
            } else {
                indexes.put(valueString, new ArrayList<>(Arrays.asList(i)));
            }
        }
        return indexes;
    }

    @Override
    public JSONArray getSorted(Comparator<String> comparator) {
        if (nameComparator == comparator) {
            return this;
        }

        IJSONValue[] sorted = Arrays.stream(values)
                .map(v -> v.getSorted(comparator))
                .toArray(size -> new IJSONValue[size]);

        return new JSONArray(comparator, sorted);
    }

    @Override
    public IJSONValue deltaWith(IJSONValue other, boolean orderMatters) {
        if (other == null) {
            return new JSONArray(this, JSONObject.MISSING);
        } else if (other == this) {
            return EMPTY;
        } else if (!(other instanceof JSONArray)) {
            return new JSONArray(this, other);
        }

        final JSONArray otherArray = (JSONArray) other;

        if (orderMatters) {
            return asIndexedObject().deltaWith(otherArray.asIndexedObject(), orderMatters);
        }

        boolean thisAllObject = this.allMatch(v -> v instanceof JSONObject || v.equals(JSONValue.Null));
        boolean otherAllObject = otherArray.allMatch(v -> v instanceof JSONObject || v.equals(JSONValue.Null));
        if (!thisAllObject || !otherAllObject) {
            return asIndexedObject().deltaWith(otherArray.asIndexedObject(), false);
        }

        //Now both arrays are composed of either null or Objects, and their order is not concerned
        Map<String, List<Integer>> thisValueIndexes = getValueIndexes(Comparator.naturalOrder());
        Map<String, List<Integer>> otherValueIndexes = otherArray.getValueIndexes(Comparator.naturalOrder());
        Set<String> allKeys = new HashSet<String>() {{
            addAll(thisValueIndexes.keySet());
            addAll(otherValueIndexes.keySet());
        }};

        //Get indexes of unequal JSONObjects of both array
        Map<Integer, Set<Integer>> thisValueTokens = new HashMap();
        Map<Integer, Set<Integer>> otherValueTokens = new HashMap();
        for (String key : allKeys) {
            if (!thisValueIndexes.containsKey(key)) {
                otherValueIndexes.get(key).forEach(i -> otherValueTokens.put(i, values[i].getSignatures()));
            } else if (!otherValueIndexes.containsKey(key)) {
                thisValueIndexes.get(key).forEach(i -> thisValueTokens.put(i, values[i].getSignatures()));
            } else {
                int countDif = thisValueIndexes.get(key).size() - otherValueIndexes.get(key).size();
                if (countDif < 0) {
                    otherValueIndexes.get(key).subList(0, -countDif).forEach(i -> otherValueTokens.put(i, values[i].getSignatures()));
                } else if (countDif > 0) {
                    thisValueIndexes.get(key).subList(0, countDif).forEach(i -> thisValueTokens.put(i, values[i].getSignatures()));
                }
            }
        }

        List<Tuple3<Integer, Integer, Integer>> similarities = new ArrayList<>();
        for (Integer thisIndex : thisValueTokens.keySet()) {
            for (Integer otherIndex : otherValueTokens.keySet()) {
                Set<Integer> set = new HashSet(thisValueTokens.get(thisIndex));
                set.retainAll(otherValueTokens.get(otherIndex));
                Integer similarity = set.size();
                similarities.add(Tuple.create(similarity, thisIndex, otherIndex));
            }
        }
        Comparator<Tuple3<Integer, Integer, Integer>> _comparator = Comparator.comparing(tuple -> tuple.getFirst());
        _comparator = _comparator.reversed();
        Collections.sort(similarities, _comparator);

        List<IJSONValue> deltas = new ArrayList<>();
        for (Tuple3<Integer, Integer, Integer> tuple3 : similarities) {
            Integer index1 = tuple3.getSecond();
            Integer index2 = tuple3.getThird();
            if (!thisValueTokens.containsKey(index1) || !otherValueTokens.containsKey(index2)) {
                continue;
            }
            thisValueTokens.remove(index1);
            otherValueTokens.remove(index2);

            IJSONValue delta = this.getValue(index1).deltaWith(otherArray.getValue(index2), false);
            if (!delta.isEmpty()) {
                deltas.add(delta);
            }
        }

        return deltas.isEmpty() ? EMPTY : new JSONArray(nameComparator, deltas.toArray(new IJSONValue[deltas.size()]));
    }

    /**
     * Re-arrange the orders of the element {@code IJSONValue}s to compose another {@code JSONArray}
     * @return  a new {@code JSONArray} composed with the shuffled elements.
     */
    public JSONArray shuffle(){
        IJSONValue[] shuffledValues = (IJSONValue[])ArrayHelper.shuffle(values);
        return new JSONArray(nameComparator, shuffledValues);
    }
}

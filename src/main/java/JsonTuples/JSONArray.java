package JsonTuples;

import io.github.cruisoring.tuple.Tuple;
import io.github.cruisoring.tuple.Tuple2;
import io.github.cruisoring.tuple.Tuple3;
import io.github.cruisoring.utility.ArrayHelper;
import io.github.cruisoring.utility.SetHelper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.TextStringBuilder;

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import static io.github.cruisoring.Asserts.*;

/**
 * An ordered collection of {@code IJSONValue}. An array begins with [ (left bracket) and ends with ]
 * (right bracket). Values are separated by , (comma).
 * @see <a href="http://www.json.org">http://www.json.org</a>
 */
public class JSONArray extends Tuple<IJSONValue> implements IJSONValue<IJSONValue>, List<Object> {

    //Indicates if the order of the elements composing this JSONArray is matter, which has impact on how deltaWith() works
    public static boolean defaultElementOrderMatters = false;

    static final String JSONArray_UNMODIFIABLE = "JSONArray instance is not modifiable, asMutableObject() would return a modifiable List of the underlying values that can be modified and then convert back to another JSONArray instance.";

    //Name of the index pair of the elements to show their deltas
    public static String defaultIndexName = null;

    //Predicate to evaluate the indexName of deltaWith()
    public final static Predicate<String> includeDifferentIndexesPredicate = name -> name != null && name.contains("+");
    /**
     * Assuming the concerned valueString is of array, parse it as a {@code JSONArray}
     * @param valueString   text to be parsed that shall begins with [(left bracket) and ends with ](right bracket).
     * @return      a {@code JSONArray} instance from the given text.
     */
    public static JSONArray parse(String valueString) {
        return (JSONArray) Parser.parse(valueString);
    }

    public static final JSONArray EMPTY = new JSONArray();

    //Pattern of string to represent a solid JSON Array
    public static final Pattern JSON_ARRAY_PATTERN = Pattern.compile("^\\[[\\s\\S]*?\\]$", Pattern.MULTILINE);

    // the Comparator<String> used by this {@code JSONArray} to sort its children JSONObjects.
    final Comparator<String> nameComparator;

    // the buffered list of {@code Object} represented by this {@code IJSONValue}
    protected List<Object> objects = null;

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
        return _getObjects().toArray();
    }

    private List<Object> _getObjects(){
        if(objects == null){
            List<Object> objs = new ArrayList<>();
            for (int i = 0; i < values.length; i++) {
                objs.add(values[i].getObject());
            }
            objects = Collections.unmodifiableList(objs);
        }
        return objects;
    }

    @Override
    public Object asMutableObject() {
        List<Object> list = new ArrayList<>();
        for (int i = 0; i < values.length; i++) {
            list.add(values[i].asMutableObject());
        }
        return list;
    }

    @Override
    public String toJSONString(String indent) {
        checkStates(StringUtils.isBlank(indent));

        if (values.length == 0 || "".equals(indent)) {
            return toString();
        }

        //*/
        if(indent == null) {
            String[] elementStrings = Arrays.stream(values).parallel().map(v -> v.toJSONString(null)).toArray(size -> new String[size]);
            return "[" + String.join(",", elementStrings) + "]";
        }

        TextStringBuilder sb = new TextStringBuilder();
        String[] lines = toString().split(JSONValue.NEW_LINE);
        int length = lines.length;
        sb.append(LEFT_BRACKET);
        for (int i = 1; i < length; i++) {
            sb.append(JSONValue.NEW_LINE + indent + lines[i]);
        }
        return sb.toString();
        /*/
        if(indent == null){
            String indented = toString().replaceAll("(?m)\\n\\s*", "");
            return indented;
        } else {
            String indented = toString().replaceAll("(?m)\\n", JSONValue.NEW_LINE + indent);
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
                sb.append(LEFT_BRACKET + JSONValue.NEW_LINE + JSONValue.SPACE);
                for (int i = 0; i < length - 1; i++) {
                    sb.append(values[i].toJSONString(JSONValue.SPACE) + JSONValue.COMMA_NEWLINE_SPACE);
                }
                sb.append(values[length-1].toJSONString(JSONValue.SPACE) + JSONValue.NEW_LINE + RIGHT_BRACKET);
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

        return deltaWith(other).getLength() == 0;
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
    public IJSONValue deltaWith(IJSONValue other, String indexName) {
        if (other == null) {
            return new JSONArray(this, JSONObject.MISSING);
        } else if (other == this) {
            return EMPTY;
        } else if (!(other instanceof JSONArray) || isEmpty() || other.isEmpty()) {
            return new JSONArray(this, other);
        }

        indexName = indexName==null ? indexName : JSONString.unescapeJson(indexName);
        final JSONArray otherArray = (JSONArray) other;
        //Sort both JSONArrays with the same Comparator
        if(this.nameComparator == null && otherArray.nameComparator == null){
            Comparator<String> comparator = new OrdinalComparator<>();
            return getSorted(comparator).deltaWith(otherArray.getSorted(comparator), indexName);
        } else if (this.nameComparator == null) {
            return getSorted(otherArray.nameComparator).deltaWith(otherArray, indexName);
        } else if (otherArray.nameComparator == null){
            return deltaWith(otherArray.getSorted(this.nameComparator), indexName);
        }

        if (indexName == null) {
            return asIndexedObject().deltaWith(otherArray.asIndexedObject(), indexName);
        }

        final int leftSize, rightSize;
        final IJSONValue[] leftValues, rightValues;
        int thisSize = values.length;
        int otherSize = otherArray.size();
        final boolean thisBigger = thisSize >= otherSize;

        if(thisBigger){
            leftSize = thisSize;
            rightSize = otherSize;
            leftValues = this.values;
            rightValues = otherArray.values;
        } else {
            leftSize = otherSize;
            rightSize = thisSize;
            leftValues = otherArray.values;
            rightValues = this.values;
        }

        final Set<Integer>[] leftSignaturesAll = Arrays.stream(leftValues).map(v -> v.getSignatures()).toArray(size -> new Set[size]);
        final Set<Integer>[] rightSignaturesAll = Arrays.stream(rightValues).map(v -> v.getSignatures()).toArray(size -> new Set[size]);

        final Map<Integer, Tuple3<Integer, Integer, List<Integer>>> leastDifferences = new LinkedHashMap<>();
        Comparator<Tuple3<Integer, Integer, List<Integer>>> _comparator = Comparator.comparing(tuple -> tuple.getFirst());
        _comparator = _comparator.thenComparing(tuple -> tuple.getThird().size());
        List<Tuple2<Integer, Integer>> leftRightIndexPairs = new ArrayList<>();

        while (true) {
            for (int i = 0; i < leftSize; i++) {
                Set<Integer> leftSignatures = leftSignaturesAll[i];
                if(leftSignatures == null){
                    continue;
                }

                for (int j = 0; j < rightSize; j++) {
                    Set<Integer> rightSignatures = rightSignaturesAll[j];
                    if(rightSignatures == null){
                        continue;
                    }
                    Set<Integer> differences = SetHelper.symmetricDifference(leftSignatures, rightSignatures);
                    int differenceSize = differences.size();
                    if(differenceSize == 0){
                        leastDifferences.put(i, Tuple.create(0, i, Arrays.asList(j)));
                        leftSignaturesAll[i] = null;
                        rightSignaturesAll[j] = null;
                        break;
                    }

                    if(!leastDifferences.containsKey(i)){
                        List<Integer> list = new ArrayList<>();
                        list.add(j);
                        leastDifferences.put(i, Tuple.create(differenceSize, i, list));
                    } else {
                        Integer difSize = leastDifferences.get(i).getFirst();
                        if(difSize > differenceSize){
                            List<Integer> list = leastDifferences.get(i).getThird();
                            list.clear();
                            list.add(j);
                            leastDifferences.put(i, Tuple.create(differenceSize, i, list));
                        } else if (difSize == differenceSize) {
                            leastDifferences.get(i).getThird().add(j);
                        }
                    }
                }

                if(!leastDifferences.containsKey(i)){
                    List<Integer> rights = new ArrayList<>();
                    rights.add(null);
                    leastDifferences.put(i, Tuple.create(leftSignaturesAll[i].size(), i, rights));
                    leftSignaturesAll[i] = null;
                }
            }

            //Stop matching when there is no left values unmatched
            if(leastDifferences.isEmpty()){
                break;
            }

            Tuple3<Integer, Integer, List<Integer>>[] bestMatches = leastDifferences.values().stream().toArray(size -> new Tuple3[size]);
            Arrays.sort(bestMatches, _comparator);
            leastDifferences.clear();

            for (Tuple3<Integer, Integer, List<Integer>> tuple3 : bestMatches) {
                Integer index1 = tuple3.getSecond();
                if(tuple3.getFirst() == 0){
                    //The two matched elements with identical signatures shall be identical?
                    assertEquals(leftValues[index1], rightValues[tuple3.getThird().get(0)]);
                    leftRightIndexPairs.add(Tuple.create(index1, tuple3.getThird().get(0)));
                    continue;
                }
                List<Integer> index2List = tuple3.getThird();
                for (int i = 0; i < index2List.size(); i++) {
                    Integer index2 = index2List.get(i);
                    if(index2 == null){
                        leftSignaturesAll[index1] = null;
                        leftRightIndexPairs.add(Tuple.create(index1, -1));
                        break;
                    } else if(rightSignaturesAll[index2] != null){
                        leftSignaturesAll[index1] = null;
                        rightSignaturesAll[index2] = null;
                        leftRightIndexPairs.add(Tuple.create(index1, index2));
                        break;
                    }
                }
            }
        }

        assertTrue(leftRightIndexPairs.size() == leftSize,
                Arrays.stream(rightSignaturesAll).allMatch(s -> s == null),
                Arrays.stream(leftSignaturesAll).allMatch(s -> s == null));

        if(thisBigger) {
            leftRightIndexPairs.sort(Comparator.comparing(tuple2 -> tuple2.getFirst()));
        } else {
            leftRightIndexPairs.sort(Comparator.comparing(tuple2 -> (leftSize+tuple2.getSecond()) % leftSize));
        }

        List<IJSONValue> deltas = new ArrayList<>();
        Integer thisIndex, otherIndex;
        IJSONValue thisValue, otherValue;
        for (Tuple2<Integer, Integer> indexPair : leftRightIndexPairs) {
            if(thisBigger){
                thisIndex = indexPair.getFirst();
                thisValue = this.values[thisIndex];
                otherIndex = indexPair.getSecond();
                otherValue = otherIndex < 0 ? JSONValue.Null : otherArray.getValue(otherIndex);
            } else {
                thisIndex = indexPair.getSecond();
                thisValue = thisIndex < 0 ? JSONValue.Null : this.values[thisIndex];
                otherIndex = indexPair.getFirst();
                otherValue = otherArray.getValue(otherIndex);
            }
            IJSONValue delta = thisValue.deltaWith(otherValue, indexName).getSorted(nameComparator);
            if(!delta.isEmpty()){
                if(!StringUtils.isEmpty(indexName)) {
                    if(delta instanceof JSONArray){
                        Map<String, Object> map = new LinkedHashMap<>();
                        map.put(thisIndex.toString(), delta.getValue(0));
                        map.put(otherIndex.toString(), delta.getValue(1));
                        delta = Utilities.asJSONObject(map);
                    } else {
                        JSONObject indexObject = JSONObject.parse(String.format("{\"%s\":[%s, %s]}",
                                indexName.trim(),
                                thisIndex.toString(),
                                otherIndex.toString()));
                        delta = indexObject.withDelta((JSONObject)delta);
                    }
                }

                deltas.add(delta);
            } else if(includeDifferentIndexesPredicate.test(indexName) && thisIndex != otherIndex){
                JSONObject indexObject = JSONObject.parse(String.format("{\"%s\":[%s, %s]}",
                        indexName.trim(),
                        thisIndex.toString(),
                        otherIndex.toString()));
                deltas.add(indexObject);
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

    @Override
    public boolean isEmpty() {
        return values.length == 0;
    }

    //region Implementation of List<Object>

    @Override
    public Object get(int index) {
        if (index > values.length || index < 0)
            throw new IndexOutOfBoundsException("index: "+index + ", size: " + values.length);
        return values[index].getObject();
    }

    @Override
    public int indexOf(Object o) {
        return _getObjects().indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return _getObjects().lastIndexOf(o);
    }

    @Override
    public ListIterator<Object> listIterator() {
        return _getObjects().listIterator();
    }

    @Override
    public ListIterator<Object> listIterator(int index) {
        return _getObjects().listIterator(index);
    }

    @Override
    public List<Object> subList(int fromIndex, int toIndex) {
        return _getObjects().subList(fromIndex,toIndex);
    }

    @Override
    public int size() {
        return values.length;
    }

    @Override
    public boolean contains(Object o) {
        return _getObjects().contains(o);
    }

    @Override
    public Iterator<Object> iterator() {
        return _getObjects().iterator();
    }

    @Override
    public Object[] toArray() {
        return _getObjects().toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        List<Object> objects = _getObjects();
        int size = objects.size();
        if(a == null || a.length < size) {
            return (T[]) ArrayHelper.create(a.getClass().getComponentType(), size, i -> objects.get(i));
        } else {
            //When the given array is bigger, then implements as ArrayList.toArray(T[] a) by setting remaining elments to be nulls
            //Is that reasonable?
            ArrayHelper.setAll(a, i -> a[i] = (i<size) ? (T) values[i] : null);
            return a;
        }
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return _getObjects().containsAll(c);
    }

    @Override
    public Object set(int index, Object element) {
        throw new UnsupportedOperationException(JSONArray_UNMODIFIABLE);
    }

    @Override
    public void add(int index, Object element) {
        throw new UnsupportedOperationException(JSONArray_UNMODIFIABLE);
    }

    @Override
    public Object remove(int index) {
        throw new UnsupportedOperationException(JSONArray_UNMODIFIABLE);
    }

    @Override
    public boolean add(Object object) {
        throw new UnsupportedOperationException(JSONArray_UNMODIFIABLE);
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException(JSONArray_UNMODIFIABLE);
    }

    @Override
    public boolean addAll(Collection<?> c) {
        throw new UnsupportedOperationException(JSONArray_UNMODIFIABLE);
    }

    @Override
    public boolean addAll(int index, Collection<?> c) {
        throw new UnsupportedOperationException(JSONArray_UNMODIFIABLE);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException(JSONArray_UNMODIFIABLE);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException(JSONArray_UNMODIFIABLE);
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException(JSONArray_UNMODIFIABLE);
    }
    //endregion
}

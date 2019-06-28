package jsonTuples;

import io.github.cruisoring.tuple.Tuple;
import io.github.cruisoring.tuple.Tuple2;
import io.github.cruisoring.tuple.Tuple3;
import io.github.cruisoring.utility.ArrayHelper;
import io.github.cruisoring.utility.ReadOnlyList;
import io.github.cruisoring.utility.SetHelper;
import io.github.cruisoring.utility.SimpleTypedList;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.TextStringBuilder;

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static io.github.cruisoring.Asserts.*;
import static jsonTuples.Parser.*;

/**
 * An ordered collection of {@code IJSONValue}. An array begins with [ (left bracket) and ends with ]
 * (right bracket). Values are separated by , (comma).
 * @see <a href="http://www.json.org">http://www.json.org</a>
 */
public class JSONArray extends Tuple<IJSONValue> implements IJSONValue<IJSONValue>, List<Object> {

    static final String JSONArray_UNMODIFIABLE = "JSONArray instance is not modifiable, asMutableObject() would return a modifiable List of the underlying values that can be modified and then convert back to another JSONArray instance.";

    //Name of the index pair of the elements to show their deltas
    public static String defaultIndexName = "";

    //region Constants
    //Predicate to evaluate the indexName of deltaWith()
    public static final Predicate<String> includeDifferentIndexesPredicate = name -> name != null && name.contains("+");

    public static final JSONArray EMPTY = new JSONArray();

    //Pattern of string to represent a solid JSON Array
    public static final Pattern JSON_ARRAY_PATTERN = Pattern.compile("^\\[[\\s\\S]*?\\]$", Pattern.MULTILINE);
    //endregion

    //region Short-hand parse methods to get JSONArray from String
    /**
     * Assuming the concerned valueString is of array, parse it as a {@code JSONArray} with given nameComparator.
     *
     * @param comparator    the name comparator used to sort the {@code IJSONValue} with fixed orders.
     * @param valueString   text to be parsed that shall begins with [(left bracket) and ends with ](right bracket).
     * @return      a {@code JSONArray} instance from the given text.
     */
    public static JSONArray parse(Comparator<String> comparator, String valueString) {
        return (JSONArray) Parser.parse(comparator, valueString);
    }

    /**
     * Assuming the concerned valueString is of array, parse it as a {@code JSONArray} with default nameComparator.
     *
     * @param valueString   text to be parsed that shall begins with [(left bracket) and ends with ](right bracket).
     * @return      a {@code JSONArray} instance from the given text.
     */
    public static JSONArray parse(String valueString) {
        return (JSONArray) Parser.parse(valueString);
    }

    /**
     * Assuming the concerned valueString is of array, parse it as a {@code JSONArray} with default nameComparator.
     *
     * @param isStrictly    indicate if strict rules shall be applied for parsing
     * @param valueString   text to be parsed that shall begins with [(left bracket) and ends with ](right bracket).
     * @return      a {@code JSONArray} instance from the given text.
     */
    public static JSONArray parse(boolean isStrictly, String valueString) {
        return (JSONArray) Parser.parse(isStrictly, valueString);
    }
    //endregion

    //region static methods to assist comparing two JSONArrays
    private static List<Tuple2<Integer, Integer>> matchElementWithIndexes(JSONArray leftArray, JSONArray rightArray) {
        assertAllFalse(leftArray == null, rightArray == null);

        final boolean leftBigger = leftArray.size() >= rightArray.size();
        final JSONArray left = leftBigger ? leftArray : rightArray;
        final JSONArray right = leftBigger ? rightArray : leftArray;
        final int leftSize = left.size();
        final int rightSize = right.size();

        //Keep the signatures of all elements of both JSONArray and also used as flags of element maching status,
        // nulls means the element at that position has been matched with element of the other JSONArray
        Tuple3<Set<Integer>[], Set<Integer>[], SimpleTypedList<Tuple2<Integer, Integer>>> signatures = getPairsOfSameSignatures(left, right);
        final Set<Integer>[] leftSignaturesAll = signatures.getFirst();
        final Set<Integer>[] rightSignaturesAll = signatures.getSecond();
        SimpleTypedList<Tuple2<Integer, Integer>> leftRightIndexPairs = signatures.getThird();

        //leastDifferences, key=leftIndex, value=[distanceToRight, leftIndex, all rightIndexes with this distance]
        final Map<Integer, Tuple3<Integer, Integer, List<Integer>>> leastDifferences = new HashMap<>();
        //differencesToRights: key=rightIndex, value= leftIndexes has leastDifferences with this rightIndex
        final Map<Integer, List<Integer>> differencesToRights = new LinkedHashMap<>();
        Comparator<Tuple3<Integer, Integer, List<Integer>>> _comparator = Comparator.comparing(Tuple3::getFirst);
        _comparator = _comparator.thenComparing(tuple -> tuple.getThird().size());
        do {
            int lastSize = leftRightIndexPairs.size();
            Integer[] rightIndexes = findDifferences(leftSignaturesAll, rightSignaturesAll, leastDifferences, differencesToRights);

            //Get the pairs whose element has only one matching element
            for (Integer rightIndex : rightIndexes) {
                List<Integer> leftIndexes = differencesToRights.get(rightIndex);
                if(leftIndexes.size()==1 && leastDifferences.get(leftIndexes.get(0)).getThird().size()==1) {
                    addIndexPair(leftIndexes.get(0), rightIndex,
                            leftRightIndexPairs, leftSignaturesAll, rightSignaturesAll, leastDifferences, differencesToRights);
                }
            }

            Tuple3<Integer, Integer, List<Integer>>[] bestMatches = leastDifferences.values().stream()
                    .sorted(_comparator)
                    .toArray(size -> new Tuple3[size]);

            for (int i = 0; i < bestMatches.length; i++) {
                Tuple3<Integer, Integer, List<Integer>> match = bestMatches[i];
                if(match.getThird().size() > 1 && addIndexPair(match.getSecond(), match.getThird().get(0),
                        leftRightIndexPairs, leftSignaturesAll, rightSignaturesAll, leastDifferences, differencesToRights)){
                    bestMatches[i] = null;
                }
            }

            boolean rightAllNull = Arrays.stream(rightSignaturesAll).allMatch(Objects::isNull);
            boolean leftAllNull = Arrays.stream(leftSignaturesAll).allMatch(Objects::isNull);
            if(leftAllNull && rightAllNull) {
                //Stop matching attempts when there is no new pair unmatched
                break;
            } else if (rightAllNull) {
                IntStream.range(0, leftSize).filter(i -> leftSignaturesAll[i] != null).forEach(
                        i -> {
                            leftRightIndexPairs.add(Tuple.create(i, -1));
                            leftSignaturesAll[i] = null;
                        }
                );
            } else if (leftAllNull) {   //Shall never happen?!
                throw new IllegalStateException("Right side shall be all nulls first!");
            } else if (lastSize == leftRightIndexPairs.size()){
                Map<Integer, Set<Integer>> useOfRightIndexes = differencesToRights.entrySet().stream()
                        .collect(Collectors.groupingBy(
                                e -> e.getValue().size(),
                                Collectors.mapping(Map.Entry::getKey, Collectors.toSet())
                        ));

                Integer[] useCounts = useOfRightIndexes.keySet().toArray(new Integer[0]);
                Arrays.sort(useCounts, Comparator.naturalOrder());
                Set<Integer> leastUsedRightIndexes = useOfRightIndexes.get(useCounts.length == 1 ? useCounts[0] : useCounts[1]);

                for (int i = 0; i < bestMatches.length; i++) {
                    Tuple3<Integer, Integer, List<Integer>> tuple3 = bestMatches[i];
                    Set<Integer> acceptableRightIndexes = SetHelper.intersection(leastUsedRightIndexes, SetHelper.asSet(tuple3.getThird()));
                    if(!acceptableRightIndexes.isEmpty()) {
                        Iterator<Integer> iterator = acceptableRightIndexes.iterator();
                        Integer rightIndex = iterator.next();
                        if(addIndexPair(tuple3.getSecond(), rightIndex, leftRightIndexPairs, leftSignaturesAll, rightSignaturesAll, leastDifferences, differencesToRights)) {
                            break;
                        }
                    }
                }

                assertFalse(lastSize == leftRightIndexPairs.size(), "No right Index would least references found in this round.");
            }

            leastDifferences.clear();
            differencesToRights.clear();
        } while(true);

        assertAllTrue(leftRightIndexPairs.size() == leftSize,
                Arrays.stream(rightSignaturesAll).allMatch(Objects::isNull),
                Arrays.stream(leftSignaturesAll).allMatch(Objects::isNull));

        if(leftBigger) {
            leftRightIndexPairs.sort(Comparator.comparing(Tuple2::getFirst));
            return leftRightIndexPairs;
        }

        Tuple2<Integer, Integer>[] reordered = leftRightIndexPairs.stream()
                .sorted(Comparator.comparing(Tuple2::getFirst))
                .map(tuple2 -> Tuple.create(tuple2.getSecond(), tuple2.getFirst()))
                .sorted(Comparator.comparing(tuple2 -> (rightSize+leftSize+tuple2.getFirst()) % (rightSize+leftSize)))
                .toArray(size -> new Tuple2[size]);
        return new SimpleTypedList<>(reordered);
    }

    private static Tuple3<Set<Integer>[], Set<Integer>[], SimpleTypedList<Tuple2<Integer, Integer>>> getPairsOfSameSignatures(JSONArray left, JSONArray right) {
        assertAllNotNull(left, right);

        final int leftSize = left.size();
        final int rightSize = right.size();
        final Set<Integer>[] leftSignaturesAll = (Set<Integer>[]) ArrayHelper.create(Set.class, leftSize, i -> left.values[i].getSignatures());
        final Set<Integer>[] rightSignaturesAll = (Set<Integer>[]) ArrayHelper.create(Set.class, rightSize, i -> right.values[i].getSignatures());

        SimpleTypedList<Tuple2<Integer, Integer>> leftRightIndexPairs = new SimpleTypedList<>();

        //Find the pairs with identical signatures
        for (int i = 0; i < leftSize; i++) {
            int leftHashcode = left.values[i].hashCode();
            Set<Integer> leftSignatures = leftSignaturesAll[i];
            for (int j = 0; j < rightSize; j++) {
                if(leftHashcode == right.values[j].hashCode()){
                    Set<Integer> rightSignatures = rightSignaturesAll[j];
                    if( rightSignatures != null && SetHelper.isSameAs(leftSignatures, rightSignatures)) {
                        leftSignaturesAll[i] = null;
                        rightSignaturesAll[j] = null;
                        leftRightIndexPairs.add(Tuple.create(i, j));
                        break;
                    }
                }
            }
        }
        return Tuple.create(leftSignaturesAll, rightSignaturesAll, leftRightIndexPairs);
    }

    private static Tuple3<Comparator<String>, JSONArray, JSONArray> getSortedWithSameComparotor(JSONArray array1, JSONArray array2){
        assertAllNotNull(array1, array2);

        //Sort both JSONArrays with the same Comparator
        if(array1.nameComparator == null && array2.nameComparator == null){
            Comparator<String> comparator = new OrdinalComparator<>();
            return Tuple.create(comparator, array1.getSorted(comparator), array2.getSorted(comparator));
        } else if (array1.nameComparator == null) {
            Comparator<String> comparator = array2.nameComparator;
            return Tuple.create(comparator, array1.getSorted(comparator), array2);
        } else {
            Comparator<String> comparator = array1.nameComparator;
            return Tuple.create(comparator, array1, array2.getSorted(comparator));
        }
    }

    private static boolean addIndexPair(Integer leftIndex, Integer rightIndex, List<Tuple2<Integer, Integer>> leftRightIndexPairs, Set<Integer>[] leftSignaturesAll, Set<Integer>[] rightSignaturesAll, Map<Integer, Tuple3<Integer, Integer, List<Integer>>> leastDifferences, Map<Integer, List<Integer>> differencesToRights) {
        if(leftSignaturesAll[leftIndex]!=null && rightSignaturesAll[rightIndex]!=null){
            leftRightIndexPairs.add(Tuple.create(leftIndex, rightIndex));
            leftSignaturesAll[leftIndex] = null;
            rightSignaturesAll[rightIndex] = null;
            leastDifferences.remove(leftIndex);
            differencesToRights.remove(rightIndex);
            return true;
        }
        return false;
    }

    private static Integer[] findDifferences(Set<Integer>[] leftSignaturesAll, Set<Integer>[] rightSignaturesAll,
                                             Map<Integer, Tuple3<Integer, Integer, List<Integer>>> leastDifferences, Map<Integer, List<Integer>> differencesToRights) {
        int leftSize = leftSignaturesAll.length;
        int rightSize = rightSignaturesAll.length;
        for (int i = 0; i < leftSize; i++) {
            Set<Integer> leftSignatures = leftSignaturesAll[i];
            if(leftSignatures != null) {
                for (int j = 0; j < rightSize; j++) {
                    Set<Integer> rightSignatures = rightSignaturesAll[j];
                    if (rightSignatures != null) {
                        Set<Integer> differences = SetHelper.symmetricDifference(leftSignatures, rightSignatures);
                        int differenceSize = differences.size();
                        if (!leastDifferences.containsKey(i)) {
                            leastDifferences.put(i, Tuple.create(differenceSize, i, new SimpleTypedList<>()));
                        }

                        Tuple3<Integer, Integer, List<Integer>> tuple3 = leastDifferences.get(i);
                        Integer difSize = tuple3.getFirst();
                        if (difSize < differenceSize) {
                            continue;
                        }

                        List<Integer> list = tuple3.getThird();
                        if (difSize > differenceSize) {
                            for (Integer rIndex : list) {
                                differencesToRights.get(rIndex).remove((Integer)i);
                            }
                            list.clear();
                            leastDifferences.put(i, Tuple.create(differenceSize, i, list));
                        }
                        list.add(j);

                        if (!differencesToRights.containsKey(j)) {
                            differencesToRights.put(j, new SimpleTypedList<>());
                        }
                        differencesToRights.get(j).add(i);
                    }
                }
            }
        }
        return differencesToRights.keySet().toArray(new Integer[0]);
    }
    //endregion

    //region Instance fields and constructors
    // the Comparator<String> used by this {@code JSONArray} to sort its children JSONObjects.
    final Comparator<String> nameComparator;

    protected List<Object> objects = null;

    protected JSONArray(IJSONValue... values) {
        super(IJSONValue.class, values);
        this.nameComparator = null;
    }

    protected JSONArray(Comparator<String> comparator, IJSONValue... values) {
        super(IJSONValue.class, values);
        this.nameComparator = comparator;
    }

    protected JSONArray(Comparator<String> comparator, List<IJSONable> values) {
        super(IJSONValue.class, values);
        nameComparator = comparator;
    }
    //endregion

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
            objects = new ReadOnlyList(() -> ArrayHelper.create(Object.class, values.length, i -> values[i].getObject()));
        }
        return objects;
    }

    @Override
    public Object asMutableObject() {
        Object[] mutables = (Object[]) ArrayHelper.create(Object.class, values.length, i -> values[i].asMutableObject());
        List<Object> list = new SimpleTypedList<>(mutables);
        return list;
    }

    @Override
    public int getLeafCount() {
        int count = 0;
        for (IJSONValue element : values) {
            count += element.getLeafCount();
        }
        return count;
    }

    @Override
    public String toJSONString(String indent) {
        assertAllTrue(StringUtils.isBlank(indent));

        int length = values.length;
        if (length == 0) {
            return "[]";
        } else if(indent == null) {
            String[] elementStrings = Arrays.stream(values).parallel().map(v -> v.toJSONString(null)).toArray(size -> new String[size]);
            return "[" + String.join(",", elementStrings) + "]";
        } else {
            TextStringBuilder sb = new TextStringBuilder();
            final String elementIndent = indent+SPACE;
            final String elementEnding = COMMA_NEWLINE + indent+SPACE;
            sb.append(LEFT_BRACKET + NEW_LINE + elementIndent);
            for (int i = 0; i < length - 1; i++) {
                sb.append(values[i].toJSONString(elementIndent) + elementEnding);
            }
            sb.append(values[length-1].toJSONString(elementIndent) + NEW_LINE + indent + RIGHT_BRACKET);
            return sb.toString();
        }
    }

    @Override
    public String toString() {
        return toJSONString("");
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

    private IJSONValue deltaWithOtherType(IJSONValue other) {
        if (other == null) {
            return new JSONArray(this, JSONValue.MISSING);
        } else if (other == this) {
            return EMPTY;
        } else if (!(other instanceof JSONArray)) {
            return new JSONArray(this, other);
        }
        return null;
    }

    @Override
    public IJSONValue deltaWith(IJSONValue other, String indexName) {
        IJSONValue result = deltaWithOtherType(other);
        if(result != null) {
            return result;
        }

        Tuple3<Comparator<String>, JSONArray, JSONArray> sortedArrays = getSortedWithSameComparotor(this, (JSONArray) other);
        final Comparator<String> sharedNameComparator = sortedArrays.getFirst();
        final JSONArray left = sortedArrays.getSecond();
        final JSONArray right = sortedArrays.getThird();

        if (indexName == null) {
            return left.asIndexedObject().deltaWith(right.asIndexedObject(), indexName);
        }

        List<Tuple2<Integer, Integer>> leftRightIndexPairs = matchElementWithIndexes(left, right);

        List<IJSONValue> deltas = new SimpleTypedList<>();
        Integer thisIndex, otherIndex;
        IJSONValue thisValue, otherValue;
        for (Tuple2<Integer, Integer> indexPair : leftRightIndexPairs) {
            thisIndex = indexPair.getFirst();
            thisValue = thisIndex < 0 ? JSONValue.Null : left.values[thisIndex];
            otherIndex = indexPair.getSecond();
            otherValue = otherIndex < 0 ? JSONValue.Null : right.getValue(otherIndex);
            IJSONValue delta = thisValue.deltaWith(otherValue, indexName).getSorted(sharedNameComparator);
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
                JSONObject indexObject = JSONObject.parse(sharedNameComparator, String.format("{\"%s\":[%s, %s]}",
                        indexName.trim(),
                        thisIndex.toString(),
                        otherIndex.toString()));
                deltas.add(indexObject);
            }
        }

        result = deltas.isEmpty() ? EMPTY : new JSONArray(left.nameComparator, deltas.toArray(new IJSONValue[deltas.size()]));
        return result;
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
        return _getObjects().toArray(a);
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

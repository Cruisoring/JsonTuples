package JsonTuples;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import io.github.cruisoring.tuple.Tuple;
import io.github.cruisoring.tuple.Tuple2;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Comparator.comparing;

public class Range extends Tuple2<Integer, Integer> {
    public static final long INFINITE_LENGTH = Long.MAX_VALUE;

    //Integer number reserved to represent negative infinity: −∞, shall not be used explicitly as argument to specify the below or upper bound
    public static final Integer NEGATIVE_INFINITY;
    //Integer number reserved to represent positive infinity: +∞, shall not be used explicitly as argument to specify the below or upper bound
    public static final Integer POSITIVE_INFINITY;

    public static final long POSITIVE_INFINITE_LONG;

    private static final int _RUN_PARALLEL = 100;

    static {
        //Set both infinities by default, or load from config file in future.
        NEGATIVE_INFINITY = Integer.MIN_VALUE;
        POSITIVE_INFINITY = Integer.MAX_VALUE;
        POSITIVE_INFINITE_LONG = new Long(POSITIVE_INFINITY).longValue();
    }

    //All integer numbers except the resevered Integer.MIN_VALUE and Integer.MAX_VALUE that represent −∞ and +∞ respectively
    public static final Range ALL_INT = new Range(NEGATIVE_INFINITY, POSITIVE_INFINITY);

    //Empty integer range
    public static final Range NONE = new Range(0, 0);

    /**
     * Check if the Range is contained by the indexes of bufferSize.
     * @param range     Range to be checked.
     * @param length    Length of the valid range [0, bufferSize)
     * @return          True if the range is contained by [0, bufferSize), otherwise False.
     */
    public static boolean isValidOfLength(Range range, Integer length) {
        checkState(length>=0);

        return range != null && range._start >= 0 && range._end <= length;
    }

    /**
     * Returns a range contains all indexes for a enumerable object with specific bufferSize.
     * @param length Length of the enumerable object, must be greater than or equal to 0.
     * @return  Range of the indexes.
     */
    public static Range indexesOfLength(int length) {
        checkState(length >= 0, "Length shall not be negative value: "+length);
        return length == 0 ? NONE : new Range(0, length);
    }

    /**
     * Returns a range that contains all values strictly greater than {@code startExclusive} and less than {@code endExclusive}
     * @param startExclusive    Value below the first one of the range, shall be greater than NEGATIVE_INFINITY+1.
     * @param endExclusive      Value greater than the last one of the range, shall be less than POSITIVE_INFINITY-1.
     * @return  An Range object between {@code startExclusive} and {@code endExclusive} exclusively.
     */
    public static Range open(int startExclusive, int endExclusive) {
        checkState(startExclusive > NEGATIVE_INFINITY+1, "To represent infinitive range below endEnclusive, use belowOpen(endExclusive)");
        checkState(endExclusive < POSITIVE_INFINITY-1, "To represent range above startExclusive, use aboveOpen(startExclusive)");
        return new Range(startExclusive+1, endExclusive);
    }

    /**
     * Returns a range that contains all values greater than or equal to {@code startInclusive} and less than or equal to {@code endInclusive}
     * @param startInclusive    First value of the defined range, shall be greater than NEGATIVE_INFINITY+1.
     * @param endInclusive      Last value of the defined range, shall be less than POSITIVE_INFINITY-1.
     * @return  An Range object between {@code startExclusive} and {@code endExclusive} inclusively.
     */
    public static Range closed(int startInclusive, int endInclusive) {
        checkState(startInclusive > NEGATIVE_INFINITY+1, "To represent infinitive range below endEnclusive, use belowClosed(endInclusive)");
        checkState(endInclusive < POSITIVE_INFINITY-1, "To represent range above startExclusive, use aboveClosed(startInclusive)");
        return new Range(startInclusive, endInclusive+1);
    }

    /**
     * Returns a range that contains all values greater than {@code startExclusive} and less than or equal to {@code endInclusive}
     * @param startExclusive    Value below the first one of the range, shall be greater than NEGATIVE_INFINITY+1.
     * @param endInclusive      Last value of the defined range, shall be less than POSITIVE_INFINITY-1.
     * @return  An Range object between {@code startExclusive} exclusive and {@code endInclusive} inclusive.
     */
    public static Range openClosed(int startExclusive, int endInclusive) {
        checkState(startExclusive > NEGATIVE_INFINITY+1, "To represent infinitive range below endEnclusive, use belowOpen(endExclusive)");
        checkState(endInclusive < POSITIVE_INFINITY-1, "To represent range above startExclusive, use aboveClosed(startInclusive)");
        return new Range(startExclusive+1, endInclusive+1);
    }

    /**
     * Returns a range that contains all values greater than or equal to {@code startInclusive} and less than {@code endExclusive}
     * @param startInclusive    First value of the defined range, shall be greater than NEGATIVE_INFINITY+1.
     * @param endExclusive      Value greater than the last one of the range, shall be less than POSITIVE_INFINITY-1.
     * @return  An Range object between {@code startInclusive} exclusive and {@code endExclusive} inclusive.
     */
    public static Range closedOpen(int startInclusive, int endExclusive) {
        checkState(startInclusive > NEGATIVE_INFINITY+1, "To represent infinitive range below endEnclusive, use belowClosed(endInclusive)");
        checkState(endExclusive < POSITIVE_INFINITY-1, "To represent range above startExclusive, use aboveOpen(startExclusive)");
        return new Range(startInclusive, endExclusive);
    }

    /**
     * Returns a range that contains all values greater than or equal to {@code startInclusive}
     * @param startInclusive    First value of the defined range, shall be greater than NEGATIVE_INFINITY+1.
     * @return  An Range object above {@code startInclusive} inclusive.
     */
    public static Range aboveClosed(int startInclusive) {
        return startInclusive+1 > NEGATIVE_INFINITY+1 ?
                new Range(startInclusive, POSITIVE_INFINITY) : ALL_INT;
    }

    /**
     * Returns a range that contains all values greater than {@code startExclusive}
     * @param startExclusive    Value below the first one of the range, shall be greater than NEGATIVE_INFINITY+1.
     * @return  An Range object above {@code startInclusive} exclusive.
     */
    public static Range aboveOpen(int startExclusive) {
        return startExclusive > NEGATIVE_INFINITY+1 ?
                new Range(startExclusive+1, POSITIVE_INFINITY) : ALL_INT;
    }

    /**
     * Returns a range that contains all values less than or equal to {@code endInclusive}
     * @param endInclusive      Last value of the defined range, shall be less than POSITIVE_INFINITY-1.
     * @return  An Range object below {@code endInclusive} inclusive.
     */
    public static Range belowClosed(int endInclusive) {
        return endInclusive < POSITIVE_INFINITY-1 ?
                new Range(NEGATIVE_INFINITY, endInclusive+1) : ALL_INT;
    }

    /**
     * Returns a range that contains all values less than {@code endExclusive}
     * @param endExclusive      Value greater than the last one of the range, shall be less than POSITIVE_INFINITY-1.
     * @return  An Range object below {@code endExclusive} exclusive.
     */
    public static Range belowOpen(int endExclusive) {
        return endExclusive < POSITIVE_INFINITY-1 ?
                new Range(NEGATIVE_INFINITY, endExclusive) : ALL_INT;
    }

    /**
     * Get subString of the concerned JSON text with its Range.
     * @param jsonText  All JSON text to be parsed.
     * @param range     Range of the subString within the jsonText.
     * @return          SubString specified by the given Range.
     */
    public static String subString(String jsonText, Range range) {
        checkState(StringUtils.isNotBlank(jsonText));
        checkState(Range.isValidOfLength(range, jsonText.length()));

        return jsonText.substring(range.getStartInclusive(), range.getEndExclusive());
    }

    /**
     * Get subSequence of the concerned CharSequence with its Range.
     * @param charSequence  CharSequence to get the subCharSequence from.
     * @param range     Range of the subSequence within the given CharSequence.
     * @return          subSequence specified by the given Range.
     */
    public static CharSequence subSequence(CharSequence charSequence, Range range) {
        checkNotNull(charSequence, range);

        return charSequence.subSequence(range.getStartInclusive(), range.getEndExclusive());
    }

    /**
     * Extract content of a JSON String object.
     * @param jsonText      All JSON Text to be parsed.
     * @param stringRange   Range of the JSON String object including leading and ending quotation marks '"".
     * @return              Unescaped content of the JSON String object returned as a String.
     */
    public static String getStringContent(String jsonText, Range stringRange) {
        checkState(StringUtils.isNotBlank(jsonText));
        checkNotNull(stringRange);
        checkState('"'== jsonText.charAt(stringRange.getStartInclusive()) && '"'== jsonText.charAt(stringRange.getEndInclusive()));

        String enclosedText = jsonText.substring(stringRange.getStartInclusive()+1, stringRange.getEndInclusive());
        return StringEscapeUtils.unescapeJson(enclosedText);
    }

    /**
     * Converting the indexes of wrapping characters in pairs as unmodifiable Range list.
     * @param indexes   List of the indexes that must be even.
     * @return      List of the ranges with even indexes as startInclusive, and odd indexes as endInclusive.
     */
    public static List<Range> indexesToRanges(Collection<Integer> indexes) {
        checkNotNull(indexes);
        checkState(indexes.size() % 2 == 0);

        return _indexesToRanges(indexes);
    }

    static List<Range> _indexesToRanges(Collection<Integer> indexes) {
        int size = indexes.size();

        Iterator<Integer> iterator = indexes.iterator();
        List<Range> ranges = new ArrayList<>();
        while (iterator.hasNext()) {
            Integer startIndex = iterator.next();
            Integer endIndex = iterator.next();
            Range range = Range.closed(startIndex, endIndex);
            ranges.add(range);
        }

        return Collections.unmodifiableList(ranges);
    }

    /**
     * Find the subList of the given index list within a specific range.
     *
     * @param allIndexes Indexes which would not contain the lower and upper end point of the range.
     * @param range      Range under concern.
     * @return Sublist of the given sorted index list within a specific range.
     */
    public static List<Integer> getIndexesInRange(List<Integer> allIndexes, Range range) {
        checkNotNull(allIndexes);
        checkNotNull(range);

        if (allIndexes.isEmpty()) {
            return new ArrayList<Integer>();
        }

        //Sort the indexes with nature order
        Collections.sort(allIndexes, Comparator.naturalOrder());

        return _getIndexesInRange(allIndexes, range);
    }

    private static List<Integer> _getIndexesInRange(List<Integer> allIndexes, Range range) {
        List<Integer> result = new ArrayList<>();

        int count = allIndexes.size();
        Integer lower = range.getStartInclusive();
        Integer upper = range.getEndInclusive();
        if (count == 0 || lower > allIndexes.get(count - 1) || upper < allIndexes.get(0)) {
            return result;
        }

        boolean belowRange = true;
        for (int i = 0; i < count; i++) {
            Integer index = allIndexes.get(i);

            if (belowRange) {
                if (index < lower)
                    continue;
                if (range.contains(index)) {
                    result.add(index);
                } else if (index > upper) {
                    return result;
                }
                belowRange = false;
            } else {
                if (range.contains(index)) {
                    result.add(index);
                } else if (index > upper) {
                    return result;
                }
            }
        }
        return result;
    }

    private static List<Range> getPairsWithValueRanges(Set<Range> nameRangeSet, Collection<Range> valueRanges, Set<Integer> indicatorIndexes) {
        List<Range> nvpRanges = new ArrayList<>();
        for (Range valueRange : valueRanges) {
            Range nameRange = nameRangeSet.stream()
                    .filter(scope -> scope.getEndInclusive() < valueRange.getStartInclusive())
                    .sorted(comparing(Range::getEndInclusive).reversed())
                    .findFirst().orElse(null);
            if (nameRange != null) {
                Range gapRange = valueRange.gapWith(nameRange);
                List<Integer> colonsWithin = indicatorIndexes.stream().filter(i -> gapRange.contains(i)).collect(Collectors.toList());
                checkState(colonsWithin.size() == 1,
                        String.format("Failed to get one single indictor between '%s' and '%s'", nameRange, valueRange));
                Range nameValueRange = nameRange.intersection(valueRange);
                nameRangeSet.remove(nameRange);
                indicatorIndexes.remove(colonsWithin.get(0));
                nvpRanges.add(nameValueRange);
            }
        }
        return nvpRanges;
    }

    private static List<Range> _getNamedValueRanges(Set<Range> nameRangeSet, Set<Integer> indicatorIndexes, List<Integer> sortedEnderIndexes) {

        List<Range> nvpRanges = new ArrayList<>();
        for (Integer joinerIndex : indicatorIndexes) {
            Range nameRange = nameRangeSet.stream()
                    .filter(r -> r.getEndInclusive() < joinerIndex)
                    .sorted(comparing(Range::getEndInclusive).reversed())
                    .findFirst().orElse(null);

            if (nameRange == null)
                checkNotNull(nameRange, "Failed to locate the name range right before COLON at " + joinerIndex);
            Integer endIndex = sortedEnderIndexes.stream()
                    .filter(i -> i > joinerIndex)
                    .sorted()
                    .findFirst().orElse(null);
            checkNotNull(endIndex, "Failed to find the end of value after COLON at " + joinerIndex);

            Range range = Range.closedOpen(nameRange.getStartInclusive(), endIndex);
            nvpRanges.add(range);
        }
        return nvpRanges;
    }



    /**
     * Converting the indexes of starts and indexes of ends in pairs as unmodifiable Range list.
     * @param startIndexes  Index list of the starting characters.
     * @param endIndexes    Index list of the ending characters.
     * @return      Unmodifiable list of the ranges with one of the index of starting character, and one of the index of the ending character.
     */
    public static List<Range> indexesToRanges(Collection<Integer> startIndexes, Collection<Integer> endIndexes) {
        checkNotNull(startIndexes);
        checkNotNull(endIndexes);

        int size = startIndexes.size();
        checkState(size == endIndexes.size());

        return _indexesToRanges(startIndexes, endIndexes);
    }

    static List<Range> _indexesToRanges(Collection<Integer> startIndexes, Collection<Integer> endIndexes) {
        TreeSet<Integer> sortedSet = new TreeSet<>(startIndexes);
        sortedSet.addAll(endIndexes);

        List<Range> ranges = new ArrayList<>();

        for (int i = startIndexes.size()-1; i>=0; i--) {
            Integer start = null;
            Integer end = null;
            for (Integer index : sortedSet) {
                if(start == null || startIndexes.contains(index)) {
                    start = index;
                } else {
                    end = index;
                    break;
                }
            }
            ranges.add(Range.closed(start, end));
            sortedSet.remove(start);
            sortedSet.remove(end);
        }

        return ranges;
    }

    public static List<Tuple2<Range, Range>> getRangePairs(Collection<Range> ranges1, Collection<Range> ranges2, Predicate<Tuple2<Range, Range>> predicate) {
        checkNotNull(ranges1);
        checkNotNull(ranges2);
        checkNotNull(predicate);

        int size1 = ranges1.size();
        int size2 = ranges2.size();
        int combinations = size1*size2;
        if(combinations == 0)
            return new ArrayList<>();

        Stream<Tuple2<Range, Range>> options = ranges1.stream()
                .flatMap(x -> ranges2.stream().map(y -> Tuple.create(x, y)));

        List<Tuple2<Range, Range>> result;
        if(combinations < _RUN_PARALLEL) {
            result = options.filter(predicate).collect(Collectors.toList());
        } else {
            result = options.parallel().filter(predicate).collect(Collectors.toList());
        }
        return result;
    }

    private final static Predicate<Tuple2<Range, Range>> overlapPredicate = tuple -> tuple.getFirst().overlaps(tuple.getSecond());
    public static List<Tuple2<Range, Range>> getOverlappedRangePairs(Collection<Range> ranges1, Collection<Range> ranges2) {
        return getRangePairs(ranges1, ranges2, overlapPredicate);
    }

    //Represent the size of the range in long, -1 when size is infinite
    private final long _size;
    private final int _start, _end;

    /**
     * Constructor of Range support limited scope specified by the start and end index.
     * @param startInclusive    StartIndex of the concerned scope which might be included in the scope.
     * @param endExclusive      EndIndex of the concerned scope that is above the last index of the scope.
     */
    protected Range(Integer startInclusive, Integer endExclusive) {
        super(checkNotNull(startInclusive), checkNotNull(endExclusive));

        checkState(startInclusive <= endExclusive,
                String.format("Range startInclusive %d shall not be greater or equal to endExclusive %d.", startInclusive, endExclusive));

        _start = startInclusive <= NEGATIVE_INFINITY+1 ? NEGATIVE_INFINITY : startInclusive;
        _end = endExclusive > POSITIVE_INFINITY-1 ? POSITIVE_INFINITY : endExclusive;
        if(_start == NEGATIVE_INFINITY || _end == POSITIVE_INFINITY)
            _size = INFINITE_LENGTH;
        else
            _size = new Long(_end) - new Long(_start);
    }

    /**
     * Get the size of the range as a long value, -1 when it is infinitive.
     * @return Count of values between _start (inclusive) and _end (exclusive), -1L when it is infinitive.
     */
    public long size() {
        return _size;
    }

    public boolean isEmpty() {
        return _size == 0;
    }

    public Stream<Integer> getStream() {
        if(_size == 0) {
            return Stream.empty();
        } else if (_start == NEGATIVE_INFINITY && _end == POSITIVE_INFINITY) {
            throw new IllegalStateException("Cannot get stream for all integers");
        } else if (_size != INFINITE_LENGTH) {
            return IntStream.range(_start, _end).boxed();
        } else if (_start == NEGATIVE_INFINITY) {
            return IntStream.iterate(_end-1, i -> i-1).boxed();
        } else if (_end == POSITIVE_INFINITY) {
            return IntStream.iterate(_start, i -> i+1).boxed();
        } else {
            throw new IllegalStateException(String.format("Failed to define the process when _start=%d and _end=%d.", _start, _end));
        }
    }

    public boolean contains(int value) {
        return _start <= value && value < _end;
    }

    public boolean containsAll(List<Integer> values) {
        if(_size == 0) {
            return false;
        } else if (Iterables.isEmpty(values)) {
            return true;
        }

        Collections.sort(values);
        return contains(values.get(0)) && (values.size() == 0 || contains(values.get(values.size()-1)));
    }

    public boolean containsAll(int... values) {
        if(_size == 0) {
            return false;
        } else if (values.length == 0) {
            return true;
        }

        for (int value :
                values) {
            if(!contains(value))
                return false;
        }
        return true;
    }


    public boolean contains(Range other) {
        checkNotNull(other);
        return this._start <= other._start && this._end >= other._end;
    }

    public boolean isConnected(Range other) {
        checkNotNull(other);
        return this._start <= other._end && other._start <= this._end;
    }

    public boolean overlaps(Range other) {
        checkNotNull(other);

        return (_start<other._start &&_end >other._start && _end<other._end)
                || (_start>other._start && _start < other._end && _end>other._end);
    }

    public Range intersection(Range other) {
        checkNotNull(other);

        int minStart = Math.min(_start, other._start);
        int maxEnd = Math.max(_end, other._end);

        if(minStart == NEGATIVE_INFINITY && maxEnd == POSITIVE_INFINITY) {
            return ALL_INT;
        } else if (minStart == _start && maxEnd == _end) {
            return this;
        } else if (minStart == other._start && maxEnd == other._end) {
            return other;
        } else {
            return new Range(minStart, maxEnd);
        }
    }

    public Range gapWith(Range other) {
        checkNotNull(other);

        if(_start == other._start || _end == other._end || _start==other._end || _end==other._start) {
            return NONE;
        } else if (_start < other._start) {
            return _end >= other._start ? NONE : closedOpen(_end, other._start);
        } else {
            return other._end >= _start ? NONE : closedOpen(other._end, _start);
        }
    }

    public int getStartInclusive() {
        return _start == NEGATIVE_INFINITY ? NEGATIVE_INFINITY : _start;
    }

    public int getStartExclusive() {
        return _start == NEGATIVE_INFINITY ? NEGATIVE_INFINITY : _start -1;
    }

    public int getEndInclusive() {
        return _end == POSITIVE_INFINITY ? POSITIVE_INFINITY : _end-1;
    }

    public int getEndExclusive() {
        return _end == POSITIVE_INFINITY ? POSITIVE_INFINITY : _end;
    }

    /**
     * Get the direct children Ranges as a list.
     * @param ranges    Sorted Ranges instances with no one overlaps with another to be evaluated.
     * @return      All Range instances contained by this Range only.
     */
    public List<Range> getChildRanges(TreeSet<Range> ranges) {
        List<Range> children = new ArrayList<>();
        Range lastChild = null;
        for (Range range : ranges) {
            if(_end < range._start) {
                return children;
            } else if( equals(range)) {
                continue;
            } else if (contains(range)) {
                if(lastChild == null || !lastChild.contains(range)) {
                    children.add(range);
                    lastChild = range;
                }
            }
        }
        return children;
    }

    /**
     * Get the indexes within this Range as a list.
     * @param indexes    Sorted indexes to be evaluated.
     * @return      All indexes fall into this Range but not on the boundries as a list.
     */
    public List<Integer> getWithinIndexes(TreeSet<Integer> indexes) {
        SortedSet<Integer> withinSet = indexes.subSet(_start+1, _end-1);
        List<Integer> children = new ArrayList<>(withinSet);
        return children;
    }

    public Range getInside(){
        if(_size <= 2) {
            return NONE;
        } else {
            return new Range(_start+1, _end-1);
        }
    }

    @Override
    public int compareTo(Tuple o) {
        if(o == null) {
            //Range always greater than null
            return 1;
        } else if(! (o instanceof Range)) {
            //Use String representations to do comparison
            return this.toString().compareTo(o.toString());
        }

        Range other = (Range) o;
        if(_start == other._start){
            //Keep the largest one first
            return other._end - _end;
        } else {
            //Or keep the leftest one first
            return _start - other._start;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null || !(obj instanceof Range))
            return false;
        if (obj == this)
            return true;
        Range other = (Range) obj;
        return _size == other._size && _start == other._start;
    }

    @Override
    public boolean canEqual(Object obj) {
        return obj instanceof Range;
    }

    @Override
    public String toString() {
        return String.format("%s, %s",
                _start <= NEGATIVE_INFINITY+1 ? "(−∞" : String.format("[%d", _start),
                _end > POSITIVE_INFINITY-1 ? "+∞)" : String.format("%d)", _end));
    }
}
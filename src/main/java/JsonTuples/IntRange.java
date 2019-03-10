package JsonTuples;

import com.google.common.collect.Iterables;
import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;
import io.github.cruisoring.tuple.Tuple;
import io.github.cruisoring.tuple.Tuple2;

import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public class IntRange extends Tuple2<Integer, Integer> {
    public static final long INFINITE_LENGTH = -1;

    //Integer number reserved to represent negative infinity: −∞, shall not be used explicitly as argument to specify the below or upper bound
    public static final Integer NEGATIVE_INFINITY;
    //Integer number reserved to represent positive infinity: +∞, shall not be used explicitly as argument to specify the below or upper bound
    public static final Integer POSITIVE_INFINITY;

    public static final long POSITIVE_INFINITE_LONG;

    static {
        //Set both infinities by default, or load from config file in future.
        NEGATIVE_INFINITY = Integer.MIN_VALUE;
        POSITIVE_INFINITY = Integer.MAX_VALUE;
        POSITIVE_INFINITE_LONG = new Long(POSITIVE_INFINITY).longValue();
    }

    //All integer numbers except the resevered Integer.MIN_VALUE and Integer.MAX_VALUE that represent −∞ and +∞ respectively
    public static final IntRange ALL_INT = new IntRange(NEGATIVE_INFINITY, POSITIVE_INFINITY);

    //Empty integer range
    public static final IntRange NONE = new IntRange(0, 0);

    /**
     * Returns a range contains all indexes for a enumerable object with specific length.
     * @param length Length of the enumerable object, must be greater than or equal to 0.
     * @return  Range of the indexes.
     */
    public static IntRange indexesOfLength(int length) {
        checkState(length >= 0, "Length shall not be negative value: "+length);
        return length == 0 ? NONE : new IntRange(0, length);
    }

    /**
     * Returns a range that contains all values strictly greater than {@code startExclusive} and less than {@code endExclusive}
     * @param startExclusive    Value below the first one of the range, shall be greater than NEGATIVE_INFINITY+1.
     * @param endExclusive      Value greater than the last one of the range, shall be less than POSITIVE_INFINITY-1.
     * @return  An IntRange object between {@code startExclusive} and {@code endExclusive} exclusively.
     */
    public static IntRange open(int startExclusive, int endExclusive) {
        checkState(startExclusive > NEGATIVE_INFINITY+1, "To represent infinitive range below endEnclusive, use belowOpen(endExclusive)");
        checkState(endExclusive < POSITIVE_INFINITY-1, "To represent range above startExclusive, use aboveOpen(startExclusive)");
        return new IntRange(startExclusive+1, endExclusive);
    }

    /**
     * Returns a range that contains all values greater than or equal to {@code startInclusive} and less than or equal to {@code endInclusive}
     * @param startInclusive    First value of the defined range, shall be greater than NEGATIVE_INFINITY+1.
     * @param endInclusive      Last value of the defined range, shall be less than POSITIVE_INFINITY-1.
     * @return  An IntRange object between {@code startExclusive} and {@code endExclusive} inclusively.
     */
    public static IntRange closed(int startInclusive, int endInclusive) {
        checkState(startInclusive > NEGATIVE_INFINITY+1, "To represent infinitive range below endEnclusive, use belowClosed(endInclusive)");
        checkState(endInclusive < POSITIVE_INFINITY-1, "To represent range above startExclusive, use aboveClosed(startInclusive)");
        return new IntRange(startInclusive, endInclusive+1);
    }

    /**
     * Returns a range that contains all values greater than {@code startExclusive} and less than or equal to {@code endInclusive}
     * @param startExclusive    Value below the first one of the range, shall be greater than NEGATIVE_INFINITY+1.
     * @param endInclusive      Last value of the defined range, shall be less than POSITIVE_INFINITY-1.
     * @return  An IntRange object between {@code startExclusive} exclusive and {@code endInclusive} inclusive.
     */
    public static IntRange openClosed(int startExclusive, int endInclusive) {
        checkState(startExclusive > NEGATIVE_INFINITY+1, "To represent infinitive range below endEnclusive, use belowOpen(endExclusive)");
        checkState(endInclusive < POSITIVE_INFINITY-1, "To represent range above startExclusive, use aboveClosed(startInclusive)");
        return new IntRange(startExclusive+1, endInclusive+1);
    }

    /**
     * Returns a range that contains all values greater than or equal to {@code startInclusive} and less than {@code endExclusive}
     * @param startInclusive    First value of the defined range, shall be greater than NEGATIVE_INFINITY+1.
     * @param endExclusive      Value greater than the last one of the range, shall be less than POSITIVE_INFINITY-1.
     * @return  An IntRange object between {@code startInclusive} exclusive and {@code endExclusive} inclusive.
     */
    public static IntRange closedOpen(int startInclusive, int endExclusive) {
        checkState(startInclusive > NEGATIVE_INFINITY+1, "To represent infinitive range below endEnclusive, use belowClosed(endInclusive)");
        checkState(endExclusive < POSITIVE_INFINITY-1, "To represent range above startExclusive, use aboveOpen(startExclusive)");
        return new IntRange(startInclusive, endExclusive);
    }

    /**
     * Returns a range that contains all values greater than or equal to {@code startInclusive}
     * @param startInclusive    First value of the defined range, shall be greater than NEGATIVE_INFINITY+1.
     * @return  An IntRange object above {@code startInclusive} inclusive.
     */
    public static IntRange aboveClosed(int startInclusive) {
        return startInclusive+1 > NEGATIVE_INFINITY+1 ?
                new IntRange(startInclusive, POSITIVE_INFINITY) : ALL_INT;
    }

    /**
     * Returns a range that contains all values greater than {@code startExclusive}
     * @param startExclusive    Value below the first one of the range, shall be greater than NEGATIVE_INFINITY+1.
     * @return  An IntRange object above {@code startInclusive} exclusive.
     */
    public static IntRange aboveOpen(int startExclusive) {
         return startExclusive > NEGATIVE_INFINITY+1 ?
                 new IntRange(startExclusive+1, POSITIVE_INFINITY) : ALL_INT;
    }

    /**
     * Returns a range that contains all values less than or equal to {@code endInclusive}
     * @param endInclusive      Last value of the defined range, shall be less than POSITIVE_INFINITY-1.
     * @return  An IntRange object below {@code endInclusive} inclusive.
     */
    public static IntRange belowClosed(int endInclusive) {
        return endInclusive < POSITIVE_INFINITY-1 ?
                new IntRange(NEGATIVE_INFINITY, endInclusive+1) : ALL_INT;
    }

    /**
     * Returns a range that contains all values less than {@code endExclusive}
     * @param endExclusive      Value greater than the last one of the range, shall be less than POSITIVE_INFINITY-1.
     * @return  An IntRange object below {@code endExclusive} exclusive.
     */
    public static IntRange belowOpen(int endExclusive) {
        return endExclusive < POSITIVE_INFINITY-1 ?
                new IntRange(NEGATIVE_INFINITY, endExclusive) : ALL_INT;
    }

    //Represent the size of the range in long, -1 when size is infinite
    private final long _size;
    private final int _start, _end;

    /**
     * Constructor of IntRange support limited scope specified by the start and end index.
     * @param startInclusive    StartIndex of the concerned scope which might be included in the scope.
     * @param endExclusive      EndIndex of the concerned scope that is above the last index of the scope.
     */
    protected IntRange(Integer startInclusive, Integer endExclusive) {
        super(checkNotNull(startInclusive), checkNotNull(endExclusive));

        checkState(startInclusive <= endExclusive,
                String.format("IntRange startInclusive %d shall not be greater or equal to endExclusive %d.", startInclusive, endExclusive));

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


    public boolean contains(IntRange other) {
        checkNotNull(other);
        return this._start <= other._start && this._end >= other._end;
    }

    public boolean isConnected(IntRange other) {
        checkNotNull(other);
        return this._start <= other._end && other._start <= this._end;
    }

    public IntRange intersection(IntRange other) {
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
            return new IntRange(minStart, maxEnd);
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

    @Override
    public int compareTo(Tuple o) {
        if(o == null) {
            //IntRange always greater than null
            return 1;
        } else if(! (o instanceof IntRange)) {
            //Use String representations to do comparison
            return this.toString().compareTo(o.toString());
        }

        IntRange other = (IntRange) o;
        if(_start == other._start){
            return _end - other._end;
        } else {
            return _start - other._start;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null || !(obj instanceof IntRange))
            return false;
        if (obj == this)
            return true;
        IntRange other = (IntRange) obj;
        return _size == other._size && _start == other._start;
    }

    @Override
    public boolean canEqual(Object obj) {
        return obj instanceof IntRange;
    }

    @Override
    public String toString() {
        return String.format("%s, %s",
                _start <= NEGATIVE_INFINITY+1 ? "(−∞" : String.format("[%d", _start),
                _end > POSITIVE_INFINITY-1 ? "+∞)" : String.format("%d)", _end));
    }
}

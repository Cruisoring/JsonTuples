package JsonTuples;

import io.github.cruisoring.tuple.Tuple;
import io.github.cruisoring.tuple.Tuple2;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public class IntRange extends Tuple2<Integer, Integer> {
    public static final Integer MIN_INT = Integer.MIN_VALUE;
    public static final Integer MAX_INT = Integer.MAX_VALUE;

    public static final IntRange ALL_INT = new IntRange(MIN_INT, MAX_INT);
    public static final IntRange NO_INT = new IntRange(0, 0);

    public static IntRange open(int startExclusive, int endExclusive) {
        return new IntRange(startExclusive+1, endExclusive);
    }

    public static IntRange closed(int startInclusive, int endInclusive) {
        return new IntRange(startInclusive, endInclusive+1);
    }

    public static IntRange openClosed(int startExclusive, int endInclusive) {
        return new IntRange(startExclusive+1, endInclusive+1);
    }

    public static IntRange closedOpen(int startInclusive, int endExclusive) {
        return new IntRange(startInclusive, endExclusive);
    }

    public static IntRange fromClosed(int startInclusive) {
        return new IntRange(startInclusive, MAX_INT);
    }

    public static IntRange fromOpen(int startExclusive) {
        return new IntRange(startExclusive+1, MAX_INT);
    }

    public static IntRange toClosed(int endInclusive) {
        return MAX_INT.intValue()==endInclusive ? ALL_INT : new IntRange(MIN_INT, endInclusive+1);
    }

    public static IntRange toOpen(int endExclusive) {
        return new IntRange(MIN_INT, endExclusive);
    }

    //Start, End and Length of the IntRange for easier processing
    private final int _length;
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

        _length = endExclusive-startInclusive-1;
        _start = startInclusive;
        _end = endExclusive;
    }

    public int length() {
        return _length;
    }

    public boolean isEmpty() {
        return _length == 0;
    }

    public Stream<Integer> getStream() {
        if(_length == 0) {
            return Stream.empty();
        }

        return IntStream.iterate(_start, i -> i+1).boxed();
    }

    public boolean contains(int index) {
        return _start <= index && index < _end;
    }

    public boolean encloses(IntRange other) {
        checkNotNull(other);
        return this._start <= other._start && this._end >= other._end;
    }

    public boolean isConnected(IntRange other) {
        checkNotNull(other);
        return this._start <= other._end && other._start <= this._end;
    }

    public IntRange intersection(IntRange other) {
        checkNotNull(other);

        int lowerDif = this._start - other._start;
        int upperDif = this._end - other._end;
        if(lowerDif >= 0 && upperDif <= 0) {
            return this;
        } else if (lowerDif <= 0 && upperDif >= 0) {
            return other;
        } else {
            return closedOpen(lowerDif<0 ? _start : other._start, upperDif < 0 ? _end : other._end);
        }
    }

    public int getStartInclusive() {
        return _start;
    }

    public int getStartExclusive() {
        return _start -1;
    }

    public int getEndInclusive() {
        return _end-1;
    }

    public int getEndExclusive() {
        return _end;
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
        return _length == other._length && _start == other._start;
    }

    @Override
    public boolean canEqual(Object obj) {
        return obj instanceof IntRange;
    }

    @Override
    public String toString() {
        if(_length == 0)
            return "[)";
        return String.format("[%d, %d)", _start, _end);
    }
}

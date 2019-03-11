package JsonTuples;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class RangeTest {

    @Test
    public void testWHOLE() {
        assertEquals(Range.NEGATIVE_INFINITY.intValue(), Range.ALL_INT.getStartInclusive());
        assertEquals(Range.POSITIVE_INFINITY.intValue(), Range.ALL_INT.getEndInclusive());
        assertEquals("(−∞, +∞)", Range.ALL_INT.toString());

        assertEquals(Range.INFINITE_LENGTH, Range.ALL_INT.size());

//        //java.lang.IllegalStateException: Cannot get stream for all integers
//        Integer first = Range.ALL_INT.getStream().findFirst().orElse(null);
    }

    @Test
    public void indexesOfLength() {
        Range range;
        range = Range.indexesOfLength(0);
        assertEquals(Range.NONE, range);
        assertEquals("[0, 0)", range.toString());
        assertEquals(0, range.size());
        assertEquals(0, range.getStartInclusive());
        assertEquals(-1, range.getStartExclusive());
        assertEquals(-1, range.getEndInclusive());
        assertEquals(0, range.getEndExclusive());
        assertEquals(Integer.valueOf(100), range.getStream().findFirst().orElse(100));

        range = Range.indexesOfLength(1);
        assertEquals("[0, 1)", range.toString());
        assertEquals(1, range.size());
        assertEquals(0, range.getStartInclusive());
        assertEquals(-1, range.getStartExclusive());
        assertEquals(0, range.getEndInclusive());
        assertEquals(1, range.getEndExclusive());
        assertEquals(Arrays.asList(0), range.getStream().collect(Collectors.toList()));

        range = Range.indexesOfLength(3);
        assertEquals("[0, 3)", range.toString());
        assertEquals(3, range.size());
        assertEquals(0, range.getStartInclusive());
        assertEquals(-1, range.getStartExclusive());
        assertEquals(2, range.getEndInclusive());
        assertEquals(3, range.getEndExclusive());
        assertEquals(Arrays.asList(0, 1, 2), range.getStream().collect(Collectors.toList()));

        //java.lang.IllegalStateException: Length shall not be negative value: -1
        //range = Range.indexesOfLength(-1);
    }

    @Test
    public void open() {
        Range range;
        range = Range.open(3, 8);
        assertEquals("[4, 8)", range.toString());
        assertEquals(Arrays.asList(4, 5, 6, 7), range.getStream().collect(Collectors.toList()));
        assertEquals(4, range.size());
        assertEquals(4, range.getStartInclusive());
        assertEquals(3, range.getStartExclusive());
        assertEquals(7, range.getEndInclusive());
        assertEquals(8, range.getEndExclusive());

        range = Range.open(3, Range.POSITIVE_INFINITY-2);
        assertEquals("[4, 2147483645)", range.toString());
        assertEquals(Arrays.asList(4, 5, 6, 7), range.getStream().limit(4).collect(Collectors.toList()));
        assertEquals(Range.POSITIVE_INFINITY-6, range.size());
        assertEquals(4, range.getStartInclusive());
        assertEquals(3, range.getStartExclusive());
        assertEquals(Range.POSITIVE_INFINITY-3, range.getEndInclusive());
        assertEquals(Range.POSITIVE_INFINITY-2, range.getEndExclusive());
    }

    @Test
    public void closed() {
        Range range;
        range = Range.closed(3, 8);
        assertEquals("[3, 9)", range.toString());
        assertEquals(Arrays.asList(3, 4, 5, 6, 7, 8), range.getStream().collect(Collectors.toList()));
        assertEquals(6, range.size());
        assertEquals(3, range.getStartInclusive());
        assertEquals(2, range.getStartExclusive());
        assertEquals(8, range.getEndInclusive());
        assertEquals(9, range.getEndExclusive());

        range = Range.closed(3, Range.POSITIVE_INFINITY-2);
        assertEquals("[3, 2147483646)", range.toString());
        assertEquals(Range.POSITIVE_INFINITY-4, range.size());
        assertEquals(3, range.getStartInclusive());
        assertEquals(2, range.getStartExclusive());
        assertEquals(Range.POSITIVE_INFINITY-2, range.getEndInclusive());
        assertEquals(Range.POSITIVE_INFINITY-1, range.getEndExclusive());
    }

    @Test
    public void openClosed() {
        Range range;
        range = Range.openClosed(3, 8);
        assertEquals("[4, 9)", range.toString());
        assertEquals(Arrays.asList(4, 5, 6, 7, 8), range.getStream().collect(Collectors.toList()));
        assertEquals(5, range.size());
        assertEquals(4, range.getStartInclusive());
        assertEquals(3, range.getStartExclusive());
        assertEquals(8, range.getEndInclusive());
        assertEquals(9, range.getEndExclusive());

        range = Range.openClosed(3, Range.POSITIVE_INFINITY-2);
        assertEquals("[4, 2147483646)", range.toString());
        assertEquals(Range.POSITIVE_INFINITY-5, range.size());
        assertEquals(4, range.getStartInclusive());
        assertEquals(3, range.getStartExclusive());
        assertEquals(Range.POSITIVE_INFINITY-2, range.getEndInclusive());
        assertEquals(Range.POSITIVE_INFINITY-1, range.getEndExclusive());
    }

    @Test
    public void closedOpen() {
        Range range;
        range = Range.closedOpen(3, 8);
        assertEquals("[3, 8)", range.toString());
        assertEquals(Arrays.asList(3, 4, 5, 6, 7), range.getStream().collect(Collectors.toList()));
        assertEquals(5, range.size());
        assertEquals(3, range.getStartInclusive());
        assertEquals(2, range.getStartExclusive());
        assertEquals(7, range.getEndInclusive());
        assertEquals(8, range.getEndExclusive());

        range = Range.closedOpen(3, Range.POSITIVE_INFINITY-2);
        assertEquals("[3, 2147483645)", range.toString());
        assertEquals(Range.POSITIVE_INFINITY-5, range.size());
        assertEquals(3, range.getStartInclusive());
        assertEquals(2, range.getStartExclusive());
        assertEquals(Range.POSITIVE_INFINITY-3, range.getEndInclusive());
        assertEquals(Range.POSITIVE_INFINITY-2, range.getEndExclusive());
    }

    @Test
    public void aboveClosed() {
        Range range;
        range = Range.aboveClosed(-1);
        assertEquals("[-1, +∞)", range.toString());
        assertEquals(Arrays.asList(-1, 0, 1, 2, 3), range.getStream().limit(5).collect(Collectors.toList()));
        assertEquals(Range.INFINITE_LENGTH, range.size());
        assertEquals(-1, range.getStartInclusive());
        assertEquals(-2, range.getStartExclusive());
        assertEquals(Range.POSITIVE_INFINITY.intValue(), range.getEndInclusive());
        assertEquals(Range.POSITIVE_INFINITY.intValue(), range.getEndExclusive());

        range = Range.aboveClosed(3);
        assertEquals("[3, +∞)", range.toString());
        assertEquals(Arrays.asList(3, 4, 5, 6, 7), range.getStream().limit(5).collect(Collectors.toList()));
        assertEquals(Range.INFINITE_LENGTH, range.size());
        assertEquals(3, range.getStartInclusive());
        assertEquals(2, range.getStartExclusive());
        assertEquals(Range.POSITIVE_INFINITY.intValue(), range.getEndInclusive());
        assertEquals(Range.POSITIVE_INFINITY.intValue(), range.getEndExclusive());
    }

    @Test
    public void aboveOpen() {
        Range range;
        range = Range.aboveOpen(-2);
        assertEquals("[-1, +∞)", range.toString());
        assertEquals(Range.INFINITE_LENGTH, range.size());
        assertEquals(-1, range.getStartInclusive());
        assertEquals(-2, range.getStartExclusive());
        assertEquals(Range.POSITIVE_INFINITY.intValue(), range.getEndInclusive());
        assertEquals(Range.POSITIVE_INFINITY.intValue(), range.getEndExclusive());

        range = Range.aboveOpen(2);
        assertEquals("[3, +∞)", range.toString());
        assertEquals(Range.INFINITE_LENGTH, range.size());
        assertEquals(3, range.getStartInclusive());
        assertEquals(2, range.getStartExclusive());
        assertEquals(Range.POSITIVE_INFINITY.intValue(), range.getEndInclusive());
        assertEquals(Range.POSITIVE_INFINITY.intValue(), range.getEndExclusive());
    }

    @Test
    public void belowClosed() {
        Range range;
        range = Range.belowClosed(-1);
        assertEquals("(−∞, 0)", range.toString());
        assertEquals(Arrays.asList(-1, -2, -3, -4, -5), range.getStream().limit(5).collect(Collectors.toList()));
        assertEquals(Range.INFINITE_LENGTH, range.size());
        assertEquals(Range.NEGATIVE_INFINITY.intValue(), range.getStartInclusive());
        assertEquals(Range.NEGATIVE_INFINITY.intValue(), range.getStartExclusive());
        assertEquals(-1, range.getEndInclusive());
        assertEquals(0, range.getEndExclusive());

        range = Range.belowClosed(3);
        assertEquals("(−∞, 4)", range.toString());
        assertEquals(Arrays.asList(3, 2, 1, 0, -1, -2), range.getStream().limit(6).collect(Collectors.toList()));
        assertEquals(Range.INFINITE_LENGTH, range.size());
        assertEquals(Range.NEGATIVE_INFINITY.intValue(), range.getStartInclusive());
        assertEquals(Range.NEGATIVE_INFINITY.intValue(), range.getStartExclusive());
        assertEquals(3, range.getEndInclusive());
        assertEquals(4, range.getEndExclusive());
    }

    @Test
    public void belowOpen() {
        Range range;
        range = Range.belowOpen(-1);
        assertEquals("(−∞, -1)", range.toString());
        assertEquals(Arrays.asList(-2, -3, -4, -5), range.getStream().limit(4).collect(Collectors.toList()));
        assertEquals(Range.INFINITE_LENGTH, range.size());
        assertEquals(Range.NEGATIVE_INFINITY.intValue(), range.getStartInclusive());
        assertEquals(Range.NEGATIVE_INFINITY.intValue(), range.getStartExclusive());
        assertEquals(-2, range.getEndInclusive());
        assertEquals(-1, range.getEndExclusive());

        range = Range.belowOpen(3);
        assertEquals("(−∞, 3)", range.toString());
        assertEquals(Arrays.asList(2, 1, 0, -1, -2, -3, -4, -5), range.getStream().limit(8).collect(Collectors.toList()));
        assertEquals(Range.INFINITE_LENGTH, range.size());
        assertEquals(Range.NEGATIVE_INFINITY.intValue(), range.getStartInclusive());
        assertEquals(Range.NEGATIVE_INFINITY.intValue(), range.getStartExclusive());
        assertEquals(2, range.getEndInclusive());
        assertEquals(3, range.getEndExclusive());
    }

    @Test
    public void contains() {
        Range range;
        range = Range.indexesOfLength(0);
        assertEquals(Range.NONE, range);
        assertEquals("[0, 0)", range.toString());
        assertFalse(range.contains(0));
        assertFalse(range.contains(1));
        assertFalse(range.contains(-1));

        range = Range.indexesOfLength(1);
        assertEquals("[0, 1)", range.toString());
        assertTrue(range.contains(0));
        assertFalse(range.contains(1));
        assertFalse(range.contains(-1));

        range = Range.indexesOfLength(3);
        assertEquals("[0, 3)", range.toString());
        assertTrue(range.contains(0));
        assertTrue(range.contains(1));
        assertTrue(range.contains(2));
        assertFalse(range.contains(3));
        assertFalse(range.contains(-1));
    }

    @Test
    public void encloses() {
        Range range0_0 = Range.NONE;
        Range range0_1 = new Range(0, 1);
        Range range1_1 = new Range(1, 1);
        Range range1_2 = new Range(1, 2);

        assertTrue(range0_0.contains(range0_0));
        assertFalse(range0_0.contains(range0_1));
        assertFalse(range0_0.contains(range1_1));
        assertFalse(range0_0.contains(range1_2));

        assertTrue(range1_2.contains(range1_1));
        assertFalse(range1_2.contains(range0_0));
        assertFalse(range1_2.contains(range0_1));

        Range range2_4 = new Range(2, 4);
        Range range3_4 = new Range(3, 4);
        assertFalse(range2_4.contains(range0_0));
        assertFalse(range2_4.contains(range1_1));
        assertFalse(range2_4.contains(range1_2));
        assertTrue(range2_4.contains(range3_4));

        Range range0_9 = Range.indexesOfLength(9);
        assertTrue(range0_9.contains(range0_1));
        assertTrue(range0_9.contains(range1_1));
        assertTrue(range0_9.contains(range1_2));
        assertTrue(range0_9.contains(range2_4));
        assertTrue(range0_9.contains(range3_4));

        Range range0_9_2 = Range.indexesOfLength(9);
        Range range0_15 = Range.indexesOfLength(15);
        assertTrue(range0_9.contains(range0_9_2));
        assertTrue(range0_15.contains(range0_9));
    }

    @Test
    public void isConnected() {
        Range range0_0 = Range.NONE;
        Range range0_1 = new Range(0, 1);
        Range range1_1 = new Range(1, 1);
        Range range1_2 = new Range(1, 2);
        Range range0_3 = new Range(0, 3);
        Range range1_3 = new Range(1, 3);
        Range range2_4 = new Range(2, 4);
        Range range3_4 = new Range(3, 4);

        assertTrue(range0_0.isConnected(range0_1));
        assertTrue(range0_1.isConnected(range1_1));
        assertTrue(range0_1.isConnected(range1_2));
        assertFalse(range0_1.isConnected(range2_4));

        assertFalse(range0_0.isConnected(range1_1));
        assertTrue(range0_0.isConnected(range0_3));
        assertFalse(range0_0.isConnected(range1_3));
        assertFalse(range1_2.isConnected(range3_4));
        assertTrue(range1_3.isConnected(range3_4));
    }

    @Test
    public void intersection() {
        Range range0_0 = Range.NONE;
        Range range0_1 = new Range(0, 1);
        Range range2_4 = new Range(2, 4);
        Range range3_4 = new Range(3, 4);

        assertEquals(Range.closedOpen(0, 4), range0_0.intersection(range2_4));
        assertEquals(Range.closedOpen(0, 4), range0_1.intersection(range3_4));

        Range range7_9 = new Range(7, 9);
        assertEquals(Range.closedOpen(0, 9), range0_1.intersection(range3_4).intersection(range7_9));

        assertEquals(Range.aboveClosed(2), range2_4.intersection(Range.aboveOpen(10)));

        assertEquals(Range.ALL_INT, Range.aboveClosed(5).intersection(Range.belowClosed(-100)));
    }

    @Test
    public void overlaps(){
        Range range1_4 = new Range(1, 4);

        assertFalse(range1_4.overlaps(new Range(-1, 0)));
        assertFalse(range1_4.overlaps(new Range(0, 1)));
        assertFalse(range1_4.overlaps(new Range(4, 7)));
        assertFalse(range1_4.overlaps(new Range(1, 2)));
        assertFalse(range1_4.overlaps(new Range(1, 4)));
        assertFalse(range1_4.overlaps(new Range(2, 4)));
        assertFalse(range1_4.overlaps(Range.aboveOpen(0)));
        assertFalse(range1_4.overlaps(Range.aboveClosed(0)));
        assertFalse(range1_4.overlaps(Range.aboveOpen(3)));
        assertFalse(range1_4.overlaps(Range.aboveOpen(-3)));
        assertFalse(range1_4.overlaps(Range.belowOpen(1)));
        assertFalse(range1_4.overlaps(Range.belowOpen(-1)));
        assertFalse(range1_4.overlaps(Range.belowOpen(4)));

        assertTrue(range1_4.overlaps(new Range(-1, 2)));
        assertTrue(range1_4.overlaps(new Range(0, 3)));
        assertTrue(range1_4.overlaps(new Range(3, 7)));
        assertTrue(range1_4.overlaps(new Range(2, 9)));
        assertTrue(range1_4.overlaps(Range.aboveOpen(1)));
        assertTrue(range1_4.overlaps(Range.aboveOpen(2)));
        assertTrue(range1_4.overlaps(Range.aboveClosed(2)));
        assertTrue(range1_4.overlaps(Range.aboveClosed(3)));
        assertTrue(range1_4.overlaps(Range.belowOpen(3)));
        assertTrue(range1_4.overlaps(Range.belowOpen(2)));
        assertTrue(range1_4.overlaps(Range.belowClosed(1)));

        Range range2_2 = new Range(2, 2);
        assertFalse(range2_2.overlaps(new Range(-1, 0)));
        assertFalse(range2_2.overlaps(new Range(0, 1)));
        assertFalse(range2_2.overlaps(new Range(4, 7)));
        assertFalse(range2_2.overlaps(new Range(1, 2)));
        assertFalse(range2_2.overlaps(new Range(1, 4)));
        assertFalse(range2_2.overlaps(new Range(2, 4)));
        assertFalse(range2_2.overlaps(Range.aboveOpen(3)));
        assertFalse(range2_2.overlaps(Range.aboveOpen(-3)));
        assertFalse(range2_2.overlaps(Range.belowOpen(1)));
        assertFalse(range2_2.overlaps(Range.belowOpen(-1)));
    }

    @Test
    public void compareTo() {
        Range range3_4 = new Range(3, 4);
        Range range3_5 = new Range(3, 5);
        Range range7_8 = new Range(7, 8);
        Range range0_1 = new Range(0, 1);
        Range range0_10 = new Range(0, 10);
        Range range1_2 = new Range(1, 2);
        Range range0_3 = new Range(0, 3);
        Range range1_1 = new Range(1, 1);
        Range range2_4 = new Range(2, 4);
        Range range1_9 = new Range(1, 9);

        List<Range> rangeList = Arrays.asList(range3_4, range3_5, range7_8, range0_1, range0_10, range1_2, range0_3, range1_1, range2_4, range1_9);
        Collections.sort(rangeList);
        assertEquals(Arrays.asList(range0_1, range0_3, range0_10, range1_1, range1_2, range1_9, range2_4, range3_4, range3_5,  range7_8), rangeList);
    }

    @Test
    public void gapWith() {
        Range range1_4 = new Range(1, 4);

        assertEquals(Range.NONE, range1_4.gapWith(new Range(1, 2)));
        assertEquals(Range.NONE, range1_4.gapWith(new Range(1, 1)));
        assertEquals(Range.NONE, range1_4.gapWith(new Range(1, 3)));
        assertEquals(Range.NONE, range1_4.gapWith(new Range(1, 4)));
        assertEquals(Range.NONE, range1_4.gapWith(new Range(1, 5)));
        assertEquals(Range.NONE, range1_4.gapWith(new Range(4, 5)));
        assertEquals(Range.NONE, range1_4.gapWith(Range.aboveClosed(0)));
        assertEquals(Range.NONE, range1_4.gapWith(Range.aboveClosed(1)));
        assertEquals(Range.NONE, range1_4.gapWith(Range.aboveClosed(3)));
        assertEquals(Range.NONE, range1_4.gapWith(Range.aboveClosed(4)));
        assertEquals(Range.NONE, range1_4.gapWith(new Range(-1, 1)));
        assertEquals(Range.NONE, range1_4.gapWith(Range.belowOpen(5)));
        assertEquals(Range.NONE, range1_4.gapWith(Range.belowOpen(4)));
        assertEquals(Range.NONE, range1_4.gapWith(Range.belowOpen(3)));
        assertEquals(Range.NONE, range1_4.gapWith(Range.belowOpen(1)));

        assertEquals(new Range(4, 5), range1_4.gapWith(new Range(5, 5)));
        assertEquals(new Range(4, 6), range1_4.gapWith(new Range(6, 6)));
        assertEquals(new Range(4, 6), range1_4.gapWith(new Range(6, 9)));
        assertEquals(new Range(4, 8), range1_4.gapWith(Range.aboveClosed(8)));
        assertEquals(new Range(0, 1), range1_4.gapWith(new Range(-9, 0)));
        assertEquals(new Range(-1, 1), range1_4.gapWith(new Range(-9, -1)));
        assertEquals(new Range(-7, 1), range1_4.gapWith(Range.belowClosed(-8)));
    }

    @Test
    public void equals() {
    }

    @Test
    public void canEqual() {
    }


}
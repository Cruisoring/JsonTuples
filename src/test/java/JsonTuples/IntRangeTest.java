package JsonTuples;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class IntRangeTest {

    @Test
    public void testWHOLE() {
        assertEquals(IntRange.NEGATIVE_INFINITY.intValue(), IntRange.ALL_INT.getStartInclusive());
        assertEquals(IntRange.POSITIVE_INFINITY.intValue(), IntRange.ALL_INT.getEndInclusive());
        assertEquals("(−∞, +∞)", IntRange.ALL_INT.toString());

        assertEquals(IntRange.INFINITE_LENGTH, IntRange.ALL_INT.size());

//        //java.lang.IllegalStateException: Cannot get stream for all integers
//        Integer first = IntRange.ALL_INT.getStream().findFirst().orElse(null);
    }

    @Test
    public void indexesOfLength() {
        IntRange range;
        range = IntRange.indexesOfLength(0);
        assertEquals(IntRange.NONE, range);
        assertEquals("[0, 0)", range.toString());
        assertEquals(0, range.size());
        assertEquals(0, range.getStartInclusive());
        assertEquals(-1, range.getStartExclusive());
        assertEquals(-1, range.getEndInclusive());
        assertEquals(0, range.getEndExclusive());
        assertEquals(Integer.valueOf(100), range.getStream().findFirst().orElse(100));

        range = IntRange.indexesOfLength(1);
        assertEquals("[0, 1)", range.toString());
        assertEquals(1, range.size());
        assertEquals(0, range.getStartInclusive());
        assertEquals(-1, range.getStartExclusive());
        assertEquals(0, range.getEndInclusive());
        assertEquals(1, range.getEndExclusive());
        assertEquals(Arrays.asList(0), range.getStream().collect(Collectors.toList()));

        range = IntRange.indexesOfLength(3);
        assertEquals("[0, 3)", range.toString());
        assertEquals(3, range.size());
        assertEquals(0, range.getStartInclusive());
        assertEquals(-1, range.getStartExclusive());
        assertEquals(2, range.getEndInclusive());
        assertEquals(3, range.getEndExclusive());
        assertEquals(Arrays.asList(0, 1, 2), range.getStream().collect(Collectors.toList()));

        //java.lang.IllegalStateException: Length shall not be negative value: -1
        //range = IntRange.indexesOfLength(-1);
    }

    @Test
    public void open() {
        IntRange range;
        range = IntRange.open(3, 8);
        assertEquals("[4, 8)", range.toString());
        assertEquals(Arrays.asList(4, 5, 6, 7), range.getStream().collect(Collectors.toList()));
        assertEquals(4, range.size());
        assertEquals(4, range.getStartInclusive());
        assertEquals(3, range.getStartExclusive());
        assertEquals(7, range.getEndInclusive());
        assertEquals(8, range.getEndExclusive());

        range = IntRange.open(3, IntRange.POSITIVE_INFINITY-2);
        assertEquals("[4, 2147483645)", range.toString());
        assertEquals(Arrays.asList(4, 5, 6, 7), range.getStream().limit(4).collect(Collectors.toList()));
        assertEquals(IntRange.POSITIVE_INFINITY-6, range.size());
        assertEquals(4, range.getStartInclusive());
        assertEquals(3, range.getStartExclusive());
        assertEquals(IntRange.POSITIVE_INFINITY-3, range.getEndInclusive());
        assertEquals(IntRange.POSITIVE_INFINITY-2, range.getEndExclusive());
    }

    @Test
    public void closed() {
        IntRange range;
        range = IntRange.closed(3, 8);
        assertEquals("[3, 9)", range.toString());
        assertEquals(Arrays.asList(3, 4, 5, 6, 7, 8), range.getStream().collect(Collectors.toList()));
        assertEquals(6, range.size());
        assertEquals(3, range.getStartInclusive());
        assertEquals(2, range.getStartExclusive());
        assertEquals(8, range.getEndInclusive());
        assertEquals(9, range.getEndExclusive());

        range = IntRange.closed(3, IntRange.POSITIVE_INFINITY-2);
        assertEquals("[3, 2147483646)", range.toString());
        assertEquals(IntRange.POSITIVE_INFINITY-4, range.size());
        assertEquals(3, range.getStartInclusive());
        assertEquals(2, range.getStartExclusive());
        assertEquals(IntRange.POSITIVE_INFINITY-2, range.getEndInclusive());
        assertEquals(IntRange.POSITIVE_INFINITY-1, range.getEndExclusive());
    }

    @Test
    public void openClosed() {
        IntRange range;
        range = IntRange.openClosed(3, 8);
        assertEquals("[4, 9)", range.toString());
        assertEquals(Arrays.asList(4, 5, 6, 7, 8), range.getStream().collect(Collectors.toList()));
        assertEquals(5, range.size());
        assertEquals(4, range.getStartInclusive());
        assertEquals(3, range.getStartExclusive());
        assertEquals(8, range.getEndInclusive());
        assertEquals(9, range.getEndExclusive());

        range = IntRange.openClosed(3, IntRange.POSITIVE_INFINITY-2);
        assertEquals("[4, 2147483646)", range.toString());
        assertEquals(IntRange.POSITIVE_INFINITY-5, range.size());
        assertEquals(4, range.getStartInclusive());
        assertEquals(3, range.getStartExclusive());
        assertEquals(IntRange.POSITIVE_INFINITY-2, range.getEndInclusive());
        assertEquals(IntRange.POSITIVE_INFINITY-1, range.getEndExclusive());
    }

    @Test
    public void closedOpen() {
        IntRange range;
        range = IntRange.closedOpen(3, 8);
        assertEquals("[3, 8)", range.toString());
        assertEquals(Arrays.asList(3, 4, 5, 6, 7), range.getStream().collect(Collectors.toList()));
        assertEquals(5, range.size());
        assertEquals(3, range.getStartInclusive());
        assertEquals(2, range.getStartExclusive());
        assertEquals(7, range.getEndInclusive());
        assertEquals(8, range.getEndExclusive());

        range = IntRange.closedOpen(3, IntRange.POSITIVE_INFINITY-2);
        assertEquals("[3, 2147483645)", range.toString());
        assertEquals(IntRange.POSITIVE_INFINITY-5, range.size());
        assertEquals(3, range.getStartInclusive());
        assertEquals(2, range.getStartExclusive());
        assertEquals(IntRange.POSITIVE_INFINITY-3, range.getEndInclusive());
        assertEquals(IntRange.POSITIVE_INFINITY-2, range.getEndExclusive());
    }

    @Test
    public void aboveClosed() {
        IntRange range;
        range = IntRange.aboveClosed(-1);
        assertEquals("[-1, +∞)", range.toString());
        assertEquals(Arrays.asList(-1, 0, 1, 2, 3), range.getStream().limit(5).collect(Collectors.toList()));
        assertEquals(IntRange.INFINITE_LENGTH, range.size());
        assertEquals(-1, range.getStartInclusive());
        assertEquals(-2, range.getStartExclusive());
        assertEquals(IntRange.POSITIVE_INFINITY.intValue(), range.getEndInclusive());
        assertEquals(IntRange.POSITIVE_INFINITY.intValue(), range.getEndExclusive());

        range = IntRange.aboveClosed(3);
        assertEquals("[3, +∞)", range.toString());
        assertEquals(Arrays.asList(3, 4, 5, 6, 7), range.getStream().limit(5).collect(Collectors.toList()));
        assertEquals(IntRange.INFINITE_LENGTH, range.size());
        assertEquals(3, range.getStartInclusive());
        assertEquals(2, range.getStartExclusive());
        assertEquals(IntRange.POSITIVE_INFINITY.intValue(), range.getEndInclusive());
        assertEquals(IntRange.POSITIVE_INFINITY.intValue(), range.getEndExclusive());
    }

    @Test
    public void aboveOpen() {
        IntRange range;
        range = IntRange.aboveOpen(-2);
        assertEquals("[-1, +∞)", range.toString());
        assertEquals(IntRange.INFINITE_LENGTH, range.size());
        assertEquals(-1, range.getStartInclusive());
        assertEquals(-2, range.getStartExclusive());
        assertEquals(IntRange.POSITIVE_INFINITY.intValue(), range.getEndInclusive());
        assertEquals(IntRange.POSITIVE_INFINITY.intValue(), range.getEndExclusive());

        range = IntRange.aboveOpen(2);
        assertEquals("[3, +∞)", range.toString());
        assertEquals(IntRange.INFINITE_LENGTH, range.size());
        assertEquals(3, range.getStartInclusive());
        assertEquals(2, range.getStartExclusive());
        assertEquals(IntRange.POSITIVE_INFINITY.intValue(), range.getEndInclusive());
        assertEquals(IntRange.POSITIVE_INFINITY.intValue(), range.getEndExclusive());
    }

    @Test
    public void belowClosed() {
        IntRange range;
        range = IntRange.belowClosed(-1);
        assertEquals("(−∞, 0)", range.toString());
        assertEquals(Arrays.asList(-1, -2, -3, -4, -5), range.getStream().limit(5).collect(Collectors.toList()));
        assertEquals(IntRange.INFINITE_LENGTH, range.size());
        assertEquals(IntRange.NEGATIVE_INFINITY.intValue(), range.getStartInclusive());
        assertEquals(IntRange.NEGATIVE_INFINITY.intValue(), range.getStartExclusive());
        assertEquals(-1, range.getEndInclusive());
        assertEquals(0, range.getEndExclusive());

        range = IntRange.belowClosed(3);
        assertEquals("(−∞, 4)", range.toString());
        assertEquals(Arrays.asList(3, 2, 1, 0, -1, -2), range.getStream().limit(6).collect(Collectors.toList()));
        assertEquals(IntRange.INFINITE_LENGTH, range.size());
        assertEquals(IntRange.NEGATIVE_INFINITY.intValue(), range.getStartInclusive());
        assertEquals(IntRange.NEGATIVE_INFINITY.intValue(), range.getStartExclusive());
        assertEquals(3, range.getEndInclusive());
        assertEquals(4, range.getEndExclusive());
    }

    @Test
    public void belowOpen() {
        IntRange range;
        range = IntRange.belowOpen(-1);
        assertEquals("(−∞, -1)", range.toString());
        assertEquals(Arrays.asList(-2, -3, -4, -5), range.getStream().limit(4).collect(Collectors.toList()));
        assertEquals(IntRange.INFINITE_LENGTH, range.size());
        assertEquals(IntRange.NEGATIVE_INFINITY.intValue(), range.getStartInclusive());
        assertEquals(IntRange.NEGATIVE_INFINITY.intValue(), range.getStartExclusive());
        assertEquals(-2, range.getEndInclusive());
        assertEquals(-1, range.getEndExclusive());

        range = IntRange.belowOpen(3);
        assertEquals("(−∞, 3)", range.toString());
        assertEquals(Arrays.asList(2, 1, 0, -1, -2, -3, -4, -5), range.getStream().limit(8).collect(Collectors.toList()));
        assertEquals(IntRange.INFINITE_LENGTH, range.size());
        assertEquals(IntRange.NEGATIVE_INFINITY.intValue(), range.getStartInclusive());
        assertEquals(IntRange.NEGATIVE_INFINITY.intValue(), range.getStartExclusive());
        assertEquals(2, range.getEndInclusive());
        assertEquals(3, range.getEndExclusive());
    }

    @Test
    public void contains() {
        IntRange range;
        range = IntRange.indexesOfLength(0);
        assertEquals(IntRange.NONE, range);
        assertEquals("[0, 0)", range.toString());
        assertFalse(range.contains(0));
        assertFalse(range.contains(1));
        assertFalse(range.contains(-1));

        range = IntRange.indexesOfLength(1);
        assertEquals("[0, 1)", range.toString());
        assertTrue(range.contains(0));
        assertFalse(range.contains(1));
        assertFalse(range.contains(-1));

        range = IntRange.indexesOfLength(3);
        assertEquals("[0, 3)", range.toString());
        assertTrue(range.contains(0));
        assertTrue(range.contains(1));
        assertTrue(range.contains(2));
        assertFalse(range.contains(3));
        assertFalse(range.contains(-1));
    }

    @Test
    public void encloses() {
        IntRange range0_0 = IntRange.NONE;
        IntRange range0_1 = new IntRange(0, 1);
        IntRange range1_1 = new IntRange(1, 1);
        IntRange range1_2 = new IntRange(1, 2);

        assertTrue(range0_0.contains(range0_0));
        assertFalse(range0_0.contains(range0_1));
        assertFalse(range0_0.contains(range1_1));
        assertFalse(range0_0.contains(range1_2));

        assertTrue(range1_2.contains(range1_1));
        assertFalse(range1_2.contains(range0_0));
        assertFalse(range1_2.contains(range0_1));

        IntRange range2_4 = new IntRange(2, 4);
        IntRange range3_4 = new IntRange(3, 4);
        assertFalse(range2_4.contains(range0_0));
        assertFalse(range2_4.contains(range1_1));
        assertFalse(range2_4.contains(range1_2));
        assertTrue(range2_4.contains(range3_4));

        IntRange range0_9 = IntRange.indexesOfLength(9);
        assertTrue(range0_9.contains(range0_1));
        assertTrue(range0_9.contains(range1_1));
        assertTrue(range0_9.contains(range1_2));
        assertTrue(range0_9.contains(range2_4));
        assertTrue(range0_9.contains(range3_4));

        IntRange range0_9_2 = IntRange.indexesOfLength(9);
        IntRange range0_15 = IntRange.indexesOfLength(15);
        assertTrue(range0_9.contains(range0_9_2));
        assertTrue(range0_15.contains(range0_9));
    }

    @Test
    public void isConnected() {
        IntRange range0_0 = IntRange.NONE;
        IntRange range0_1 = new IntRange(0, 1);
        IntRange range1_1 = new IntRange(1, 1);
        IntRange range1_2 = new IntRange(1, 2);
        IntRange range0_3 = new IntRange(0, 3);
        IntRange range1_3 = new IntRange(1, 3);
        IntRange range2_4 = new IntRange(2, 4);
        IntRange range3_4 = new IntRange(3, 4);

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
        IntRange range0_0 = IntRange.NONE;
        IntRange range0_1 = new IntRange(0, 1);
        IntRange range2_4 = new IntRange(2, 4);
        IntRange range3_4 = new IntRange(3, 4);

        assertEquals(IntRange.closedOpen(0, 4), range0_0.intersection(range2_4));
        assertEquals(IntRange.closedOpen(0, 4), range0_1.intersection(range3_4));

        IntRange range7_9 = new IntRange(7, 9);
        assertEquals(IntRange.closedOpen(0, 9), range0_1.intersection(range3_4).intersection(range7_9));

        assertEquals(IntRange.aboveClosed(2), range2_4.intersection(IntRange.aboveOpen(10)));

        assertEquals(IntRange.ALL_INT, IntRange.aboveClosed(5).intersection(IntRange.belowClosed(-100)));
    }

    @Test
    public void overlaps(){
        IntRange range1_4 = new IntRange(1, 4);

        assertFalse(range1_4.overlaps(new IntRange(-1, 0)));
        assertFalse(range1_4.overlaps(new IntRange(0, 1)));
        assertFalse(range1_4.overlaps(new IntRange(4, 7)));
        assertFalse(range1_4.overlaps(new IntRange(1, 2)));
        assertFalse(range1_4.overlaps(new IntRange(1, 4)));
        assertFalse(range1_4.overlaps(new IntRange(2, 4)));
        assertFalse(range1_4.overlaps(IntRange.aboveOpen(0)));
        assertFalse(range1_4.overlaps(IntRange.aboveClosed(0)));
        assertFalse(range1_4.overlaps(IntRange.aboveOpen(3)));
        assertFalse(range1_4.overlaps(IntRange.aboveOpen(-3)));
        assertFalse(range1_4.overlaps(IntRange.belowOpen(1)));
        assertFalse(range1_4.overlaps(IntRange.belowOpen(-1)));
        assertFalse(range1_4.overlaps(IntRange.belowOpen(4)));

        assertTrue(range1_4.overlaps(new IntRange(-1, 2)));
        assertTrue(range1_4.overlaps(new IntRange(0, 3)));
        assertTrue(range1_4.overlaps(new IntRange(3, 7)));
        assertTrue(range1_4.overlaps(new IntRange(2, 9)));
        assertTrue(range1_4.overlaps(IntRange.aboveOpen(1)));
        assertTrue(range1_4.overlaps(IntRange.aboveOpen(2)));
        assertTrue(range1_4.overlaps(IntRange.aboveClosed(2)));
        assertTrue(range1_4.overlaps(IntRange.aboveClosed(3)));
        assertTrue(range1_4.overlaps(IntRange.belowOpen(3)));
        assertTrue(range1_4.overlaps(IntRange.belowOpen(2)));
        assertTrue(range1_4.overlaps(IntRange.belowClosed(1)));

        IntRange range2_2 = new IntRange(2, 2);
        assertFalse(range2_2.overlaps(new IntRange(-1, 0)));
        assertFalse(range2_2.overlaps(new IntRange(0, 1)));
        assertFalse(range2_2.overlaps(new IntRange(4, 7)));
        assertFalse(range2_2.overlaps(new IntRange(1, 2)));
        assertFalse(range2_2.overlaps(new IntRange(1, 4)));
        assertFalse(range2_2.overlaps(new IntRange(2, 4)));
        assertFalse(range2_2.overlaps(IntRange.aboveOpen(3)));
        assertFalse(range2_2.overlaps(IntRange.aboveOpen(-3)));
        assertFalse(range2_2.overlaps(IntRange.belowOpen(1)));
        assertFalse(range2_2.overlaps(IntRange.belowOpen(-1)));
    }

    @Test
    public void compareTo() {
        IntRange range3_4 = new IntRange(3, 4);
        IntRange range3_5 = new IntRange(3, 5);
        IntRange range7_8 = new IntRange(7, 8);
        IntRange range0_1 = new IntRange(0, 1);
        IntRange range0_10 = new IntRange(0, 10);
        IntRange range1_2 = new IntRange(1, 2);
        IntRange range0_3 = new IntRange(0, 3);
        IntRange range1_1 = new IntRange(1, 1);
        IntRange range2_4 = new IntRange(2, 4);
        IntRange range1_9 = new IntRange(1, 9);

        List<IntRange> rangeList = Arrays.asList(range3_4, range3_5, range7_8, range0_1, range0_10, range1_2, range0_3, range1_1, range2_4, range1_9);
        Collections.sort(rangeList);
        assertEquals(Arrays.asList(range0_1, range0_3, range0_10, range1_1, range1_2, range1_9, range2_4, range3_4, range3_5,  range7_8), rangeList);
    }

    @Test
    public void gapWith() {
        IntRange range1_4 = new IntRange(1, 4);

        assertEquals(IntRange.NONE, range1_4.gapWith(new IntRange(1, 2)));
        assertEquals(IntRange.NONE, range1_4.gapWith(new IntRange(1, 1)));
        assertEquals(IntRange.NONE, range1_4.gapWith(new IntRange(1, 3)));
        assertEquals(IntRange.NONE, range1_4.gapWith(new IntRange(1, 4)));
        assertEquals(IntRange.NONE, range1_4.gapWith(new IntRange(1, 5)));
        assertEquals(IntRange.NONE, range1_4.gapWith(new IntRange(4, 5)));
        assertEquals(IntRange.NONE, range1_4.gapWith(IntRange.aboveClosed(0)));
        assertEquals(IntRange.NONE, range1_4.gapWith(IntRange.aboveClosed(1)));
        assertEquals(IntRange.NONE, range1_4.gapWith(IntRange.aboveClosed(3)));
        assertEquals(IntRange.NONE, range1_4.gapWith(IntRange.aboveClosed(4)));
        assertEquals(IntRange.NONE, range1_4.gapWith(new IntRange(-1, 1)));
        assertEquals(IntRange.NONE, range1_4.gapWith(IntRange.belowOpen(5)));
        assertEquals(IntRange.NONE, range1_4.gapWith(IntRange.belowOpen(4)));
        assertEquals(IntRange.NONE, range1_4.gapWith(IntRange.belowOpen(3)));
        assertEquals(IntRange.NONE, range1_4.gapWith(IntRange.belowOpen(1)));

        assertEquals(new IntRange(4, 5), range1_4.gapWith(new IntRange(5, 5)));
        assertEquals(new IntRange(4, 6), range1_4.gapWith(new IntRange(6, 6)));
        assertEquals(new IntRange(4, 6), range1_4.gapWith(new IntRange(6, 9)));
        assertEquals(new IntRange(4, 8), range1_4.gapWith(IntRange.aboveClosed(8)));
        assertEquals(new IntRange(0, 1), range1_4.gapWith(new IntRange(-9, 0)));
        assertEquals(new IntRange(-1, 1), range1_4.gapWith(new IntRange(-9, -1)));
        assertEquals(new IntRange(-7, 1), range1_4.gapWith(IntRange.belowClosed(-8)));
    }

    @Test
    public void equals() {
    }

    @Test
    public void canEqual() {
    }


}
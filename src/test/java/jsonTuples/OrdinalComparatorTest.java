package jsonTuples;

import org.junit.Test;

import java.util.Arrays;

import static io.github.cruisoring.Asserts.assertEquals;

public class OrdinalComparatorTest {

    @Test
    public void compare() {
        OrdinalComparator comparator = new OrdinalComparator();
        String[] names = new String[]{"x", "y", "a", "c", "b", "y"};
        Arrays.sort(names,  comparator);
        assertEquals(new String[]{"x", "y", "y", "a", "c", "b"}, names);
        names = new String[]{"d", "a", "y", "b", "h"};
        Arrays.sort(names, comparator);
        assertEquals(new String[]{"y", "a", "b", "d", "h"}, names);
        names = new String[]{"y", "a", "b", "d", "h", "x", "e", "c", "b"};
        Arrays.sort(names, comparator);
        assertEquals(new String[]{"x", "y", "a", "c", "b", "b", "d", "h", "e"}, names);
    }

}
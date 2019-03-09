package JsonTuples;

import com.google.common.collect.Range;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class ParserTest {

    @Test
    public void testGetIndexesInRange() {
        List<Integer> subList;
        List<Integer> list = Arrays.asList(1, 3, 4, 5, 9, 10, 12, 15, 21, 23, 25);

        //No overlapped:
        subList = Parser.getIndexesInRange(list, Range.open(3, 4));
        assertTrue(subList.size() == 0);

        subList = Parser.getIndexesInRange(list, Range.closed(17, 19));
        assertTrue(subList.size() == 0);

        subList = Parser.getIndexesInRange(list, Range.openClosed(-1, 0));
        assertTrue(subList.size() == 0);

        subList = Parser.getIndexesInRange(list, Range.open(25, 27));
        assertTrue(subList.size() == 0);

        //With 1 element in range:
        subList = Parser.getIndexesInRange(list, Range.closed(4, 4));
        assertTrue(subList.size() == 1 && subList.get(0).equals(4));

        subList = Parser.getIndexesInRange(list, Range.closed(5, 8));
        assertTrue(subList.size() == 1 && subList.get(0).equals(5));

        subList = Parser.getIndexesInRange(list, Range.closed(0, 1));
        assertTrue(subList.size() == 1 && subList.get(0).equals(1));

        subList = Parser.getIndexesInRange(list, Range.closed(24, 29));
        assertTrue(subList.size() == 1 && subList.get(0).equals(25));

        //With multiple element in range:
        subList = Parser.getIndexesInRange(list, Range.closed(4, 5));
        assertEquals(Arrays.asList(4, 5), subList);

        subList = Parser.getIndexesInRange(list, Range.open(4, 10));
        assertEquals(Arrays.asList(5, 9), subList);

        subList = Parser.getIndexesInRange(list, Range.closed(0, 25));
        assertEquals(Arrays.asList(1, 3, 4, 5, 9, 10, 12, 15, 21, 23, 25), subList);

        subList = Parser.getIndexesInRange(list, Range.closed(22, 30));
        assertEquals(Arrays.asList(23, 25), subList);


    }


    @Test
    public void testAsRange_WithPairedIndexes_getAllRanges() {
        List<Range<Integer>> ranges;

        ranges = Parser.asRanges(new ArrayList<Integer>(), new ArrayList<Integer>());
        assertTrue(ranges.size() == 0);

        ranges = Parser.asRanges(Arrays.asList(1), Arrays.asList(5));
        assertEquals(Range.closed(1,5), ranges.get(0));

        ranges = Parser.asRanges(Arrays.asList(1, 3, 5), Arrays.asList(7, 9, 11));
        assertEquals(Arrays.asList(Range.closed(5, 7), Range.closed(3, 9), Range.closed(1, 11)), ranges);

        ranges = Parser.asRanges(Arrays.asList(1, 3, 5, 10), Arrays.asList(7, 9, 11, 13));
        assertEquals(Arrays.asList(Range.closed(5, 7), Range.closed(3, 9), Range.closed(10, 11), Range.closed(1, 13)), ranges);
    }

    @Test
    public void testParse() {
        Parser parser = new Parser("{\n" +
                "    \"glossary\": {\n" +
                "        \"title\": \"example glossary\",\n" +
                "\t\t\"GlossDiv\": {\n" +
                "            \"title\": \"S\",\n" +
                "\t\t\t\"GlossList\": {\n" +
                "                \"GlossEntry\": {\n" +
                "                    \"ID\": \"SGML\",\n" +
                "\t\t\t\t\t\"SortAs\": \"SGML\",\n" +
                "\t\t\t\t\t\"GlossTerm\": \"Standard Generalized Markup Language\",\n" +
                "\t\t\t\t\t\"Acronym\": \"SGML\",\n" +
                "\t\t\t\t\t\"Abbrev\": \"ISO 8879:1986\",\n" +
                "\t\t\t\t\t\"GlossDef\": {\n" +
                "                        \"para\": \"A meta-markup language, used to create markup languages such as DocBook.\",\n" +
                "\t\t\t\t\t\t\"GlossSeeAlso\": [\"GML\", \"XML\"]\n" +
                "                    },\n" +
                "\t\t\t\t\t\"GlossSee\": \"markup\"\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "}");

        parser.parse();
    }
}
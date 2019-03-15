package JsonTuples;

import Utilities.ResourceHelper;
import org.junit.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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

    private void compareJsonParsed(String jsonName) {
        String rawFileName = String.format("%s.json", jsonName);
        String expectedFileName = String.format("%s_parsed.json", jsonName);

        String jsonText = ResourceHelper.getTextFromResourceFile(rawFileName);
        String expectedParsedJson = ResourceHelper.getTextFromResourceFile(expectedFileName)
                .replaceAll("\r\n", "\n");
        Parser parser = new Parser(jsonText);

        IJSONValue value = parser.parse();
        assertTrue(value instanceof JSONObject);
        TupleMap<String, IJSONValue> map = (TupleMap<String, IJSONValue>) value.getObject();
        String actual = map.toString();

        assertEquals(expectedParsedJson, actual);
    }

    @Test
    public void testParse1() {
        compareJsonParsed("sample1");
    }

    @Test
    public void testParse2() {
        compareJsonParsed("sample2");
    }

    @Test
    public void testParse3() {
        compareJsonParsed("sample3");
    }


    @Test
    public void testParse4() {
        compareJsonParsed("sample4");
    }

    @Test
    public void testParse5() {
        compareJsonParsed("sample5");
    }

    @Test
    public void loadMediumJSON() {
        String jsonText = ResourceHelper.getTextFromResourceFile("catalog.json");
        Parser parser = new Parser(jsonText);

        LocalDateTime start = LocalDateTime.now();
        IJSONValue value = parser.parse();
        Duration timeToParse = Duration.between(start, LocalDateTime.now());
        assertTrue(value instanceof JSONObject);

        start = LocalDateTime.now();
        TupleMap<String, IJSONValue> map = (TupleMap<String, IJSONValue>) value.getObject();
        String actual = map.toString();
        Duration timeToShow = Duration.between(start, LocalDateTime.now());

        System.out.println(actual);
        System.out.println(String.format("Time elapsed to parse jsonText of %d: %s, show: %s",
                parser.length, timeToParse.toString(), timeToShow.toString()));
    }
}
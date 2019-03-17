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
        System.out.println(actual);

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

    private void testPerformance(String jsonFilename) {
        String jsonText = ResourceHelper.getTextFromResourceFile(jsonFilename);
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

    @Test
    public void testMediumJson1() {
        testPerformance("navigation.json");
    }

    @Test
    public void testMediumJson2() {
        testPerformance("catalog.json");
    }
}
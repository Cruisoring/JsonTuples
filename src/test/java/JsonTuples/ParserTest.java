package JsonTuples;

import Utilities.ResourceHelper;
import io.github.cruisoring.logger.Logger;
import org.junit.Test;

import java.util.Comparator;

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

        IJSONValue value = Logger.M(() ->(IJSONValue) parser.parse());
        assertTrue(value instanceof JSONObject);
        String actual = Logger.M(() ->value.toString());
        Logger.D(actual);

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

        JSONObject value = Logger.M(()-> (JSONObject) parser.parse(), "parse()");
        assertTrue(value != null);
        JSONObject natualValue = Logger.M(()-> value.getSorted(Comparator.naturalOrder()), "getSorted()");
        String sortedString = Logger.M(() -> natualValue.toString(), "toString()");
//        Logger.V(sortedString);
    }

    @Test
    public void test70KJson() {
        testPerformance("navigation.json");
    }

    @Test
    public void test180KJson() {
        testPerformance("paths.json");
    }

    @Test
    public void test1314KJson() {
        testPerformance("help.json");
    }

    @Test
    public void test392KJson1() {
        testPerformance("nls.metadata.json");
    }

    @Test
    public void test626KJson1() {
        testPerformance("places.json");
    }

    @Test
    public void test1238KJson1() {
        testPerformance("people.json");
    }

    @Test
    public void test6257KJson() {
        testPerformance("catalog.json");
    }

}
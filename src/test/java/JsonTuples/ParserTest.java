package JsonTuples;

import io.github.cruisoring.logger.LogLevel;
import io.github.cruisoring.logger.Logger;
import io.github.cruisoring.logger.Measurement;
import io.github.cruisoring.utility.ResourceHelper;
import org.junit.Test;

import java.util.Comparator;
import java.util.Map;

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

        IJSONValue value = Logger.M(Measurement.start("parse()"), parser.parse(), LogLevel.info);
        assertTrue(value instanceof JSONObject);
        String actual = Logger.M(Measurement.start("value.toString()"), value.toString(), LogLevel.info);
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
        int jsonTextLength = jsonText.length();

        String sortedString = null;
        for (int i = 0; i < 10; i++) {
            JSONObject result = Logger.M(Measurement.start("Parsing JSON text of %d", jsonTextLength),
                    () -> JSONObject.parse(jsonText));
            IJSONValue sortedValue = Logger.M(Measurement.start("Sorting JSONObject of size %d", result.size()),
                    () -> result.getSorted(Comparator.naturalOrder()));
            sortedString = Logger.M(Measurement.start("ToJSONString(null)"), () -> sortedValue.toJSONString(null));
        }
//        Logger.V(sortedString);
        Map<String, String> performanceSummary = Measurement.getAllSummary();
        performanceSummary.entrySet().forEach(entry -> Logger.I("%s--> %s", entry.getKey(), entry.getValue()));
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

    @Test
    public void parseWithNameComparator_withExpectedOrders() {
        assertEquals("{\"age\": 12,\"id\": \"111\",\"name\": \"Grace\"}",
                Parser.parse("{\"name\":\"Grace\",\"id\":\"111\",\"age\":12}", Comparator.naturalOrder()).toJSONString(null));
        assertEquals("{\"id\": \"111\",\"name\": \"Grace\",\"age\": 12}",
                Parser.parse("{\"name\":\"Grace\",\"id\":\"111\",\"age\":12}", new OrdinalComparator<>(new String[]{"id","name","age"})).toJSONString(null));
        //'id' always as first, 'score' present before 'name' since it is processed first
        assertEquals("{\"id\": \"111\",\"name\": \"Grace\",\"classes\": [{\"id\": 11,\"name\": \"english\",\"score\": 77},{\"id\": 33,\"name\": \"math\",\"score\": 88},{\"id\": 22,\"name\": \"science\",\"score\": 99}]}",
                Parser.parse("{\"name\":\"Grace\",\"id\":\"111\",\"classes\":[{\"score\":77,\"id\":11,\"name\":\"english\"},{\"name\":\"math\",\"score\":88,\"id\":33},{\"score\":99,\"id\":22,\"name\":\"science\"}]}",
                        new OrdinalComparator<>(new String[]{"id"})).toJSONString(null));
        //'id' and 'name' would always be listed in order
        assertEquals("{\"id\": \"111\",\"name\": \"Grace\",\"classes\": [{\"id\": 11,\"name\": \"english\",\"score\": 77},{\"id\": 33,\"name\": \"math\",\"score\": 88},{\"id\": 22,\"name\": \"science\",\"score\": 99}]}",
                Parser.parse("{\"name\":\"Grace\",\"id\":\"111\",\"classes\":[{\"score\":77,\"id\":11,\"name\":\"english\"},{\"name\":\"math\",\"score\":88,\"id\":33},{\"score\":99,\"id\":22,\"name\":\"science\"}]}",
                        new OrdinalComparator<>(new String[]{"id", "name"})).toJSONString(null));
        //"id", "name" "classes" and "score" would be recorded based on their original orders
        assertEquals("{\"name\": \"Grace\",\"id\": \"111\",\"classes\": [{\"name\": \"english\",\"id\": 11,\"score\": 77},{\"name\": \"math\",\"id\": 33,\"score\": 88},{\"name\": \"science\",\"id\": 22,\"score\": 99}]}",
                Parser.parse("{\"name\":\"Grace\",\"id\":\"111\",\"classes\":[{\"score\":77,\"id\":11,\"name\":\"english\"},{\"name\":\"math\",\"score\":88,\"id\":33},{\"score\":99,\"id\":22,\"name\":\"science\"}]}",
                        new OrdinalComparator<>()).toJSONString(null));
    }
}
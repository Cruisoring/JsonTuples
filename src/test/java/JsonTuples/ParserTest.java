package JsonTuples;

import io.github.cruisoring.Revokable;
import io.github.cruisoring.logger.LogLevel;
import io.github.cruisoring.logger.Logger;
import io.github.cruisoring.logger.Measurement;
import io.github.cruisoring.utility.ResourceHelper;
import org.junit.Test;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;

import static io.github.cruisoring.Asserts.*;

public class ParserTest {

    private void compareJsonParsed(String jsonName) {
        String rawFileName = String.format("%s.json", jsonName);
        String expectedFileName = String.format("%s_parsed.json", jsonName);

        String jsonText = ResourceHelper.getTextFromResourceFile(rawFileName);
        String expectedParsedJson = ResourceHelper.getTextFromResourceFile(expectedFileName)
                .replaceAll("\r\n", "\n");
        Parser parser = new Parser(null, jsonText);

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
            JSONObject result = Logger.M(Measurement.start("Parsing JSON text of %dk", jsonTextLength/1024),
                    () -> JSONObject.parse(jsonText));
            IJSONValue sortedValue = Logger.M(Measurement.start("Sorting JSONObject of size %d", result.size()),
                    () -> result.getSorted(Comparator.naturalOrder()));
            sortedString = Logger.M(Measurement.start("ToJSONString(null)"), () -> sortedValue.toJSONString(null));
        }
//        Logger.V(sortedString);
        Measurement.printMeasurementSummaries(LogLevel.info);
        Measurement.clear();
    }

    @Test
    public void test69K() {
        testPerformance("navigation.json");
    }

    @Test
    public void test722K() {
        testPerformance("paths.json");
    }

    @Test
    public void test1296K() {
        testPerformance("help.json");
    }

    @Test
    public void test391K() {
        testPerformance("nls.metadata.json");
    }

    @Test
    public void test625K() {
        testPerformance("places.json");
    }

    @Test
    public void test1237K() {
        testPerformance("people.json");
    }

    @Test
    public void test6104KJson() {
        String jsonText = ResourceHelper.getTextFromResourceFile("catalog.json");
        int jsonTextLength = jsonText.length();

        String sortedString = null;
        for (int i = 0; i < 10; i++) {
            JSONObject result = Logger.M(Measurement.start("Parsing JSON text of %dk", jsonTextLength/1024),
                    () -> JSONObject.parse(jsonText));
            int leafCount = result.getLeafCount(true);
            IJSONValue sortedValue = Logger.M(Measurement.start("Sorting JSONObject with %d leaf nodes", leafCount),
                    () -> result.getSorted(Comparator.naturalOrder()));
            sortedString = Logger.M(Measurement.start("ToJSONString(null)"), () -> sortedValue.toJSONString(null));
        }
        Map<String, String> performanceSummary = Measurement.getAllSummary();
        performanceSummary.values().forEach(v -> Logger.I("%s", v));
        Measurement.clear();
    }

    @Test
    public void parseWithNameComparator_withExpectedOrders() {
        assertEquals("{\"age\":12,\"id\":\"111\",\"name\":\"Grace\"}",
                Parser.parse(Comparator.naturalOrder(),"{\"name\":\"Grace\",\"id\":\"111\",\"age\":12}").toJSONString(null));
        assertEquals("{\"id\":\"111\",\"name\":\"Grace\",\"age\":12}",
                Parser.parse(new OrdinalComparator("id","name","age"), "{\"name\":\"Grace\",\"id\":\"111\",\"age\":12}").toJSONString(null));
        //'id' always as first, 'score' present before 'name' since it is processed first
        assertEquals("{\"id\":\"111\",\"name\":\"Grace\",\"classes\":[{\"id\":11,\"name\":\"english\",\"score\":77},{\"id\":33,\"name\":\"math\",\"score\":88},{\"id\":22,\"name\":\"science\",\"score\":99}]}",
                Parser.parse(new OrdinalComparator("id"),
                        "{\"name\":\"Grace\",\"id\":\"111\",\"classes\":[{\"score\":77,\"id\":11,\"name\":\"english\"},{\"name\":\"math\",\"score\":88,\"id\":33},{\"score\":99,\"id\":22,\"name\":\"science\"}]}"
                        ).toJSONString(null));
        //'id' and 'name' would always be listed in order
        assertEquals("{\"id\":\"111\",\"name\":\"Grace\",\"classes\":[{\"id\":11,\"name\":\"english\",\"score\":77},{\"id\":33,\"name\":\"math\",\"score\":88},{\"id\":22,\"name\":\"science\",\"score\":99}]}",
                Parser.parse(new OrdinalComparator("id", "name"),
                        "{\"name\":\"Grace\",\"id\":\"111\",\"classes\":[{\"score\":77,\"id\":11,\"name\":\"english\"},{\"name\":\"math\",\"score\":88,\"id\":33},{\"score\":99,\"id\":22,\"name\":\"science\"}]}"
                        ).toJSONString(null));
        //"id", "name" "classes" and "score" would be recorded based on their original orders
        assertEquals("{\"name\":\"Grace\",\"id\":\"111\",\"classes\":[{\"name\":\"english\",\"id\":11,\"score\":77},{\"name\":\"math\",\"id\":33,\"score\":88},{\"name\":\"science\",\"id\":22,\"score\":99}]}",
                Parser.parse(new OrdinalComparator<>(),
                        "{\"name\":\"Grace\",\"id\":\"111\",\"classes\":[{\"score\":77,\"id\":11,\"name\":\"english\"},{\"name\":\"math\",\"score\":88,\"id\":33},{\"score\":99,\"id\":22,\"name\":\"science\"}]}"
                        ).toJSONString(null));
    }

    @Test
    public void parseText_getRightIJSONValue() {
        //parse texts of null, true or false
        assertTrue(JSONValue.Null == Parser.parse("null"));
        assertTrue(JSONValue.True == Parser.parse("true"));
        assertTrue(JSONValue.False == Parser.parse("false"));

        //text of a number to JSONNumber
        JSONNumber number = (JSONNumber) Parser.parse(" 12.345  ");
        assertEquals(12.345, number.getObject());

        //text enclosed by '""s would be parsed as JSONString
        JSONString string = (JSONString) Parser.parse("  \" abc \n \t\"\r");
        assertEquals(" abc  ", string.getObject());

        //If JSONString.forbidUnescapedControls is set to false, then special characters like \n, \r would not be removed
        try {
            Revokable.register(() -> JSONString.forbidUnescapedControls, v -> JSONString.forbidUnescapedControls = v, false);
            string = (JSONString) Parser.parse("  \" abc \n \t\"\r");
            assertEquals(" abc \n \t", string.getObject());
        }finally {
            Revokable.revokeAll();
        }

        //Map alike text would be parsed as JSONObject
//        JSONObject object = JSONObject.parse("{\"id\":123,\"name\":null,\"courses\":[\"English\", \"Math\", \"Science\"]}");
        JSONObject object = (JSONObject) Parser.parse("{\"id\":123,\"name\":null,\"courses\":[\"English\", \"Math\", \"Science\"]}");
        assertEquals(123, object.get("id"));
        assertNull(object.get("name"));
        assertEquals(new Object[]{"English", "Math", "Science"}, object.get("courses"));

        //Array alike text would be parsed as JSONArray
//        JSONArray array = JSONArray.parse("[1, null, true, \"abc\", [false, null], {\"id\":123}]");
        JSONArray array = (JSONArray) Parser.parse("[1, null, true, \"abc\", [false, null], {\"id\":123}]");
        assertEquals(1, array.get(0));
        assertTrue(
                array.size() == 6,
                array.contains(null),
                array.containsAll(Arrays.asList(true, "abc"))
        );
        assertEquals(new Object[]{false, null}, array.get(4));
        Map mapAt5 = (Map)array.get(5);
        assertEquals(123, mapAt5.get("id"));
    }

    @Test
    public void parseJSON_withSyntaxErrors_throwExceptions() throws Exception{
        assertLogging(() -> Parser.parse("null}"), "Missing '{' to pair '}'");
        assertLogging(() -> Parser.parse("[  , null]"), "IllegalStateException", "\"  \"", '[', ',');
        assertLogging(() -> Parser.parse("}"), "Missing '{' to pair '}'");
        assertLogging(() -> Parser.parse("]"), "Missing '[' to pair ']'");
        assertLogging(() -> Parser.parse("[123, null]]"), "Missing '[' to pair ']'");
        assertLogging(() -> Parser.parse("[123, null"), "JSONArray is not closed");
        assertLogging(() -> Parser.parse("{ abc: 123}"), "Fail to enclose", "\" abc\"");
        assertLogging(() -> Parser.parse("{\"abc\": 123ab}"), "IllegalStateException", "123ab");
        assertLogging(() -> Parser.parse("{\"abc\": \"xxx}"), "JSONString is not enclosed properly");
    }
}
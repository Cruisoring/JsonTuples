package jsonTuples;

import io.github.cruisoring.Range;
import io.github.cruisoring.Revokable;
import io.github.cruisoring.logger.LogLevel;
import io.github.cruisoring.logger.Logger;
import io.github.cruisoring.logger.Measurement;
import io.github.cruisoring.utility.ResourceHelper;
import org.junit.Ignore;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;

import static io.github.cruisoring.Asserts.*;

public class ParserTest {

    @Test
    public void testParsingJSONValues_withValidJSON_shallHandleAll(){
        assertEquals(JSONValue.Null, Parser.parse(false, "null "));
        assertEquals(JSONValue.True, Parser.parse(false, " true"));
        assertEquals(JSONValue.False, Parser.parse(false, "false"));
        assertEquals(Integer.valueOf(123), Parser.parse(false, "123").getObject());
        assertEquals(Double.valueOf(123.0), Parser.parse(false, "123.000").getObject());
        assertEquals(BigInteger.valueOf(2147483648L), Parser.parse(false, "2147483648").getObject());
        assertEquals(BigInteger.valueOf(-2147483649L), Parser.parse(false, "-2147483649").getObject());
        assertEquals(new BigDecimal("-123.456789012345678901"), Parser.parse(false, "-123.456789012345678901").getObject());
        assertEquals("", Parser.parse(false, "\"\"").getObject());
        assertEquals("xyz", Parser.parse(false, "\"xyz\"").getObject());
        assertEquals("null", Parser.parse(false, "\"null\"").getObject());
        assertEquals("", Parser.parse(false, "\"\"").getObject());
    }

    @Test
    public void testParsingJSONValues_withInvalidJSON_shallThrows(){
        assertException(() -> Parser.parse(false, ""), IllegalStateException.class);
        assertException(() -> Parser.parse(false, "nu_ll"), IllegalStateException.class);
        assertException(() -> Parser.parse(false, "true,"), IllegalStateException.class);
        assertException(() -> Parser.parse(false, "falsE"), IllegalStateException.class);
        assertException(() -> Parser.parse(false, "0x123a"), IllegalStateException.class); //No support of hexadecimal string
        assertException(() -> Parser.parse(false, "123 456"), IllegalStateException.class);

        assertException(() -> Parser.parse(false, "\""), IllegalStateException.class);
        assertException(() -> Parser.parse(false, "\"abc"), IllegalStateException.class);
        assertException(() -> Parser.parse(false, "\"abc\"\""), IllegalStateException.class, "two JSONStrings must be seperated by a control char");
    }

    @Test
    public void testParsingNamedValue_shallThrows(){
        assertException(() -> Parser.parse(false, "\"name\": 123"), IllegalStateException.class);
        assertException(() -> Parser.parse(false, "\"name\": {\"value\":123}"), IllegalStateException.class);
        assertException(() -> Parser.parse(false, "\"name\": []"), IllegalStateException.class);
    }

    private void compareJsonParsed(String jsonName) {
        String rawFileName = String.format("%s.json", jsonName);
        String expectedFileName = String.format("%s_parsed.json", jsonName);

        String jsonText = ResourceHelper.getTextFromResourceFile(rawFileName);
        String expectedParsedJson = ResourceHelper.getTextFromResourceFile(expectedFileName)
                .replaceAll("\r\n", "\n");
        Parser parser = new Parser(null, jsonText);

        IJSONValue value = Logger.M(Measurement.start("parse()"), parser.parse(false), LogLevel.info);
        assertAllTrue(value instanceof JSONObject);
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
        Measurement.purge(LogLevel.info);
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

    @Test @Ignore
    public void test6104KJson() {
        String jsonText = ResourceHelper.getTextFromResourceFile("catalog.json");
        int jsonTextLength = jsonText.length();

        String sortedString = null;
        for (int i = 0; i < 10; i++) {
            JSONObject result = Logger.M(Measurement.start("Parsing JSON text of %dk", jsonTextLength/1024),
                    () -> (JSONObject) Parser.parse(false, jsonText));
            int leafCount = checkNotNull(result, "Failed to parse the JOSN text.").getLeafCount();
            IJSONValue sortedValue = Logger.M(Measurement.start("Sorting JSONObject with %d leaf nodes", leafCount),
                    () -> result.getSorted(Comparator.naturalOrder()));
            sortedString = Logger.M(Measurement.start("ToJSONString(null)"), () -> sortedValue.toJSONString(null));
        }
        Measurement.purge(LogLevel.warning);
    }

    @Test @Ignore
    public void test181MJson() {
        String jsonText = Logger.M(Measurement.start("Load JSON as String"), () -> ResourceHelper.getTextFromResourceFile("c:/temp/citylots.json"));
        int jsonTextLength = jsonText.length();

        String sortedString = null;
        JSONObject result = Logger.M(Measurement.start("Parsing JSON text of %dk", jsonTextLength / 1024),
                () -> (JSONObject)Parser.parse(true, jsonText));
        assertNotNull(result, "Failed to parse the JSON text.");
        Measurement.purge(LogLevel.warning);
    }

    @Test
    public void parseWithNameComparator_withExpectedOrders() {
        assertEquals("{\"age\":12,\"id\":\"111\",\"name\":\"Grace\"}",
                Parser.parse(false, Comparator.naturalOrder(),"{\"name\":\"Grace\",\"id\":\"111\",\"age\":12}").toJSONString(null));
        assertEquals("{\"id\":\"111\",\"name\":\"Grace\",\"age\":12}",
                Parser.parse(false, new OrdinalComparator("id","name","age"), "{\"name\":\"Grace\",\"id\":\"111\",\"age\":12}").toJSONString(null));
        //'id' always as first, 'score' present before 'name' since it is processed first
        assertEquals("{\"id\":\"111\",\"name\":\"Grace\",\"classes\":[{\"id\":11,\"name\":\"english\",\"score\":77},{\"id\":33,\"name\":\"math\",\"score\":88},{\"id\":22,\"name\":\"science\",\"score\":99}]}",
                Parser.parse(false, new OrdinalComparator("id"),
                        "{\"name\":\"Grace\",\"id\":\"111\",\"classes\":[{\"score\":77,\"id\":11,\"name\":\"english\"},{\"name\":\"math\",\"score\":88,\"id\":33},{\"score\":99,\"id\":22,\"name\":\"science\"}]}"
                        ).toJSONString(null));
        //'id' and 'name' would always be listed in order
        assertEquals("{\"id\":\"111\",\"name\":\"Grace\",\"classes\":[{\"id\":11,\"name\":\"english\",\"score\":77},{\"id\":33,\"name\":\"math\",\"score\":88},{\"id\":22,\"name\":\"science\",\"score\":99}]}",
                Parser.parse(false, new OrdinalComparator("id", "name"),
                        "{\"name\":\"Grace\",\"id\":\"111\",\"classes\":[{\"score\":77,\"id\":11,\"name\":\"english\"},{\"name\":\"math\",\"score\":88,\"id\":33},{\"score\":99,\"id\":22,\"name\":\"science\"}]}"
                        ).toJSONString(null));
        //"id", "name" "classes" and "score" would be recorded based on their original orders
        assertEquals("{\"name\":\"Grace\",\"id\":\"111\",\"classes\":[{\"name\":\"english\",\"id\":11,\"score\":77},{\"name\":\"math\",\"id\":33,\"score\":88},{\"name\":\"science\",\"id\":22,\"score\":99}]}",
                Parser.parse(false, new OrdinalComparator<>(),
                        "{\"name\":\"Grace\",\"id\":\"111\",\"classes\":[{\"score\":77,\"id\":11,\"name\":\"english\"},{\"name\":\"math\",\"score\":88,\"id\":33},{\"score\":99,\"id\":22,\"name\":\"science\"}]}"
                        ).toJSONString(null));
    }

    @Test
    public void parseText_getRightIJSONValue() {
        //parse texts of null, true or false
        assertAllTrue(JSONValue.Null == Parser.parse(false, "null"));
        assertAllTrue(JSONValue.True == Parser.parse(false, "true"));
        assertAllTrue(JSONValue.False == Parser.parse(false, "false"));

        //text of a number to JSONNumber
        JSONNumber number = (JSONNumber) Parser.parse(false, " 12.345  ");
        assertEquals(12.345, number.getObject());

        //text enclosed by '""s would be parsed as JSONString
        JSONString string = (JSONString) Parser.parse(false, "  \" abc \n \t\"\r");
        assertEquals(" abc  ", string.getObject());

        //If JSONString.forbidUnescapedControls is set to false, then special characters like \n, \r would not be removed
        try {
            Revokable.register(() -> JSONString.forbidUnescapedControls, v -> JSONString.forbidUnescapedControls = v, false);
            string = (JSONString) Parser.parse(false, "  \" abc \n \t\"\r");
            assertEquals(" abc \n \t", string.getObject());
        }finally {
            Revokable.revokeAll();
        }

        //Map alike text would be parsed as JSONObject
//        JSONObject object = JSONObject.parse("{\"id\":123,\"name\":null,\"courses\":[\"English\", \"Math\", \"Science\"]}");
        JSONObject object = (JSONObject) Parser.parse(false, "{\"id\":123,\"name\":null,\"courses\":[\"English\", \"Math\", \"Science\"]}");
        assertEquals(123, object.get("id"));
        assertAllNull(object.get("name"));
        assertEquals(new Object[]{"English", "Math", "Science"}, object.get("courses"));

        //Array alike text would be parsed as JSONArray
//        JSONArray array = JSONArray.parse("[1, null, true, \"abc\", [false, null], {\"id\":123}]");
        JSONArray array = (JSONArray) Parser.parse(false, "[1, null, true, \"abc\", [false, null], {\"id\":123}]");
        assertEquals(1, array.get(0));
        assertAllTrue(
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

        //Parsing in lenient mode
        assertLogging(() -> Parser.parse(false, "null}"), "Missing '{' to pair '}'");
        assertLogging(() -> Parser.parse(false, "[  , null]"), "IllegalStateException", "\"  \"", '[', ',');
        assertLogging(() -> Parser.parse(false, "}"), "Missing '{' to pair '}'");
        assertLogging(() -> Parser.parse(false, "]"), "Missing '[' to pair ']'");
        assertLogging(() -> Parser.parse(false, "[123, null]]"), "Missing '[' to pair ']'");
        assertLogging(() -> Parser.parse(false, "[123, null"), "JSONArray is not closed");
        assertLogging(() -> Parser.parse(false, "{ abc: 123}"), "Fail to enclose", "\" abc\"");
        assertLogging(() -> Parser.parse(false, "{\"abc\": 123ab}"), "IllegalStateException", "123ab");
        assertLogging(() -> Parser.parse(false, "{\"abc\": \"xxx}"), "JSONString is not enclosed properly");

        //Parsing in strict mode
        assertException(() -> Parser.parse(true, "{\"abc\": \"xxx}"), IllegalStateException.class, "JSONString is not enclosed properly");
        assertException(() -> Parser.parse(true, "null}"), IllegalStateException.class, "Illegal control char:'^' shall never be presented before '}'.");
        assertException(() -> Parser.parse(true, "[  , null]"), IllegalStateException.class, "Missing value string");
        assertException(() -> Parser.parse(true, "}"), IllegalStateException.class, "Illegal control char:'^' shall never be presented before '}'");
        assertException(() -> Parser.parse(true, "]"), IllegalStateException.class, "Illegal control char:'^' shall never be presented before ']'");
        assertException(() -> Parser.parse(true, "[123, null]]"), IllegalStateException.class, "Missing '[' to pair ']'");
        assertException(() -> Parser.parse(true, "[123, null"), IllegalStateException.class, "JSONArray is not closed properly?");
        assertException(() -> Parser.parse(true, "{ abc: 123}"), IllegalStateException.class, "Wrong state before COLON(':'): lastName=null, lastStringValue=null");
        assertException(() -> Parser.parse(true, "{\"abc\": 123ab}"), IllegalStateException.class, "Cannot parse \" 123ab\" as a JSONValue");
    }

    @Test
    public void testLogging_problemTobeHighlighted() {
        try (
                Revokable<Integer> revokable = Revokable.register(() -> Parser.JSON_TEXT_LENGTH_TO_LOG, l -> Parser.JSON_TEXT_LENGTH_TO_LOG = l, 100);
        ) {
            String text = "{\"address\":null,\"scores\":{\"English\":80,\"Science\":88,\"Math\":90},\"name\":\"test name\",\"id\":123456,\"isActive\":true,\"class\":\"7A\"}";
            JSONObject object = checkNotNull(JSONObject.parse(text), "Failed to parse JSON text with sound syntax");
            assertLogging(() -> Parser.parse(false, text.substring(0, 9) + "\"" + text.substring(9)),
                    "two JSONStrings must be seperated by a control char", "{\"address[9]>>>\"\"<<<[10]:null,\"scores\":{");
            assertLogging(() -> Parser.parse(false, text.substring(0, 15) + "," + text.substring(15)),
                    "(Cannot parse \"\" as a JSONValue)",
                    "{\"address\":null[15]>>>,,<<<[16]\"scores\":{\"English\"");
            assertLogging(() -> Parser.parse(false, text.substring(0, 24) + ":" + text.substring(24)),
                    "Fail to enclose \"\" with quotation marks before ':'",
                    "\"scores\"[24]>>>::<<<[25]{");
            assertLogging(() -> Parser.parse(false, text.substring(0, 25) + "," + text.substring(25)),
                    "\"scores\"[24]>>>:,<<<[25]{");
            assertLogging(() -> Parser.parse(false, text.substring(0, 81) + "," + text.substring(81)),
                    "Cannot parse \"\" as a JSONValue",
                    "\"test name\"[81]>>>,,<<<[82]\"id\"");
            assertLogging(() -> Parser.parse(false, text.substring(0, 92) + "a" + text.substring(92)),
                    "Cannot parse \"12345a6\" as a JSONValue",
                    "\"id\"[86]>>>:12345a6,<<<[94]\"isActive\":true");
            assertLogging(() -> Parser.parse(false, text.substring(0, 109) + "s" + text.substring(109)),
                    "Cannot parse \"trues\" as a JSONValue",
                    "\"isActive\"[104]>>>:trues,<<<[110]\"class\"");
            assertLogging(() -> Parser.parse(false, text.substring(0, 117) + text.substring(118)),
                    "two JSONStrings must be seperated by a control char.",
                    ",\"class[116]>>>\"\"<<<[117]7A\"}");
            assertLogging(() -> Parser.parse(false, text + "]"),
                    "IllegalStateException: Missing '[' to pair ']'",
                    "\"isActive\":true,\"class\":\"7A\"[122]>>>}]<<<[123]");

            //assertLogging(() -> Parser.parse(text.substring(0, 10) + "a" + text.substring(10)));    //The extra 'a' between '"' and ':' is ignored: "address"a:null
        }
    }

    @Test
    public void testParseRange() {
        final String jsonText = "[{\"id\":222, \"name\":\"Alice\", \"grade\":9},null,false,{\"id\":333, \"name\":\"Grace\", \"grade\":7} ]";
        assertEquals("{\"id\":222,\"name\":\"Alice\",\"grade\":9}",
                Parser.parseRange(false, jsonText, 1, 38).toJSONString(null));
        assertEquals("null",
                Parser.parseRange(false, jsonText, 39, 43).toJSONString());
        assertEquals(JSONValue.False,
                Parser.parseRange(false, Comparator.naturalOrder(), jsonText, Range.open(43, 49)));
        assertEquals("{\"id\":333,\"grade\":7,\"name\":\"Grace\"}",
                Parser.parseRange(false, new OrdinalComparator<>("id", "grade"), jsonText, Range.open(49, jsonText.length() - 1)).toJSONString(null));
    }

    @Test
    public void validateLenientParsing_withUnexpectedChars_ignoreIfNoConfusion() {
        assertEquals("{\"id\":222,\"name\":\"Alice\"}",
                Parser.parse(false, "XXX{\"id\"XXX:222, \"name\":\"Alice\"}").toJSONString(null));
        assertEquals("{\"id\":222,\"name\":\"Alice\"}",
                Parser.parse(false, "XXX{\"\":\"id\":222, \"name\":\"Alice\"}XXX").toJSONString(null));
        assertEquals("{\"id\":222,\"name\":\"Alice\"}",
                Parser.parse(false, "{XXX\"id\":222, XXX \"name\":\"Alice\"}").toJSONString(null));
        assertEquals("{\"id\":222,\"name\":\"Alice\"}",
                Parser.parse(false, "{\"id\":222,  \"name\":\"Alice\"}, null").toJSONString(null));
    }

    @Test
    public void validateStrictParsing_withUnexpectedChars_throwExceptions() {
        assertException(() -> Parser.parse(true, "\"id\":\"id\":222, \"name\":\"Alice\"}"),
                NullPointerException.class, "COLON(':') shall only present in JSONObject");
        assertException(() -> Parser.parse(true, "{\"id\":\"id\":222, \"name\":\"Alice\"}"),
                IllegalStateException.class, "Wrong state before COLON(':'): lastName=\"id\", lastStringValue=\"id\"");
        assertException(() -> Parser.parse(true, "XXX{\"id\"XXX:222, \"name\":\"Alice\"}"),
                IllegalStateException.class, "Only white spaces are expected");
        assertException(() -> Parser.parse(true, "{\"id\"XXX:222, \"name\":\"Alice\"}"),
                IllegalStateException.class, "Only white spaces are expected");
        assertException(() -> Parser.parse(true, "{\"id\"\"\":222, \"name\":\"Alice\"}"),
                IllegalStateException.class, "two JSONStrings must be seperated by a control char");
        assertException(() -> Parser.parse(true, "{\"id\":222, \"name\":\"Alice\"}xxx"),
                IllegalStateException.class, "Only white spaces are expected");
        assertException(() -> Parser.parse(true, "{\"id\":222, , \"name\":\"Alice\"}"),
                IllegalStateException.class, "Missing value string");
    }
}
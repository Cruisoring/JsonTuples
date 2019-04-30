package JsonTuples;

import io.github.cruisoring.Range;
import io.github.cruisoring.logger.Logger;
import io.github.cruisoring.tuple.Tuple;
import io.github.cruisoring.tuple.Tuple1;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

import static io.github.cruisoring.Asserts.*;

public class JSONValueTest {

    public final static String jsonContext = "{\"web-app\": {\n" +
            "  \"servlet\": [   \n" +
            "    {\n" +
            "      \"bigDecimal\": 12345678901234567890.12345678909,\n" +
            "      \"servlet-name\": \"cofaxCDS\",\n" +
            "      \"integer\": -100 ,\n" +
            "      \"bigInteger\": 12345678901234567890,\n" +
            "      \"double\": -77.23,\n" +
            "      \"null\": null,\n" +
            "      \"true\": true,\n" +
            "      \"false\": false,\n" +
            "      \"servlet-class\": \"org.cofax.cds.CDSServlet\",\n" +
            "      \"init-param\": {\n" +
            "        \"configGlossary:installationAt\": \"Philadelphia, PA\",\n" +
            "        \"configGlossary:adminEmail\": \"ksm@pobox.com\",\n" +
            "        \"configGlossary:poweredBy\": \"Cofax\",\n" +
            "        \"configGlossary:poweredByIcon\": \"/images/cofax.gif\",\n" +
            "        \"configGlossary:staticPath\": \"/content/static\",\n" +
            "        \"dataStoreDriver\": \"com.microsoft.jdbc.sqlserver.SQLServerDriver\",\n" +
            "        \"dataStoreUrl\": \"jdbc:microsoft:sqlserver://LOCALHOST:1433;DatabaseName=goon\",\n" +
            "        \"dataStoreUser\": \"sa\",\n" +
            "        \"dataStorePassword\": \"dataStoreTestQuery\",\n" +
            "        \"dataStoreTestQuery\": \"SET NOCOUNT ON;select test='test';\",\n" +
            "        \"dataStoreLogFile\": \"/usr/local/tomcat/logs/datastore.log\",\n" +
            "        \"dataStoreInitConns\": 10,\n" +
            "        \"dataStoreMaxConns\": 100,\n" +
            "        \"dataStoreConnUsageLimit\": 100,\n" +
            "        \"dataStoreLogLevel\": \"debug\",\n" +
            "        \"maxUrlLength\": 500}},\n" +
            " \n" +
            "  \"taglib\": {\n" +
            "    \"taglib-uri\": \"cofax.tld\",\n" +
            "    \"taglib-location\": \"/WEB-INF/tlds/cofax.tld\"}}}";

    static final Map<String, Range> namedRanges = new HashMap<>();

    static {
        Matcher matcher = NamedValue.NAME_VALUE_PATTERN.matcher(jsonContext);
        while (matcher.find()) {
            String name = matcher.group(1);
            String value = matcher.group(2);
            int valueStart = matcher.start(2);
            int valueEnd = matcher.end(2);
            Range range = Range.closedOpen(valueStart, valueEnd);
            namedRanges.putIfAbsent(name, range);
        }
    }


    @Test
    public void fromJSONRaw_withNull_getNull() {
        String name = "null";
        Range range = namedRanges.get(name);
        IJSONValue nullValue = JSONValue.parse(jsonContext, range);
        Assert.assertNull(nullValue.getObject());

    }

    @Test
    public void fromJSONRaw_withTrue_getTrue() {
        String name = "true";
        Range range = namedRanges.get(name);
        IJSONValue booleanValue = JSONValue.parse(jsonContext, range);
        assertEquals(Boolean.TRUE, booleanValue.getObject());
    }

    @Test
    public void fromJSONRaw_withFalse_getFalse() {
        String name = "false";
        Range range = namedRanges.get(name);
        IJSONValue booleanValue = JSONValue.parse(jsonContext, range);
        assertEquals(Boolean.FALSE, booleanValue.getObject());
    }


    @Test
    public void fromJSONRaw() {
        String name = "servlet-name";
        Range range = namedRanges.get(name);
        IJSONValue value = JSONValue.parse(jsonContext, range);
        assertEquals("cofaxCDS", value.getObject());

        range = namedRanges.get("dataStoreUrl");
        value = JSONValue.parse(jsonContext, range);
        assertEquals("jdbc:microsoft:sqlserver://LOCALHOST:1433;DatabaseName=goon", value.getObject());
    }

    @Test
    public void fromJSONRaw_ofInteger_objectMatched() {
        String name = "integer";
        Range range = namedRanges.get(name);
        IJSONValue number = JSONValue.parse(jsonContext, range);
        assertEquals(Integer.valueOf(-100), number.getObject());
    }

    @Test
    public void fromJSONRaw_ofBigInteger_objectMatched() {
        String name = "bigInteger";
        Range range = namedRanges.get(name);
        IJSONValue number = JSONValue.parse(jsonContext, range);
        assertEquals(new BigInteger("12345678901234567890"), number.getObject());
    }

    @Test
    public void fromJSONRaw_ofBigDecimal_objectMatched() {
        String name = "bigDecimal";
        Range range = namedRanges.get(name);
        IJSONValue number = JSONValue.parse(jsonContext, range);
        assertEquals(new BigDecimal("12345678901234567890.12345678909"), number.getObject());
    }

    @Test
    public void testToString(){
        assertEquals("null", JSONValue.Null.toString());
        assertEquals("true", JSONValue.True.toString());
        assertEquals("false", JSONValue.False.toString());
        assertEquals("\"abc\"", new JSONString("abc").toString());
        assertEquals("77", new JSONNumber(77).toString());
    }

    @Test
    public void testEqals(){
        assertEquals(JSONValue.Null, new JSONValue<>(null));
        assertEquals(JSONValue.True, new JSONValue<>(true));
        assertEquals(JSONValue.False, new JSONValue<>(false));
//        assertEquals(new JSONString(null), JSONValue.Null);               //Shall JSONString(null) be JSONValue.Null treated as equal?
        assertEquals(JSONValue.Null, new JSONNumber(null));
//        assertEquals(new JSONString(""), new JSONValue<>(""));            //How to prevent new JSONValue<>("")?
//        assertEquals(new JSONString("ABC"), new JSONValue<>("ABC"));        //Shall never create new JSONValue<>("ABC")
        assertEquals(new JSONNumber(3.3f), new JSONNumber(3.3));
        assertEquals(new JSONNumber(25), new JSONNumber(25L));
        assertEquals(new JSONNumber(25L), new JSONNumber(Integer.valueOf(25).byteValue()));
        assertEquals(new JSONNumber(25), new JSONNumber(BigInteger.valueOf(25L)));
        assertEquals(new JSONNumber(25.0), new JSONNumber(25.0));
        assertEquals(new JSONNumber(Short.valueOf("25")), new JSONNumber(BigInteger.valueOf(25)));
        assertEquals(new JSONNumber(0d), new JSONNumber(BigDecimal.valueOf(0.0)));

        assertNotEquals(JSONValue.Null, new JSONValue<>(true));
        assertNotEquals(null, new JSONValue<>(null));
        assertNotEquals(JSONValue.False, new JSONValue<>(true));
        assertNotEquals(new JSONString(""), new JSONString(" "));
        assertNotEquals(JSONValue.Null, Tuple.create(null));
        assertNotEquals(new JSONString("ABC"), Tuple.create("ABC"));
        assertNotEquals(new JSONString(""), Tuple1.create(""));
        assertNotEquals(Tuple.create(3.3), new JSONNumber(3.3));
        assertNotEquals(new JSONNumber(25), new JSONNumber(25d));
        assertNotEquals(new JSONNumber(25.0f), new JSONNumber(BigInteger.valueOf(25L)));
        assertNotEquals(new JSONNumber(0d), new JSONNumber(BigDecimal.valueOf(0)));
    }

    @Test
    public void deltaWith(){
        assertEquals(JSONArray.EMPTY, JSONValue.Null.deltaWith(new JSONValue<>(null)));
        assertEquals(JSONArray.EMPTY, JSONValue.True.deltaWith(new JSONValue<>(true)));
        assertEquals(JSONArray.EMPTY, JSONValue.False.deltaWith(new JSONValue<>(false)));
        assertEquals(JSONArray.EMPTY, new JSONString(null).deltaWith(JSONValue.Null));          //Shall JSONString(null) be JSONValue.Null treated as equal?
        assertEquals(JSONArray.EMPTY, JSONValue.Null.deltaWith(new JSONNumber(null)));
        assertEquals(JSONArray.EMPTY, new JSONNumber(3.3f).deltaWith(new JSONNumber(3.3)));
        assertEquals(JSONArray.EMPTY, new JSONNumber(25).deltaWith(new JSONNumber(25L)));
        assertEquals(JSONArray.EMPTY, new JSONNumber(25).deltaWith(new JSONNumber(BigInteger.valueOf(25L))));
        assertEquals(JSONArray.EMPTY, new JSONNumber(25.0).deltaWith(new JSONNumber(25.0)));
        assertEquals(JSONArray.EMPTY, new JSONNumber(Short.valueOf("25")).deltaWith(new JSONNumber(BigInteger.valueOf(25))));

        assertEquals("[null,\"null\"]", JSONValue.Null.deltaWith(new JSONString("null")).toJSONString(null));
        assertEquals("[]", JSONValue.Null.deltaWith(new JSONString(null)).toJSONString(null));
        assertEquals("[1.0,1]", new JSONNumber(1.0).deltaWith(new JSONNumber(1L)).toJSONString(null));
        assertEquals("[-1.0,\"-1.0\"]", new JSONNumber(-1.0).deltaWith(new JSONString("-1.0")).toJSONString(null));

        assertEquals("[\"ABC\",ABC]", new JSONString("ABC").deltaWith(new JSONValue<>("ABC")).toJSONString(null)); //How to prevent new JSONValue<>("ABC")?
        assertEquals("[,\"\"]", new JSONValue("").deltaWith(new JSONString("")).toJSONString(null)); //How to prevent new JSONValue<>("")?
    }

    @Test
    public void getLength(){
        assertEquals(1, JSONValue.Null.getLength());
        assertEquals(1, JSONValue.True.getLength());
        assertEquals(1, JSONValue.False.getLength());
        assertEquals(1, new JSONString("").getLength());
        assertEquals(1, new JSONNumber(222).getLength());
    }

    @Test
    public void getSignatures() {
        Set<Integer> signatures;
        signatures = JSONValue.Null.getSignatures();
        assertTrue(signatures.contains("null".hashCode()) && signatures.size()==1);
        signatures = JSONValue.True.getSignatures();
        assertTrue(signatures.contains("true".hashCode()) && signatures.size()==1);
        signatures = JSONValue.False.getSignatures();
        assertTrue(signatures.contains("false".hashCode()) && signatures.size()==1);

        signatures = new JSONString("string").getSignatures();
        assertTrue(signatures.contains("\"string\"".hashCode()) && signatures.size()==1);
        signatures = new JSONString("").getSignatures();
        assertTrue(signatures.contains("\"\"".hashCode()) && signatures.size()==1);
        signatures = new JSONNumber(-0L).getSignatures();
        assertTrue(signatures.contains("0".hashCode()) && signatures.size()==1);
        signatures = new JSONNumber(Short.valueOf("33")).getSignatures();
        assertTrue(signatures.contains("33".hashCode()) && signatures.size()==1);
        signatures = new JSONNumber(37.0f).getSignatures();
        assertTrue(signatures.contains("37.0".hashCode()) && signatures.size()==1);
        signatures = new JSONNumber(-0.0002).getSignatures();
        assertTrue(signatures.contains("-2.0E-4".hashCode()) && signatures.size()==1);
        signatures = JSONNumber.parseNumber("-20.0000000000000000000000002").getSignatures();
        assertTrue(signatures.contains("-20.0000000000000000000000002".hashCode()) && signatures.size()==1);

    }
}

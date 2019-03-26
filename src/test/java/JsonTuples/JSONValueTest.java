package JsonTuples;

import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

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
        Assert.assertEquals(Boolean.TRUE, booleanValue.getObject());
    }

    @Test
    public void fromJSONRaw_withFalse_getFalse() {
        String name = "false";
        Range range = namedRanges.get(name);
        IJSONValue booleanValue = JSONValue.parse(jsonContext, range);
        Assert.assertEquals(Boolean.FALSE, booleanValue.getObject());
    }


    @Test
    public void fromJSONRaw() {
        String name = "servlet-name";
        Range range = namedRanges.get(name);
        IJSONValue value = JSONValue.parse(jsonContext, range);
        Assert.assertEquals("cofaxCDS", value.getObject());

        range = namedRanges.get("dataStoreUrl");
        value = JSONValue.parse(jsonContext, range);
        Assert.assertEquals("jdbc:microsoft:sqlserver://LOCALHOST:1433;DatabaseName=goon", value.getObject());
    }

    @Test
    public void fromJSONRaw_ofInteger_objectMatched() {
        String name = "integer";
        Range range = namedRanges.get(name);
        IJSONValue number = JSONValue.parse(jsonContext, range);
        Assert.assertEquals(Integer.valueOf(-100), number.getObject());
    }

    @Test
    public void fromJSONRaw_ofBigInteger_objectMatched() {
        String name = "bigInteger";
        Range range = namedRanges.get(name);
        IJSONValue number = JSONValue.parse(jsonContext, range);
        Assert.assertEquals(new BigInteger("12345678901234567890"), number.getObject());
    }

    @Test
    public void fromJSONRaw_ofBigDecimal_objectMatched() {
        String name = "bigDecimal";
        Range range = namedRanges.get(name);
        IJSONValue number = JSONValue.parse(jsonContext, range);
        Assert.assertEquals(new BigDecimal("12345678901234567890.12345678909"), number.getObject());
    }

}

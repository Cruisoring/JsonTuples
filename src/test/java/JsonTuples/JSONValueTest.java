package JsonTuples;

import org.junit.Assert;
import org.junit.Ignore;
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
            "        \"templateProcessorClass\": \"org.cofax.WysiwygTemplate\",\n" +
            "        \"templateLoaderClass\": \"org.cofax.FilesTemplateLoader\",\n" +
            "        \"templatePath\": \"templates\",\n" +
            "        \"templateOverridePath\": \"\",\n" +
            "        \"defaultListTemplate\": \"listTemplate.htm\",\n" +
            "        \"defaultFileTemplate\": \"articleTemplate.htm\",\n" +
            "        \"useJSP\": false,\n" +
            "        \"jspListTemplate\": \"listTemplate.jsp\",\n" +
            "        \"jspFileTemplate\": \"articleTemplate.jsp\",\n" +
            "        \"cachePackageTagsTrack\": 200,\n" +
            "        \"cachePackageTagsStore\": 200,\n" +
            "        \"cachePackageTagsRefresh\": 60,\n" +
            "        \"cacheTemplatesTrack\": 100,\n" +
            "        \"cacheTemplatesStore\": 50,\n" +
            "        \"cacheTemplatesRefresh\": 15,\n" +
            "        \"cachePagesTrack\": 200,\n" +
            "        \"cachePagesStore\": 100,\n" +
            "        \"cachePagesRefresh\": 10,\n" +
            "        \"cachePagesDirtyRead\": 10,\n" +
            "        \"searchEngineListTemplate\": \"forSearchEnginesList.htm\",\n" +
            "        \"searchEngineFileTemplate\": \"forSearchEngines.htm\",\n" +
            "        \"searchEngineRobotsDb\": \"WEB-INF/robots.db\",\n" +
            "        \"useDataStore\": true,\n" +
            "        \"dataStoreClass\": \"org.cofax.SqlDataStore\",\n" +
            "        \"redirectionClass\": \"org.cofax.SqlRedirection\",\n" +
            "        \"dataStoreName\": \"cofax\",\n" +
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
            "    {\n" +
            "      \"servlet-name\": \"cofaxEmail\",\n" +
            "      \"servlet-class\": \"org.cofax.cds.EmailServlet\",\n" +
            "      \"init-param\": {\n" +
            "      \"mailHost\": \"mail1\",\n" +
            "      \"mailHostOverride\": \"mail2\"}},\n" +
            "    {\n" +
            "      \"servlet-name\": \"cofaxAdmin\",\n" +
            "      \"servlet-class\": \"org.cofax.cds.AdminServlet\"},\n" +
            " \n" +
            "    {\n" +
            "      \"servlet-name\": \"fileServlet\",\n" +
            "      \"servlet-class\": \"org.cofax.cds.FileServlet\"},\n" +
            "    {\n" +
            "      \"servlet-name\": \"cofaxTools\",\n" +
            "      \"servlet-class\": \"org.cofax.cms.CofaxToolsServlet\",\n" +
            "      \"init-param\": {\n" +
            "        \"templatePath\": \"toolstemplates/\",\n" +
            "        \"log\": 1,\n" +
            "        \"logLocation\": \"/usr/local/tomcat/logs/CofaxTools.log\",\n" +
            "        \"logMaxSize\": \"\",\n" +
            "        \"dataLog\": 1,\n" +
            "        \"dataLogLocation\": \"/usr/local/tomcat/logs/dataLog.log\",\n" +
            "        \"dataLogMaxSize\": \"\",\n" +
            "        \"removePageCache\": \"/content/admin/remove?cache=pages&id=\",\n" +
            "        \"removeTemplateCache\": \"/content/admin/remove?cache=templates&id=\",\n" +
            "        \"fileTransferFolder\": \"/usr/local/tomcat/webapps/content/fileTransferFolder\",\n" +
            "        \"lookInContext\": 1,\n" +
            "        \"adminGroupID\": 4,\n" +
            "        \"betaServer\": true}}],\n" +
            "  \"servlet-mapping\": {\n" +
            "    \"cofaxCDS\": \"/\",\n" +
            "    \"cofaxEmail\": \"/cofaxutil/aemail/*\",\n" +
            "    \"cofaxAdmin\": \"/admin/*\",\n" +
            "    \"fileServlet\": \"/static/*\",\n" +
            "    \"cofaxTools\": \"/tools/*\"},\n" +
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
    @Ignore
    //Ignored since only big Decimal is used to keep float point numbers.
    public void fromJSONRaw_ofDouble_objectMatched() {
        String name = "double";
        Range range = namedRanges.get(name);
        IJSONValue number = JSONValue.parse(jsonContext, range);
        Assert.assertEquals(Double.valueOf(-77.23), number.getObject());
    }

    @Test
    public void fromJSONRaw_ofBigDecimal_objectMatched() {
        String name = "bigDecimal";
        Range range = namedRanges.get(name);
        IJSONValue number = JSONValue.parse(jsonContext, range);
        Assert.assertEquals(new BigDecimal("12345678901234567890.12345678909"), number.getObject());
    }

}

package JsonTuples;

import io.github.cruisoring.Revokable;
import io.github.cruisoring.logger.LogLevel;
import io.github.cruisoring.logger.Logger;
import io.github.cruisoring.utility.ResourceHelper;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Comparator;

import static io.github.cruisoring.Asserts.assertEquals;

public class JSONArrayTest {
    private static Revokable<LogLevel> logLevelRevokable;

    @BeforeClass
    public static void setup(){
        logLevelRevokable = Logger.setLevelInScope(LogLevel.debug);
    }

    @AfterClass
    public static void cleandown(){
        logLevelRevokable.close();
    }

    @Test
    public void toJSONString() {
        JSONArray array = JSONArray.parseArray("[123, \"abc\"]");
        assertEquals("[\n  123,\n  \"abc\"\n]", array.toString());
        assertEquals("[123,\"abc\"]", array.toJSONString(null));
        assertEquals("[\n    123,\n    \"abc\"\n  ]", array.toJSONString("  "));
        assertEquals("[\n      123,\n      \"abc\"\n    ]", array.toJSONString("    "));
    }

    String steps = ResourceHelper.getTextFromResourceFile("steps.json");
    @Test
    public void parseArray() {
        JSONArray array = JSONArray.parseArray(steps);
        assertEquals(10, array.getLength());
        Logger.V(array.toString());

        JSONArray sorted = array.getSorted(Comparator.naturalOrder());
        Logger.V(sorted.toString());

        IJSONValue delta = array.deltaWith(sorted);
        assertEquals(JSONArray.EMPTY, delta);
    }

    @Test
    public void getObject() {
        Object[] obj = (Object[])JSONArray.parseArray(steps).getObject();
    }

    @Test
    public void deltaWith() {
    }
}
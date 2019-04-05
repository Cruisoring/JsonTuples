package JsonTuples;

import Utilities.ResourceHelper;
import io.github.cruisoring.logger.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Comparator;

import static org.junit.Assert.assertEquals;

public class JSONArrayTest {

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
//        Logger.V(array.toString());

        JSONArray sorted = array.getSorted(Comparator.naturalOrder());
//        Logger.V(sorted.toString());

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
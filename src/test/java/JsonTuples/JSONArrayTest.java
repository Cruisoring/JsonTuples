package JsonTuples;

import io.github.cruisoring.Revokable;
import io.github.cruisoring.logger.LogLevel;
import io.github.cruisoring.logger.Logger;
import io.github.cruisoring.utility.ResourceHelper;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Comparator;
import java.util.Map;

import static io.github.cruisoring.Asserts.*;

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
    public void testToString() {
        JSONArray array = JSONArray.parseArray("[123, \"abc\", null, {\"id\": 32, \"note\": \"none\"}, [false, \"x\"]]");
        assertEquals("[\n" +
                "  123,\n" +
                "  \"abc\",\n" +
                "  null,\n" +
                "  {\n" +
                "    \"id\": 32,\n" +
                "    \"note\": \"none\"\n" +
                "  },\n" +
                "  [\n" +
                "    false,\n" +
                "    \"x\"\n" +
                "  ]\n" +
                "]", array.toString());
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
    public void shuffle() {
        Object[] objects = new Object[]{null, 99, true, 2.3, "Good", 'a', false, new int[]{1, 2}};
        JSONArray array = Utilities.asJSONArrayFromArray(objects);
        Logger.D(array.toJSONString(null));

        JSONArray shuffled = array.shuffle();
        Logger.D(shuffled.toJSONString(null));

        //For array of 8 elements, shuffle() shall always get an array containing same set elements but with different orders
        assertNotEquals(array, shuffled);
        assertEquals(array.getLength(), shuffled.getLength());
    }

    @Test
    public void deltaWith_arrayOfSimpleValues_alwaysWithDifferences() {
        Object[] objects = new Object[]{null, 99, true, 2.3, "Good", 'a', false, new int[]{1, 2}};
        JSONArray array = Utilities.asJSONArrayFromArray(objects);
        JSONArray shuffled = array.shuffle();

        IJSONValue delta = array.deltaWith(shuffled, true);
        Logger.D(delta.toJSONString(null));
        //There shall always be some differences shown as JSONObject
        assertTrue(delta instanceof JSONObject, !delta.isEmpty());

        //No difference if the array is not composed of all Maps
        assertEquals(delta, array.deltaWith(shuffled, false));
    }

    final JSONObject alice = JSONObject.parse("{\"name\":\"Alice\",\"gender\":\"F\",\"age\":12,\"class\":\"7N\",\"scores\":{\"english\":89,\"science\":77,\"humanity\":85,\"math\":90}}");
    final JSONObject bob = JSONObject.parse("{\"name\":\"Bob\",\"gender\":\"M\",\"age\":13,\"class\":\"7C\",\"scores\":{\"english\":76,\"science\":62,\"humanity\":62,\"math\":80}}");
    final JSONObject carl = JSONObject.parse("{\"name\":\"Carl\",\"gender\":\"M\",\"age\":12,\"class\":\"7B\",\"scores\":{\"english\":77,\"science\":66,\"humanity\":87,\"math\":91}}");
    final JSONObject dave = JSONObject.parse("{\"name\":\"Dave\",\"gender\":\"M\",\"age\":12,\"class\":\"7C\",\"scores\":{\"english\":82,\"science\":71,\"humanity\":82,\"math\":76}}");
    final JSONObject ellen = JSONObject.parse("{\"name\":\"Ellen\",\"gender\":\"F\",\"age\":12,\"class\":\"7B\",\"scores\":{\"english\":82,\"science\":87,\"humanity\":75,\"math\":80}}");
    final JSONObject tom = JSONObject.parse("{\"name\":\"Tom\",\"gender\":\"M\",\"age\":12,\"class\":\"7B\",\"scores\":{\"english\":82,\"science\":87,\"humanity\":82,\"math\":80}}");

    @Test
    public void deltaWith_shuffledArrayOfMaps_withDifferencesIfOrderMatters() {
        JSONArray array = new JSONArray(alice, bob, carl, dave, ellen);
        JSONArray shuffled = array.shuffle();

        IJSONValue delta = array.deltaWith(shuffled, true);
        Logger.D(delta.toJSONString(null));
        //There shall always be some differences shown as JSONObject
        assertTrue(delta instanceof JSONObject, !delta.isEmpty());

        delta = array.deltaWith(shuffled, false);
        assertEquals(JSONArray.EMPTY, delta);
    }

    @Test
    public void deltaWithOrderNotMatters_arrayOfOneDifferentObject_getDifferencesOnly() {
        JSONArray array = new JSONArray(alice, bob, carl, dave, ellen);
        JSONArray other = new JSONArray(carl, alice, bob, ellen, tom);

        IJSONValue delta = array.deltaWith(other, false).getSorted(Comparator.naturalOrder());
        assertEquals("[{\"class\": [\"7C\",\"7B\"],\"name\": [\"Dave\",\"Tom\"],\"scores\": {\"math\": [76,80],\"science\": [71,87]}}]",
                delta.toJSONString(null));

        Map<String, Object> alterAlice = JSONObject.parse("{\"class\":\"7F\",\"scores\":{\"science\":76,\"humanity\":80}}");
        JSONObject alice2 = alice.withDelta(alterAlice);
        JSONArray other2 = new JSONArray(bob, alice2, ellen, tom, carl);
        delta = array.deltaWith(other2, false).getSorted(Comparator.naturalOrder());
        assertEquals("[{\"class\": [\"7C\",\"7B\"],\"name\": [\"Dave\",\"Tom\"],\"scores\": {\"math\": [76,80],\"science\": [71,87]}}"+
                        ",{\"class\": [\"7N\",\"7F\"],\"scores\": {\"english\": [89,null],\"humanity\": [85,80],\"math\": [90,null],\"science\": [77,76]}}]",
                delta.toJSONString(null));
    }
}
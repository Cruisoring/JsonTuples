package JsonTuples;

import io.github.cruisoring.tuple.Tuple;
import io.github.cruisoring.utility.Logger;
import org.junit.Test;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JSONObjectTest {

    @Test
    public void parse() {
        JSONObject obj = JSONObject.parse("{ \"age\": 123, \"other\": \"none\", \"name\": null }");
        String string = obj.toString();
        assertEquals("{\n" +
                "  \"age\": 123,\n" +
                "  \"other\": \"none\",\n" +
                "  \"name\": null\n" +
                "}", string);
    }

    @Test
    public void getObject() {
        JSONObject obj = JSONObject.parse("{ \"age\": 123, \"other\": \"none\", \"name\": null }");
        Object map = obj.getObject();
        assertTrue(map instanceof Map);
    }

    @Test
    public void getSorted() {
        JSONObject obj = JSONObject.parse("{ \"age\": 123, \"name\": null, \"other\": \"none\" }");
        JSONObject naturalOrdered = obj.getSorted(Comparator.naturalOrder());
        String string = naturalOrdered.toString();
        assertEquals("{\n" +
                "  \"age\": 123,\n" +
                "  \"name\": null,\n" +
                "  \"other\": \"none\"\n" +
                "}", string);

        JSONObject nullOrdered = obj.getSorted((Comparator<String>) null);
        assertTrue(nullOrdered.equals(naturalOrdered));

        JSONObject naturalReversed = obj.getSorted(naturalOrdered.nameComparator.reversed());
        string = naturalReversed.toString();
        assertEquals("{\n" +
                "  \"other\": \"none\",\n" +
                "  \"name\": null,\n" +
                "  \"age\": 123\n" +
                "}", string);
    }

    @Test
    public void getSortedWithOrderedNames() {
        JSONObject obj = JSONObject.parse("{ \"age\": 123, \"name\": null, \"other\": \"none\" }");
        JSONObject naturalOrdered = obj.getSorted(Arrays.asList("name", "other"));
        String string = naturalOrdered.toString();
        assertEquals("{\n" +
                "  \"name\": null,\n" +
                "  \"other\": \"none\",\n" +
                "  \"age\": 123\n" +
                "}", string);

        JSONObject naturalReversed = obj.getSorted(naturalOrdered.nameComparator.reversed());
        string = naturalReversed.toString();
        assertEquals("{\n" +
                "  \"age\": 123,\n" +
                "  \"other\": \"none\",\n" +
                "  \"name\": null\n" +
                "}", string);
    }

    @Test
    public void deltaWith() {
        JSONObject obj1 = JSONObject.parse("{ \"age\": 123, \"name\": null }");
        JSONObject obj2 = JSONObject.parse("{ \"age\": 24, \"name\": \"Tom\", \"other\": \"OK\" }");

        JSONObject delta = obj1.deltaWith(obj2);
        System.out.println(delta.toString());
    }

    @Test
    public void withDelta(){
        JSONObject object = (JSONObject) Parser.parse("{\n" +
                "  \"address\": null,\n" +
                "  \"scores\": {\n" +
                "    \"English\": 80,\n" +
                "    \"Science\": 88,\n" +
                "    \"Math\": 90\n" +
                "  },\n" +
                "  \"name\": \"test name\",\n" +
                "  \"id\": 123456,\n" +
                "  \"isActive\": true,\n" +
                "  \"class\": \"7A\"\n" +
                "}");
//        Logger.D(object.toString());

        Map<String, Object> map = new HashMap<String, Object>(){{
            put("name", "another");
            put("id", 778388);
            put("isActive", false);
            put("address", "11 ABC St, My City");
        }};

        JSONObject updated = object.withDelta(map);
        Logger.D(updated.toString());

        JSONObject expected = (JSONObject) Parser.parse("{\n" +
                "  \"address\": \"11 ABC St, My City\",\n" +
                "  \"name\": \"another\",\n" +
                "  \"id\": 778388,\n" +
                "  \"isActive\": false,\n" +
                "  \"scores\": {\n" +
                "    \"English\": 80,\n" +
                "    \"Science\": 88,\n" +
                "    \"Math\": 90\n" +
                "  },\n" +
                "  \"class\": \"7A\"\n" +
                "}");

        JSONObject delta = expected.deltaWith(updated);
        Logger.D(delta.toString());

        assertEquals(expected, updated);
    }

    @Test
    public void toJSONString() {
        JSONObject obj = JSONObject.parse("{ \"age\": 123, \"other\": \"none\", \"name\": null }");
        assertEquals("{\"age\": 123,\"other\": \"none\",\"name\": null}", obj.toJSONString(null));
        assertEquals("{\n  \"age\": 123,\n  \"other\": \"none\",\n  \"name\": null\n}", obj.toJSONString(""));
        assertEquals("{\n    \"age\": 123,\n    \"other\": \"none\",\n    \"name\": null\n  }", obj.toJSONString("  "));
        assertEquals("{\n      \"age\": 123,\n      \"other\": \"none\",\n      \"name\": null\n    }", obj.toJSONString("    "));

        obj = JSONObject.parse("{ \"age\": 123, \"other\": [123, \"abc\"], \"name\": null }");
        assertEquals("{\"age\": 123,\"other\": [123,\"abc\"],\"name\": null}", obj.toJSONString(null));
        assertEquals("{\n  \"age\": 123,\n  \"other\": [\n    123,\n    \"abc\"\n  ],\n  \"name\": null\n}", obj.toJSONString(""));
        assertEquals("{\n    \"age\": 123,\n    \"other\": [\n      123,\n      \"abc\"\n    ],\n    \"name\": null\n  }", obj.toJSONString("  "));
        assertEquals("{\n      \"age\": 123,\n      \"other\": [\n        123,\n        \"abc\"\n      ],\n      \"name\": null\n    }", obj.toJSONString("    "));
    }

    @Test
    public void testToString() {
    }

    @Test
    public void getHashCode(){
        JSONObject object = (JSONObject) Parser.parse("{\n" +
                "  \"address\": null,\n" +
                "  \"scores\": {\n" +
                "    \"English\": 80,\n" +
                "    \"Science\": 88,\n" +
                "    \"Math\": 90\n" +
                "  },\n" +
                "  \"name\": \"test name\",\n" +
                "  \"id\": 123456,\n" +
                "  \"isActive\": true,\n" +
                "  \"class\": \"7A\"\n" +
                "}");

        int superHashCode = ((Tuple)object).hashCode();
        int hashCode = object.hashCode();
        Logger.D("hashCode=%d, superHashCode=%d", hashCode, superHashCode);
    }
}
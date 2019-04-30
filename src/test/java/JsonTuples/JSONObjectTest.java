package JsonTuples;

import io.github.cruisoring.TypeHelper;
import io.github.cruisoring.logger.Logger;
import org.junit.Test;

import java.util.*;

import static io.github.cruisoring.Asserts.*;

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
        JSONObject naturalOrdered = (JSONObject) obj.getSorted(Arrays.asList("name", "other"));
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
        assertEquals("[{},[]]", JSONObject.EMPTY.deltaWith(JSONArray.EMPTY).toJSONString(null));
        assertEquals("[{},null]", JSONObject.EMPTY.deltaWith(JSONValue.Null).toJSONString(null));
        assertEquals("[null,{}]", JSONValue.Null.deltaWith(JSONObject.EMPTY).toJSONString(null));

        JSONObject obj1 = JSONObject.parse("{ \"age\": 123, \"name\": null }");
        assertEquals(JSONObject.EMPTY, obj1.deltaWith(obj1));
        //TODO: if MISSING shall be treated as equal with null?
        assertEquals("{\"age\": [123,null]}", obj1.deltaWith(JSONObject.EMPTY).toJSONString(null));
        assertEquals("{\"age\": [null,123]}", JSONObject.EMPTY.deltaWith(obj1).toJSONString(null));

        JSONObject obj11 = JSONObject.parse("{\"name\": null,  \"age\": 123, }");
        assertEquals(JSONObject.EMPTY, obj1.deltaWith(obj11));

        //TODO: to differentiate null or MISSING from JSONValue.Null?
        assertEquals("[{\"age\": 123,\"name\": null},null]", obj1.deltaWith(null).toJSONString(null));
        assertEquals("[{\"age\": 123,\"name\": null},null]", obj1.deltaWith(JSONValue.Null).toJSONString(null));

        assertEquals("[true,{\"age\": 123,\"name\": null}]", JSONValue.True.deltaWith(obj1).toJSONString(null));
        assertEquals("[{\"age\": 123,\"name\": null},[null,1]]", obj1.deltaWith(new JSONArray(JSONValue.Null, new JSONNumber(1))).toJSONString(null));


        JSONObject obj2 = JSONObject.parse("{ \"age\": 24, \"name\": \"Tom\", \"other\": \"OK\" }");

        IJSONValue delta = obj1.deltaWith(obj2, false).getSorted(Comparator.naturalOrder());
        assertEquals("{\"age\": [123,24],\"name\": [null,\"Tom\"],\"other\": [null,\"OK\"]}", delta.toJSONString(null));
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

        IJSONValue delta = expected.deltaWith(updated);
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
        String text = "{\n" +
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
                "}";
        JSONObject object1 = JSONObject.parse(text);
        JSONObject object2 = JSONObject.parse(text);

        int superHashCode = TypeHelper.deepHashCode(object1.asArray());;
        int hashCode = object2.hashCode();
        String string1 = object1.toString();
        assertEquals(hashCode, object1.hashCode());
        assertEquals(hashCode, string1.hashCode());
        assertTrue(hashCode != superHashCode);
    }

    @Test
    public void getSignatures() {
        String text = "{\n" +
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
                "}";
        JSONObject object1 = JSONObject.parse(text);
        Set<Integer> signature1 = new HashSet<>(object1.getSignatures());

        JSONObject object2 = object1.getSorted(Comparator.naturalOrder());
        Set<Integer> signature2 = new HashSet<>(object2.getSignatures());

        Set<Integer> common = new HashSet(Arrays.asList("\"address\": null".hashCode(), "\"name\": \"test name\"".hashCode(),
                "\"id\": 123456".hashCode(), "\"isActive\": true".hashCode(), "\"class\": \"7A\"".hashCode()));
        assertTrue(signature1.containsAll(common));
        assertTrue(signature2.containsAll(common));

        signature1.removeAll(signature2);
        signature2.removeAll(common);
        signature1.addAll(signature2);
        assertEquals(4, signature1.size());
    }

}
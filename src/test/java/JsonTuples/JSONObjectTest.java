package JsonTuples;

import io.github.cruisoring.TypeHelper;
import io.github.cruisoring.logger.Logger;
import org.junit.Test;

import java.util.*;

import static io.github.cruisoring.Asserts.*;
import static io.github.cruisoring.TypeHelper.deepToString;

public class JSONObjectTest {

    @Test
    public void parse_getImmutableMap() {
        JSONObject obj = JSONObject.parse("{ \"age\": 123, \"other\": null, \"name\": [\"Alex\", \"Tedd\"] }");
        String string = obj.toString();
        assertEquals("{\n" +
                "  \"age\": 123,\n" +
                "  \"other\": null,\n" +
                "  \"name\": [\n" +
                "    \"Alex\",\n" +
                "    \"Tedd\"\n" +
                "  ]\n" +
                "}", string);
        assertEquals(3, obj.size());
        assertTrue(obj.containsKey("age"), obj.containsValue(null), obj.containsValue(123));
        assertFalse(obj.containsKey("Age"), obj.containsKey(null), obj.containsValue("Alex"));
        assertEquals(123, obj.get("age"));
        assertEquals(null, obj.get("other"));
        assertEquals(null, obj.get(null));
        assertEquals(new Object[]{"Alex", "Tedd"}, obj.get("name"));
        assertException(() -> obj.put("age", 23), UnsupportedOperationException.class, JSONObject.JSONObject_UNMODIFIABLE);
        assertException(() -> obj.putAll(new HashMap<>()), UnsupportedOperationException.class, JSONObject.JSONObject_UNMODIFIABLE);
        assertException(() -> obj.remove("age"), UnsupportedOperationException.class, JSONObject.JSONObject_UNMODIFIABLE);
        assertException(() -> obj.clear(), UnsupportedOperationException.class, JSONObject.JSONObject_UNMODIFIABLE);
    }

    @Test
    public void testGetObject_getImmutableMap() {
        JSONObject obj = JSONObject.parse("{ \"age\": 123, \"other\": \"none\", \"name\": null }");
        Map<String, Object> map = (Map<String, Object>)obj.getObject();
        assertEquals(123, map.get("age"));
        assertEquals(null, map.get("name"));
        assertException(() -> map.put("",""), UnsupportedOperationException.class);
        assertException(() -> map.remove("age"), UnsupportedOperationException.class);
        assertException(() -> map.putAll(new HashMap<>()), UnsupportedOperationException.class);
        assertException(() -> map.clear(), UnsupportedOperationException.class);
    }

    @Test
    public void testAsMutableObject_canBeUpdated() {
        JSONObject obj = JSONObject.parse("{ \"age\": 123, \"other\": \"none\", \"name\": null, \"members\":[\"Alice\", \"Bob\"] }");
        Map<String, Object> map = (Map<String, Object>)obj.asMutableObject();
        assertEquals(123, map.get("age"));
        assertEquals(null, map.get("name"));
        map.put("age", 23);
        map.remove("name");
        List members = (List)map.get("members");
        members.remove("Alice");
        members.add(0, "Alan");
        members.add("Carter");

        JSONObject updated = Utilities.asJSONObject(map);
        assertEquals("{\"age\":23,\"other\":\"none\",\"members\":[\"Alan\",\"Bob\",\"Carter\"]}", updated.toJSONString(null));
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
        JSONObject naturalOrdered = (JSONObject) obj.getSorted("name", "other");
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
        assertEquals(JSONArray.EMPTY, obj1.deltaWith(obj1));
        //TODO: if MISSING shall be treated as equal with null?
        assertEquals("{\"age\":[123,null]}", obj1.deltaWith(JSONObject.EMPTY).toJSONString(null));
        assertEquals("{\"age\":[null,123]}", JSONObject.EMPTY.deltaWith(obj1).toJSONString(null));

        JSONObject obj11 = JSONObject.parse("{\"name\": null,  \"age\": 123, }");
        assertEquals(JSONArray.EMPTY, obj1.deltaWith(obj11));

        //TODO: to differentiate null or MISSING from JSONValue.Null?
        assertEquals("[{\"age\":123,\"name\":null},null]", obj1.deltaWith(null).toJSONString(null));
        assertEquals("[{\"age\":123,\"name\":null},null]", obj1.deltaWith(JSONValue.Null).toJSONString(null));

        assertEquals("[true,{\"age\":123,\"name\":null}]", JSONValue.True.deltaWith(obj1).toJSONString(null));
        assertEquals("[{\"age\":123,\"name\":null},[null,1]]", obj1.deltaWith(new JSONArray(JSONValue.Null, new JSONNumber(1))).toJSONString(null));


        JSONObject obj2 = JSONObject.parse("{ \"age\": 24, \"name\": \"Tom\", \"other\": \"OK\" }");

        IJSONValue delta = obj1.deltaWith(obj2, null).getSorted(Comparator.naturalOrder());
        assertEquals("{\"age\":[123,24],\"name\":[null,\"Tom\"],\"other\":[null,\"OK\"]}", delta.toJSONString(null));
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
        assertEquals("{\"age\":123,\"other\":\"none\",\"name\":null}", obj.toJSONString(null));
        assertEquals("{\n  \"age\": 123,\n  \"other\": \"none\",\n  \"name\": null\n}", obj.toJSONString(""));
        assertEquals("{\n    \"age\": 123,\n    \"other\": \"none\",\n    \"name\": null\n  }", obj.toJSONString("  "));
        assertEquals("{\n      \"age\": 123,\n      \"other\": \"none\",\n      \"name\": null\n    }", obj.toJSONString("    "));

        obj = JSONObject.parse("{ \"age\": 123, \"other\": [123, \"abc\"], \"name\": null }");
        assertEquals("{\"age\":123,\"other\":[123,\"abc\"],\"name\":null}", obj.toJSONString(null));
        assertEquals("{\n  \"age\": 123,\n  \"other\": [\n    123,\n    \"abc\"\n  ],\n  \"name\": null\n}", obj.toJSONString(""));
        assertEquals("{\n    \"age\": 123,\n    \"other\": [\n      123,\n      \"abc\"\n    ],\n    \"name\": null\n  }", obj.toJSONString("  "));
        assertEquals("{\n      \"age\": 123,\n      \"other\": [\n        123,\n        \"abc\"\n      ],\n      \"name\": null\n    }", obj.toJSONString("    "));
    }

    @Test
    public void testToString() {
        JSONObject obj = JSONObject.parse("{ \"age\": 123, \"other\": [\"none\", true], \"name\": {\"first\":\"Tom\", \"last\":\"Clarks\"} }");
        assertEquals("{\n" +
                "  \"age\": 123,\n" +
                "  \"other\": [\n" +
                "    \"none\",\n" +
                "    true\n" +
                "  ],\n" +
                "  \"name\": {\n" +
                "    \"first\": \"Tom\",\n" +
                "    \"last\": \"Clarks\"\n" +
                "  }\n" +
                "}", obj.toString());
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
        NamedValue address = object1.getValue(0);
        NamedValue scores = object1.getValue(1);
        NamedValue name = object1.getValue(2);
        NamedValue id = object1.getValue(3);
        NamedValue isActive = object1.getValue(4);
        NamedValue classNamedValue = object1.getValue(5);

        Set<Integer> signature1 = object1.getSignatures();
        Logger.D("object1: %s\n\taddress: %s\n\tscores: %s\n\tname: %s\n\tid: %s\n\tisActive: %s\n\tclass: %s",
                deepToString(signature1), address.hashCode(), scores.hashCode(), name.hashCode(), id.hashCode(), isActive.hashCode(), classNamedValue.hashCode());
//                deepToString(address.getSignatures()), deepToString(scores.getSignatures()), deepToString(name.getSignatures()),
//                deepToString(id.getSignatures()), deepToString(isActive.getSignatures()), deepToString(classNamedValue.getSignatures()));
        assertTrue(signature1.size() == 7,
                signature1.containsAll(Arrays.asList(object1.hashCode(), address.hashCode(), scores.hashCode(), name.hashCode(), id.hashCode(), isActive.hashCode(), classNamedValue.hashCode())));

        JSONObject object2 = object1.getSorted(Comparator.naturalOrder());
        Set<Integer> signature2 = new HashSet<>(object2.getSignatures());
        NamedValue sortedScores = NamedValue.parse("\"scores\": {\"English\":80,\"Math\": 90,\"Science\": 88}");
        assertTrue(signature2.size() == 7,
                signature2.containsAll(Arrays.asList(object2.hashCode(), address.hashCode(), name.hashCode(), id.hashCode(), isActive.hashCode(), classNamedValue.hashCode())),
                signature2.contains(sortedScores.hashCode()));
    }

}
package jsonTuples;

import io.github.cruisoring.Revokable;
import io.github.cruisoring.TypeHelper;
import io.github.cruisoring.logger.LogLevel;
import io.github.cruisoring.logger.Logger;
import io.github.cruisoring.logger.Measurement;
import io.github.cruisoring.utility.ArrayHelper;
import io.github.cruisoring.utility.DateTimeHelper;
import io.github.cruisoring.utility.ResourceHelper;
import io.github.cruisoring.utility.SetHelper;
import org.junit.Ignore;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        assertAllTrue(obj.containsKey("age"), obj.containsValue(null), obj.containsValue(123));
        assertAllFalse(obj.containsKey("Age"), obj.containsKey(null), obj.containsValue("Alex"));
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
        assertAllTrue(nullOrdered.equals(naturalOrdered));

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

        JSONObject obj11 = JSONObject.parse("{\"name\": null,  \"age\": 123}");   //Empty NamedValue is allowed at the closing tag?
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
    public void testGetLeafCount(){
        assertEquals(0, JSONObject.EMPTY.getLeafCount());
        assertEquals(6, JSONObject.parse("{ \"age\": 24, \"name\": null, \"other\": \"OK\" }").getLeafCount());
        assertEquals(10, JSONObject.parse("{ \"age\": 123, \"other\": [\"none\", true], \"name\": {\"first\":\"Tom\", \"last\":\"Clarks\"} }").getLeafCount());
        assertEquals(10, JSONObject.parse("{ \"age\": null, \"other\": [null, null], \"name\": {\"first\":null, \"last\":null} }").getLeafCount());

        assertEquals(10, JSONObject.parse("{ \"age\": 123, \"other\": [\"none\", null], \"name\": {\"first\":null, \"last\":\"none\"} }").getLeafCount());

    }

    @Test
    public void getHashCode(){
        String text = "{\"address\":null,\"scores\":{\"English\":80,\"Science\":88,\"Math\":90},\"name\":\"test name\",\"id\":123456,\"isActive\":true,\"class\":\"7A\"}";
        JSONObject object1 = JSONObject.parse(null, text);
        Logger.D("object1: %s", object1.toJSONString(null));
        Integer[] childrenHashCodes1 = new Integer[]{
                NamedValue.parse("\"address\":null").hashCode(),
                NamedValue.parse("\"scores\":{\"English\":80,\"Science\":88,\"Math\":90}").hashCode(),
                NamedValue.parse("\"name\":\"test name\"").hashCode(),
                NamedValue.parse("\"id\":123456").hashCode(),
                NamedValue.parse("\"isActive\":true").hashCode(),
                NamedValue.parse("\"class\":\"7A\"").hashCode()
        };
        Set<Integer> signatures1 = SetHelper.asSet(childrenHashCodes1);
        signatures1.add(TypeHelper.deepHashCode(childrenHashCodes1));
        assertEquals(signatures1, object1.getSignatures());

        Comparator<String> naturalOrder = Comparator.naturalOrder();
        JSONObject object2 = JSONObject.parse(naturalOrder, text);
        Logger.D("object2: %s", object2.toJSONString(null));
        Integer[] childrenHashCodes2 = new Integer[] {
                childrenHashCodes1[0],
                childrenHashCodes1[5],
                childrenHashCodes1[3],
                childrenHashCodes1[4],
                childrenHashCodes1[2],
                NamedValue.parse("\"scores\":{\"English\":80,\"Math\":90,\"Science\":88}").hashCode()
        };
        assertEquals(TypeHelper.deepHashCode(childrenHashCodes2), object2.hashCode());

        Set<Integer> signatures2 = SetHelper.asSet(childrenHashCodes2);
        signatures2.add(TypeHelper.deepHashCode(childrenHashCodes2));
        assertEquals(signatures2, object2.getSignatures());

        Set<Integer> symDif = SetHelper.symmetricDifference(signatures1, signatures2);
        List<Integer> expected = Arrays.asList(childrenHashCodes1[1], childrenHashCodes2[5], object1.hashCode(), object2.hashCode());
        assertAllTrue(symDif.size() == 4 && symDif.containsAll(expected));

        assertNotEquals(object1.toJSONString(null), object2.toJSONString(null));
        assertNotEquals(object1.getSignatures(), object2.getSignatures());
    }

    @Test
    public void testGetSignatures() {
        String text = "{\"address\": null, \"scores\": {\"English\":80,\"Science\":88,\"Math\":90}, \"name\": \"test name\" ,\"id\" :123456, \"isActive\": true, \"class\": \"7A\"}";
        JSONObject object1 = JSONObject.parse(text);
        NamedValue address = object1.getValue(0);
        NamedValue scores = object1.getValue(1);
        NamedValue name = object1.getValue(2);
        NamedValue id = object1.getValue(3);
        NamedValue isActive = object1.getValue(4);
        NamedValue classNamedValue = object1.getValue(5);

        Set<Integer> signature1 = object1.getSignatures();
        Logger.I(object1.toJSONString(null));
        Logger.D("address: %s\tscores: %s\tname: %s\tid: %s\tisActive: %s\tclass: %s\n\tobject1: %s\n",
                address.hashCode(), scores.hashCode(), name.hashCode(), id.hashCode(), isActive.hashCode(), classNamedValue.hashCode(), deepToString(signature1));
        assertAllTrue(signature1.size() == 7,
                signature1.containsAll(Arrays.asList(object1.hashCode(), address.hashCode(), scores.hashCode(), name.hashCode(), id.hashCode(), isActive.hashCode(), classNamedValue.hashCode())));

        JSONObject object2 = object1.getSorted(Comparator.naturalOrder());
        Logger.I(object2.toJSONString(null));
        Set<Integer> signature2 = object2.getSignatures();
        NamedValue sortedScores = NamedValue.parse("\"scores\": {\"English\":80,\"Math\": 90,\"Science\": 88}");
        Logger.D("address: %s\tsortedScores: %s\tname: %s\tid: %s\tisActive: %s\tclass: %s\n\tobject2: %s\n",
                address.hashCode(), sortedScores.hashCode(), name.hashCode(), id.hashCode(), isActive.hashCode(), classNamedValue.hashCode(), deepToString(signature2));
        assertAllTrue(signature2.size() == 7,
                signature2.containsAll(Arrays.asList(object2.hashCode(), address.hashCode(), name.hashCode(), id.hashCode(), isActive.hashCode(), classNamedValue.hashCode())),
                signature2.contains(sortedScores.hashCode()));
    }

    @Test @Ignore
    public void testDeltaWith_ofSimilarFiles(){
        String jsonText = ResourceHelper.getTextFromResourceFile("catalog.json");
        String modifiedText = ResourceHelper.getTextFromResourceFile("c:/temp/modified.json");

        JSONObject original = Logger.M(Measurement.start("Parse JSON of %dk", jsonText.length()/1024), () -> JSONObject.parse(jsonText));
        JSONObject modified = Logger.M(Measurement.start("Parse JSON of %dk", modifiedText.length()/1024), () -> JSONObject.parse(modifiedText));

        IJSONValue delta = Logger.M(Measurement.start("deltaWith()"),
                () -> original.deltaWith(modified, "index"));
        Matcher matcher = Pattern.compile("changedValue\\d{3}").matcher(delta.toJSONString(null));
        int count = 0;
        while (matcher.find()){
            count++;
        }

        assertTrue(count>0, "There shall be 1000 differences?");
        Logger.W("There are %d differences", count);
    }

    @Test @Ignore
    public void testDeltaWith_ofLargeObjects() {
        String jsonText = ResourceHelper.getTextFromResourceFile("catalog.json");
        int changes = 1000;
        int jsonTextLength = jsonText.length();
        JSONObject original = Logger.M(Measurement.start("Parse JSON of %dk", jsonTextLength/1024), () -> JSONObject.parse(jsonText));
        int originalLeafCount = original.getLeafCount();

        try(Revokable revokable = Logger.setLevelInScope(LogLevel.debug)) {
            for (int i = 0; i < 10; i++) {
                Map<String, Object> modifiableMap = (Map<String, Object>) Logger.M(Measurement.start("Get Modifiable Map"), () -> original.asMutableObject());
                Object packagesList = modifiableMap.get("packages");

                Logger.M(Measurement.start("Modify %d leaf values", changes), () -> modifyAndShuffle(packagesList, changes));

                Object shuffledArray = Logger.M(Measurement.start("Shuffle packages list as array"), () -> ArrayHelper.shuffle(((List) packagesList).toArray()));
                modifiableMap.put("packages", shuffledArray);
                JSONObject modifiedObject = Logger.M(Measurement.start("jsonify() back to JSONObject"), () ->(JSONObject) Utilities.jsonify(modifiableMap));
                int modifiedLeafCount = modifiedObject.getLeafCount();
                Logger.D("Save modified JSONObject with %d leaves to %s", modifiedLeafCount,
                        Logger.M(Measurement.start("save modified JSON"), () ->ResourceHelper.saveTextToTargetFile(modifiedObject.toString(), "modified.json")));

                IJSONValue delta = Logger.M(Measurement.start("deltaWith() between two JSONObjects", originalLeafCount, modifiedLeafCount),
                        () -> original.deltaWith(modifiedObject, "pos"));
                assertNotNull(delta, "Failed to get result.");
                String deltaString = delta.toString();
                Matcher matcher = Pattern.compile("changedValue\\d{3}").matcher(deltaString);
                int count = 0;
                while (matcher.find()){
                    count++;
                }

                String timeStamp = DateTimeHelper.asString(LocalDateTime.now(), "MMddHHmmss");
                String modifedFile = ResourceHelper.saveTextToTargetFile(modifiedObject.toString(), String.format("modified%s.json", timeStamp));
                String deltaFile = ResourceHelper.saveTextToTargetFile(delta.toString(), String.format("delta%s.json", timeStamp));
                Logger.I("There are %d changed values found, the modified file is saved as %s, and delta is saved as %s.", count, modifedFile, deltaFile);
            }
        }finally {
            Measurement.purge(LogLevel.warning);
        }
    }

    public Object modifyAndShuffle(Object modifiableObject, int changeCount) {
        Random random = new Random();
        for (int i = 0; i < changeCount; i++) {
            String newValue = String.format("changedValue%03d", i);
            while(true){
                if(replaceAnyLeafValue(modifiableObject, newValue, random))
                    break;
            }
        }
        return modifiableObject;
    }

    private boolean replaceAnyLeafValue(Object object, String newValue, Random random){
        if(object instanceof List) {
            List list = (List)object;
            int size = list.size();
            if(size == 0){
                return false;
            }

            Object child = list.get(random.nextInt(size));
            if (child == null) {
                list.remove(child);
                list.add(newValue);
                Logger.V("%s of List is replaced by %s", "null", newValue);
                return true;
            } else if (child instanceof List || child instanceof Map) {
                return replaceAnyLeafValue(child, newValue, random);
            } else if (child.toString().contains("changedValue")) {
                return false;
            } else {
                list.remove(child);
                list.add(newValue);
                Logger.V("%s of List is replaced by %s", child, newValue);
                return true;
            }
        } else if (object instanceof Map) {
            try {
                Map map = (Map) object;
                int size = map.size();
                if (size == 0) {
                    return false;
                }

                Object[] keys = map.keySet().toArray();
                Object key = keys[random.nextInt(keys.length)];
                Object value = map.get(key);
                if (value == null) {
                    map.put(key, newValue);
                    Logger.V("%s: value %s is replaced by %s", key, "null", newValue);
                    return true;
                } else if (value instanceof List || value instanceof Map) {
                    return replaceAnyLeafValue(value, newValue, random);
                } else if (value != null && value.toString().contains("changedValue")) {
                    return false;
                } else {
                    map.put(key, newValue);
                    Logger.V("%s: value %s is replaced by %s", key, value, newValue);
                    return true;
                }
            } catch (Exception e) {
                Logger.D(e);
                return false;
            }
        } else {
            return false;
        }
    }
}
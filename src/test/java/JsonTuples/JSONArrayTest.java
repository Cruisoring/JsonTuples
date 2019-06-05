package JsonTuples;

import io.github.cruisoring.Revokable;
import io.github.cruisoring.logger.LogLevel;
import io.github.cruisoring.logger.Logger;
import io.github.cruisoring.utility.ResourceHelper;
import io.github.cruisoring.utility.SetHelper;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.github.cruisoring.Asserts.*;
import static io.github.cruisoring.TypeHelper.deepToString;

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
        JSONArray array = JSONArray.parse("[123, \"abc\", null, {\"id\": 32, \"note\": \"none\"}, [false, \"x\"]]");
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
        JSONArray array = JSONArray.parse("[123, \"abc\"]");
        assertEquals("[\n  123,\n  \"abc\"\n]", array.toString());
        assertEquals("[123,\"abc\"]", array.toJSONString(null));
        assertEquals("[\n    123,\n    \"abc\"\n  ]", array.toJSONString("  "));
        assertEquals("[\n      123,\n      \"abc\"\n    ]", array.toJSONString("    "));
    }

    String steps = ResourceHelper.getTextFromResourceFile("steps.json");
    @Test
    public void parseArray() {
        JSONArray array = JSONArray.parse(steps);
        assertEquals(10, array.getLength());
        Logger.D(array.toString());

        JSONArray sorted = array.getSorted(Comparator.naturalOrder());
        Logger.D(sorted.toString());

        IJSONValue delta = array.deltaWith(sorted);
        assertEquals(JSONArray.EMPTY, delta);
    }

    @Test
    public void testMutability_tryUpdate_throwException(){
        JSONArray array = JSONArray.parse("[1, null, true, [\"abc\", {\"id\":111, \"notes\":null}], {}, 3456]");
        assertException(() -> array.remove(null), UnsupportedOperationException.class, JSONArray.JSONArray_UNMODIFIABLE);
        assertException(() -> array.removeAll(Arrays.asList(true, 3456)), UnsupportedOperationException.class, JSONArray.JSONArray_UNMODIFIABLE);
        assertException(() -> array.clear(), UnsupportedOperationException.class, JSONArray.JSONArray_UNMODIFIABLE);
        assertException(() -> array.retainAll(new ArrayList<>()), UnsupportedOperationException.class, JSONArray.JSONArray_UNMODIFIABLE);
        assertException(() -> array.add(array.getValue(0)), UnsupportedOperationException.class, JSONArray.JSONArray_UNMODIFIABLE);
        assertException(() -> array.addAll(new ArrayList<>()), UnsupportedOperationException.class, JSONArray.JSONArray_UNMODIFIABLE);
    }

    @Test
    public void testGetObject() {
        JSONArray array = JSONArray.parse("[33., null, false, [null, \"ok\", 123], {\"result\":\"good\"}]");
        Object[] objArray = (Object[])array.getObject();
        assertEquals(5, objArray.length);
        assertEquals(null, objArray[1]);
        assertEquals(false, objArray[2]);
        assertEquals(new Object[]{null, "ok", 123}, objArray[3]);
        Map<String, Object> map = (Map)objArray[4];
        assertTrue(map.size() == 1, map.get("result").equals("good"), map.get("else")==null);
        assertException(() -> map.put("ok", true), UnsupportedOperationException.class);

        //Updating the Object[] would not affect the JSONArray.
        objArray[0] = null;
        Object[] obj1 = (Object[])array.getObject();
        assertNotEquals(objArray, obj1);
        checkStates(obj1[0] != null);
    }

    @Test
    public void testAsMutableObject() {
        JSONArray array = JSONArray.parse("[1, null, true, [\"abc\", {\"id\":111, \"notes\":null}], {}, 3456]");
        List list = (List)array.asMutableObject();
        assertEquals(6, list.size());
        //Update the List
        list.add(2, false);
        list.remove(3);
        List subList = (List)list.get(3);
        Map<String, Object> mapOfSubList = (Map<String, Object>)subList.get(1);
        assertTrue(mapOfSubList.get("id").equals(111), mapOfSubList.get("notes")==null);
        mapOfSubList.remove("notes");
        mapOfSubList.put("id", 222);
        mapOfSubList.put("name", "Bill");
        Map<String, Object> map4 = (Map)list.get(4);
        map4.put("value", "something");
        list.remove(Integer.valueOf(1));
        list.add(33);

        JSONArray newArray = Utilities.asJSONArrayFromCollection(list);
        assertEquals("[null,false,[\"abc\",{\"id\":222,\"name\":\"Bill\"}],{\"value\":\"something\"},3456,33]", newArray.toJSONString(null));
    }

    @Test
    public void shuffle() {
        Object[] objects = new Object[]{null, 99, true, 2.3, "Good", 'a', false, new int[]{1, 2}};
        JSONArray array = Utilities.asJSONArrayFromArray(objects);
        Logger.D(array.toJSONString(null));

        JSONArray shuffled = array.shuffle();
        Logger.D(shuffled.toJSONString(null));

        //For array of 8 elements, shuffle() shall always get an array containing same set elements but with different orders
        assertEquals(array, shuffled);
        assertTrue(array.deltaWith(shuffled, "").isEmpty(), array.deltaWith(shuffled, "index").isEmpty());
        assertFalse(array.deltaWith(shuffled, null).isEmpty(), array.deltaWith(shuffled, "index+").isEmpty());

        try {
            Revokable.register(() -> JSONArray.defaultIndexName, v -> JSONArray.defaultIndexName =v, "");
            assertTrue(array.equals(shuffled));

            Revokable.register(() -> JSONArray.defaultIndexName, v -> JSONArray.defaultIndexName =v, "+index");
            assertFalse(array.equals(shuffled));

            Revokable.register(() -> JSONArray.defaultIndexName, v -> JSONArray.defaultIndexName =v, "index");
            assertTrue(array.equals(shuffled));

            Revokable.register(() -> JSONArray.defaultIndexName, v -> JSONArray.defaultIndexName =v, null);
            assertFalse(array.equals(shuffled));
        }finally {
            Revokable.revokeAll();
        }

        assertTrue(array.equals(shuffled));
    }

    @Test
    public void testDeltaWith_arraysOfSameDifferences(){
        assertEquals("[[1,4],[2,5],[3,6]]",
                JSONArray.parse("[1, 2, 3]").deltaWith(JSONArray.parse("[4, 5, 6]"), "").toJSONString(null));
        assertEquals("[[4,1],[5,2],[6,3]]",
                JSONArray.parse("[4, 5, 6]").deltaWith(JSONArray.parse("[1, 2, 3]"), "").toJSONString(null));
        assertEquals("[[1,5],[2,6],[3,null]]",
                JSONArray.parse("[1, 2, 3, 4]").deltaWith(JSONArray.parse("[4, 5, 6]"), "").toJSONString(null));
        assertEquals("[[4,1],[5,2],[6,null]]",
                JSONArray.parse("[3, 4, 5, 6]").deltaWith(JSONArray.parse("[1, 2, 3]"), "").toJSONString(null));
    }

    @Test
    public void deltaWith_arrayOfSimpleValues() {
        Object[] objects = new Object[]{null, 99, true, 2.3, "Good", 'a', false, new int[]{1, 2}};
        JSONArray array = Utilities.asJSONArrayFromArray(objects);
        //The order of the elements shall always been changed
        JSONArray shuffled = array.shuffle();

        IJSONValue delta = array.deltaWith(shuffled, null);
        Logger.D("array: %s\nshaffled: %s\ndelta: %s", array.toJSONString(null), shuffled.toJSONString(null), delta.toJSONString(null));
        //There shall always be some differences shown as JSONObject
        assertTrue(delta instanceof JSONObject, !delta.isEmpty());

        //No difference when order doesn't matter
        assertEquals(JSONArray.EMPTY, array.deltaWith(shuffled, ""));

        //There always always be some element indexes pairs identified by "index"
        IJSONValue delta2 = array.deltaWith(shuffled, "index");
        String delta2String = delta2.toJSONString(null);
        Matcher matcher = Pattern.compile("\\{\\\"index\\\":\\[\\d,\\d\\]}").matcher(delta2String);
        int count = 0;
        while (matcher.find()){
            count++;
        }
        assertEquals(delta2.getLength(), count);
        Logger.D("delta2 with %d matches: %s", count, delta2String);
    }

    final JSONObject alice = JSONObject.parse("{\"name\":\"Alice\",\"gender\":\"F\",\"age\":12,\"class\":\"7N\",\"scores\":{\"english\":89,\"science\":77,\"humanity\":85,\"math\":90}}");
    final JSONObject bob = JSONObject.parse("{\"gender\":\"M\",\"name\":\"Bob\",\"age\":13,\"class\":\"7C\",\"scores\":{\"english\":76,\"science\":62,\"humanity\":62,\"math\":80}}");
    final JSONObject carl = JSONObject.parse("{\"age\":12,\"class\":\"7B\",\"name\":\"Carl\",\"gender\":\"M\",\"scores\":{\"english\":77,\"science\":66,\"humanity\":87,\"math\":91}}");
    final JSONObject dave = JSONObject.parse("{\"class\":\"7C\",\"name\":\"Dave\",\"gender\":\"M\",\"age\":12,\"scores\":{\"english\":82,\"science\":71,\"humanity\":82,\"math\":76}}");
    final JSONObject ellen = JSONObject.parse("{\"age\":12,\"class\":\"7B\",\"scores\":{\"english\":82,\"science\":87,\"humanity\":75,\"math\":80},\"name\":\"Ellen\",\"gender\":\"F\"}");
    final JSONObject tom = JSONObject.parse("{\"gender\":\"M\",\"age\":12,\"name\":\"Tom\",\"class\":\"7B\",\"scores\":{\"english\":82,\"science\":87,\"humanity\":82,\"math\":80}}");

    @Test
    public void testGetSignatures(){
        JSONArray array = new JSONArray(alice, bob, carl, dave, ellen);
        Logger.D("signatures: %s\narray: %s\n\talice: %s\n\tbob: %s\n\tcarl: %s\n\tdave: %s\n\tellen: %s\n\t",
                deepToString(array.getSignatures()),
                array.hashCode(), alice.hashCode(), bob.hashCode(), carl.hashCode(), dave.hashCode(), ellen.hashCode());
        assertEquals(SetHelper.asSet(array.hashCode(), alice.hashCode(), bob.hashCode(), carl.hashCode(), dave.hashCode(), ellen.hashCode()),
                array.getSignatures());
    }

    @Test
    public void compareTwoArrays_sameSizeWithElementDifferences_returnNonEmpty(){
        JSONArray array = new JSONArray(alice, bob, carl, dave, ellen);
        JSONArray array2 = new JSONArray(
                alice.withDelta("{\"age\":13,\"class\":\"7F\"}"),
                dave.withDelta("{\"name\":\"Darothy\",\"gender\":\"F\"}"),
                bob.withDelta("{\"scores\":{\"english\":77,\"science\":99,\"humanity\":62,\"math\":80}}"),
                ellen,
                carl.withDelta("{\"age\":11}")
        );
        Logger.V("array2: %s", array2);
        IJSONable delta1 = array.deltaWith(array2, null);
        IJSONable delta2 = array.deltaWith(array2, "");
        IJSONable delta3 = array.deltaWith(array2, "pos");
        IJSONable delta4 = array.deltaWith(array2, "+pos");
        String delta1String = delta1.toJSONString(null);
        String delta2String = delta2.toJSONString(null);
        String delta3String = delta3.toJSONString(null);
        String delta4String = delta4.toJSONString(null);
        Logger.D("delta1: %s\ndelta2: %s\ndelta3: %s\ndelta4: %s", delta1String, delta2String, delta3String, delta4String);

        assertEquals("{\"0\":{\"class\":[\"7N\",\"7F\"],\"age\":[12,13]}," +
                        "\"1\":{\"gender\":[\"M\",\"F\"],\"scores\":{\"science\":[62,71],\"english\":[76,82],\"math\":[80,76],\"humanity\":[62,82]},\"name\":[\"Bob\",\"Darothy\"],\"age\":[13,12]}," +
                        "\"2\":{\"scores\":{\"science\":[66,99],\"math\":[91,80],\"humanity\":[87,62]},\"name\":[\"Carl\",\"Bob\"],\"class\":[\"7B\",\"7C\"],\"age\":[12,13]}," +
                        "\"3\":{\"gender\":[\"M\",\"F\"],\"scores\":{\"science\":[71,87],\"math\":[76,80],\"humanity\":[82,75]},\"name\":[\"Dave\",\"Ellen\"],\"class\":[\"7C\",\"7B\"]}," +
                        "\"4\":{\"gender\":[\"F\",\"M\"],\"scores\":{\"science\":[87,66],\"english\":[82,77],\"math\":[80,91],\"humanity\":[75,87]},\"name\":[\"Ellen\",\"Carl\"],\"age\":[12,11]}}",
                delta1String);
        assertEquals("[{\"age\":[12,13],\"class\":[\"7N\",\"7F\"]},{\"scores\":{\"english\":[76,77],\"science\":[62,99]}},{\"age\":[12,11]},{\"name\":[\"Dave\",\"Darothy\"],\"gender\":[\"M\",\"F\"]}]",
                delta2String);
        assertEquals("[{\"pos\":[0,0],\"age\":[12,13],\"class\":[\"7N\",\"7F\"]},{\"pos\":[1,2],\"scores\":{\"english\":[76,77],\"science\":[62,99]}},{\"pos\":[2,4],\"age\":[12,11]},{\"pos\":[3,1],\"name\":[\"Dave\",\"Darothy\"],\"gender\":[\"M\",\"F\"]}]",
                delta3String);
         assertEquals("[{\"+pos\":[0,0],\"age\":[12,13],\"class\":[\"7N\",\"7F\"]},{\"+pos\":[1,2],\"scores\":{\"english\":[76,77],\"science\":[62,99]}},{\"+pos\":[2,4],\"age\":[12,11]},{\"+pos\":[3,1],\"name\":[\"Dave\",\"Darothy\"],\"gender\":[\"M\",\"F\"]},{\"+pos\":[4,3]}]",
                delta4String);

         assertEquals("{\"0\":{\"class\":[\"7F\",\"7N\"],\"age\":[13,12]}," +
                         "\"1\":{\"gender\":[\"F\",\"M\"],\"scores\":{\"science\":[71,62],\"english\":[82,76],\"math\":[76,80],\"humanity\":[82,62]},\"name\":[\"Darothy\",\"Bob\"],\"age\":[12,13]}," +
                         "\"2\":{\"scores\":{\"science\":[99,66],\"math\":[80,91],\"humanity\":[62,87]},\"name\":[\"Bob\",\"Carl\"],\"class\":[\"7C\",\"7B\"],\"age\":[13,12]}," +
                         "\"3\":{\"gender\":[\"F\",\"M\"],\"scores\":{\"science\":[87,71],\"math\":[80,76],\"humanity\":[75,82]},\"name\":[\"Ellen\",\"Dave\"],\"class\":[\"7B\",\"7C\"]}," +
                         "\"4\":{\"gender\":[\"M\",\"F\"],\"scores\":{\"science\":[66,87],\"english\":[77,82],\"math\":[91,80],\"humanity\":[87,75]},\"name\":[\"Carl\",\"Ellen\"],\"age\":[11,12]}}",
                 array2.deltaWith(array, null).toJSONString(null));
         assertEquals("[{\"age\":[13,12],\"class\":[\"7F\",\"7N\"]},{\"name\":[\"Darothy\",\"Dave\"],\"gender\":[\"F\",\"M\"]},{\"scores\":{\"english\":[77,76],\"science\":[99,62]}},{\"age\":[11,12]}]",
                 array2.deltaWith(array, "").toJSONString(null));
         assertEquals("[{\"?\":[0,0],\"age\":[13,12],\"class\":[\"7F\",\"7N\"]},{\"?\":[1,3],\"name\":[\"Darothy\",\"Dave\"],\"gender\":[\"F\",\"M\"]},{\"?\":[2,1],\"scores\":{\"english\":[77,76],\"science\":[99,62]}},{\"?\":[4,2],\"age\":[11,12]}]",
                 array2.deltaWith(array, "?").toJSONString(null));
         assertEquals("[{\"+\":[0,0],\"age\":[13,12],\"class\":[\"7F\",\"7N\"]},{\"+\":[1,3],\"name\":[\"Darothy\",\"Dave\"],\"gender\":[\"F\",\"M\"]},{\"+\":[2,1],\"scores\":{\"english\":[77,76],\"science\":[99,62]}},{\"+\":[3,4]},{\"+\":[4,2],\"age\":[11,12]}]",
                 array2.deltaWith(array, "+").toJSONString(null));
    }

    @Test
    public void compareTwoArrays_difSizeWithElementDifferences_returnNonEmpty(){
        JSONArray array = new JSONArray(alice, bob, ellen);
        JSONArray array2 = new JSONArray(
                alice.withDelta("{\"age\":13,\"class\":\"7F\"}"),
                ellen
        );
        Logger.D("array2: %s", array2);
        IJSONable delta1 = array.deltaWith(array2, "");
        IJSONable delta2 = array2.deltaWith(array, "index");
        IJSONable delta3 = array2.deltaWith(array, "+pos");
        Logger.D("delta1: %s\ndelta2: %s\ndelta3: %s", delta1, delta2, delta3);

        assertEquals("[{\"age\":[12,13],\"class\":[\"7N\",\"7F\"]},[{\"name\":\"Bob\",\"gender\":\"M\",\"age\":13,\"class\":\"7C\",\"scores\":{\"english\":76,\"science\":62,\"humanity\":62,\"math\":80}},null]]",
                delta1.toJSONString(null));
        assertEquals("[{\"index\":[0,0],\"age\":[13,12],\"class\":[\"7F\",\"7N\"]},{\"-1\":null,\"1\":{\"name\":\"Bob\",\"gender\":\"M\",\"age\":13,\"class\":\"7C\",\"scores\":{\"english\":76,\"science\":62,\"humanity\":62,\"math\":80}}}]",
                delta2.toJSONString(null));
        assertEquals("[{\"+pos\":[0,0],\"age\":[13,12],\"class\":[\"7F\",\"7N\"]},{\"+pos\":[1,2]},{\"-1\":null,\"1\":{\"name\":\"Bob\",\"gender\":\"M\",\"age\":13,\"class\":\"7C\",\"scores\":{\"english\":76,\"science\":62,\"humanity\":62,\"math\":80}}}]",
                delta3.toJSONString(null));
    }

    @Test
    public void compareTwoArrays_objectWithDifferentOrders_noDifferent(){
        JSONArray array = new JSONArray(alice, bob, carl, dave, ellen);
        JSONArray array2 = array.getSorted(Comparator.naturalOrder());
        Logger.V("array2: %s", array2);
        assertTrue(array.deltaWith(array2).isEmpty());

        JSONArray array3 = new JSONArray(dave, alice.getSorted(Comparator.naturalOrder()), bob.getSorted(new OrdinalComparator<>("class", "age", "name")),
                carl.getSorted(new OrdinalComparator<>("id", "class", "name", "age")), ellen.getSorted((Comparator<String>) Comparator.naturalOrder().reversed()));
        Logger.V("array3: %s", array3);
        assertTrue(array.deltaWith(array3, "").isEmpty(),
                array3.deltaWith(array2, "").isEmpty());

        IJSONValue delta1 = array.deltaWith(array3, "index");
        IJSONValue delta2 = array3.deltaWith(array2, "index");
        assertTrue(delta1.isEmpty(), delta2.isEmpty());
        Logger.D("delta1: %s\ndelta2: %s", delta1, delta2);
    }

    @Test
    public void deltaWith_shuffledArrayOfMaps_withDifferencesIfOrderMatters() {
        JSONArray array = new JSONArray(alice, bob, carl, dave, ellen);
        Logger.D(deepToString(array.getDeepIndexes()));

        JSONArray shuffled = array.shuffle();

        IJSONValue delta = array.deltaWith(shuffled, "+");
        Logger.D(delta.toJSONString(null));
        //There shall always be some differences shown as JSONObject
        assertTrue(delta instanceof JSONArray, !delta.isEmpty());

        delta = array.deltaWith(shuffled, "");
        assertEquals(JSONArray.EMPTY, delta);
    }

    @Test
    public void deltaWithOrderNotMatters_arrayOfOneDifferentObject_getDifferencesOnly() {
        JSONArray array = new JSONArray(alice, bob, carl, dave, ellen);
        JSONArray other = new JSONArray(carl, alice, bob, ellen, tom);

        assertEquals("[{\"name\":[\"Dave\",\"Tom\"],\"class\":[\"7C\",\"7B\"],\"scores\":{\"science\":[71,87],\"math\":[76,80]}}]",
                array.deltaWith(other, "").toJSONString(null));
        assertEquals("[{\"index\":[3,4],\"name\":[\"Dave\",\"Tom\"],\"class\":[\"7C\",\"7B\"],\"scores\":{\"science\":[71,87],\"math\":[76,80]}}]",
                array.deltaWith(other, "index").toJSONString(null));
        assertEquals("[{\"+\":[0,1]},{\"+\":[1,2]},{\"+\":[2,0]},{\"+\":[3,4],\"name\":[\"Dave\",\"Tom\"],\"class\":[\"7C\",\"7B\"],\"scores\":{\"science\":[71,87],\"math\":[76,80]}},{\"+\":[4,3]}]",
                array.deltaWith(other, "+").toJSONString(null));

        JSONObject alice2 = alice.withDelta("{\"class\":\"7F\",\"scores\":{\"science\":76,\"humanity\":80}}");
        JSONArray other2 = new JSONArray(bob, alice2, ellen, tom, carl);
        assertEquals("[{\"class\":[\"7N\",\"7F\"],\"scores\":{\"english\":[89,null],\"science\":[77,76],\"humanity\":[85,80],\"math\":[90,null]}}," +
                        "{\"name\":[\"Dave\",\"Tom\"],\"class\":[\"7C\",\"7B\"],\"scores\":{\"science\":[71,87],\"math\":[76,80]}}]",
                array.deltaWith(other2, "").toJSONString(null));
        assertEquals("[{\"i\":[0,1],\"class\":[\"7N\",\"7F\"],\"scores\":{\"english\":[89,null],\"science\":[77,76],\"humanity\":[85,80],\"math\":[90,null]}}," +
                        "{\"i\":[3,3],\"name\":[\"Dave\",\"Tom\"],\"class\":[\"7C\",\"7B\"],\"scores\":{\"science\":[71,87],\"math\":[76,80]}}]",
                array.deltaWith(other2, "i").toJSONString(null));
        assertEquals("[{\"+i\":[0,1],\"class\":[\"7N\",\"7F\"],\"scores\":{\"english\":[89,null],\"science\":[77,76],\"humanity\":[85,80],\"math\":[90,null]}}," +
                        "{\"+i\":[1,0]},{\"+i\":[2,4]}," +
                        "{\"+i\":[3,3],\"name\":[\"Dave\",\"Tom\"],\"class\":[\"7C\",\"7B\"],\"scores\":{\"science\":[71,87],\"math\":[76,80]}}," +
                        "{\"+i\":[4,2]}]",
                array.deltaWith(other2, "+i").toJSONString(null));
    }

    @Test
    public void testSorting() {
        String text = "[{\"name\":\"Alice\",\"id\":111,\"level\":\"dolphin\"}," +
                "{\"level\":\"dolphin\",\"name\":\"Bob\",\"id\":222},{\"name\":\"Charlie\",\"level\":\"shark\",\"id\":333}]";
        JSONArray array1 = JSONArray.parse(null, text);
        Logger.V("array1: %s", array1.toJSONString());
        assertEquals("[{\"name\":\"Alice\",\"id\":111,\"level\":\"dolphin\"},{\"level\":\"dolphin\",\"name\":\"Bob\",\"id\":222},{\"name\":\"Charlie\",\"level\":\"shark\",\"id\":333}]",
                array1.toJSONString(null));

        JSONArray array2 = (JSONArray) Parser.parse(Comparator.naturalOrder(), text);
        Logger.V("array2: %s", array2);
        assertEquals("[{\"id\":111,\"level\":\"dolphin\",\"name\":\"Alice\"},{\"id\":222,\"level\":\"dolphin\",\"name\":\"Bob\"},{\"id\":333,\"level\":\"shark\",\"name\":\"Charlie\"}]",
                array2.toJSONString(null));

        JSONArray array3 = array1.getSorted(new OrdinalComparator("id", "name"));
        Logger.V("array3 compact: %s", array3.toJSONString(null));
        assertEquals("[{\"id\":111,\"name\":\"Alice\",\"level\":\"dolphin\"},{\"id\":222,\"name\":\"Bob\",\"level\":\"dolphin\"},{\"id\":333,\"name\":\"Charlie\",\"level\":\"shark\"}]",
                array3.toJSONString(null));

        //Verify sorted arrays are still equal
        assertEquals(array1, array2);
        assertEquals(array3, array2);
        assertEquals(array3, array1);

        assertEquals("\"name\": \"Alice\"", array1.getValue(0).getValue(0).toString());
        assertEquals("\"id\": 111", array3.getValue(0).getValue(0).toString());
    }
}
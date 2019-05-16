package JsonTuples;

import io.github.cruisoring.logger.Logger;
import io.github.cruisoring.utility.ResourceHelper;
import org.junit.Test;

import java.math.BigInteger;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

import static io.github.cruisoring.Asserts.*;

public class UtilitiesTest {

    @Test
    public void asJSONValue() {
        assertEquals(JSONValue.Null, Utilities.jsonify(null));
        assertEquals(JSONValue.True, Utilities.jsonify(true));
        assertEquals(JSONValue.False, Utilities.jsonify(false));
        assertEquals(new JSONString("string"), Utilities.jsonify("string"));
    }

    @Test
    public void asJSONString() {
        JSONString string = Utilities.asJSONString("test");
        assertEquals("test", string.getObject());
        string = Utilities.asJSONString("");
        assertEquals("", string.getObject());
    }

    @Test
    public void asJSONNumber() {
        IJSONValue number = Utilities.asJSONNumber(66);
        assertEquals(66, number.getObject());

        IJSONValue bigInteger = Utilities.asJSONNumber(new BigInteger("123456778901234567890"));
        assertEquals(new BigInteger("123456778901234567890"), bigInteger.getObject());

        IJSONValue dbl = Utilities.asJSONNumber(33.4);
        assertEquals(33.4, dbl.getObject());
    }

    @Test
    public void asJSONObject() {
        Map<String, Object> map = new HashMap<String, Object>(){{
           put("name", "test name");
           put("id", 123456);
           put("scores", new HashMap<String, Integer>(){{
               put("Math", 90);
               put("English", 80);
               put("Science", 88);
           }});
           put("class", "7L");
           put("isActive", true);
           put("address", null);
        }};

        JSONObject jsonObject = Utilities.asJSONObject(map);
        JSONObject withOrdered = (JSONObject) jsonObject.getSorted("id", "name", "class", "isActive", "address", "scores", "English", "Science", "Math");
        assertEquals("{\"id\": 123456,\"name\": \"test name\",\"class\": \"7L\",\"isActive\": true,\"address\": null,\"scores\": {\"English\": 80,\"Science\": 88,\"Math\": 90}}",
                withOrdered.toJSONString(null));
        Logger.V(jsonObject.toString());
    }

    @Test
    public void asJSONArrayFromArray() {
        Object[] simpleArray = new Object[] { null, true, false, 123, -77.9, "abc", '\t', "today's weather\n"};
        JSONArray array = Utilities.asJSONArrayFromArray(simpleArray);
        assertEquals("[null,true,false,123,-77.9,\"abc\",\"\\t\",\"today's weather\\n\"]",
                array.toJSONString(null));

        Object[] multiDimensional = new Object[] { new Character[]{'a', 'b'}, true, new int[]{1, 2},
                new double[][]{new double[]{-1.2, 0}, new double[]{3.3}}, new Object[]{"OK", null}};
        JSONArray array2 = Utilities.asJSONArrayFromArray(multiDimensional);
        assertEquals("[[\"a\",\"b\"],true,[1,2],[[-1.2,0.0],[3.3]],[\"OK\",null]]",
                array2.toJSONString(null));
        Object object = array2.getObject();
        assertEquals(new Object[] { new Object[]{"a", "b"}, true, new Object[]{1, 2},
                new Object[]{new Object[]{-1.2, 0d}, new Object[]{3.3}}, new Object[]{"OK", null}}, object);

        JSONArray steps = JSONArray.parseArray(ResourceHelper.getTextFromResourceFile("steps.json"));
        assertEquals("skipped",
                ((JSONObject)steps.getValue(9)).get("status").toString());
        Object[] stepArray = (Object[])steps.getObject();
        Map<String,Object>[] stepMaps = Arrays.stream(stepArray).map(e -> (Map<String,Object>)e).toArray(size -> new Map[size]);
        assertTrue(stepMaps.length==10,
                stepMaps[0].get("name").equals("Given I get some test data from  \"ABC Indroduction.xlsx\""),
                stepMaps[9].get("name").equals("And I logout from the server"));
        JSONArray converted = Utilities.asJSONArrayFromArray(stepArray);
        assertEquals(steps.toString(), converted.toString());
        Logger.I(converted.toString());
//        assertEquals("Given I get some test data from  \"ABC Indroduction.xlsx\"", converted.getObject())
    }

    @Test
    public void asJSONArrayFromCollection() {
        List<Object> list = Arrays.asList( null, true, false, 123, -77.9, "abc", '\t', "today's weather\n");
        JSONArray array = Utilities.asJSONArrayFromCollection(list);
        assertEquals("[null,true,false,123,-77.9,\"abc\",\"\\t\",\"today's weather\\n\"]",
                array.toJSONString(null));

        Set<Object> complexSet = new LinkedHashSet<>(Arrays.asList( new Character[]{'a', 'b'}, true, new int[]{1, 2},
                new double[][]{new double[]{-1.2, 0}, new double[]{3.3}}, new Object[]{"OK", null}));
        JSONArray array2 = Utilities.asJSONArrayFromCollection(complexSet);
        Object object = array2.getObject();
        assertTrue(Objects.deepEquals(new Object[] { new Object[]{"a", "b"}, true, new Object[]{1, 2},
                new Object[]{new Object[]{-1.2, 0d}, new Object[]{3.3}}, new Object[]{"OK", null}}, object));

    }

    @Test
    public void asJSONStringFromOthers() {
        List list = Arrays.asList(Locale.CANADA, LocalDate.of(2017, 3, 19), null, DayOfWeek.FRIDAY);
        IJSONValue array = Utilities.jsonify(list);
        assertEquals("[\"en_CA\",\"2017-03-19\",null,\"FRIDAY\"]", array.toJSONString(null));
    }

    @Test
    public void deltaWith_TwoJSONValues_returnEmpty(){
        assertEquals(JSONArray.EMPTY, Utilities.deltaWith(null, null));
        assertEquals(JSONArray.EMPTY, Utilities.deltaWith(null, JSONValue.Null));
        assertEquals(JSONArray.EMPTY, Utilities.deltaWith(1, Integer.valueOf(1)));
        assertEquals(JSONArray.EMPTY, Utilities.deltaWith(Double.valueOf(0.0), 0.0));
        assertEquals(JSONArray.EMPTY, Utilities.deltaWith('a', "a"));
        assertEquals(JSONArray.EMPTY, Utilities.deltaWith(true, JSONValue.True));
        assertEquals(JSONArray.EMPTY, Utilities.deltaWith(JSONValue.False, false));
        assertEquals(JSONArray.EMPTY, Utilities.deltaWith(LocalDate.of(2017, 3, 19), "2017-03-19"));
    }

    @Test
    public void deltaWith_TwoSimpleDifferentJSONValues_returnJSONArray(){
        assertEquals(new Object[]{null, ""},
                Utilities.deltaWith(null, "").getObject());
        assertEquals(new Object[]{"a", ""},
                Utilities.deltaWith('a', "").getObject());
        assertEquals(new Object[]{1, false},
                Utilities.deltaWith(Integer.valueOf(1), false).getObject());
        assertEquals(new Object[]{0.0, 2.0},
                Utilities.deltaWith(Double.valueOf(0.0), 2.0).getObject());
        assertEquals(new Object[]{"2017-03-19", "2017-03-20"},
                Utilities.deltaWith(LocalDate.of(2017, 3, 19), LocalDate.of(2017, 3, 20)).getObject());
        assertEquals("[true,\"true\"]",
                Utilities.deltaWith(true, "true").toJSONString(null));
        assertEquals("[\"\",\" \"]",
                Utilities.deltaWith("", " ").toJSONString(null));
        assertEquals("[\"abc\",\"Abc\"]",
                Utilities.deltaWith("abc", "Abc").toJSONString(null));
    }

    @Test
    public void deltaWith_OneSimpleAndOneComplexValue_returnJSONArray(){
        assertEquals("[null,[]]",
                Utilities.deltaWith(null, new Number[0]).toJSONString(null));
        assertEquals("[[null],null]",
                Utilities.deltaWith(new Number[]{null}, null).toJSONString(null));
        assertEquals("[true,{}]",
                Utilities.deltaWith(true, new HashMap()).toJSONString(null));
        assertEquals("[{},123]",
                Utilities.deltaWith(new HashMap(), 123).toJSONString(null));

    }
}
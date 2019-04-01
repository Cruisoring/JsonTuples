package JsonTuples;

import io.github.cruisoring.logger.Logger;
import org.junit.Test;

import java.math.BigInteger;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ConverterTest {

    @Test
    public void asJSONValue() {
        assertEquals(JSONValue.Null, Converter.jsonify(null));
        assertEquals(JSONValue.True, Converter.jsonify(true));
        assertEquals(JSONValue.False, Converter.jsonify(false));
        assertEquals(new JSONString("string"), Converter.jsonify("string"));
    }

    @Test
    public void asJSONString() {
        JSONString string = Converter.asJSONString("test");
        assertEquals("test", string.getObject());
        string = Converter.asJSONString("");
        assertEquals("", string.getObject());
    }

    @Test
    public void asJSONNumber() {
        IJSONValue number = Converter.asJSONNumber(66);
        assertEquals(66, number.getObject());

        IJSONValue bigInteger = Converter.asJSONNumber(new BigInteger("123456778901234567890"));
        assertEquals(new BigInteger("123456778901234567890"), bigInteger.getObject());

        IJSONValue dbl = Converter.asJSONNumber(33.4);
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

        JSONObject jsonObject = Converter.asJSONObject(map);
        JSONObject withOrdered = jsonObject.getSorted(Arrays.asList("id", "name", "class", "isActive", "address", "scores", "English", "Science", "Math"));
        assertEquals("{\n" +
                "  \"id\": 123456,\n" +
                "  \"name\": \"test name\",\n" +
                "  \"class\": \"7L\",\n" +
                "  \"isActive\": true,\n" +
                "  \"address\": null,\n" +
                "  \"scores\": {\n" +
                "    \"English\": 80,\n" +
                "    \"Science\": 88,\n" +
                "    \"Math\": 90\n" +
                "  }\n" +
                "}", withOrdered.toString());
        Logger.V(jsonObject.toString());
    }

    @Test
    public void asJSONArrayFromArray() {
        Object[] simpleArray = new Object[] { null, true, false, 123, -77.9, "abc", '\t', "today's weather\n"};
        JSONArray array = Converter.asJSONArrayFromArray(simpleArray);
        assertEquals("[\n" +
                "  null,\n" +
                "  true,\n" +
                "  false,\n" +
                "  123,\n" +
                "  -77.9,\n" +
                "  \"abc\",\n" +
                "  \"\\t\",\n" +
                "  \"today's weather\\n\"\n" +
                "]", array.toString());

        Object[] multiDimensional = new Object[] { new Character[]{'a', 'b'}, true, new int[]{1, 2},
                new double[][]{new double[]{-1.2, 0}, new double[]{3.3}}, new Object[]{"OK", null}};
        JSONArray array2 = Converter.asJSONArrayFromArray(multiDimensional);
        Object object = array2.getObject();
        assertTrue(Objects.deepEquals(new Object[] { new Object[]{"a", "b"}, true, new Object[]{1, 2},
                new Object[]{new Object[]{-1.2, 0d}, new Object[]{3.3}}, new Object[]{"OK", null}}, object));
    }

    @Test
    public void asJSONArrayFromCollection() {
        List<Object> list = Arrays.asList( null, true, false, 123, -77.9, "abc", '\t', "today's weather\n");
        JSONArray array = Converter.asJSONArrayFromCollection(list);
        assertEquals("[\n" +
                "  null,\n" +
                "  true,\n" +
                "  false,\n" +
                "  123,\n" +
                "  -77.9,\n" +
                "  \"abc\",\n" +
                "  \"\\t\",\n" +
                "  \"today's weather\\n\"\n" +
                "]", array.toString());

        Set<Object> complexSet = new LinkedHashSet<>(Arrays.asList( new Character[]{'a', 'b'}, true, new int[]{1, 2},
                new double[][]{new double[]{-1.2, 0}, new double[]{3.3}}, new Object[]{"OK", null}));
        JSONArray array2 = Converter.asJSONArrayFromCollection(complexSet);
        Object object = array2.getObject();
        assertTrue(Objects.deepEquals(new Object[] { new Object[]{"a", "b"}, true, new Object[]{1, 2},
                new Object[]{new Object[]{-1.2, 0d}, new Object[]{3.3}}, new Object[]{"OK", null}}, object));

    }

    @Test
    public void asJSONStringFromOthers() {
    }
}
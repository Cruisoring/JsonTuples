package JsonTuples;

import org.junit.Test;

import java.math.BigInteger;

import static org.junit.Assert.assertEquals;

public class ConverterTest {

    @Test
    public void asJSONValue() {
        assertEquals(JSONValue.Null, Converter.asJSONValue(null));
        assertEquals(JSONValue.True, Converter.asJSONValue(true));
        assertEquals(JSONValue.False, Converter.asJSONValue(false));
        assertEquals(new JSONString("string"), Converter.asJSONValue("string"));
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

        Object[] multiDimensional = new Object[] { true, new int[]{1, 2}, new Character[]{'a', 'b'},
                new double[][]{new double[]{-1.2, 0}, new double[]{3.3}}, new Object[]{"OK", null}};
        JSONArray array2 = Converter.asJSONArrayFromArray(multiDimensional);
        Object object = array2.getObject();
        System.out.println(array2.toString());
    }

    @Test
    public void asJSONArrayFromCollection() {
    }

    @Test
    public void asJSONStringFromOthers() {
    }
}
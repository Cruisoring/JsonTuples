package JsonTuples;

import org.junit.Test;

import static org.junit.Assert.*;

public class JSONObjectTest {

    @Test
    public void parse() {
        JSONObject obj = JSONObject.parse("{ \"age\": 123, \"name\": null, \"other\": \"none\" }");
        String string = obj.toString();
        assertEquals("{\n" +
                "  \"age\": 123,\n" +
                "  \"name\": null,\n" +
                "  \"other\": \"none\"\n" +
                "}", string);
    }

    @Test
    public void deltaWith() {
        JSONObject obj1 = JSONObject.parse("{ \"age\": 123, \"name\": null, \"other\": \"OK\" }");
        JSONObject obj2 = JSONObject.parse("{ \"age\": 24, \"name\": \"Tom\", \"other\": \"OK\" }");

        JSONObject delta = obj1.deltaWith(obj2);
        System.out.println(delta.toString());
    }

    @Test
    public void toJSONString() {
    }

    @Test
    public void testToString() {
    }
}
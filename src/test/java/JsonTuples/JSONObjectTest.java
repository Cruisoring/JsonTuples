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
    public void sorted() {
    }

    @Test
    public void toJSONString() {
    }

    @Test
    public void testToString() {
    }
}
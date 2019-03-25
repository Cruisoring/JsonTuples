package JsonTuples;

import org.junit.Test;

import java.util.Arrays;
import java.util.Comparator;

import static org.junit.Assert.*;

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
        assertEquals(nullOrdered, naturalOrdered);

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
    public void toJSONString() {
    }

    @Test
    public void testToString() {
    }
}
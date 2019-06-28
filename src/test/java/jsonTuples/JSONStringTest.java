package jsonTuples;

import org.junit.Test;

import static io.github.cruisoring.Asserts.assertEquals;

public class JSONStringTest {

    @Test
    public void parseString_withControlChars_getControls() {
        String json = "\"\\\"\\\\\\b\\/\\n\\r\\t\"";
        JSONString string = JSONString.parseString(json);
        assertEquals("\"\\\"\\\\\\b\\/\\n\\r\\t\"", string.toString());
        assertEquals("\"\\\b/\n\r\t", string.getObject());
    }
}
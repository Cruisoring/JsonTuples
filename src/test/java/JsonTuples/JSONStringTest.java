package JsonTuples;

import org.junit.Test;

import static org.junit.Assert.*;

public class JSONStringTest {

    @Test
    public void fromJSONRaw() {
        JSONString string = JSONString.parseString("\"\\b\\n\\rAbc\\u2077  \"");
        assertEquals("\b\n\rAbc⁷  ", string.getObject());
        assertEquals("\"\\b\\n\\rAbc\\u2077  \"", string.toJSONString());
    }

    @Test
    public void toJSONString() {
    }
}
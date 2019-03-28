package JsonTuples;

import org.junit.Test;

import static org.junit.Assert.*;

public class JSONArrayTest {

    @Test
    public void toJSONString() {
        JSONArray array = JSONArray.parseArray("[123, \"abc\"]");
        assertEquals("[\n  123,\n  \"abc\"\n]", array.toString());
        assertEquals("[123, \"abc\"]", array.toJSONString(null));
        assertEquals("[\n    123,\n    \"abc\"\n  ]", array.toJSONString("  "));
        assertEquals("[\n      123,\n      \"abc\"\n    ]", array.toJSONString("    "));
    }
}
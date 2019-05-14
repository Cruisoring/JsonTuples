package JsonTuples;

import io.github.cruisoring.logger.Logger;
import org.apache.commons.text.StringEscapeUtils;
import org.junit.Test;

import static io.github.cruisoring.Asserts.assertEquals;

public class JSONStringTest {

    @Test
    public void unescapeJSON(){
        String json = "\t  \"\b\n\rAb\tc\u2077  \\t\" \n";
        String unescaped = JSONString.unescapeJson(json);
        assertEquals(StringEscapeUtils.unescapeJson(json), unescaped);
        Logger.V("%s    ->    %s", unescaped, StringEscapeUtils.escapeJson(unescaped));
    }

    @Test
    public void parseString_withControlChars_getControls() {
        String json = "\\\"\\\\\\b\\/\\n\\r\\t";
        JSONString string = JSONString.parseString(json);
        assertEquals("\"\\\"\\\\\\b\\/\\n\\r\"", string.toString());
        assertEquals("\"\\\b/\n\r", string.getObject());
    }
}
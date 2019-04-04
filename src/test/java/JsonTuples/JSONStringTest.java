package JsonTuples;

import io.github.cruisoring.logger.Logger;
import org.apache.commons.text.StringEscapeUtils;
import org.junit.Test;

import static org.junit.Assert.*;

public class JSONStringTest {

    @Test
    public void unescapeJSON(){
        String json = "\t  \"\b\n\rAb\tc\u2077  \\t\" \n";
        String unescaped = JSONString.unescapeJson(json);
        assertEquals(StringEscapeUtils.unescapeJson(json), unescaped);
        Logger.V("%s    ->    %s", unescaped, StringEscapeUtils.escapeJson(unescaped));
    }


    //TODO: see how others handle control chars within quotes
//    @Test
//    public void fromJSONRaw() {
//        String json = "\t  \"\b\n\rAb\tc\u2077  \\t\" \n";
//        JSONString string = JSONString.parseString(json);
//        assertEquals("|b\n\n" +
//                "Ab\tc⁷  \t", string.getObject());
//        assertEquals("\"Abc\\u2077  \\t\"", string.toJSONString());
//    }

}
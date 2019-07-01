package jsonTuples;

import io.github.cruisoring.TypeHelper;
import io.github.cruisoring.logger.Logger;
import io.github.cruisoring.utility.SetHelper;
import org.junit.Test;

import java.math.BigInteger;

import static io.github.cruisoring.Asserts.assertEquals;

public class NamedValueTest {
    @Test
    public void getName() {
        NamedValue namedValue = new NamedValue("name", Parser.parse("\"Alice\""));
        assertEquals("name", namedValue.getName());
    }

    @Test
    public void getValue() {
        NamedValue namedValue = new NamedValue("name", Parser.parse("\"Alice\""));
        assertEquals("Alice", namedValue.getValue());
    }

    @Test
    public void toJSONString() {
        NamedValue namedValue = new NamedValue("name", Parser.parse("\"Alice\""));
        assertEquals("\"name\":\"Alice\"", namedValue.toJSONString(null));
        assertEquals("   \"name\": \"Alice\"", namedValue.toJSONString("   "));
    }

    @Test
    public void testToString() {
        NamedValue namedValue = new NamedValue("name", Parser.parse("\"Alice\""));
        assertEquals("\"name\": \"Alice\"", namedValue.toString());
    }

    @Test
    public void testHashCode() {
        NamedValue namedValue = new NamedValue("name", Parser.parse("\"Alice\""));
        assertEquals(TypeHelper.deepHashCode(new String[]{"\"name\"", "\"Alice\""}), namedValue.hashCode());
        Logger.D("hashCode(): %s, signatures: %s", namedValue.hashCode(), TypeHelper.deepToString(namedValue.getSignatures()));
    }

    @Test
    public void getSorted() {
        NamedValue namedValue = NamedValue.parse("\"name\": [\"Tom\", \"Cruise\"]");
        assertEquals(namedValue, namedValue.getSorted(new OrdinalComparator<>()));

        namedValue = NamedValue.parse("\"member\":{\"id\":111, \"name\": \"Tom\", \"vip\": false}");
        assertEquals("\"member\":{\"id\":111,\"name\":\"Tom\",\"vip\":false}", namedValue.toJSONString(null));
        assertEquals("\"member\":{\"name\":\"Tom\",\"id\":111,\"vip\":false}", namedValue.getSorted(new OrdinalComparator<>("name")).toJSONString(null));
    }

    @Test
    public void testGetSignatures() {
        NamedValue namedValue = NamedValue.parse("\"name\": 12345");
        Logger.D("HashCodes: \"name\": %d, 12345: %d, namedValue: %d", "\"name\"".hashCode(), "12345".hashCode(),
                namedValue.hashCode());
        assertEquals(SetHelper.asSet(TypeHelper.deepHashCode(new String[]{"\"name\"", "12345"}), "\"name\"".hashCode(), "12345".hashCode()),
                namedValue.getSignatures());

        String raw = "\"member\": {\n  \"id\": 111,\n  \"name\": \"Tom\",\n  \"vip\": false\n}";
        namedValue = NamedValue.parse(raw);
        JSONObject obj = JSONObject.parse("{\"id\":111,\"name\": \"Tom\",\"vip\": false}");
        assertEquals(SetHelper.asSet(TypeHelper.deepHashCode(new Object[]{"\"member\"", obj.hashCode()}), "\"member\"".hashCode(), obj.hashCode()),
                namedValue.getSignatures());

        NamedValue sorted = namedValue.getSorted(new OrdinalComparator<>("name"));
        obj = JSONObject.parse("{\"name\": \"Tom\",\"id\":111,\"vip\": false}");
        assertEquals(SetHelper.asSet(TypeHelper.deepHashCode(new Object[]{"\"member\"", obj.hashCode()}), "\"member\"".hashCode(), obj.hashCode()),
                sorted.getSignatures());

        Logger.D("namedValue: %s\nsorted: %s", TypeHelper.deepToString(namedValue.getSignatures()), TypeHelper.deepToString(sorted.getSignatures()));
    }

    @Test
    public void testParse() {
        assertEquals(Integer.valueOf(200), NamedValue.parse("\"cachePackageTagsTrack\": 200").getValue());
        assertEquals(-20.0, NamedValue.parse("\"cachePackageTagsTrack\": -20.0").getValue());
        assertEquals(new BigInteger("20000000000000000000000000000"),
                NamedValue.parse("\"cachePackageTagsTrack\": 20000000000000000000000000000").getValue());
        assertEquals("cofaxCDS", NamedValue.parse("\"servlet-name\": \"cofaxCDS\"").getValue());
        assertEquals(Boolean.TRUE, NamedValue.parse("\"servlet-name\": true").getValue());
        assertEquals(null, NamedValue.parse("\"servlet-name\": null").getValue());
        assertEquals("/images/cofax.gif", NamedValue.parse("\"configGlossary:poweredByIcon\": \"/images/cofax.gif\"").getValue());
        assertEquals("cachePackage\"TagsTrack", NamedValue.parse("\"cachePackage\"TagsTrack\": 11").getName());
    }
}
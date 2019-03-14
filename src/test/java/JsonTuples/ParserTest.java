package JsonTuples;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ParserTest {

    @Test
    public void testGetIndexesInRange() {
        List<Integer> subList;
        List<Integer> list = Arrays.asList(1, 3, 4, 5, 9, 10, 12, 15, 21, 23, 25);

        //No overlapped:
        subList = Parser.getIndexesInRange(list, Range.open(3, 4));
        assertTrue(subList.size() == 0);

        subList = Parser.getIndexesInRange(list, Range.closed(17, 19));
        assertTrue(subList.size() == 0);

        subList = Parser.getIndexesInRange(list, Range.openClosed(-1, 0));
        assertTrue(subList.size() == 0);

        subList = Parser.getIndexesInRange(list, Range.open(25, 27));
        assertTrue(subList.size() == 0);

        //With 1 element in range:
        subList = Parser.getIndexesInRange(list, Range.closed(4, 4));
        assertTrue(subList.size() == 1 && subList.get(0).equals(4));

        subList = Parser.getIndexesInRange(list, Range.closed(5, 8));
        assertTrue(subList.size() == 1 && subList.get(0).equals(5));

        subList = Parser.getIndexesInRange(list, Range.closed(0, 1));
        assertTrue(subList.size() == 1 && subList.get(0).equals(1));

        subList = Parser.getIndexesInRange(list, Range.closed(24, 29));
        assertTrue(subList.size() == 1 && subList.get(0).equals(25));

        //With multiple element in range:
        subList = Parser.getIndexesInRange(list, Range.closed(4, 5));
        assertEquals(Arrays.asList(4, 5), subList);

        subList = Parser.getIndexesInRange(list, Range.open(4, 10));
        assertEquals(Arrays.asList(5, 9), subList);

        subList = Parser.getIndexesInRange(list, Range.closed(0, 25));
        assertEquals(Arrays.asList(1, 3, 4, 5, 9, 10, 12, 15, 21, 23, 25), subList);

        subList = Parser.getIndexesInRange(list, Range.closed(22, 30));
        assertEquals(Arrays.asList(23, 25), subList);


    }

    @Test
    public void testParse() {
        Parser parser = new Parser("{\n" +
                "    \"glossary\": {\n" +
                "        \"title\": \"example glossary\",\n" +
                "\t\t\"GlossDiv\": {\n" +
                "            \"title\": \"S\",\n" +
                "\t\t\t\"GlossList\": {\n" +
                "                \"GlossEntry\": {\n" +
                "                    \"ID\": \"SGML\",\n" +
                "\t\t\t\t\t\"SortAs\": \"SGML\",\n" +
                "\t\t\t\t\t\"GlossTerm\": \"Standard Generalized Markup Language\",\n" +
                "\t\t\t\t\t\"Acronym\": \"SGML\",\n" +
                "\t\t\t\t\t\"Abbrev\": \"ISO 8879:1986\",\n" +
                "\t\t\t\t\t\"GlossDef\": {\n" +
                "                        \"para\": \"A meta-markup language, used to create markup languages such as DocBook.\",\n" +
                "\t\t\t\t\t\t\"GlossSeeAlso\": [\"GML\", \"XML\"]\n" +
                "                    },\n" +
                "\t\t\t\t\t\"GlossSee\": \"markup\"\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "}");

        IJSONValue value = parser.parse();
        assertTrue(value instanceof JSONObject);
        TupleMap<String, IJSONValue> map = (TupleMap<String, IJSONValue>) value.getObject();
        System.out.println(map);
    }

    @Test
    public void testParse2() {
        Parser parser = new Parser("{\"user\": {\n" +
                "    \"id\": \"9999912398\",\n" +
                "    \"name\": \"Kelly Clarkson\",\n" +
                "    \"friends\": [\n" +
                "        {\n" +
                "            \"name\": \"Tom Cruise\",\n" +
                "            \"id\": \"55555555555555\",\n" +
                "            \"likes\": {\"data\": [\n" +
                "                {\n" +
                "                    \"category\": \"Movie\",\n" +
                "                    \"name\": \"The Shawshank Redemption\",\n" +
                "                    \"id\": \"103636093053996\",\n" +
                "                    \"created_time\": \"2012-11-20T15:52:07+0000\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"category\": \"Community\",\n" +
                "                    \"name\": \"Positiveretribution\",\n" +
                "                    \"id\": \"471389562899413\",\n" +
                "                    \"created_time\": \"2012-12-16T21:13:26+0000\"\n" +
                "                }\n" +
                "            ]}\n" +
                "        },\n" +
                "        {\n" +
                "            \"name\": \"Tom Hanks\",\n" +
                "            \"id\": \"88888888888888\",\n" +
                "            \"likes\": {\"data\": [\n" +
                "                {\n" +
                "                    \"category\": \"Journalist\",\n" +
                "                    \"name\": \"Janelle Wang\",\n" +
                "                    \"id\": \"136009823148851\",\n" +
                "                    \"created_time\": \"2013-01-01T08:22:17+0000\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"category\": \"Tv show\",\n" +
                "                    \"name\": \"Now With Alex Wagner\",\n" +
                "                    \"id\": \"305948749433410\",\n" +
                "                    \"created_time\": \"2012-11-20T06:14:10+0000\"\n" +
                "                }\n" +
                "            ]}\n" +
                "        }\n" +
                "    ]\n" +
                "}}");

        IJSONValue value = parser.parse();
        assertTrue(value instanceof JSONObject);
        TupleMap<String, IJSONValue> map = (TupleMap<String, IJSONValue>) value.getObject();
        System.out.println(map);
    }

    @Test
    public void testParse3() {
        Parser parser = new Parser("{\n" +
                "    \"glossary\": {\n" +
                "        \"title\": \"example glossary\",\n" +
                "\t\t\"GlossDiv\": {\n" +
                "            \"title\": \"S\",\n" +
                "\t\t\t\"GlossList\": {\n" +
                "                \"GlossEntry\": {\n" +
                "                    \"ID\": \"SGML\",\n" +
                "\t\t\t\t\t\"SortAs\": \"SGML\",\n" +
                "\t\t\t\t\t\"GlossTerm\": \"Standard Generalized Markup Language\",\n" +
                "\t\t\t\t\t\"Acronym\": \"SGML\",\n" +
                "\t\t\t\t\t\"Abbrev\": \"ISO 8879:1986\",\n" +
                "\t\t\t\t\t\"GlossDef\": {\n" +
                "                        \"para\": \"A meta-markup language, used to create markup languages such as DocBook.\",\n" +
                "\t\t\t\t\t\t\"GlossSeeAlso\": [\"GML\", \"XML\"]\n" +
                "                    },\n" +
                "\t\t\t\t\t\"GlossSee\": \"markup\"\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "}");

        IJSONValue value = parser.parse();
        assertTrue(value instanceof JSONObject);
        String jsonText = value.toString();
        String expected = "{\n" +
                "  \"glossary\": {\n" +
                "    \"GlossDiv\": {\n" +
                "      \"GlossList\": {\n" +
                "        \"GlossEntry\": {\n" +
                "          \"Abbrev\": \"ISO 8879:1986\",\n" +
                "          \"Acronym\": \"SGML\",\n" +
                "          \"GlossDef\": {\n" +
                "            \"GlossSeeAlso\": [\n" +
                "              \"GML\",\n" +
                "              \"XML\"\n" +
                "            ],\n" +
                "            \"para\": \"A meta-markup language, used to create markup languages such as DocBook.\"\n" +
                "          },\n" +
                "          \"GlossSee\": \"markup\",\n" +
                "          \"GlossTerm\": \"Standard Generalized Markup Language\",\n" +
                "          \"ID\": \"SGML\",\n" +
                "          \"SortAs\": \"SGML\"\n" +
                "        }\n" +
                "      },\n" +
                "      \"title\": \"S\"\n" +
                "    },\n" +
                "    \"title\": \"example glossary\"\n" +
                "  }\n" +
                "}";
        assertEquals(expected, jsonText);
    }
}
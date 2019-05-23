#JsonTuples
===================

Driven by [Functional Programming paradigm](https://en.wikipedia.org/wiki/Functional_programming) and using uses immutable Tuples of [functionExtensions 2.0.0](http://repo1.maven.org/maven2/io/github/cruisoring/functionExtensions/2.0.0/) to keep data, the JsonTuples is a Java library to parse big JSON strings, or converting JAVA Array/Collection/Map to corresponding JSONValues quickly, then the huge data sets embedded can be serialized, sorted or compared so as to be used as a Data Analytic tool.

The original intention of this project is to use given set of JSON texts as templates to compose requests and match responses automatically for REST API testing, that means a fast parser to convert sample REST payloads into common JAVA data structures of Maps or Collections, then part of them could be modified/removed/added easily before converting the updated data back to JSON texts as HTTP GET/POST/PUT/DELETE payloads. By studying available open-source projects to convert JAVA Objects to/from JSON, I realized that there could be steep learning curves, and might not get desirable performance and functions. This library means to expose simple APIs to parse JSON text as Maps, Arrays or combination of them that can be accessed conveniently. Further more, with the embdedded features of Tuple, the JsonTuples can compare huge datasets composed by Collections and Maps to get their deltas effectively.

## Goals

 * 	Defines limited JSON classes to enable JSON string processing as outlined in [json.org](http://www.json.org/), especially JSONObject and JSONArray.
 *  Parses big JSON text block and saves the contents as JSONObject/JSONArray that are immutable.
 *  Serialize JSONObject/JSONArray to JSON text with/without indents.
 *  Sort the orders of JSONObject elements with StringComparator recursively to get JSON texts of unified forms.
 *  Convert a Collection or Array to/from JSONArray, or a Map<String, Object> to/from JSONObject with immutable Tuples to keep the data.
 *  The JSON Objects, like JSONObject and JSONArray, can be converted to modifiable Map<String, Object> or ArrayList for update/insert/delete.
 *  The JSON Objects can be used as media to compare two big datasets to get their minimum differences as another JSON Object. For example, find out the differences between an Array and a Set whose elements may or may not be consistent.

Due to the light-weight nature of this project, following functionalities are not supported:
 *  Serialize/De-serialize complex JAVA Objects by supposing POJOs can always be replaced with Map/Collection.
 *  Converting JSONObject/JSONArray to customised Map or Collection.
 *  Expressions to locate element of JSON, like [JsonPath](https://github.com/json-path/JsonPath) are not supported yet.

## Get Started

Add the following dependency to your pom.xml:
```xml
<dependency>
    <groupId>io.github.cruisoring</groupId>
    <artifactId>JsonTuples</artifactId>
    <version>1.0.0</version>
</dependency>
```

Alternatively, get the packages directly from [Maven Central](http://repo1.maven.org/maven2/io/github/cruisoring/JsonTuples/1.0.0/)


## Converter of Text/JAVA to/from JSON

The JSON objects refer to classes/interfaces defined in JsonTuples project, JAVA Objects means common JAVA types like primitive objects, as well as generic Map, Collection and Arrays. For the JSON texts to be processed, it is assumed they have followed the correct JSON syntax.

The JsonTuples is implemented based on the information from [json.org](http://www.json.org/) with straight mappings between JsonTuples interfaces/classes and JAVA Objects below:

| Interfaces | Classes | | JAVA Object embedded | JSON sample | Notes |
| --- | --- | --- | --- | --- | --- |
|IJSONValue&lt;T&gt; | JSONValue&lt;T&gt; | **Null** | null | *null* | *NULL* is not accepted |
| | |**True** | true| *true*| *True* is not accepted |
| | |**False**| false | *false*| *FALSE* is not accepted |
| | |**JSONString** | String | \"A string\"| special chars like \'\\n\' or \'\\t\' would be trimmed by default | 
| | |**JSONNumber** | Integer, BigInteger, Double, BigDecimal | *123.45e5* | the actual Object saved doesn't affect equals() which would compare by toString\(\)  |
|IJSONValue&lt;*NamedValue*&gt;| **JSONObject** | | Map&lt;String, Object&gt; |\{\"id\":12,\"name\":\"Tom\"\}| JSONObject has implemented **Map&lt;String, Object&gt;**, the **NamedValue** is used internally | 
|IJSONValue&lt;*IJSONValue*&gt;| **JSONArray** | | Object\[\] |\[null,true,1,\"abc\",\{\},\[\]\]| JSONArray can hold any number of IJSONValue |

The two major JSON objects are **JSONObject** and **JSONArray**. Both of them extend the generic [Tuple](https://github.com/Cruisoring/functionExtensions/blob/master/src/main/java/io/github/cruisoring/tuple/Tuple.java) type and are immutable. JAVA *Map<String, Object>* is the natural equivalent of *JSONObject*, and *Collection<Object>* or *Object[]* can be mapped to JSONArray naturally. The *JSONObject* retains the orders of its elements with *LinkedHashMap*; on the other side, though *JSONArray* keeps the orders of its elements, that order might be ignored considering *Collection*s doesn't care element orders like *Set*.

Constructors of all above JSON objects in bold (**Null, True, False, JSONString, JSONNumber, JSONObject and JSONArray**) are protected, and would be created by:
  * **The JSON Syntax checking is not enforced** by assuming the JSON text supplied to this library comply JSON protocol.
  * Static methods to parse a given String or part of a CharSequence to specific type of IJSONValue:  
    * __*IJSONValue Parser.parse(CharSequence)*__: generate IJSONValue based on the given JSON text content. If the JSON text is of right syntax, the _IJSONValue_ result shall be casted to one of **Null, True, False, JSONString, JSONNumber, JSONObject or JSONArray**.
    * __*JSONString.parseString(String jsonText)*__: the given jsonText must be wrapped by a pair of quotation marks (").
    * __*JSONValue.parse(CharSequence, Range)*__: expect and parse a part of the given JSON context as one of **Null, True, False, JSONString, JSONNumber**.
    * __*JSONObject.parse(String valueString)*__: expect the *valueString* is enclosed by '{' and '}', and cast the result of __*Parser.parse()*__ to be **JSONObject**.
    * __*JSONArray.parse(String valueString)*__: expect the *valueString* is enclosed by '[' and ']', and cast the result of __*Parser.parse()*__ to be **JSONArray**.
  * Static methods of _Utilities.java_ to convert JAVA Objects to JSON Objects defined in JsonTuples:
    * __*asJSONArrayFromArray(Object array)*__: convert a JAVA Array, of primitive or object elements, to a **JSONArray**.
    * __*asJSONArrayFromCollection(Object collection)*__: convert a JAVA Collection (such as _List_, _Set_ or _Queue_) to a **JSONArray**.
    * __*asJSONObject(Object object)*__: the given object must be a Map<Object, Object>
    * __*asJSONNumber(Object object)*__: the given object must be a JAVA Number(like Integer, Float, Double, Short, BigInteger, BigDecimal), the constucted **JSONNumber** instances would be compared by their String forms. For example, JSONNumber instance from 1.1f would be equal to that from Double.valueOf(1.1). If the object is null, then the constructed JSONNumber would be equal to **JSONValue.Null**.
    * __*asJSONString(Object object)*__: the given object must be a String or null, the latter would result in a JSONString instance equal with JSONValue.Null.
    * __*asJSONStringFromOthers(Object object)*__: convert all other types of Non-null object to JSONString with their default toString().
    * __*IJSONValue jsonify(Object object)*__: would check the type of the given object to call a method above. For types not covered above, by default the  __*asJSONStringFromOthers(Object object)*__ would be called to generate a JSONString, but it is possible to inject serialization/de-serialization methods into __*Utilities.classConverters*__ that is a Map<Class, Tuple2> where the value of a given class includes both serialization and de-serialization for a concerned type of object, then the serialization method would be called to convert the matched instance to its text equivalent.  
    
Usually, the above methods shall be enough to get most JSON to/from JAVA conversions done.

## Sort and Format Text from JSON

To display the content held by JSONObject with a ordered manner, a **Comparator<String>** can be used in two ways:
  * Supplied as argument of __*Parser.parse(CharSequence jsonText, Comparator<String> comparator)*__ or __*parse(CharSequence jsonText, Comparator<String> comparator, Range range)*__, then the parsed *JSONObject* and all its children would be saved with orders specified by the given **comparator**.
  * the __*JSONObject.getSorted(Comparator<String> comparator)*__ or __*JSONArray.getSorted(Comparator<String> comparator)*__ would return a new JSONObject or JSONArray with their children elements sorted by names following rules specified by the given **comparator**.
  
As a special case, the [OrdinalComparator](https://github.com/Cruisoring/JsonTuples/blob/master/src/main/java/JsonTuples/OrdinalComparator.java) would register all names of JSON Object in order and sort all names accordingly.

The TAB of indent has been hard-coded as ```"  "``` in **IJSONable.SPACE**, the **toJSONString(String indent)** defined in **IJSONable** interface accepts a blank String that can be either _null_ or all white-spaces to get JSON text based on value of the given **indent**:
  *  If the JSONObject is empty, then it would always return ```{}```;
  *  If the JSONArray is empty, then it would always return ```[]```;
 Otherwise:
  *  If **indent** is null, then the generated JSON text would be a compact single-line String;
  *  If **indent** is ```""```, then the generated JSON text would be a multi-line String with extra ```"  "``` for each indent level.
  *  If **indent** is not empty, then the generated JSON text would be a multi-line String with given **indent** appended ahead of each lines from above case.

The toString() would show same String as if the **indent** is ```""```. Just as the [base Tuple type](https://github.com/Cruisoring/functionExtensions/blob/master/src/main/java/io/github/cruisoring/tuple/Tuple.java), the toString() result is cached for achieve performance benefits that along with cached hashCode, is critical for fast comparison of huge datasets.

## Basic Examples

This section shows how JsonTuples can be used to parse JSON text or convert common JAVA objects to JSON objects.

### Parse Text as JSON Objects

The unit test below shows how __*IJSONValue Parser.parse(CharSequence)*__ can be used to parse different text to corresponding IJSONValue types.
```java
    @Test
    public void parseText_getRightIJSONValue() {
        //parse texts of null, true or false
        assertTrue(JSONValue.Null == Parser.parse("null"));
        assertTrue(JSONValue.True == Parser.parse("true"));
        assertTrue(JSONValue.False == Parser.parse("false"));

        //text of a number to JSONNumber
        JSONNumber number = (JSONNumber) Parser.parse(" 12.345  ");
        assertEquals(12.345, number.getObject());

        //text enclosed by '""s would be parsed as JSONString
        JSONString string = (JSONString) Parser.parse("  \" abc \n \t\"\r");
        assertEquals(" abc  ", string.getObject());

        //Map alike text would be parsed as JSONObject
//        JSONObject object = JSONObject.parse("{\"id\":123,\"name\":null,\"courses\":[\"English\", \"Math\", \"Science\"]}");
        JSONObject object = (JSONObject) Parser.parse("{\"id\":123,\"name\":null,\"courses\":[\"English\", \"Math\", \"Science\"]}");
        assertEquals(123, object.get("id"));
        assertNull(object.get("name"));
        assertEquals(new Object[]{"English", "Math", "Science"}, object.get("courses"));

        //Array alike text would be parsed as JSONArray
//        JSONArray array = JSONArray.parse("[1, null, true, \"abc\", [false, null], {\"id\":123}]");
        JSONArray array = (JSONArray) Parser.parse("[1, null, true, \"abc\", [false, null], {\"id\":123}]");
        Object[] values = (Object[]) array.getObject();
        assertTrue(array.size() == 6,
                values[0].equals(1),
                values[1]==null,
                values[2].equals(true),
                values[3].equals("abc"));
        assertEquals(new Object[]{false, null}, values[4]);
        Map mapAt5 = (Map)values[5];
        assertEquals(123, mapAt5.get("id"));
    }
```

Notice: the *assertTrue()*, *assertEquals()* are helper methods defined in [Asserts.java of functionExtensions 2.0.0](https://github.com/Cruisoring/functionExtensions/blob/master/src/main/java/io/github/cruisoring/Asserts.java) to assert multiple expressions or compare elements of two Arrays or Collections.

Since JSON text shall usually be parsed as JSONObject or JSONArray, __*JSONObject.parse(String valueString)*__ and __*JSONArray.parse(String valueString)*__ act as syntactic sugar to cast the IJSONValue to JSONObject or JSONArray behind the scene.

With layered filtering and simplified state machine to enable the parsing process, and avoid JSON syntax validation whenever possible, the JsonTuples achieves a quite good performance. For example:
```java
    @Test
    public void test6257KJson() {
        String jsonText = ResourceHelper.getTextFromResourceFile("catalog.json");
        int jsonTextLength = jsonText.length();

        String sortedString = null;
        for (int i = 0; i < 10; i++) {
            JSONObject result = Logger.M(Measurement.start("Parsing JSON text of %d", jsonTextLength),
                    () -> JSONObject.parse(jsonText));
            IJSONValue sortedValue = Logger.M(Measurement.start("Sorting JSONObject of size %d", result.size()),
                    () -> result.getSorted(Comparator.naturalOrder()));
            sortedString = Logger.M(Measurement.start("ToJSONString(null)"), () -> sortedValue.toJSONString(null));
        }
        Map<String, String> performanceSummary = Measurement.getAllSummary();
        performanceSummary.entrySet().forEach(entry -> Logger.I("%s--> %s", entry.getKey(), entry.getValue()));
    }
``` 

The above unit test loads text from [catalog.json](https://github.com/Cruisoring/JsonTuples/blob/master/src/test/resources/catalog.json) that is 6.11M, then:
*  parse the text to get the JSONObject **result** instance;
*  get that JSONObject **result** instance sorted to get another JSONObject **sortedValue** instance;
*  get the compact String form as String **sortedString**;
*  and repeat the above 3 operations 10 times to get measurable performance figures.

When running from my 4-cores i7-7700HQ @ 2.8G laptop, the screenshot below shows __*the average and max time to parse the 6.11M file are 216ms and 385ms*__ respectively.
![test6257KJson outcome](/images/performance.png "Parsing 6M JSON text for 10 times")


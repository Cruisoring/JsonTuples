#JsonTuples
===================

Driven by [Functional Programming paradigm](https://en.wikipedia.org/wiki/Functional_programming) and using uses immutable Tuples of [functionExtensions 2.0.0](http://repo1.maven.org/maven2/io/github/cruisoring/functionExtensions/2.0.0/) to keep data, the JsonTuples is a Java library to parse big JSON strings, or converting JAVA Array/Collection/Map to corresponding JSONValues quickly, then the huge data sets embedded can be serialized, sorted or compared so as to be used as a Data Analytic tool.

There are many open-source projects to convert JAVA Objects to/from JSON. However, most of them are either too heavy to use when there are many parameters of different classes to config, or with undesirable performance and limited functionalities. This library means to expose simple APIs to parse JSON text as Maps, Arrays or combination of them that can be accessed conveniently. Further more, with the embdedded features of Tuple, the JsonTuples can compare huge datasets composed by Collections and Maps to get their deltas effectively.

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


## JSON Objects and Functions

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

Constructors of all above JSON objects in bold (**Null, True, False, JSONString, JSONNumber, JSONObject and JSONArray**) are protected, and would be created by:
  * **The JSON Syntax checking is not enforced** by assuming the JSON text supplied to this library comply JSON protocol.
  * Static methods to parse a given String or part of a CharSequence to specific type of IJSONValue:  
    * __*IJSONValue Parser.parse(CharSequence)*__: generate IJSONValue based on the given JSON text content. If the JSON text is of right syntax, the _IJSONValue_ result shall be casted to one of **Null, True, False, JSONString, JSONNumber, JSONObject or JSONArray**.
    * __*JSONString.parseString(String jsonText)*__: the given jsonText must be wrapped by a pair of quotation marks (").
    * __*JSONValue.parse(CharSequence, Range)*__: expect and parse a part of the given JSON context as one of **Null, True, False, JSONString, JSONNumber**.
    * __*JSONObject.parse(String valueString)*__: expect the *valueString* is enclosed by '{' and '}', and cast the result of __*Parser.parse()*__ to be **JSONObject**.
    * __*JSONArray.parseArray(String valueString)*__: expect the *valueString* is enclosed by '[' and ']', and cast the result of __*Parser.parse()*__ to be **JSONArray**.
  * Static methods of _Utilities.java_ to convert JAVA Objects to JSON Objects defined in JsonTuples:
    * __*asJSONArrayFromArray(Object array)*__: convert a JAVA Array, of primitive or object elements, to a **JSONArray**.
    * __*asJSONArrayFromCollection(Object collection)*__: convert a JAVA Collection (such as _List_, _Set_ or _Queue_) to a **JSONArray**.
    * __*asJSONObject(Object object)*__: the given object must be a Map<Object, Object>
    * __*asJSONNumber(Object object)*__: the given object must be a JAVA Number(like Integer, Float, Double, Short, BigInteger, BigDecimal), the constucted **JSONNumber** instances would be compared by their String forms. For example, JSONNumber instance from 1.1f would be equal to that from Double.valueOf(1.1). If the object is null, then the constructed JSONNumber would be equal to **JSONValue.Null**.
    * __*asJSONString(Object object)*__: the given object must be a String or null, the latter would result in a JSONString instance equal with JSONValue.Null.
    * __*asJSONStringFromOthers(Object object)*__: convert all other types of Non-null object to JSONString with their default toString().
    * __*IJSONValue jsonify(Object object)*__: would check the type of the given object to call a method above. For types not covered above, by default the  __*asJSONStringFromOthers(Object object)*__ would be called to generate a JSONString, but it is possible to inject serialization/de-serialization methods into __*Utilities.classConverters*__ that is a Map<Class, Tuple2> where the value of a given class includes both serialization and de-serialization for a concerned type of object, then the serialization method would be called to convert the matched instance to its text equivalent.  
    
Usually, the above methods shall be enough to get most JSON to/from JAVA conversions done.

## Basic Examples

Lambda expressions are a new and important feature included in Java SE 8. They provide a clear and concise way to represent one method interface using an expression. Lambda expressions also improve the Collection libraries making it easier to iterate through, filter, and extract data from a Collection. In addition, new concurrency features improve performance in multicore environments.

However, of the 43 standard functional interfaces in [java.util.function package](https://docs.oracle.com/javase/8/docs/api/java/util/function/package-summary.html), many of them are primitive arguments specific and none of them can be used to refer Java methods throwing excetpions. This package has defined a set of functional interfaces throws Exceptions while can be converted to the standard functional interfaces without throwing Exceptions easily.

To enable functional programming with JAVA with immutability, a set of Tuple classes are defined that keep up to 20 strong-typed accessible values and comparable by values for equality.

With throwable functional interfaces to define the key service logics, Tuples to keep any combinations of data or functional interfaces, the Map based Repositories are used to generate and buffer not only data, but also complex service logics on the fly. For example, as part of this package, several super utilities related with Array are developed to new any kind of array, converting one type of array to another, deep equals by values and etc that would be extremely hard without the support of the Repositories.


##Functional Interfaces without Exception Handling
In Java exceptions under Error and RuntimeException classes are unchecked exceptions, everything else under throwable is checked. If a method has any checked Exception, then it must either handle the exception explicitly or specify the exception using throws keyword.

There is no exemption with Functional Interface in Java 8: if the single abstract method defined doesn't throws some Exception, as those defined in [java.util.function package](https://docs.oracle.com/javase/8/docs/api/java/util/function/package-summary.html), then the Lambda Expressions must handle any potential exceptions properly before being assigned to any Functional Interface instance. In another words, the concerned business logic have to be wrapped with *try{}catch{}* before assigning to any such functional interfaces first. Because so many JAVA methods are decorated with "throws XxxException", they cannot be declared as such functional interfaces directly.

For example, the signature of Class.forName() is as below:
```java
public static Class<?> forName(String className) throws ClassNotFoundException
```
Assigning it to a Function<String, Class> would always get error of "Unhandled exception: ClassNotFoundException":
```java
Function<String,Class> getClass = className -> Class.forName(className);
```
Alternatively, the Class.forName must be put in the try block:
```java
        Function<String, Class> getClass = className -> {
            try {
                return Class.forName(className);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                return null;
            }
        };
```
This form of defining functional interface with length lambda expression not only make lambda expression not succinct, it actually raise more issues: why should be ClassNotFound exception being handle here before the method being called, how can it be combined with other business logics later if null is not expected when calling in a different context?

### Throwable Functional Interfaces
My solution to keep business without Exception Handling is simply define the unique method of the functional interfaces with **"throws Exception"** to match with most Java method signatures.

For instance, by defining the unique method of *FunctionThrowable<T,R>* as below:
```java
    R apply(T t) throws Exception;
```
Now the Class.forName(String className)can be referred as:
```java
    FunctionThrowable<String, Class> getClass = className -> Class.forName(className);
```
Or 
```java
    FunctionThrowable<String, Class> getClass = Class::forName;
```
More importantly, the business logic of Class.forName can be saved as a variable, and used as a block to be added to a bigger picture that needs exception handling only when called. Only without the repeative defensive Exception Handling codes, function can be used as first class member in JAVA.

Following functional interfaces are defined to accept up to 7 input arguments:

|Throwable Interfaces|Number of Inputs|With Value Returned|Non-Throwable function|Convert with Handler|Convert with defaultValue|
|:---|:---:|---|:---|:---:|:---:|
|RunnableThrowable|0|No|*Runnable*|Yes|No|
|SupplierThrowable<R>|0|Yes|*Supplier<R>*|Yes|Yes|
|ConsumerThrowable<T>|1|No|*Consumer<T>*|Yes|No|
|FunctionThrowable<T,R>|1|Yes|*Function<T,R>*|Yes|Yes|
|PredicateThrowable<T>|1|Yes|*Function<T,Boolean>*|Yes|Yes|
|BiConsumerThrowable<T,U>|2|No|*BiConsumer<T,U>*|Yes|No|
|BiFunctionThrowable<T,U,R>|2|Yes|*BiFunction<T,U,R>*|Yes|Yes|
|BiPredicateThrowable<T,U>|2|Yes|*BiFunction<T,U,Boolean>*|Yes|Yes|
|TriConsumerThrowable<T,U,V>|3|No|TriConsumer<T,U,V>|Yes|No|
|TriFunctionThrowable<T,U,V,R>|3|Yes|TriFunction<T,U,V,R>|Yes|Yes|
|QuadConsumerThrowable<T,U,V,W>|4|No|QuadConsumer<T,U,V,W>|Yes|No|
|QuadFunctionThrowable<T,U,V,W,R>|4|Yes|QuadFunction<T,U,V,W,R>|Yes|Yes|
|PentaConsumerThrowable<T,U,V,W,X>|5|No|PentaConsumer<T,U,V,W,X>|Yes|No|
|PentaFunctionThrowable<T,U,V,W,X,R>|5|Yes|PentaFunction<T,U,V,W,X,R>|Yes|Yes|
|HexaConsumerThrowable<T,U,V,W,X,Y>|6|No|HexaConsumer<T,U,V,W,X,Y>|Yes|No|
|HexaFunctionThrowable<T,U,V,W,X,Y,R>|6|Yes|HexaFunction<T,U,V,W,X,Y,R>|Yes|Yes|
|HeptaConsumerThrowable<T,U,V,W,X,Y,Z>|7|No|HeptaConsumer<T,U,V,W,X,Y,Z>|Yes|No|
|HeptaFunctionThrowable<T,U,V,W,X,Y,Z,R>|7|Yes|HeptaFunction<T,U,V,W,X,Y,Z,R>|Yes|Yes|

These 18 throwable functional interfaces follow the naming convention of [java.util.function](https://docs.oracle.com/javase/8/docs/api/java/util/function/package-summary.html), their counterpart functional interfaces without checked Exceptions are defined as their inner interfaces, if not defined in java.util.function package. For example, TriFunction<T,U,V,R> is defined in TriFunctionThrowable<T,U,V,R>, with similar method signature:
```java
R apply(T t, U u, V v);
```
as the throwable version of TriFunctionThrowable<T,U,V,R>:
```java
R apply(T t, U u, V v) throws Exception;
```
The throwable functional interfaces can be casted to their counterparts easily when called to evaluate the business logic.

### Calling Throwable Function Interfaces

Although it is fully possible to convert the above throwable functional interfaces to either RunnableThrowable (if there is no value returned) or SupplierThrowable<R> (if a value of type R is returned) with asRunnable(...) or asSupplier(...) as example of converting TriFunctionThrowable<T,U,V,R> to SupplierThrowable<R> as below:
```java
@FunctionalInterface
interface TriFunctionThrowable<T,U,V,R> extends AbstractThrowable {
    R apply(T t, U u, V v) throws Exception;

    default SupplierThrowable<R> asSupplier(T t, U u, V v){
        return () -> apply(t, u, v);
    }
}
```

Then the caller of these functional interfaces (Functions class for example) can evaluate them by first converting them as either RunnableThrowable or SupplierThrowable<R>. That is working, but not quite like the normal routing to call a method.

Preferrably, the functional interfaces without returnning values (like ConsumerThrowable<T>, BiConsumerThrowable<T,U> and etc.) can be bundled with a Consumer<Exception> would convert it gracefully to the Non-Throwable versions. For example:
```java
    default BiConsumer<T,U> withHandler(Consumer<Exception> exceptionHandler){
        BiConsumer<T,U> biConsumer = (t, u) -> {
            try {
                accept(t, u);
            } catch (Exception e) {
                if(exceptionHandler != null)
                    exceptionHandler.accept(e);
            }
        };
        return biConsumer;
    }
```

For those functional interfaces with value returned (like SupplierThrowable<R>, FunctionThrowable<T,R>, BiFunctionThrowable<T,U,R> and etc., all extending empty marker *WithValueReturned<R>* interface), two methods are defined to converting them to the Non-Throwable counterparts. Taken BiFunctionThrowable<T, U, R> for instance, following two default methods are defined to convert it to FunctionThrowable<T, U, R> (which is defined in java.util.function):
```java
    default BiFunction<T, U, R> withHandler(BiFunction<Exception, WithValueReturned, Object> exceptionHandler){
        BiFunction<T, U, R> function = (t, u) -> {
            try {
                return apply(t, u);
            } catch (Exception e) {
                return exceptionHandler == null ? null : (R)exceptionHandler.apply(e, this);
            }
        };
        return function;
    }

    default BiFunction<T,U, R> orElse(R defaultValue){
        BiFunction<T,U, R> function = (t, u) -> {
            try {
                return apply(t, u);
            } catch (Exception e) {
                return defaultValue;
            }
        };
        return function;
    }
```

The *BiFunction<Exception, WithValueReturned, Object> exceptionHandler* outlines almost almost everything need to be cared when an Exception is thrown by such a throwable functional interface:
* Exception thrown;
* The lambda expression throwing this Exception.

Consequently, before evaluating any of these throwable functional interface, a shared *BiFunction<Exception, WithValueReturned, R> exceptionHandler* can be used to convert it to something similar to a normal Java method with Exception handled by that exceptionHandler instance. For example, it is possible to define a complex static *exceptionHandler* instance shared by EVERY thrownable functional interfaces:
```java
    static Class<? extends Exception>[] negligibles = new Class[]{
            NullPointerException.class, IllegalArgumentException.class
    };
    static Class<? extends Exception>[] noticeables = new Class[]{
            SQLException.class, NumberFormatException.class
    };
    static BiFunction<Exception, WithValueReturned, Object> defaultReturner = (ex, lambda) -> {
        final Class<? extends Exception> exceptionType = ex.getClass();
        if(IntStream.range(0, noticeables.length).anyMatch(i -> noticeables[i].isAssignableFrom(exceptionType))){
            String msg = ex.getMessage();
            msg = ex.getClass().getSimpleName() + (msg == null?"":":"+msg);
            logs.add(msg);
        }else if(IntStream.range(0, negligibles.length).allMatch(i -> !negligibles[i].isAssignableFrom(exceptionType))){
            throw new RuntimeException(ex);
        }
        final Class returnType = TypeHelper.getReturnType(lambda);
        if(returnType == Integer.class || returnType == int.class)
            return -1;
        return TypeHelper.getDefaultValue(returnType);
    };
```

Then for a simple function accepting 2 arguments and return an Integer, the concerned business logic would be evaluated with invalid arguments directly after being converted to non-throwable *BiFunction<String, Boolean, Integer>*:

```java
    BiFunctionThrowable<String, Boolean, Integer> f9 = (s, b) -> Integer.valueOf(s) + (b ? 1 : 0);
	assertEquals(Integer.valueOf(-1), f9.withHandler(defaultReturner).apply("8.0", true));
```

The Functions class defined two static instances:
1. ThrowsRuntimeException: would throw RuntimeException with any Exception thrown by the funcitonal interfaces;
2. ReturnsDefaultValue: would swallow the caught exception silently and return the default value expected from the lambda expression. (0 for WithValueReturned<Integer>, false for WithValueReturned<Boolean>, 0f for WithValueReturned<Float>...)

In this way, the funcitonal interfaces can define the business logic only, leave the handling of exceptional cases to a single method. 


##Tuple

Inspired by the Tuple class of .NET framework, the Tuple classes in this JAVA package are designed to:
1. Make data generally immutable for functional programming with JAVA.
2. Use as a generic data structure to hold any numbers of strong-typed objects;
3. Support up to 20 strong-typed accessor of the objects;
4. Efficient comparing and equivalent evaluations between Tuples;
5. Implements AutoCloseable by close any AutoCloseable elements automatically;
6. To replace the Plain Old Java Objects to some extends.

Although it is possible to declare array or list of Objects to keep variable length of arguments could be a quick remedy when handling a set of data as a whole, it is usually hard to keep the type information of these data and access each individual data or element conveniently.

The Tuple class in this package use a *Object[]* to keep all its elements, a set of extended classes (Tuple0, Tuple1, Tuple2, ..., Tuple20 and TuplePlus) are defined to keep 0 to 20 strong-typed elements that could be accessed via the default method defined in WithValues interfaces (WithValues1, WithValues2, ..., WithValues20).

The constructors of these classes are not public: constructions of these Tuple is by calling *Tuple.create(...)* with fixed length of arguments directly or by calling *Tuple.of(Object... elements)* indirectly to:
1. Return the unique Tuple.UNIT instance if no elment is provided;
2. Create Tuple1<T> instance when 1 element is provided; Tuple2<T,U> when 2;... Tuple20<T,U,V...M> when 20 elements are provided;
3. Create TuplePlus<T,U,V...M> when there are more than 20 elements are provided.

Since the underlying WithValuesXX interfaces are defined as by extending the previous one: WithValues1 extends WithValues, WithValues2 extends WithValues1..., WithValues20 extends WithValues19, and TuplePlus still implements WithValues20<T,U,V,W,X,Y,Z,A,B,C,D,E,F,G,H,I,J,K,L,M> as Tuple20, the first 20 elemetns would still be accessible with getFirst(), getSecond()... getTwentieth() respectively. In this way, the accessor methods would be defined once only, and shared between all relevant Tuple classes: if it works in TuplePlus, it shall also work in Tuple20, Tuple19...Tuple1........

The define and use of Tuple is quite simple, as illustrated below:
```java
    Tuple7<Boolean, Integer, String, LocalDate, Progress, String, Double> hepta=
            Tuple.create(false, 77, "Tuple7", LocalDate.MAX, Progress.Done, "Result", 99.9d);
	        Assert.assertEquals(false, hepta.getFirst());
	Assert.assertEquals(Integer.valueOf(77), hepta.getSecond());
	Assert.assertEquals("Tuple7", hepta.getThird());
	Assert.assertEquals(LocalDate.MAX, hepta.getFourth());
	Assert.assertEquals(Progress.Done, hepta.getFifth());
	Assert.assertEquals("Result", hepta.getSixth());
	Assert.assertEquals(Double.valueOf(99.9), hepta.getSeventh());
```

When elements of the Tuple are of the same type, the *TupleSet<T>* class is used to persist the type info along with the values. The *TupleSet<T> getSetOf(Class<T> clazz)* of Tuple can be used to extract all matched elements as shown below:
```java
        Tuple t = Tuple.create(1, 2, 3, null, 4, 5);
        assertTrue(TypeHelper.valueEquals(new Integer[]{1,2,3,4,5}, t.getSetOf(int.class).asArray()));
```

With the assumption of Tuple's immutability, the hashCode, toString and deepLength (would be covered later) would be evaluated once only and used to make *int compareTo(Tuple o)*, *int hashCode()*, *String toString()* and *boolean equals(Object obj)* evaluations faster.

The close() method of AutoCloseable is implemented as below:
```java
    @Override
    public void close() throws Exception {
        if(!closed) {
            //Close AutoCloseable object in reverse order
            for (int i = values.length - 1; i >= 0; i--) {
                Object value = values[i];
                if (value != null && value instanceof AutoCloseable) {
                    Functions.Default.run(() -> ((AutoCloseable) value).close());
                    Logger.L("%s closed()", value);
                }
            }
            closed = true;
            Logger.L("%s.close() run successfully!", this);
        }
    }
```

That means if the Tuple is composed by several inter-dependent AutoCloseable objects, then closing the Tuple would ensure those element objects to be closed in reversed order of their storage. The feasibility and value shall be evaluated against real applications.

Finally, the strong-typed Tuples provide the possbility of replacing the wide use of POJOs with generic Tuple instances. Again, it shall be analysed case by case.


## Repositories

The Maps(or counterpart of Dicitionary in .NET) could be used to buffer interim results with the nature mappings of keys and mapped values related with these keys, some time it is hard to keep multiple properties of the same key together, and there are too much duplicated codes to do something like checking if a key exists or not, then either retrieving the buffered results or calculating before caching the mapped values before consuming them.

The Repository<TKey, TValue> class bundle the Map (as storage media of key-value-pairs) and the business logic to get values from given keys together. As the example below illustrated, once the Repository instance is constructed, it would calculate values with given keys, buffering them before returning back automatically:
```java
        Repository<String, Integer> repository = new Repository<>(s -> s.length());
        assertEquals(Integer.valueOf(0), repository.apply(""));
        assertEquals(Integer.valueOf(1), repository.apply("a"));
        assertEquals(Integer.valueOf(3), repository.apply("abc"));
```
Or use the alternative version with default value provided to cope with exceptional cases:
```java
        assertEquals(Integer.valueOf(-1), repository.get(null, -1));
```

With the alternative constructor with Map instance *Repository(Map<TKey, TValue> map, TriConsumerThrowable<TKey, TValue, TValue> changesConsumer, FunctionThrowable<TKey, TValue> valueFunction)*, it is possible to initialize the repository with pre-defined values as shown below:
```java
        Repository<String, Integer> repository = new Repository<>(
                new HashMap<String, Integer>(){{
                    put("Steel", 100);
                    put("Silver", 200);
                    put("Gold", 500);
                }}
                , null, s -> s.length());
        assertEquals(Integer.valueOf(4), repository.apply("Iron"));
        assertEquals(Integer.valueOf(500), repository.get("Gold", 40));
        assertEquals(Integer.valueOf(200), repository.apply("Silver"));
```

The calculated values from the given business logic can still be overriden or cleared by calling following two methods of the repository:
```java
 public TValue update(TKey tKey, TValue existingValue, TValue newValue) throws Exception;
 public int clear(BiPredicate<TKey, TValue> keyValuePredicate);
```

The *TupleRepository<TKey>*, by extending *Repository<TKey, Tuple>*, make it is possible to keep multiple values related with a single key of type TKey saved in single Tuple instances but with no value types persisted. The following types and their children types specify any combinations of 1-7 strong-typed elements as keys and 1-7 strong-typed elements as values:

| Class Name	 | Number of values	| SubClass of 1-keys | SubClass of 2-keys | SubClass of 3-keys | SubClass of 4-keys | SubClass of 5-keys | SubClass of 6-keys | SubClass of 7-keys |
|----------------|-------------------|:------------------:|:------------------:|:------------------:|:------------------:|:-----------------:|:------------------:|:------------------:|
|TupleRepository1|        1          |   TupleKeys1       |   TupleKeys2       |   TupleKeys3       |   TupleKeys4       |   TupleKeys5      |   TupleKeys6       |   TupleKeys7       |
|TupleRepository2|        2          |   TupleKeys1       |   TupleKeys2       |   TupleKeys3       |   TupleKeys4       |   TupleKeys5      |   TupleKeys6       |   TupleKeys7       |
|TupleRepository3|        3          |   TupleKeys1       |   TupleKeys2       |   TupleKeys3       |   TupleKeys4       |   TupleKeys5      |   TupleKeys6       |   TupleKeys7       |
|TupleRepository4|        4          |   TupleKeys1       |   TupleKeys2       |   TupleKeys3       |   TupleKeys4       |   TupleKeys5      |   TupleKeys6       |   TupleKeys7       |
|TupleRepository5|        5          |   TupleKeys1       |   TupleKeys2       |   TupleKeys3       |   TupleKeys4       |   TupleKeys5      |   TupleKeys6       |   TupleKeys7       |
|TupleRepository6|        6          |   TupleKeys1       |   TupleKeys2       |   TupleKeys3       |   TupleKeys4       |   TupleKeys5      |   TupleKeys6       |   TupleKeys7       |
|TupleRepository7|        7          |   TupleKeys1       |   TupleKeys2       |   TupleKeys3       |   TupleKeys4       |   TupleKeys5      |   TupleKeys6       |   TupleKeys7       |

There are corresponding factory methods to create the relevant TupleRepository with specific number of strong-typed values from strong-typed keys. For example, in TupleRepository3:
```java
    public static <K1,K2, T,U,V> TupleKeys2<K1,K2, T,U,V> fromKeys2(
            BiFunctionThrowable<K1,K2, Tuple3<T,U,V>> valueFunction){
        return new TupleKeys2(valueFunction);
    }
```
The fromKeys() method above would create an instance of TupleRepository3.TupleKeys2<K1,K2, T,U,V> with a function accepting two input arguments to return a Tuple3<T,U,V> as result. This TupleRepository would keep keys of Tuple2<K1,K2> and values of Tuple3<T,U,V> thus making the Java Map could save the tabular data of multiple columns, potentially multiple key columns, as commonly represented by SQL tables to a single map. For example, codes below would embed a function generating a Tuple2<String,Integer> with 4 input arguments:
```java
        TupleRepository2.TupleKeys4<String, String, Integer, Boolean, String, Integer> repository = TupleRepository2.fromKeys4(
                (s1, s2, n, b) -> Tuple.create(s1+s2, n + (b?s1.length(): s2.length()))
        );

        assertEquals(Tuple.create("abc", 5), repository.retrieve(Tuple.create("a", "bc", 3, false)));
        assertEquals(true, repository.containsKeyOf("a", "bc", 3, false));
        assertEquals(null, repository.getSecond("a", null, 3, false));
        assertEquals(1, repository.getSize());
```

However, using these TupleRepositories to buffer only combinations of multiple types of pure data is not the original intention for me, treating functions represented by Lambda Expressions as values stored in the repository is the meat of the matter. The next section would introduce some useful utilities that are created by Repositories.


## Utilities

With the existing and wide use of eight primitive types in JAVA (int, byte, boolean, char, short, long, float and double), there are many redundant functions that are hard to hard to used as generic ones. For example, many functional interfaces defined in [java.util.function](https://docs.oracle.com/javase/8/docs/api/java/util/function/package-summary.html) like DoubleConsumer, DoubleFunction<R>, DoubleSupplier, IntConsumer, IntFunction<R>, IntSupplier shares similar signatures as Consumer<T>, Function<T,R> and Supplier<T> but has very limited use scope.

On the other hand, many Java utilities have to define multilple copies of a generic method to be compatible with these primitive types. For instance, in [Arrays class](https://docs.oracle.com/javase/7/docs/api/java/util/Arrays.html), in addition to *static boolean equals(Object[] a, Object[] a2)*, there are 8 other *boolean equals()* for the 8 primitive typed arrays (int[], float[], ...). The same is true for *int binarySearch(Object[] a, Object key)*, *T[] copyOf(T[] original, int newLength)* and etc.

Is that possible to create generic versions of these utilities? Very hard with the conventional way that comes with many if...else... and it is extremely hard to cover every case and get the business logic maintainable. With the Repositories, some difficult things could be simplified.

For example, the following classOperators repository is defined in the TypeHelper.java class:
```java
TupleRepository6<
                    Class,              //concerned Class

                    Predicate<Class> //Equivalent predicate to check if another class is regarded as equal with the concerned class
                    , FunctionThrowable<Integer, Object>             //Array factory
                    , Class              //Class of its array
                    , TriConsumerThrowable<Object, Integer, Object>//Function to set the Element at Index of its array
                    , TriFunctionThrowable<Object, Integer, Integer, Object>//copyOfRange(original, from, to) -> new array
                    , Function<Object, String>             // Convert array to String
            > classOperators = ...
```

Some generic functions supported by this single repository, directly or indirectly, are listed below:
|				Method with Signature				|		Functional Description																		|		Note		|
|---------------------------------------------------|---------------------------------------------------------------------------------------------------|-------------------|
|boolean areEquivalent(Class class1, Class class2)  |If one class is identical or is wrpper of another													|					|
|Object getNewArray(Class clazz, int length)		|Create an array of specific element type and length												|in ArrayHelper		|
|Class getArrayClass(Class clazz)					|Get the class of the Array of concerned elements													|					|
|TriConsumerThrowable<Object, Integer, Object> getArrayElementSetter(Class clazz)|get the setter operator to set element at specific position of concerned array with given value| Internal use |
|Object copyOfRange(Object array, int from, int to)	|Copies the specified range of the specified array into a new array.								|					|
|<T> T[] mergeTypedArray(T[] array, T... others)	|Merge two array of the same type together, and returned the merged array of the same type.			|in ArrayHelper		|
|String deepToString(Object obj)					|Returns a string representation of the "deep contents" of the specified array.						|					|

Notice the above methods have no restrictions on the type of the Object, thus they can be used for both object and primitive types, and even multi-dimensional arrays. The samples below show how to use them:
```java
        Predicate<Class> classPredicate = getClassEqualitor(int.class);
        assertTrue(classPredicate.test(int.class) && classPredicate.test(Integer.class));
		classPredicate = getClassEqualitor(int[].class);
        assertTrue(classPredicate.test(int[].class) && classPredicate.test(Integer[].class));
        assertTrue(getClassEqualitor(Map.class).test(HashMap.class));

		char[] chars = (char[]) ArrayHelper.getNewArray(char.class, 2); // -> new char[2]
		
		//Array factory of 2-dimensional array would invoke and call the array factory of Comparable[] automatically
		Comparable[][] comparablesArray = (Comparable[][]) ArrayHelper.getNewArray(Comparable[].class, 2);
        comparablesArray[0] = new Integer[]{1, 2};
        comparablesArray[1] = new String[0];
		
		assertValueEquals(new long[]{1L, 2L}, TypeHelper.copyOfRange(new long[]{0L,1L,2L}, 1, 3));
		assertValueEquals(new Number[]{1,2,3,4.4}, ArrayHelper.mergeTypedArray(new Number[]{1,2}, 3, 4.4));
		
		//Tuple.toString is based on TypeHelper.deepToString()
		Tuple.create("Seven", new String[]{"arrayValue1", "arrayValue2"}, Character.valueOf('7'), '7', Integer.valueOf(7),
            new Character[]{'C'}, new char[]{'a','b'}).toString(); // -> [Seven, [arrayValue1, arrayValue2], 7, 7, 7, [C], [a, b]]
```

The above TupleRepository can be used by other Repositories to build even more advanced generic functions with class related atomic operators like generic array getter or setter, so as to create generic converters cheaply: not only primitive types to objects, but any types to any other types like Integer[][] to Number[][] purely based on the equivalent or assignability of fromType and toType.

The process of getting such converters is actually quite complex, evaluating of one type might involved evaluations of its component types. But since the related **functions** are evaluated and buffered, the overhead of even the most complicate processing is still limited even when one class *CANNOT* to converted to another class for instance, the buffered functions would return null immediately with such combination of given types.

Some helpful utilities include:
1. *Object getDefaultValue(Class clazz)*: Return the equivalent class of the concerned class.
	*null by default;
	*For primitive types: 0 for int.class, false for boolean.class as specified in [Primitive Data Types](https://docs.oracle.com/javase/tutorial/java/nutsandbolts/datatypes.html);
	*For array types, depending on the flag of TypeHelper.EMPTY_ARRAY_AS_DEFAULT that can be changed with System Property before calling, can be either null or empty array (int[].class-> new int[0]...)
2. *Object toEquivalent(Object obj)*: Convert an Object to its equivalent form.
	* For primitive types, like int, byte[], char[][], the primitive value is converted to Integer, Byte[] and Character[][] respectively
	* or wrapper types, like Boolean, Double[], Float[][], the wrappers is converted to boolean, double[] and float[][] respectively
	* or other types, they would be converted to Object, Object[] based on if they are Array and their dimension
3. *<T> T convert(Object obj, Class<T> toClass)*: Convert Object to another type either serially or parallelly.
4. *void setAll(Object array, FunctionThrowable<Integer, Object> generator)*: set all elements of the concerned array with values generated by generator.

The sample uses of the above methods are listed below:
```java
        assertEquals(0d, getDefaultValue(Double.class));
        assertEquals(0f, getDefaultValue(Float.class));
        assertEquals(false, getDefaultValue(Boolean.class));
        assertEquals(null, getDefaultValue(Object.class));
        assertEquals(null, getDefaultValue(String.class));
		
		Object converted = TypeHelper.toEquivalent(new int[]{1,2,3}); // -> Integer[]{1,2,3}
		TypeHelper.toEquivalent(new Integer[][]{new Integer[]{1,2}, new Integer[]{3}, null, new Integer[]{4,5}}); // -> new int[][]{new int[]{1,2}, new int[]{3}, null, new int[]{4,5}}
		
		TypeHelper.convert(new Object[]{1.0, 2f, 3L, 4, 5}, Number[].class); // -> Number[]{1.0, 2f, 3L, 4, 5}
		TypeHelper.convert(new Integer[][][]{new Integer[][]{new Integer[]{1}, null}, new Integer[0][]}, Object[].class); // -> Object[]{new Integer[][]{new Integer[]{1}, null}, new Integer[0][]}
		
		ArrayHelper.setAll(new int[3], i -> i+100); //-> int[]{100, 101, 102}
		ArrayHelper.setAll(new String[3], i -> String.valueOf(i)); // -> new String[]{"0", "1", "2"}
```

Another idea copied from .NET is the Lazy<T> class that is usually created with constructor of *Lazy(SupplierThrowable<T> supplier)*: instead of providing the value directly in the constructor, a supplier of the expected value is provided that shall be evaluated at the first time of retrieving expected value, so as to delay the initialization. The enhancement compared with the .NET version is the implementation of AutoCloseable interface that support chained closing as the simplified test indicated:
```java
        Lazy<String> emailLazy = new Lazy<>(() -> "email@test.com");
        try(Lazy<Account> accountLazy = emailLazy.create(Account::new)){
            Lazy<Inbox> inboxLazy = accountLazy.create(Inbox::new);
            Lazy<Outbox> outboxLazy = accountLazy.create(Outbox::new);
            outboxLazy.getValue().sendMail();
            inboxLazy.getValue().checkMail();
        }catch (Exception ex){}
        assertTrue(TypeHelper.valueEquals(logs.toArray(), new String[]{
                "Account created: email@test.com", "Outbox opened", "Send mail", "Inbox opened", "Check inbox",
                "Outbox closed", "Inbox closed", "Account closed: email@test.com"
        }));
```

In addition, most of the above utilities support 3 ways of evaluations: parallelly, serially or default (serially for small set and parallelly for huge set like arrays with length over 100k, including valueEquals() that means to replace [Arrays.deepEquals()](https://docs.oracle.com/javase/7/docs/api/java/util/Arrays.html#deepEquals(java.lang.Object[],%20java.lang.Object[])) to neglect differences between primitive types and their wrappers. The parallel processing is based on Java 8 Parallel Streams. However, the performance Parallel Streams in Java 8 is not quite satisfying, hopefully that could be improved in futrue releases.

To summarise, the io.github.cruisoring.functionExtentions package provides 3 types of blocks to build Java applications in funcitonal programming way:
1. The throwable funcitonal interfaces make lambda expressions can be declared easier without the boiler-plate exception handling codes, and they can be converted to conventional funcitonal expressions with Exceptions handled with simple and potential shared Exception handling methods. Consequently, developers can declare funcitonal interfaces as blocks of key business logics to be used to build one or multiple big pictures later.
2. The Tuple classes makes chunks of strong-typed values could be composed as a single unit for processing, that shall be immutable and could be compared efficiently with others by their values. 
3. The Repository binds the business logic to retrieve data with the media to keep data together, with dictionary matching algorithm to replace complex conditional switching and enhance the processing efficiency by its inherited buffering capacity. Combined with Tuples, TupleRepositories extended this utility with multiple keys and/or multiple values linked together and with their type information persisted.
4. Many of the utilities provided in this package shows how above techniques could be used to build complicated functionalities in a different manner than plain Java.

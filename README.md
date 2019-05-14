#JsonTuples
===================


##Brief
JsonTuples is build over Tuples of [functionExtensions 2.0.0|http://repo1.maven.org/maven2/io/github/cruisoring/functionExtensions/2.0.0/] as not only a Java serialization/deserialization library to convert Java Objects into JSON and back, but also a generic data processing engine to manipulate or compare huge JAVA array/collection/map. It is under the [MIT license](http://www.opensource.org/licenses/mit-license.php).

### Objectives
 *	A handy tools to parse JSON text to Tuple based JAVA objects, usually JSONObject or JSONArray that can handle large JSON text quickly with 
 *  Helper methods defined in Utilities.java to support efficient data processing:
    - *Convert Map<String, Object> instances to/from JSONObject that implements Map<String, Object> by itself.
    - *Convert Array or Collection to/from JSONArray.
 *  
 *	
 Declares a rich set of funcitonal interfaces throwing Exceptions, that can be converted to conventional ones with Checked Exceptions handled with shared exceptionHandlers, thus allow developers to define the concerned business logic only within the lambda expressions.
 *	Implements an immutable data structure to keep and retrieve up to first 20 strong-typed values as a set of Tuple classes.
 *	Provides Repositories as a kind of Map-based intelligent utility with pre-defined business logic to evaluate a given key or keys (upto 7 strong-typed values as a single Tuple key) to get corresponding value or values (upto 7 strong-typed values as a single Tuple value), buffer and return them if no Exception happened.
 *	Multiple powerful generic utilities to support above 3 types utilities, mainly build with Repositories, that support various combinations of primitive and objective types and arrays. For example:
	- *Object getNewArray(Class clazz, int length)*: new an array of ANY type with element type and length of the newed instance.
	- *String deepToString(Object obj)*: Returns a string representation of the "deep contents" of the specified array.
	- *Object copyOfRange(Object array, int from, int to)*: Copy all or part of the array as a new array of the same type, no matter if the array is composed by primitive values or not.
	- *T convert(Object obj, Class<T> toClass)*: convert the object to any equivalent or assignable types.
	- *boolean valueEquals(Object obj1, Object obj2)*: comparing any two objects. If both are arrays, comparing them by treating primitive values equal to their wrappers, null and empty array elements with predefined default strategies.
 
##Get Started

For maven users for example:
```xml
<dependency>
    <groupId>io.github.cruisoring</groupId>
    <artifactId>functionExtensions</artifactId>
    <version>2.0.0</version>
</dependency>
```

Or get the packages directly from [Maven Central](http://repo1.maven.org/maven2/io/github/cruisoring/functionExtensions/2.0.0/)

##Implementation Techniques

Following threads on codeproject discussed some of the techniques used to design and develop this library:
- [Throwable Functional Interfaces](https://www.codeproject.com/Articles/1231137/functionExtensions-Techniques-Throwable-Functional)
- [Tuples](https://www.codeproject.com/Articles/1232570/Function-Extensions-Techniques-Tuples)
- [Repository](https://www.codeproject.com/Articles/1233122/functionExtensions-Techniques-Repository)

##Overview

Lambda expressions are a new and important feature included in Java SE 8. They provide a clear and concise way to represent one method interface using an expression. Lambda expressions also improve the Collection libraries making it easier to iterate through, filter, and extract data from a Collection. In addition, new concurrency features improve performance in multicore environments.

However, of the 43 standard functional interfaces in [java.util.function package](https://docs.oracle.com/javase/8/docs/api/java/util/function/package-summary.html), many of them are primitive arguments specific and none of them can be used to refer Java methods throwing excetpions. This package has defined a set of functional interfaces throws Exceptions while can be converted to the standard functional interfaces without throwing Exceptions easily.

To enable functional programming with JAVA with immutability, a set of Tuple classes are defined that keep up to 20 strong-typed accessible values and comparable by values for equality.

With throwable functional interfaces to define the key service logics, Tuples to keep any combinations of data or functional interfaces, the Map based Repositories are used to generate and buffer not only data, but also complex service logics on the fly. For example, as part of this package, several super utilities related with Array are developed to new any kind of array, converting one type of array to another, deep equals by values and etc that would be extremely hard without the support of the Repositories.


##Functional Interfaces without Exception Handling

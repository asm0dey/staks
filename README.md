# kxml-parser: Elegant XML Parsing for Kotlin

[![JVM](https://img.shields.io/badge/JVM-23-blue.svg)](https://www.oracle.com/java/technologies/javase/jdk23-archive-downloads.html)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.10-blue.svg)](https://kotlinlang.org/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](https://opensource.org/licenses/MIT)

A lightweight, idiomatic Kotlin library for XML parsing with a fluent DSL. Built on top of StAX and Kotlin Coroutines, it provides both synchronous and asynchronous (Flow-based) APIs for efficient XML processing.

## Table of Contents

- [Features](#features)
- [Installation](#installation)
  - [Gradle (Kotlin DSL)](#gradle-kotlin-dsl)
  - [Maven](#maven)
- [Basic Usage](#basic-usage)
  - [Flow-based API](#flow-based-api)
- [Advanced Usage](#advanced-usage)
  - [Accessing Root Element Data](#accessing-root-element-data)
  - [Handling Complex Nested Structures](#handling-complex-nested-structures)
  - [Handling Optional Elements](#handling-optional-elements)
  - [Working with Current Element Context](#working-with-current-element-context)
  - [Using the Unary Plus Operator](#using-the-unary-plus-operator)
  - [Working with Namespaces](#working-with-namespaces)
  - [Handling CDATA Sections](#handling-cdata-sections)
- [Extending the Library](#extending-the-library)
  - [Creating Custom Value Processors](#creating-custom-value-processors)
  - [Creating Custom Element Collectors](#creating-custom-element-collectors)
  - [Creating Domain-Specific Extensions](#creating-domain-specific-extensions)
- [Best Practices](#best-practices)
  - [Error Handling](#error-handling)
  - [Performance Considerations](#performance-considerations)
- [API Reference](#api-reference)
  - [Core Functions](#core-functions)
  - [Flow Extensions](#flow-extensions)
  - [DSL Extensions](#dsl-extensions)
  - [Type Conversions](#type-conversions)
- [Contributing](#contributing)
- [License](#license)

## Features

- **Idiomatic Kotlin DSL** for clean, readable XML parsing code
- **Type-safe conversions** for XML values (Int, Long, Double, Boolean)
- **Nullable handling** for optional XML elements
- **Flow-based API** for asynchronous and streaming processing
- **Minimal dependencies** (only Kotlin stdlib, Coroutines, and StAX)
- **Extensible design** for custom handlers and processors

## Installation

### Gradle (Kotlin DSL)

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("com.github.asm0dey.kxml-parser:kxml-parser:1.0-SNAPSHOT")
}
```

### Maven

```xml
<dependency>
    <groupId>com.github.asm0dey.kxml-parser</groupId>
    <artifactId>kxml-parser</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

## Basic Usage

### Flow-based API

```kotlin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import java.io.File

// Using a String directly
val xmlString = """
    <library>
        <book id="1">
            <title>Kotlin in Action</title>
            <year>2017</year>
        </book>
        <book id="2">
            <title>Effective Kotlin</title>
            <year>2020</year>
        </book>
    </library>
""".trimIndent()

data class Book(val id: Int, val title: String, val year: Int)

val books = runBlocking {
    staks(xmlString) {
        collectElements("book") {
            val id = collectAttribute("book", "id").first().toInt()
            val title = collectText("title").first()
            val year = collectText("year").first().toInt()
            Book(id, title, year)
        }.toList()
    }
}

// books is now a List<Book> with two entries

// You can also use an InputStream
val xmlInputStream = xmlString.byteInputStream()
val booksFromStream = runBlocking {
    staks(xmlInputStream) {
        // Same parsing logic as above
        collectElements("book") {
            val id = collectAttribute("book", "id").first().toInt()
            val title = collectText("title").first()
            val year = collectText("year").first().toInt()
            Book(id, title, year)
        }.toList()
    }
}

// Or parse from a File
val xmlFile = File("books.xml") // Assuming this file exists
val booksFromFile = runBlocking {
    staks(xmlFile) {
        // Same parsing logic as above
        collectElements("book") {
            val id = collectAttribute("book", "id").first().toInt()
            val title = collectText("title").first()
            val year = collectText("year").first().toInt()
            Book(id, title, year)
        }.toList()
    }
}
```

## Advanced Usage

### Accessing Root Element Data

The library provides functions to access data from the root element:

```kotlin
import com.github.asm0dey.kxml.rootName
import com.github.asm0dey.kxml.rootAttribute
import com.github.asm0dey.kxml.rootText
import com.github.asm0dey.kxml.staks
import kotlinx.coroutines.runBlocking

val xml = """
    <library version="1.0" count="2">
        Library content
        <book>Book 1</book>
        <book>Book 2</book>
    </library>
""".trimIndent()

val result = runBlocking {
    staks(xml) {
        // Get the root element name
        val rootElementName = rootName() // rootName() is a suspend function

        // Get attributes from the root element
        val version = rootAttribute("version").string()
        val count = rootAttribute("count").int()

        // Get text content from the root element
        val rootText = rootText().string()

        Triple(rootElementName, version, "Count: $count, Text: $rootText")
    }
}

// result = Triple("library", "1.0", "Count: 2, Text: Library content")
```

### Handling Complex Nested Structures

```kotlin
val xml = """
    <products>
        <product id="1" available="true">
            <name>Product 1</name>
            <price>99.99</price>
            <stock>100</stock>
            <categories>
                <category id="1">Electronics</category>
                <category id="2">Computers</category>
            </categories>
        </product>
    </products>
""".trimIndent()

data class Category(val id: Int, val name: String)
data class Product(val id: Int, val name: String, val price: Double, 
                  val stock: Int, val available: Boolean, 
                  val categories: List<Category>)

val products = staks(xml) {
    list("product") {
        val id = attribute("product", "id").int()
        val available = attribute("product", "available").boolean()
        val name = tagValue("name").string()
        val price = tagValue("price").double()
        val stock = tagValue("stock").int()
        val categories = list("category") {
            val categoryId = attribute("category", "id").int()
            val categoryName = tagValue("category").string()
            Category(categoryId, categoryName)
        }
        Product(id, name, price, stock, available, categories)
    }
}

// Using flow instead of list
val productsFlow = staks(xml) {
    flow("product") {
        val id = attribute("product", "id").int()
        val available = attribute("product", "available").boolean()
        val name = tagValue("name").string()
        val price = tagValue("price").double()
        val stock = tagValue("stock").int()
        val categories = list("category") {
            val categoryId = attribute("category", "id").int()
            val categoryName = tagValue("category").string()
            Category(categoryId, categoryName)
        }
        Product(id, name, price, stock, available, categories)
    }
    // The result is a Flow<Product> that can be processed asynchronously
}
```

### Handling Optional Elements

```kotlin
val xml = "<root><item>value</item></root>"

val existingTag = staks(xml) {
    tagValue("item").nullable().string()
}

val nonExistingTag = staks(xml) {
    tagValue("non-existing").nullable().string()
}

// existingTag = "value"
// nonExistingTag = null
```

### Working with Current Element Context

The library provides functions to work directly with the current element context, which is especially useful when parsing lists of elements:

```kotlin
val xml = """
    <root>
        <item>1</item>
        <item id="2">2</item>
        <item id="3" active="true">3</item>
    </root>
""".trimIndent()

data class Item(val id: Int?, val value: Int, val active: Boolean?)

val items = staks(xml) {
    list("item") {
        val id = attribute("id").nullable().int()
        val value = text().int()
        val active = attribute("active").nullable().boolean()
        Item(id, value, active)
    }
}

// items = [
//   Item(id=null, value=1, active=null),
//   Item(id=2, value=2, active=null),
//   Item(id=3, value=3, active=true)
// ]
```

This approach is more concise than specifying the element name for each attribute or text value, especially when working with nested structures.

### Using the Unary Plus Operator

The library provides a convenient unary plus operator (`+`) as a shorthand for `.value()`:

```kotlin
val name: String = +tagValue("name")  // Same as tagValue("name").value()
```

### Working with Namespaces

The library provides comprehensive support for XML namespaces. You can work with namespaces in several ways:

#### Using Namespace Prefixes

You can directly use namespace prefixes in element and attribute names:

```kotlin
val xml = """
    <root xmlns:ns1="http://example.com/ns1">
        <ns1:element>Value</ns1:element>
    </root>
""".trimIndent()

val value = staks(xml) {
    tagValue("ns1:element").string()
}
// value = "Value"
```

#### Using Namespace URIs

You can also use namespace URIs directly, which is useful when the prefix in the XML document might change:

```kotlin
val xml = """
    <root xmlns:ns1="http://example.com/ns1">
        <ns1:element>Value</ns1:element>
    </root>
""".trimIndent()

val value = staks(xml) {
    tagText("element", "http://example.com/ns1").string()
}
// value = "Value"
```

#### Using a Namespace Map

For more complex scenarios, you can define a map of namespaces at the beginning of the staks block:

```kotlin
val xml = """
    <root xmlns:ns1="http://example.com/ns1">
        <ns1:element>Value</ns1:element>
    </root>
""".trimIndent()

val value = staks(xml) {
    // Define namespaces in a map at the beginning of the staks block
    namespaces = mapOf("myns" to "http://example.com/ns1")

    // Use the namespace in a query
    tagText("element", namespaces["myns"]).string()
}
// value = "Value"
```

This approach is particularly useful when working with XML documents where the prefix might change but the namespace URI remains the same.

#### Getting Namespace Information

You can also get information about namespaces declared in the XML document:

```kotlin
val xml = """
    <root xmlns:ns1="http://example.com/ns1" xmlns:ns2="http://example.com/ns2">
        <ns1:element>Value 1</ns1:element>
        <ns2:element>Value 2</ns2:element>
    </root>
""".trimIndent()

val namespaces = staks(xml) {
    getNamespaces()
}
// namespaces = {"ns1" to "http://example.com/ns1", "ns2" to "http://example.com/ns2"}

val uri = staks(xml) {
    resolveNamespace("ns1")
}
// uri = "http://example.com/ns1"
```

### Handling CDATA Sections

The library handles CDATA sections transparently, treating them as regular text content. This is useful for parsing XML with embedded HTML, JavaScript, or other content that might contain characters that would normally need to be escaped in XML.

```kotlin
val xml = """
    <root>
        <item><![CDATA[<tag>This & that</tag>]]></item>
    </root>
""".trimIndent()

val content = staks(xml) {
    tagValue("item").string()
}
// content = "<tag>This & that</tag>"
```

CDATA sections can be used in any text content, including mixed content:

```kotlin
val xml = """
    <root>
        <item>Regular text <![CDATA[<CDATA text>]]> more regular text</item>
    </root>
""".trimIndent()

val content = staks(xml) {
    tagValue("item").string()
}
// content = "Regular text <CDATA text> more regular text"
```

Multiple CDATA sections are concatenated:

```kotlin
val xml = """
    <root>
        <item><![CDATA[First]]><![CDATA[Second]]></item>
    </root>
""".trimIndent()

val content = staks(xml) {
    tagValue("item").string()
}
// content = "FirstSecond"
```

## Best Practices

### Error Handling

When parsing XML, it's important to handle potential errors gracefully. Here are some best practices for error handling:

#### Using Nullable Results

For optional elements or attributes, use the `nullable()` method to avoid NullPointerExceptions:

```kotlin
val optionalValue = staks(xml) {
    tagValue("optional-element").nullable().string()
}
// optionalValue will be null if the element doesn't exist
```

#### Handling Parsing Exceptions

The library may throw exceptions in certain cases, such as when converting values to incorrect types. Always wrap your parsing code in try-catch blocks when dealing with untrusted XML:

```kotlin
try {
    val number = staks(xml) {
        tagValue("number").int()
    }
} catch (e: NumberFormatException) {
    // Handle the case where the value is not a valid integer
} catch (e: Exception) {
    // Handle other exceptions
}
```

#### Validating XML Structure

For complex XML structures, consider validating the structure before parsing:

```kotlin
val isValid = staks(xml) {
    // Check if required elements exist
    val hasRequiredElements = tagValue("required-element").nullable().string() != null

    // Check if values are in expected format
    val isValidFormat = try {
        tagValue("number").int()
        true
    } catch (e: Exception) {
        false
    }

    hasRequiredElements && isValidFormat
}

if (isValid) {
    // Proceed with parsing
} else {
    // Handle invalid XML
}
```

### Performance Considerations

The library is designed to be efficient, but there are some best practices to ensure optimal performance:

#### Use Streaming for Large Files

For large XML files, use the Flow-based API to process elements as they are parsed, rather than loading the entire document into memory:

```kotlin
val result = staks(largeXmlFile) {
    flow("item") {
        // Process each item as it's parsed
        processItem(tagValue("name").string())
    }
}
```

#### Reuse Parsing Logic

For repeated parsing tasks, define reusable extension functions:

```kotlin
// Define a suspend extension function for parsing books
suspend fun Flow<XmlEvent>.parseBook(): Book {
  val title = tagValue("title").string()
  val author = tagValue("author").string()
  val year = tagValue("year").int()
  return Book(title, author, year)
}

// Usage in a coroutine context
val books = runBlocking {
  staks(xml) {
    list("book") {
      // The list function provides a coroutine context for its lambda
      parseBook()
    }
  }
}
```

#### Minimize Namespace Lookups

When working with namespaces, define a namespace map at the beginning of the staks block to minimize repeated lookups:

```kotlin
val result = staks(xml) {
    namespaces = mapOf(
        "ns1" to "http://example.com/ns1",
        "ns2" to "http://example.com/ns2"
    )

    // Use namespaces["ns1"] instead of resolveNamespace("ns1") in multiple places
    list("element", namespaces["ns1"]) {
        // ...
    }
}
```

## Extending the Library

The library is designed to be extensible. You can create your own handlers and processors on top of the existing primitives.

### Creating Custom Value Processors

You can extend the `ValueResult` interface to create custom value processors:

```kotlin
// Create a custom processor for dates
fun ValueResult.date(pattern: String = "yyyy-MM-dd"): LocalDate {
    val formatter = DateTimeFormatter.ofPattern(pattern)
    return LocalDate.parse(value(), formatter)
}

// Usage
val publishDate = staks(xml) {
    tagValue("publishDate").date()
}
```

### Creating Custom Element Collectors

You can create custom element collectors for specific XML structures:

```kotlin
// Custom collector for address elements
fun Flow<XmlEvent>.collectAddress(): Flow<Address> = flow {
    collectElements("address") {
        val street = collectText("street").first()
        val city = collectText("city").first()
        val zipCode = collectText("zipCode").first()
        emit(Address(street, city, zipCode))
    }
}

// Usage
val addresses = staks(xml) {
    collectAddress().toList()
}
```

### Creating Domain-Specific Extensions

You can create domain-specific extensions for your particular XML format:

```kotlin
// Extension for RSS feeds
fun Flow<XmlEvent>.collectRssItems(): Flow<RssItem> = flow {
    collectElements("item") {
        val title = collectText("title").first()
        val link = collectText("link").first()
        val description = collectText("description").firstOrNull()
        emit(RssItem(title, link, description))
    }
}

// Usage
val rssItems = staks(rssXml) {
    collectRssItems().toList()
}
```

## API Reference

### Core Functions

- `staks(input: InputStream): Flow<XmlEvent>` - Creates a Flow of XML events from an input stream
- `staks(input: InputStream, block: suspend Flow<XmlEvent>.() -> T): T` - Main entry point for the DSL with input stream
- `staks(input: String): Flow<XmlEvent>` - Creates a Flow of XML events from a string
- `staks(input: String, block: suspend Flow<XmlEvent>.() -> T): T` - Main entry point for the DSL with string input
- `staks(input: File): Flow<XmlEvent>` - Creates a Flow of XML events from a file
- `staks(input: File, block: suspend Flow<XmlEvent>.() -> T): T` - Main entry point for the DSL with file input

### Flow Extensions

- `Flow<XmlEvent>.collectText(elementName: String): Flow<String>` - Collects text content of a specific element
- `Flow<XmlEvent>.collectCurrentText(): Flow<String>` - Collects text content of the current element
- `Flow<XmlEvent>.collectAttribute(elementName: String, attributeName: String): Flow<String>` - Collects attributes of a specific element
- `Flow<XmlEvent>.collectCurrentAttribute(attributeName: String): Flow<String>` - Collects an attribute of the current element
- `Flow<XmlEvent>.collectElements(elementName: String, transform: suspend Flow<XmlEvent>.() -> T): Flow<T>` - Collects and transforms elements

### DSL Extensions

- `tagValue(tagName: String): TagValueResult` - Gets a tag value
- `text(): TagValueResult` - Gets the text content of the current element
- `attribute(tagName: String, attributeName: String): AttributeResult` - Gets an attribute value
- `attribute(attributeName: String): AttributeResult` - Gets an attribute value from the current element
- `list(tagName: String, block: suspend Flow<XmlEvent>.() -> T): List<T>` - Parses a list of elements
- `flow(tagName: String, block: suspend Flow<XmlEvent>.() -> T): Flow<T>` - Parses a flow of elements
- `nullable(): NullableValueResult<T>` - Makes a value nullable
- `rootName(): String?` - Gets the name of the root element
- `rootAttribute(attributeName: String): AttributeResult` - Gets an attribute value from the root element
- `rootText(): TagValueResult` - Gets the text content of the root element

### Type Conversions

- `.int()` - Converts to Int
- `.long()` - Converts to Long
- `.double()` - Converts to Double
- `.boolean()` - Converts to Boolean
- `.string()` - Gets the string value
- `.value()` - Gets the raw string value
- `+result` - Shorthand for `result.value()`

## Contributing

Contributions are welcome! Here's how you can contribute:

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Commit your changes: `git commit -am 'Add my feature'`
4. Push to the branch: `git push origin feature/my-feature`
5. Submit a pull request

Please make sure to update tests as appropriate.

## License

This project is licensed under the MIT License - see the LICENSE file for details.

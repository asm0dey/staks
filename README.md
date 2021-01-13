# staks [![Maven Central](https://img.shields.io/maven-central/v/com.github.asm0dey/staks.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.github.asm0dey%22%20AND%20a:%22staks%22)
Kotlin wrapper for StAX

How many times did you try to remember how to use StAX or SAX? How many times did you need to parse more or less complex data structures from XML using StAX API? It is a pain, isn't it?

This project aims to solve this problem in idiomatic way for Kotlin.

## Why should I use it?

You should use it in several cases:

1. You don't want to build the complex parser based on SAX/StAX yourself
2. DOM is too heavy for your goals
3. Your XML document may be not well-formed and the DOM parser will fail on parsing.

## Usage

The core of library is `staks` function, lying in package `staks`. For example imagine we need to gather all texts of `a` tag in XML document. Code will looks like this:

```kotlin
val result = staks<List<String>>("root-tag"){ // ①
  val texts = list(tagText("a")); // ②
  { texts() } // ③
}
```

What we can see here:
1. We initialize staks function, defining return type (Yes, to a pity we should do it for now).
2. We are creating a list of texts of contents of the `a` tag. Please, note the semicolon on this line. We need this because the next line starts with `{`, without semicolon Kotlin will decide that it's a call of the function.
3. We're defining closure, building return type. Please, note that on this line we are *calling* `texts` variable. That's because all `Handler`s are functions which eventually return parsed value or will throw an exception.

But obviously, this is not enough to build a list of strings, in real life we usually need to build more or less complex structures. Let's look at more complex examples.

```kotlin
        val inp =
            "<root><el><a>a1</a><b>b1</b><c>c1</c></el><el><a>a2</a><b>b2</b><c>c2</c></el><el><a>a3</a><b>b3</b><c>c3</c></el></root>".byteInputStream()
        val lst = staks<List<Triple<String, String, String>>>(inp) {
            val lst = list("el") {
                val a = tagText("a")
                val b = tagText("b")
                val c = tagText("c");
                { Triple(a(), b(), c()) }
            };
            { lst() }
        }
```

Here we're building `Triples` of `a`, `b` and `c` subtags of `el` subtag. Note that these elements may be in any order and at any depth inside the `el` tag as well as the `el` tag may be located at any depth inside the root of XML.
BTW, you may operate on root tags too!

## How to not parse the whole document?

Actually, there are to types of handlers in staks — singular and plural. The only plural handlers OOTB are list handlers. If you have no list builders inside your code — it will stop parsing as soon as all the data will be found.

But what if you have a list in the first part of the document inside a certain tag and don't want to process the document after this tag?

Easy! You need to define a tag inside which you will look for data and we have `single for that`.

For example:

```kotlin
val lst = staks<List<Pair<Int, String>>>(input){
  val data = single("header"){
    val list = list("person"){
      val name = tagText("name")
      val age = tagText("age").int();
      { age() to name() }
    };
    { list() };
  };
  { data() }
}
```

Here we will look for `person` tags only inside the `header` tag. As soon as we meet the closing `header` tag, processing will be stopped and we'll obtain a list of persons inside `lst` variable.

One more thing to meet look at is `.int()` call. It's delegation to the handler, which just converts String to int, its code is very simple and may be found here: https://github.com/asm0dey/staks/blob/f8fd73e1482e838dfa8c17781226407ba49b7fde/src/main/kotlin/staks/delegates.kt#L9-L17

Also, there is `.optional()` delegate which allows to return null from standard handlers (of course you can write your own, allowing null by default).

Further usage examples may be found in [tests](https://github.com/asm0dey/staks/blob/main/src/test/kotlin/staks/BaseKtTest.kt)

## Writing own handlers

A handler is just a class, which implements [`Handler`](https://github.com/asm0dey/staks/blob/main/src/main/kotlin/staks/Handler.kt) interface.

To make parent handlers aware of child ones we should call `register` or decorate on children. For example `list` from first exampe decorates underlying handler: https://github.com/asm0dey/staks/blob/f8fd73e1482e838dfa8c17781226407ba49b7fde/src/main/kotlin/staks/base.kt#L196, and `tagText` https://github.com/asm0dey/staks/blob/f8fd73e1482e838dfa8c17781226407ba49b7fde/src/main/kotlin/staks/base.kt#L166-L167 registers new one in parent.

Both of these methods are available to extensions of `CompountHandler` interface.

Be advised to read docs of all `Hadler` methods.


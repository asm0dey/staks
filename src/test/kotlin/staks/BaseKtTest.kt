package staks

import ch.tutteli.atrium.api.infix.en_GB.*
import ch.tutteli.atrium.api.verbs.expect
import io.kotest.core.spec.style.FunSpec

class BaseKtTest : FunSpec({
    test("should parse basic xml") {
        val input =
            ("<note> <to>Tove</to> <from>Jani</from> <heading>Reminder</heading>" +
                    "<body>Don't forget me this weekend!</body></note>").byteInputStream()

        data class Note(
            val to: String,
            val from: String,
            val heading: String,
            val body: String
        )

        val note = staks<Note>(input) {
            val note = single("note") {
                val to = tagText("to")
                val from = tagText("from")
                val heading = tagText("heading")
                val body = tagText("body");
                { Note(to(), from(), heading(), body()) }
            };
            { note() }
        }
        expect(note) toEqual Note("Tove", "Jani", "Reminder", "Don't forget me this weekend!")

    }

    test("should parse multiple similar tags if isSingular is false") {
        val input = """<root> <a>one</a> <a>two</a> <a>three</a> <a>four</a> </root>""".byteInputStream()
        val list = staks<List<String>>(input) {
            val list = list(tagText("a"));
            { list() }
        }

        expect(list) toContain o inGiven order and only the values("one", "two", "three", "four")
    }

    test("should parse attributes") {
        val input = "<root><data attr='text'>inside</data></root>".byteInputStream()
        val attr = staks<String>(input) {
            val txt = attribute("data", "attr");
            { txt() }
        }
        expect(attr) toEqual "text"
    }
    test("should parse pair of attributes") {
        val input = "<root><data attr='text' attr2='text2'>inside2</data></root>".byteInputStream()
        val attr = staks<Pair<String, String>>(input) {
            val txt = attribute("data", "attr");
            val txt2 = attribute("data", "attr2");
            { txt() to txt2() }
        }
        expect(attr) toEqual ("text" to "text2")
    }
    test("should parse list of attributes") {
        val input =
            ("<root><data attr='text' attr2='text2'>inside2</data><data attr='text3' attr2='text4'>inside2</data>" +
                    "</root>").byteInputStream()
        val attr = staks<List<String>>(input) {
            val lst = list(attribute("data", "attr"));
            { lst() }
        }
        expect(attr) toContain o inGiven order and only the values("text", "text3")
    }

    test("handler calls to StaxBuilder should fail") {
        staks<Unit>("<root/>".byteInputStream()) {
            val ev = reader.nextEvent()
            val errorMessage = "Root builder should not process anything!"
            expect { isSingular }.toThrow<IllegalStateException>().message toEqual errorMessage
            expect { matches(ev) }.toThrow<IllegalStateException>().message toEqual errorMessage
            expect { process(ev) }.toThrow<IllegalStateException>().message toEqual errorMessage
            expect { reset() }.toThrow<IllegalStateException>().message toEqual errorMessage
            expect { value() }.toThrow<IllegalStateException>().message toEqual errorMessage
            ;
            {}
        }

    }
    test("compound list should produce list of compound objects") {
        val inp =
            ("<root><el><a>a1</a><b>b1</b><c>c1</c></el><el><a>a2</a><b>b2</b><c>c2</c></el><el><a>a3</a><b>b3</b>" +
                    "<c>c3</c></el></root>").byteInputStream()
        val lst = staks<List<Triple<String, String, String>>>(inp) {
            {
                list("el") {
                    { Triple(tagText("a")(), tagText("b")(), tagText("c")()) }
                }()
            }
        }
        expect(lst) toContain o inGiven order and only the entries(
            {
                it.feature { f(it::first) } toEqual "a1"
                it.feature { f(it::second) } toEqual "b1"
                it.feature { f(it::third) } toEqual "c1"
            },
            {
                it.feature { f(it::first) } toEqual "a2"
                it.feature { f(it::second) } toEqual "b2"
                it.feature { f(it::third) } toEqual "c2"
            },
            {
                it.feature { f(it::first) } toEqual "a3"
                it.feature { f(it::second) } toEqual "b3"
                it.feature { f(it::third) } toEqual "c3"
            },
        )
    }
    test("optional should return null of object is not found") {
        val inp = ("<root><el><a>a1</a><b>b1</b><d>c1</d></el><el><a>a2</a><b>b2</b><c>c2</c></el><el><a>a3</a>" +
                "<b>b3</b><c>c3</c></el></root>").byteInputStream()
        val lst = staks<List<String?>>(inp) {
            val lst = list("el") {
                val c = tagText("d").optional();
                { c() }
            };
            { lst() }
        }
        expect(lst) toContain o inGiven order and only the values("c1", null, null)

    }
    test("int should return int if can convert") {
        val inp = ("<root><el><a>a1</a><b>b1</b><c>1</c></el><el><a>a2</a><b>b2</b><c>2</c></el><el><a>a3</a>" +
                "<b>b3</b><c>3</c></el></root>").byteInputStream()
        val lst = staks<List<Int>>(inp) {
            val lst = list("el") {
                val c = tagText("c").int();
                { c() }
            };
            { lst() }
        }
        expect(lst) toContain o inGiven order and only the values(1, 2, 3)

    }
    test("error while building list should fail build of list") {
        val inp = ("<root><el><a>a1</a><b>b1</b><c>a</c></el><el><a>a2</a><b>b2</b><c>2</c></el>" +
                "<el><a>a3</a><b>b3</b><c>3</c></el></root>").byteInputStream()
        expect {
            staks<List<Int>>(inp) {
                val lst = list("el") {
                    val c = tagText("c").int();
                    { c() }
                };
                { lst() }
            }
        }.toThrow<IllegalArgumentException>().message toEqual "Unable to build info from el"
    }

    test("compoud list of simple lists") {
        val input =
            ("<root><el><a>x1</a><a>x2</a></el><el><a>x3</a><a>x4</a></el><el><a>x5</a><a>x6</a></el></root>")
                .byteInputStream()
        val list = staks<List<List<String>>>(input) {
            val higher = list("el") {
                val inner = list(tagText("a"));
                { inner() }
            };
            { higher() }
        }
        expect(list.flatten()) toContain o inGiven order and only the values("x1", "x2", "x3", "x4", "x5", "x6")
    }
    test("tag text returns only first value") {
        val input = "<root><a>test1</a><a>test2</a></root>".byteInputStream()
        val tst = staks<String>(input) {
            val txt = tagText("a");
            { txt() }
        }
        expect(tst) toEqual "test1"
    }
    test("list inside singe inside list") {
        val input = """<root>
    <list-one>
        <single>
            <list-two>
                <a>a1</a>
                <b>b1</b>
            </list-two>
            <list-two>
                <a>a2</a>
                <b>b2</b>
            </list-two>
        </single>
    </list-one>
    <list-one>
        <single>
            <list-two>
                <a>a3</a>
                <b>b3</b>
            </list-two>
            <list-two>
                <a>a4</a>
                <b>b4</b>
            </list-two>
        </single>
    </list-one>
</root>""".byteInputStream()
        val d = staks<List<List<Pair<String, String>>>>(input) {
            val lSLP = list("list-one") {
                val sLP = single("single") {
                    val lP = list("list-two") {
                        val a = tagText("a")
                        val b = tagText("b");
                        { a() to b() }
                    };
                    { lP() }
                };
                { sLP() }
            };
            { lSLP() }
        }
        expect(d.flatten()) toContain o inGiven order and only the values(
            "a1" to "b1",
            "a2" to "b2",
            "a3" to "b3",
            "a4" to "b4",
        )
    }
    test("single wil be read only once") {
        val input =
            "<root><single><a>a1</a><b>b1</b></single><single><a>a2</a><b>b2</b></single></root>".byteInputStream()
        val d = staks<Pair<String, String>>(input) {
            val sSP = single("root") {
                val sP = single("single") {
                    val a = tagText("a")
                    val b = tagText("b");
                    { a() to b() }
                };
                { sP() }
            };
            { sSP() }
        }
        expect(d) toEqual ("a1" to "b1")
    }

})

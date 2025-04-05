package com.github.asm0dey.kxml

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests for the ValueResult implementations and their methods.
 * This test class focuses on corner cases and methods that weren't covered by other tests.
 */
class ValueResultTest {

    @Test
    fun `test AttributeResult unaryPlus operator`() = runBlocking {
        val xml = "<root><item attr='value'>content</item></root>"
        
        val attrValue = staks(xml.byteInputStream()) {
            val result = attribute("item", "attr")
            +result // Using unaryPlus operator
        }
        
        assertEquals("value", attrValue)
    }
    
    @Test
    fun `test AttributeResult string method`() = runBlocking {
        val xml = "<root><item attr='value'>content</item></root>"
        
        val attrValue = staks(xml.byteInputStream()) {
            attribute("item", "attr").string()
        }
        
        assertEquals("value", attrValue)
    }
    
    @Test
    fun `test AttributeResult value method`() = runBlocking {
        val xml = "<root><item attr='value'>content</item></root>"
        
        val attrValue = staks(xml.byteInputStream()) {
            attribute("item", "attr").value()
        }
        
        assertEquals("value", attrValue)
    }
    
    @Test
    fun `test AttributeResult toString method`() {
        val attrResult = AttributeResult("value")
        assertEquals("value", attrResult.toString())
        
        val nullAttrResult = AttributeResult(null)
        assertEquals("null", nullAttrResult.toString())
    }
    
    @Test
    fun `test TagValueResult unaryPlus operator`() = runBlocking {
        val xml = "<root><item>value</item></root>"
        
        val tagValue = staks(xml.byteInputStream()) {
            val result = tagValue("item")
            +result // Using unaryPlus operator
        }
        
        assertEquals("value", tagValue)
    }
    
    @Test
    fun `test TagValueResult toString method`() {
        val tagResult = TagValueResult("value")
        assertEquals("value", tagResult.toString())
        
        val nullTagResult = TagValueResult(null)
        assertEquals("null", nullTagResult.toString())
    }
    
    @Test
    fun `test NullableValueResult unaryPlus operator`() = runBlocking {
        val xml = "<root><item attr='value'>content</item><empty></empty></root>"
        
        val existingAttr = staks(xml.byteInputStream()) {
            val result = attribute("item", "attr").nullable()
            +result // Using unaryPlus operator
        }
        
        val nonExistingAttr = staks(xml.byteInputStream()) {
            val result = attribute("item", "non-existing").nullable()
            +result // Using unaryPlus operator
        }
        
        assertEquals("value", existingAttr)
        assertNull(nonExistingAttr)
    }
    
    @Test
    fun `test NullableValueResult toString method`() {
        val attrResult = AttributeResult("value")
        val nullableResult = NullableValueResult(attrResult)
        assertEquals(attrResult.toString(), nullableResult.toString())
        
        val nullResult = NullableValueResult<AttributeResult>(null)
        assertEquals("null", nullResult.toString())
    }
    
    @Test
    fun `test NullableValueResult double method with null value`() = runBlocking {
        val xml = "<root><item>content</item></root>"
        
        val nonExistingAttr = staks(xml.byteInputStream()) {
            attribute("item", "non-existing").nullable().double()
        }
        
        assertNull(nonExistingAttr)
    }
    
    @Test
    fun `test NullableValueResult boolean method with null value`() = runBlocking {
        val xml = "<root><item>content</item></root>"
        
        val nonExistingAttr = staks(xml.byteInputStream()) {
            attribute("item", "non-existing").nullable().boolean()
        }
        
        assertNull(nonExistingAttr)
    }
    
    @Test
    fun `test NullableValueResult long method with null value`() = runBlocking {
        val xml = "<root><item>content</item></root>"
        
        val nonExistingAttr = staks(xml.byteInputStream()) {
            attribute("item", "non-existing").nullable().long()
        }
        
        assertNull(nonExistingAttr)
    }
    
    @Test
    fun `test NullableValueResult with valid values`() = runBlocking {
        val xml = "<root><item num='123' flag='true' decimal='123.45'>content</item></root>"
        
        val intValue = staks(xml.byteInputStream()) {
            attribute("item", "num").nullable().int()
        }
        
        val boolValue = staks(xml.byteInputStream()) {
            attribute("item", "flag").nullable().boolean()
        }
        
        val longValue = staks(xml.byteInputStream()) {
            attribute("item", "num").nullable().long()
        }
        
        val doubleValue = staks(xml.byteInputStream()) {
            attribute("item", "decimal").nullable().double()
        }
        
        assertEquals(123, intValue)
        assertEquals(true, boolValue)
        assertEquals(123L, longValue)
        assertEquals(123.45, doubleValue)
    }
}
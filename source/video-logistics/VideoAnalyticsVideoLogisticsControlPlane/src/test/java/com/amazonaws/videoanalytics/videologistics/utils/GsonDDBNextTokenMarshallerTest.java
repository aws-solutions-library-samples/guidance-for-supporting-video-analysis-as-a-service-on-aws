package com.amazonaws.videoanalytics.videologistics.utils;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class GsonDDBNextTokenMarshallerTest {
    
    @Test
    void marshall_nullInput_returnsNull() {
        assertNull(GsonDDBNextTokenMarshaller.marshall(null));
    }
    
    @Test
    void marshall_emptyMap_returnsEncodedEmptyMap() {
        Map<String, AttributeValue> emptyMap = new HashMap<>();
        String result = GsonDDBNextTokenMarshaller.marshall(emptyMap);
        assertEquals("{}", new String(org.apache.commons.codec.binary.Base64.decodeBase64(result)));
    }
    
    @Test
    void marshall_stringAttribute_returnsEncodedJson() {
        Map<String, AttributeValue> nextToken = new HashMap<>();
        nextToken.put("key", AttributeValue.fromS("value"));
        String result = new String(org.apache.commons.codec.binary.Base64.decodeBase64(GsonDDBNextTokenMarshaller.marshall(nextToken)));
        assertEquals("{\"key\":{\"s\":\"value\",\"ss\":[],\"ns\":[],\"bs\":[],\"m\":{},\"l\":[],\"type\":\"S\"}}", result);
    }
    
    @Test
    void marshall_numberAttribute_returnsEncodedJson() {
        Map<String, AttributeValue> nextToken = new HashMap<>();
        nextToken.put("key", AttributeValue.fromN("123"));
        String result = new String(org.apache.commons.codec.binary.Base64.decodeBase64(GsonDDBNextTokenMarshaller.marshall(nextToken)));
        assertEquals("{\"key\":{\"n\":\"123\",\"ss\":[],\"ns\":[],\"bs\":[],\"m\":{},\"l\":[],\"type\":\"N\"}}", result);
    }
    
    @Test
    void unmarshall_nullInput_returnsNull() {
        assertNull(GsonDDBNextTokenMarshaller.unmarshall(null));
    }
    
    @Test
    void unmarshall_validStringAttribute_returnsMap() {
        Map<String, AttributeValue> expected = new HashMap<>();
        expected.put("key", AttributeValue.fromS("value"));
        
        String encoded = GsonDDBNextTokenMarshaller.marshall(expected);
        Map<String, AttributeValue> result = GsonDDBNextTokenMarshaller.unmarshall(encoded);
        
        assertEquals(expected, result);
    }
    
    @Test
    void unmarshall_validNumberAttribute_returnsMap() {
        Map<String, AttributeValue> expected = new HashMap<>();
        expected.put("key", AttributeValue.fromN("123"));
        
        String encoded = GsonDDBNextTokenMarshaller.marshall(expected);
        Map<String, AttributeValue> result = GsonDDBNextTokenMarshaller.unmarshall(encoded);
        
        assertEquals(expected, result);
    }
    
    @Test
    void unmarshall_unsupportedAttributeType_throwsRuntimeException() {
        String invalidJson = "{\"key\":{\"binary\":\"value\"}}";
        String encoded = org.apache.commons.codec.binary.Base64.encodeBase64String(invalidJson.getBytes());
        
        assertThrows(RuntimeException.class, () -> GsonDDBNextTokenMarshaller.unmarshall(encoded));
    }
}
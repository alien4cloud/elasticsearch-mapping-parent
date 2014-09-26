package org.elasticsearch.mapping;

import java.beans.IntrospectionException;
import java.io.IOException;

import junit.framework.Assert;

import org.elasticsearch.mapping.model.City;
import org.elasticsearch.mapping.model.Person;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;

/**
 * Test the mappings.
 * 
 * @author luc boutier
 */
public class MappingBuilderTest {

    @Test
    public void testSimpleClassMapping() throws IntrospectionException, JsonGenerationException, JsonMappingException,
            IOException {
        MappingBuilder mappingBuilder = new MappingBuilder();
        mappingBuilder.initialize("org.elasticsearch.mapping.model");
        String personMapping = mappingBuilder.getMapping(Person.class);
        Assert.assertEquals(
                "{\"person\":{\"_type\":{\"index\":\"not_analyzed\",\"store\":false},\"_id\":{\"index\":\"no\",\"store\":false,\"path\":\"id\"},\"_source\":{\"enabled\":true},\"properties\":{\"lastname\":{\"include_in_all\":true,\"index\":\"analyzed\",\"store\":false,\"boost\":1.0,\"term_vector\":\"no\",\"type\":\"string\"},\"firstname\":{\"include_in_all\":false,\"index\":\"no\",\"store\":false,\"boost\":1.0,\"term_vector\":\"no\",\"type\":\"string\"}},\"_all\":{\"enabled\":true}}}",
                personMapping);
    }
    
    @Test
    public void ttlTest() throws IntrospectionException, JsonGenerationException, JsonMappingException,
            IOException {
        MappingBuilder mappingBuilder = new MappingBuilder();
        mappingBuilder.initialize("org.elasticsearch.mapping.model");
        String personMapping = mappingBuilder.getMapping(City.class);
        Assert.assertEquals(
                "{\"city\":{\"_type\":{\"index\":\"not_analyzed\",\"store\":false},\"_id\":{\"index\":\"no\",\"store\":false,\"path\":\"id\"},\"_source\":{\"enabled\":true},\"_ttl\":{\"enabled\":true},\"properties\":{},\"_all\":{\"enabled\":true}}}",
                personMapping);
    }
    
    
}
package org.elasticsearch.mapping;

import java.beans.IntrospectionException;
import java.io.IOException;

import junit.framework.Assert;

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
                "{\"person\":{\"_type\":{\"index\":\"not_analyzed\",\"store\":false},\"_source\":{\"enabled\":true},\"_id\":{\"path\":\"id\",\"index\":\"no\",\"store\":false},\"_all\":{\"analyzer\":\"simple\",\"store\":false,\"enabled\":true},\"properties\":{\"firstname\":{\"include_in_all\":false,\"term_vector\":\"no\",\"index\":\"no\",\"boost\":1.0,\"store\":false,\"type\":\"string\"},\"address\":{\"type\":\"nested\",\"properties\":{\"city\":{\"include_in_all\":false,\"term_vector\":\"no\",\"index\":\"not_analyzed\",\"boost\":1.0,\"store\":false,\"type\":\"string\"}}},\"alienScore\":{\"include_in_all\":false,\"precision_step\":4,\"index\":\"not_analyzed\",\"boost\":1.0,\"store\":false,\"ignore_malformed\":false,\"type\":\"long\"},\"lastname\":{\"include_in_all\":true,\"term_vector\":\"no\",\"index\":\"analyzed\",\"boost\":1.0,\"store\":false,\"type\":\"string\"}}}}",
                personMapping);
    }
}

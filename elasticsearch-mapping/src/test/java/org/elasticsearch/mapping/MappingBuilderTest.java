package org.elasticsearch.mapping;

import java.beans.IntrospectionException;
import java.io.IOException;

import org.elasticsearch.mapping.model.Person;
import org.junit.Ignore;
import org.junit.Test;

import junit.framework.Assert;

/**
 * Test the mappings.
 * 
 * @author luc boutier
 */
public class MappingBuilderTest {

    @Test
    @Ignore("This test is failing because json keys order are not respected")
    public void testSimpleClassMapping() throws IntrospectionException, IOException {
        MappingBuilder mappingBuilder = new MappingBuilder();
        mappingBuilder.initialize("org.elasticsearch.mapping.model");
        String personMapping = mappingBuilder.getMapping(Person.class);
        Assert.assertEquals(
                "{\"person\":{\"_source\":{\"enabled\":true},\"_all\":{\"analyzer\":\"simple\",\"store\":false,\"enabled\":true},\"properties\":{\"firstname\":{\"include_in_all\":false,\"term_vector\":\"no\",\"index\":\"no\",\"boost\":1.0,\"store\":false,\"type\":\"string\"},\"address\":{\"type\":\"nested\",\"properties\":{\"city\":{\"include_in_all\":false,\"term_vector\":\"no\",\"index\":\"not_analyzed\",\"boost\":1.0,\"store\":false,\"type\":\"string\"}}},\"alienScore\":{\"include_in_all\":false,\"precision_step\":4,\"index\":\"not_analyzed\",\"boost\":1.0,\"store\":false,\"ignore_malformed\":false,\"type\":\"long\"},\"lastname\":{\"include_in_all\":true,\"term_vector\":\"no\",\"index\":\"analyzed\",\"boost\":1.0,\"store\":false,\"type\":\"string\"}}}}",
                personMapping);
    }
}

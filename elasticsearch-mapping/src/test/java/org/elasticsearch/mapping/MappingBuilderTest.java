package org.elasticsearch.mapping;

import java.beans.IntrospectionException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

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
    public void testSimpleClassMapping() throws IntrospectionException, JsonGenerationException, JsonMappingException, IOException {
        MappingBuilder mappingBuilder = new MappingBuilder();
        mappingBuilder.initialize("org.elasticsearch.mapping.model");
        String personMapping = mappingBuilder.getMapping(Person.class);
        String expected = Files.readAllLines(Paths.get("src/test/resources/mapping.json")).get(0);
        Assert.assertEquals(expected, personMapping);
    }
}

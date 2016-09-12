package org.elasticsearch.mapping;

import java.beans.IntrospectionException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;

import org.elasticsearch.mapping.model.Person;
import org.junit.Test;

import junit.framework.Assert;

/**
 * Test the mappings.
 * 
 * @author luc boutier
 */
public class MappingBuilderTest {

    @Test
    public void testSimpleClassMapping() throws IntrospectionException, IOException {
        MappingBuilder mappingBuilder = new MappingBuilder();
        mappingBuilder.initialize("org.elasticsearch.mapping.model");
        String personMapping = mappingBuilder.getMapping(Person.class);
        BufferedReader brTest = new BufferedReader(new FileReader(Paths.get("src/test/resources/mapping.json").toFile()));
        String expected = brTest.readLine();
        Assert.assertEquals(expected, personMapping);
    }
}

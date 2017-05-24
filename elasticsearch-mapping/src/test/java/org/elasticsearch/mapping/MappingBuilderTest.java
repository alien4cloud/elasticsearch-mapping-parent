package org.elasticsearch.mapping;

import java.beans.IntrospectionException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;

import org.elasticsearch.mapping.model.City;
import org.elasticsearch.mapping.model.Person;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test the mappings.
 * 
 * @author luc boutier
 */
public class MappingBuilderTest {

    private MappingBuilder mappingBuilder;

    @Before
    public void setUp() throws IntrospectionException, IOException {
        mappingBuilder = new MappingBuilder();
        mappingBuilder.initialize("org.elasticsearch.mapping.model");
    }

    @Test
    public void testSimpleClassMapping() throws IntrospectionException, IOException {
        String personMapping = mappingBuilder.getMapping(Person.class);
        assertSameContent(personMapping, "src/test/resources/person-mapping.json");
    }

    @Test
    public void testSettingsAndMapping() throws IntrospectionException, IOException {
        String citySettings = mappingBuilder.getIndexSettings(City.class);
        assertSameContent(citySettings, "src/test/resources/city-settings.json");

        String cityMapping = mappingBuilder.getMapping(City.class);
        assertSameContent(cityMapping, "src/test/resources/city-mapping.json");
    }

    private void assertSameContent(String content, String expectedContentFromFile) throws IOException {
        BufferedReader brMappingTest = new BufferedReader(new FileReader(Paths.get(expectedContentFromFile).toFile()));
        String expectedMapping = brMappingTest.readLine();
        Assert.assertEquals(expectedMapping, content);
    }

}

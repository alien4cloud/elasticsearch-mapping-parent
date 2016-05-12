package org.elasticsearch.mapping;

import java.net.UnknownHostException;
import java.util.List;

import junit.framework.Assert;

import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.util.AddressParserUtil;
import org.junit.Test;

public class AddressParserUtilTest {

    @Test
    public void testParserHostCsvListNull() throws UnknownHostException {
        List<InetSocketTransportAddress> result = AddressParserUtil.parseHostCsvList(null);
        Assert.assertEquals(0, result.size());
    }

    @Test
    public void testParserHostCsvListEmpty() throws UnknownHostException {
        List<InetSocketTransportAddress> result = AddressParserUtil.parseHostCsvList("");
        Assert.assertEquals(0, result.size());
    }

    @Test
    public void testParserHostCsvListSingle() throws UnknownHostException {
        List<InetSocketTransportAddress> result = AddressParserUtil.parseHostCsvList("192.168.0.1:9200");
        Assert.assertEquals(1, result.size());
        Assert.assertEquals("192.168.0.1", result.get(0).address().getHostString());
        Assert.assertEquals(9200, result.get(0).address().getPort());
    }

    @Test
    public void testParserHostCsvListSingle2() throws UnknownHostException {
        List<InetSocketTransportAddress> result = AddressParserUtil.parseHostCsvList("192.168.0.1:9200,");
        Assert.assertEquals(1, result.size());
        Assert.assertEquals("192.168.0.1", result.get(0).address().getHostString());
        Assert.assertEquals(9200, result.get(0).address().getPort());
    }

    @Test
    public void testParserHostCsvListDual() throws UnknownHostException {
        List<InetSocketTransportAddress> result = AddressParserUtil.parseHostCsvList("192.168.0.1:9200,www.google.com:8888");
        Assert.assertEquals(2, result.size());
        Assert.assertEquals("192.168.0.1", result.get(0).address().getHostString());
        Assert.assertEquals(9200, result.get(0).address().getPort());
        Assert.assertEquals("www.google.com", result.get(1).address().getHostString());
        Assert.assertEquals(8888, result.get(1).address().getPort());
    }

    @Test
    public void testParserHostCsvListTrim() throws UnknownHostException {
        List<InetSocketTransportAddress> result = AddressParserUtil.parseHostCsvList(" 192.168.0.1:9200, www.google.com:8888  ");
        Assert.assertEquals(2, result.size());
        Assert.assertEquals("192.168.0.1", result.get(0).address().getHostString());
        Assert.assertEquals(9200, result.get(0).address().getPort());
        Assert.assertEquals("www.google.com", result.get(1).address().getHostString());
        Assert.assertEquals(8888, result.get(1).address().getPort());
    }

    @Test
    public void testParserHostCsvListDual2() throws UnknownHostException {
        List<InetSocketTransportAddress> result = AddressParserUtil.parseHostCsvList("192.168.0.1:9200,www.google.com:8888,HOST2:PORT");
        Assert.assertEquals(2, result.size());
        Assert.assertEquals("192.168.0.1", result.get(0).address().getHostString());
        Assert.assertEquals(9200, result.get(0).address().getPort());
        Assert.assertEquals("www.google.com", result.get(1).address().getHostString());
        Assert.assertEquals(8888, result.get(1).address().getPort());
    }

    @Test
    public void testParserHostCsvListDual3() throws UnknownHostException {
        List<InetSocketTransportAddress> result = AddressParserUtil.parseHostCsvList("192.168.0.1:9200,whitespace in host:1234,www.google.com:8888");
        Assert.assertEquals(2, result.size());
        Assert.assertEquals("192.168.0.1", result.get(0).address().getHostString());
        Assert.assertEquals(9200, result.get(0).address().getPort());
        Assert.assertEquals("www.google.com", result.get(1).address().getHostString());
        Assert.assertEquals(8888, result.get(1).address().getPort());
    }

}

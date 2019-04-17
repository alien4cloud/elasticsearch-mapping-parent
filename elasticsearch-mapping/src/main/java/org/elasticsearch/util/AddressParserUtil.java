package org.elasticsearch.util;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.elasticsearch.common.transport.TransportAddress;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AddressParserUtil {

    public static List<TransportAddress> parseHostCsvList(String hostCsvList) {
        List<TransportAddress> adresses = new ArrayList<TransportAddress>();
        if (hostCsvList == null) {
            return adresses;
        }
        String[] hostArr = hostCsvList.split(",");
        if (hostArr.length > 0) {
            Pattern pattern = Pattern.compile("(\\S+):(\\d+)");
            for (String hostArrEl : hostArr) {
                Matcher matcher = pattern.matcher(hostArrEl.trim());
                if (matcher.matches()) {
                    String host = matcher.group(1);
                    Integer port = Integer.valueOf(matcher.group(2));
                    TransportAddress address = new TransportAddress(new InetSocketAddress(host, port));
                    adresses.add(address);
                } else {
                    log.warn("Host address not recognized : <" + hostArrEl + ">");
                }
            }
        }
        return adresses;
    }

}

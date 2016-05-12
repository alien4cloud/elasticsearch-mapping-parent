package org.elasticsearch.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.transport.InetSocketTransportAddress;

public class AddressParserUtil {

    private static final ESLogger LOGGER = Loggers.getLogger(AddressParserUtil.class);

    public static List<InetSocketTransportAddress> parseHostCsvList(String hostCsvList) throws UnknownHostException {
        List<InetSocketTransportAddress> adresses = new ArrayList<InetSocketTransportAddress>();
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
                    InetSocketTransportAddress address = new InetSocketTransportAddress(InetAddress.getByName(host), port);
                    adresses.add(address);
                } else {
                    LOGGER.warn("Host address not recognized : <" + hostArrEl + ">");
                }
            }
        }
        return adresses;
    }

}

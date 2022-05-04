package apixio.infraconfig.core;

import org.apache.commons.net.util.SubnetUtils;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class IPFilterHelper {
    public static boolean ipInList(Set<String> filterCidrs, String ipIn) {
        for (String cidr : filterCidrs) {
            String ip;
            if (!cidr.contains("/")) {
                cidr = cidr + "/32";
            }
            if (!ipIn.contains("/")) {
                ip = ipIn + "/32";
            } else {
                ip = ipIn;
            }
            SubnetUtils subnet = new SubnetUtils(cidr);
            subnet.setInclusiveHostCount(true);
            SubnetUtils compareSubnet = new SubnetUtils(ip);
            compareSubnet.setInclusiveHostCount(true);
            Set<Boolean> hasIpOutsideOfRange = Arrays.stream(compareSubnet.getInfo().getAllAddresses()).map(ipInRange -> subnet.getInfo().isInRange(ipInRange)).collect(Collectors.toSet());
            if (!hasIpOutsideOfRange.contains(false)) {
                return true;
            }
        }
        return false;
    }
}

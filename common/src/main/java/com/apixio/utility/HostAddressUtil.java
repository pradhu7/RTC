package com.apixio.utility;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class HostAddressUtil
{
    /**
     * from stackflow: http://stackoverflow.com/questions/9481865/getting-the-ip-address-of-the-current-machine-using-java
     * copy the method used in pipeline-core
     */

    public static String getLocalHostLANAddress()
    {

        try {
            InetAddress candidateAddress = null;
            // Iterate all NICs (network interface cards)...
            for (Enumeration ifaces = NetworkInterface.getNetworkInterfaces(); ifaces.hasMoreElements();)
            {
                NetworkInterface iface = (NetworkInterface) ifaces.nextElement();
                // Iterate all IP addresses assigned to each card...
                for (Enumeration inetAddrs = iface.getInetAddresses(); inetAddrs.hasMoreElements();)
                {
                    InetAddress inetAddr = (InetAddress) inetAddrs.nextElement();
                    if (!inetAddr.isLoopbackAddress())
                    {

                        if (inetAddr.isSiteLocalAddress())
                        {
                            // Found non-loopback site-local address and non-localhost. Return it immediately...
                            String localHostIP = "127.0.0.1";
                            if (localHostIP.equals(new String(inetAddr.getHostAddress())) == false) {
                                return inetAddr.getHostAddress();
                            }
                        }
                        else if (candidateAddress == null)
                        {
                            // Found non-loopback address, but not necessarily site-local.
                            // Store it as a candidate to be returned if site-local address is not subsequently found...
                            candidateAddress = inetAddr;
                            // Note that we don't repeatedly assign non-loopback non-site-local addresses as candidates,
                            // only the first. For subsequent iterations, candidate will be non-null.
                        }
                    }
                }
            }
            if (candidateAddress != null)
            {
                // We did not find a site-local address, but we found some other non-loopback address.
                // Server might have a non-site-local address assigned to its NIC (or it might be running
                // IPv6 which deprecates the "site-local" concept).
                // Return this non-loopback candidate address...
                return candidateAddress.getHostAddress();
            }

            // At this point, we did not find a non-loopback address.
            // Fall back to returning whatever InetAddress.getLocalHost() returns...
            InetAddress address  = InetAddress.getLocalHost();
            return (address != null ? address.getHostAddress() : null);
        }
        catch (Exception e)
        {
            return null;
        }
    }
}
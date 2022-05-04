package apixio.infraconfig.core;

import org.assertj.core.util.Sets;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class IPFilterHelperTest {

    @Test
    public void ipInList() {

        // validate source range - single ip
        assertTrue(IPFilterHelper.ipInList(Sets.newLinkedHashSet("192.168.88.10/24"), "192.168.88.33"));
        assertFalse(IPFilterHelper.ipInList(Sets.newLinkedHashSet("192.168.88.10/24"), "192.168.98.33"));
        assertFalse(IPFilterHelper.ipInList(Sets.newLinkedHashSet("192.168.88.10"), "192.168.88.33"));
        assertTrue(IPFilterHelper.ipInList(Sets.newLinkedHashSet("192.168.88.10"), "192.168.88.10"));

        //validate single ip, single ip source
        assertTrue(IPFilterHelper.ipInList(Sets.newLinkedHashSet("192.168.88.10/32"), "192.168.88.10"));
        assertTrue(IPFilterHelper.ipInList(Sets.newLinkedHashSet("192.168.88.10"), "192.168.88.10/32"));
        assertTrue(IPFilterHelper.ipInList(Sets.newLinkedHashSet("192.168.88.10/32"), "192.168.88.10/32"));

        // validate source range - ip range
        assertTrue(IPFilterHelper.ipInList(Sets.newLinkedHashSet("192.168.88.10/24"), "192.168.88.10/25"));
        assertFalse(IPFilterHelper.ipInList(Sets.newLinkedHashSet("192.168.88.10/24"), "192.168.88.33/23"));

        // validate multiple source range
        assertTrue(IPFilterHelper.ipInList(Sets.newLinkedHashSet("192.168.99.0/24", "10.10.0.0/16"), "10.10.1.1"));
        assertFalse(IPFilterHelper.ipInList(Sets.newLinkedHashSet("192.168.99.0/24", "10.10.0.0/16"), "10.0.1.1"));
        assertTrue(IPFilterHelper.ipInList(Sets.newLinkedHashSet("192.168.99.0/24", "10.10.0.0/16"), "10.10.1.5/28"));
        assertTrue(IPFilterHelper.ipInList(Sets.newLinkedHashSet("192.168.99.0/24", "10.10.0.0/16"), "192.168.99.3"));

    }
}
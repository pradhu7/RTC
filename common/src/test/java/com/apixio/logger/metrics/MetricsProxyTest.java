/*
 * This test has somehow gained the ability to confuse Eclipse and Maven.
 * Disabled.
 */

package com.apixio.logger.metrics;

import static org.junit.Assert.*;

import org.junit.Test;
import org.apache.log4j.MDC;

public class MetricsProxyTest
{
    
  @Test
  public void testException()
  {
      MetricApp proxy = (MetricApp) MetricsProxy.newInstance(new MetricAppImpl());
      MDC.clear();
      assertEquals(null, MDC.get("metered2.count"));
      assertEquals(null, MDC.get("exception.count"));
      try {
          proxy.exception(false);
      } catch (Exception e) {
          ;
      }
      assertEquals("A", "1", (String) MDC.get("metered2.count"));
      assertEquals("0", MDC.get("exception.count"));
      try {
          proxy.exception(true);
      } catch (Exception e) {
          ;
      }
      assertEquals("2", MDC.get("metered2.count"));
      assertEquals("B", "1", MDC.get("exception.count"));
  }

    @Test
    public void testTimer()
    {
        MetricApp proxy = (MetricApp) MetricsProxy.newInstance(new MetricAppImpl());
        MDC.clear();
        assertEquals(null, MDC.get("timed.count"));
        proxy.timer();
        assertEquals("A", "1", MDC.get("timed.count"));
        assertTrue(Double.parseDouble((String) MDC.get("timed.min")) > 1);
    }

    @Test
    public void testMeter()
    {
        MetricApp proxy = (MetricApp) MetricsProxy.newInstance(new MetricAppImpl());
        MDC.clear();
        assertEquals(null, MDC.get("metered1.count"));
        proxy.meter();
        assertEquals("A", "1", MDC.get("metered1.count"));
    }

}

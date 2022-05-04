package com.apixio.datasource.redis;

import static org.junit.Assert.*;

import java.util.UUID;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import redis.clients.jedis.JedisPool;

import com.apixio.datasource.redis.Queue.QueueItem;

/**
 * Test Redis-backed queues
 * 
 * TODO: TimeOrderedQueue blocking is not implemented
 */
@Ignore("Integration")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:applicationContext-test.xml")
public class TestQueues {
    Logger logger = Logger.getLogger(TestQueues.class);
    
    @Autowired
    JedisPool jedisPool;
    
//    @Autowired
    String jedisHost;
//    @Autowired
    int jedisPort;
    
    @Autowired
    String queueBase;

    @Before
    public void setUp() throws Exception
    {
        // autowiring having trouble with this
        jedisPool = new JedisPool(jedisHost, jedisPort);
     }

    @After
    public void tearDown()
    {
    }

    @Test
    public void testFifoQueue()
    {
        Queue fifoQ;
        try
        {
            fifoQ = new FifoQueue(queueBase + ".fifo."
                    + UUID.randomUUID().toString());
        } catch (Exception e)
        {
            logger.warn("Cannot create Queue: " + e.getMessage());
            return;
        }
        doMulti(fifoQ);
    }

    @Test
    public void testFifoQueueBlocking()
    {
        FifoQueue fifoQ;
        try
        {
            fifoQ = new FifoQueue(queueBase + ".fifo."
                    + UUID.randomUUID().toString());
        } catch (Exception e)
        {
            logger.warn("Cannot create Queue: " + e.getMessage());
            return;
        }
        doBlocking(fifoQ);
    }

    @Test
    public void testTimeOrderedQueue()
    {
        Queue timeQ;
        try
        {
            timeQ = new TimeOrderedQueue(queueBase + ".time.ordered."
                    + UUID.randomUUID().toString());
        } catch (Exception e)
        {
            logger.warn("Cannot create Queue: " + e.getMessage());
            return;
        }
        doMulti(timeQ);
    }

    @Test
    public void testTimeOrderedQueueBlocking()
    {
        Queue timeQ;
        try
        {
            timeQ = new TimeOrderedQueue(queueBase + ".time.ordered."
                    + UUID.randomUUID().toString());
        } catch (Exception e)
        {
            logger.warn("Cannot create Queue: " + e.getMessage());
            return;
        }
        // doBlocking(timeQ);
    }

    private void doBlocking(FifoQueue testQ)
    {
        long mark = System.currentTimeMillis();
        QueueItem qi = testQ.get();
        assertNull(qi);
        //        qi = testQ.block(5);
        assertNull(qi);
        long delta = System.currentTimeMillis() - mark;
        String memo = testQ.getClass().getName();
        assertTrue(memo + ": Block on no data was supposed to timeout at 5s",
                delta > 3000 && delta < 7000);
    }

    private void doMulti(Queue testQ)
    {
        QueueItem qi = testQ.get();
        assertNull(qi);

        testQ.post("blob1");
        qi = testQ.peek();
        assertNotNull(qi);
        assertEquals("blob1", qi.data);
        qi = testQ.get();
        assertEquals("blob1", qi.data);
        qi = testQ.peek();
        assertNull(qi);
        qi = testQ.get();
        assertNull(qi);

        testQ.post("blob3");
        testQ.post("blob4");
        qi = testQ.peek();
        assertNotNull(qi);
        assertEquals(testQ.getClass().getName(), "blob3", qi.data);
        qi = testQ.peek();
        assertNotNull(qi);
        assertEquals(testQ.getClass().getName(), "blob3", qi.data);
        assertNotNull(qi);
        qi = testQ.get();
        assertEquals(testQ.getClass().getName(), "blob3", qi.data);
        qi = testQ.get();
        assertNotNull(qi);
        assertEquals(testQ.getClass().getName(), "blob4", qi.data);
        qi = testQ.get();
        assertNull(qi);

        testQ.post("blob5");
        testQ.post("blob6");
        testQ.post("blob7");
        qi = testQ.get();
        assertEquals(testQ.getClass().getName(), "blob5", qi.data);
        qi = testQ.get();
        assertEquals(testQ.getClass().getName(), "blob6", qi.data);
        qi = testQ.get();
        assertEquals(testQ.getClass().getName(), "blob7", qi.data);
        qi = testQ.get();
        assertNull(qi);
    }

    @Test
    public void testTimeOrderedQueueScheduled()
    {
        Queue timeQ;
        try
        {
            timeQ = new TimeOrderedQueue("test_time_ordered_"
                    + UUID.randomUUID().toString());
        } catch (Exception e)
        {
            logger.warn("Cannot create Queue: " + e.getMessage());
            return;
        }
        // QueueItem qi = timeQ.get();
        // assertNull(qi);
        //
        // long mark = System.currentTimeMillis();
        // timeQ.post("blob1");
        //
        // long delta = System.currentTimeMillis() - mark;

    }

    public JedisPool getJedisPool()
    {
        return jedisPool;
    }

    public void setJedisPool(JedisPool jedisPool)
    {
        this.jedisPool = jedisPool;
    }

    public String getJedisHost()
    {
        return jedisHost;
    }

    public void setJedisHost(String jedisHost)
    {
        this.jedisHost = jedisHost;
    }

    public Integer getJedisPort()
    {
        return jedisPort;
    }

    public void setJedisPort(Integer jedisPort)
    {
        this.jedisPort = jedisPort;
    }

    public String getQueueBase()
    {
        return queueBase;
    }

    public void setQueueBase(String queueBase)
    {
        this.queueBase = queueBase;
    }
}

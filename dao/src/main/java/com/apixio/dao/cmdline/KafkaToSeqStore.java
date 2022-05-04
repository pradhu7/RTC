package com.apixio.dao.cmdline;

import java.io.FileReader;
import java.io.BufferedReader;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.*;

import com.apixio.dao.seqstore.SeqStoreDAO;

import com.apixio.datasource.kafka.SimpleKafkaEventConsumer;

import com.apixio.model.event.EventType;
import com.apixio.model.event.AttributeType;
import com.apixio.model.event.transformer.EventTypeListJSONParser;

public class  KafkaToSeqStore extends CmdLineBase
{
    private SimpleKafkaEventConsumer  consumer;
    private Properties                kafkaConfig;
    private BlockingQueue<Runnable>   q;
    private ExecutorService           executor;
    private EventTypeListJSONParser   parser;
    private int                       numThreads = 30;

    public static void main(String[] args)
    {
        try
        {
            KafkaToSeqStore ktss = new KafkaToSeqStore(args);
            ktss.executeProgram();
        }
        catch (Throwable e)
        {
            System.out.println("ERROR: " + e);
            e.printStackTrace();
        }
        finally
        {
            System.exit(0);
        }
    }

    private KafkaToSeqStore(String[] args)
    {
        parseCommandLine(args);
    }

    @Override
    protected void setup() throws Exception
    {
        setupKafka();
        setupDaoServices();
        setupExecutor();
        parser = new EventTypeListJSONParser();
    }

    @Override
    protected void executeProgram() throws Exception
    {
        if (runProgram)
        {
            setup();
            run();
            done();
            System.out.println("Done.");
        }
        else
        {
            System.out.println("Usage: KafkaToSequenceStore <kafka-config.properties> <sequence-store-config.yaml>");
        }
    }

    private EventType addTimeAttribute(EventType event)
    {
        AttributeType a = new AttributeType();
        a.setName("$time");
        a.setValue(String.valueOf(System.currentTimeMillis()));
        event.getAttributes().getAttribute().add(a);
        return event;
    }

    private List<EventType> batchEvents()
    {
        List<EventType> batch = new ArrayList<>();
        for (int i = 0; i < 1; i++)
        {
            scala.Option<EventType> maybeEvent = consumer.getMessage();
            if (maybeEvent.isEmpty())
                return batch;
            batch.add(addTimeAttribute(maybeEvent.get()));
        }
        return batch;
    }

    @Override
    protected void run() throws Exception
    {
        SeqStoreDAO ssDAO = daoServices.getSeqStoreDAO();

        while (true)
        {
            List<EventType> events = batchEvents();
            if (events.size() > 0) {
                Runnable runnable = new KafkaToSeqStoreRunnable(ssDAO, events, this.parser);
                executor.execute(runnable);
            }
        }
    }

    @Override
    protected void done() throws Exception
    {
        executor.shutdown();
        while (!executor.isTerminated())
        {
        }

    }

    private void parseCommandLine(String[] args)
    {
        if (args.length != 2)
        {
            runProgram = false;
            return;
        }
        try
        {
            kafkaConfig = new Properties();

            kafkaConfig.load(new BufferedReader(new FileReader(args[0])));

            runProgram = true;
        }
        catch (Throwable e)
        {
            e.printStackTrace();
            runProgram = false;
        }
    }

    private void setupExecutor() throws Exception
    {
        q = new ArrayBlockingQueue<Runnable>(numThreads);
        RejectedExecutionHandler rejectedExecutionHandler = new ThreadPoolExecutor.CallerRunsPolicy();
        executor = new ThreadPoolExecutor(numThreads, numThreads, 0L, TimeUnit.MILLISECONDS, q, rejectedExecutionHandler);
    }

    private void setupKafka() throws Exception
    {
        consumer = new SimpleKafkaEventConsumer(kafkaConfig);
        consumer.setShutdownHook();
    }

    private class KafkaToSeqStoreRunnable implements Runnable
    {
        private SeqStoreDAO ssDAO;
        private List<EventType> events;
        private EventTypeListJSONParser parser;

        //XXX: This currently only handles events of length 1
        KafkaToSeqStoreRunnable(SeqStoreDAO ssDAO, List<EventType> events, EventTypeListJSONParser parser)
        {
            this.ssDAO = ssDAO;
            this.events = events;
            this.parser = parser;
        }

        @Override
        public void run()
        {
            int attempts = 0;
            Boolean success = false;
            try
            {
                do
                {
                    System.out.println("Attempting write #" + attempts + " for patient " + events.get(0).getSubject().getUri());
                    attempts += 1;
                    Map<EventType, Boolean> res = ssDAO.putAndCheck(events, null);
                    success = res.values().iterator().next();
                    System.out.println("Attempted write #" + attempts + " of Event: " + success);
                }
                while (!success && attempts < 10);
                if (attempts >= 10)
                {
                    System.out.println("Failed write #" + attempts + " " + success + " of Events: " + this.parser.toJSON(this.events));
                }
            }
            catch (Exception ex)
            {
                try {
                    System.out.println("Failed write #" + attempts + " " + success + " of Events: " + this.parser.toJSON(this.events));
                    ex.printStackTrace();
                } catch (Exception fex) {
                    System.out.println("Fatal exception");
                    fex.printStackTrace();
                    System.exit(1);
                }
            }
        }
    }
}


package com.apixio.datasource.kafka;

import com.apixio.logger.EventLogger;
import io.confluent.parallelconsumer.ParallelEoSStreamProcessor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.MockConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

@RunWith(PowerMockRunner.class)
@PrepareForTest(KafkaProcessor.class)
public class KafkaProcessorTest {
    public EventLogger testEventLogger = new EventLogger(this.getClass().getCanonicalName());

    @Test
    public void testSimpleConsumeSingleMessage() throws InterruptedException, IOException {
        SynchronousQueue<ConsumerRecords<String, String>> s = new SynchronousQueue<>();

        // Setup the MockConsumer
        MockConsumer<String, String> mc = new MockConsumer<>(OffsetResetStrategy.EARLIEST);
        TopicPartition tp = new TopicPartition("topic", 0);
        ConsumerRecord<String, String> cr = new ConsumerRecord<>("topic", 0, 1, "key", "value");
        mc.schedulePollTask(() -> {
                mc.updateBeginningOffsets(Collections.singletonMap(tp, 0l));
                mc.rebalance(Collections.singleton(tp));

                // Add a record
                mc.addRecord(cr);
            });

        // Setup and run the KafkaProcessor
        KafkaProcessor<String, String> kafkaProcessor = new KafkaProcessor<>(testEventLogger,
                                                                             mc,
                                                                             Duration.ZERO,
                                                                             s::add,
                                                                             Collections.singleton("topic"));
        Thread kpRunner = new Thread(kafkaProcessor::run, "simpleKpRunner");
        kpRunner.start();

        try (Closeable _tpCleanup = kpRunner::stop) {
            // We should be able to pull out the record we put in,
            // within 10 seconds, or something's gone terribly wrong.
            assertSame(cr,
                       s.poll(10, TimeUnit.SECONDS).iterator().next());
            kafkaProcessor.stayingAlive.set(false);
            kpRunner.join(Duration.of(10, ChronoUnit.SECONDS).toMillis());
        }

        Map<TopicPartition, OffsetAndMetadata> committed = mc.committed(Collections.singleton(tp));
        OffsetAndMetadata committedTpOffset = committed.get(tp);
        assertSame(2l, committedTpOffset.offset());
    }

    @Test
    public void testComplexConsumeSingleMessage() throws InterruptedException, IOException {
        SynchronousQueue<ConsumerRecords<String, String>> s = new SynchronousQueue<>();

        // Setup the MockConsumer
        MockConsumer<String, String> mc = new MockConsumer<>(OffsetResetStrategy.EARLIEST);
        TopicPartition tp = new TopicPartition("topic", 0);
        ConsumerRecord<String, String> cr = new ConsumerRecord<>("topic", 0, 1, "key", "value");
        mc.schedulePollTask(() -> {
                mc.updateBeginningOffsets(Collections.singletonMap(tp, 0l));
                mc.rebalance(Collections.singleton(tp));

                // Add a record
                mc.addRecord(cr);
            });

        // Setup and run the KafkaProcessor
        KafkaProcessor<String, String> kafkaProcessor = new KafkaProcessor<>(testEventLogger,
                                                                             mc,
                                                                             Duration.ZERO,
                                                                             s::add,
                                                                             Collections.singleton("topic"))
            .withPipelining("complexKpTest",
                            3, 3, 0,
                            80);
        Thread kpRunner = new Thread(kafkaProcessor::run, "complexKpRunner");
        kpRunner.start();

        try (Closeable _tpCleanup = kpRunner::stop) {
            // We should be able to pull out the record we put in,
            // within 10 seconds, or something's gone terribly wrong.
            assertSame(cr,
                       s.poll(10, TimeUnit.SECONDS).iterator().next());
            // A hack in lieu of implementing separate drain +
            // shutdown methods so we get that last round to commit
            // the last records to cross the finish line. Without this
            // `Thread.sleep`, the last few records won't "commit".
            Thread.sleep(50);
            kafkaProcessor.stayingAlive.set(false);
            kpRunner.join(Duration.of(10, ChronoUnit.SECONDS).toMillis());
        }

        Map<TopicPartition, OffsetAndMetadata> committed = mc.committed(Collections.singleton(tp));
        OffsetAndMetadata committedTpOffset = committed.get(tp);
        assertSame(2l, committedTpOffset.offset());
    }

    @Test
    public void testComplexConsumeManyMessages() throws InterruptedException, IOException {
        int msgCount = 100;
        List<SynchronousQueue<ConsumerRecords<String, String>>> s = Stream
            .iterate(0, (i) -> i+1).limit(msgCount)
            .map((i) -> new SynchronousQueue<ConsumerRecords<String, String>>())
            .collect(Collectors.toList());

        // Setup the MockConsumer
        MockConsumer<String, String> mc = new MockConsumer<>(OffsetResetStrategy.EARLIEST);
        TopicPartition tp = new TopicPartition("topic", 0);
        mc.schedulePollTask(() -> {
                mc.updateBeginningOffsets(Collections.singletonMap(tp, 0l));
                mc.rebalance(Collections.singleton(tp));

                // Add the records
                IntStream.range(0, msgCount).forEach((i) -> {
                        ConsumerRecord<String, String> cr = new ConsumerRecord<>("topic", 0, i, "key", "value");
                        mc.addRecord(cr);
                    });
            });

        Random r = new Random();

        // Setup and run the KafkaProcessor
        Consumer<ConsumerRecords<String, String>> consumer = (crs) -> {
            try {
                Thread.sleep(r.nextInt(10));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            int offset = (int) crs.iterator().next().offset();
            SynchronousQueue<ConsumerRecords<String, String>> sq = s.get(offset);
            try {
                sq.put(crs);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };
        KafkaProcessor<String, String> kafkaProcessor = new KafkaProcessor<>(testEventLogger,
                                                                             mc,
                                                                             Duration.ZERO,
                                                                             consumer,
                                                                             Collections.singleton("topic"))
            .withPipelining("complexManyRecordTest",
                            3, 3, 0,
                            80);
        Thread kpRunner = new Thread(kafkaProcessor::run, "complexManyRecordKpRunner");
        kpRunner.start();

        try (Closeable _tpCleanup = kpRunner::stop) {
            // We should be able to pull out the record we put in,
            // within 10 seconds, or something's gone terribly wrong.
            IntStream.range(0, msgCount).forEach((i) -> {
                    try {
                        SynchronousQueue<ConsumerRecords<String, String>> sq = s.get(i);
                        ConsumerRecords<String, String> crs = sq.poll(1, TimeUnit.MINUTES);
                        ConsumerRecord<String, String> cr = crs.iterator().next();
                        assertEquals(i,
                                     cr.offset());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });
            // A hack in lieu of implementing separate drain +
            // shutdown methods so we get that last round to commit
            // the last records to cross the finish line. Without this
            // `Thread.sleep`, the last few records won't "commit".
            Thread.sleep(50);
            kafkaProcessor.stayingAlive.set(false);
            kpRunner.join(Duration.of(1, ChronoUnit.MINUTES).toMillis());
        }

        Map<TopicPartition, OffsetAndMetadata> committed = mc.committed(Collections.singleton(tp));
        OffsetAndMetadata committedTpOffset = committed.get(tp);
        assertEquals(msgCount, committedTpOffset.offset());
    }

    @Test
    public void testComplexConsumeManyMessagesMultiPartition() throws InterruptedException, IOException {
        int msgCount = 7;
        List<SynchronousQueue<ConsumerRecords<String, String>>> s = Stream
            .iterate(0, (i) -> i+1).limit(msgCount)
            .map((i) -> new SynchronousQueue<ConsumerRecords<String, String>>())
            .collect(Collectors.toList());

        // Setup the MockConsumer
        MockConsumer<String, String> mc = new MockConsumer<>(OffsetResetStrategy.EARLIEST);
        TopicPartition tp0 = new TopicPartition("topic", 0);
        TopicPartition tp1 = new TopicPartition("topic", 1);
        TopicPartition tp2 = new TopicPartition("topic", 2);
        Map<TopicPartition, Long> initialOffsetMap = new HashMap<>();
        initialOffsetMap.put(tp0, 0l);
        initialOffsetMap.put(tp1, 0l);
        initialOffsetMap.put(tp2, 0l);
        mc.schedulePollTask(() -> {
                mc.updateBeginningOffsets(initialOffsetMap);
                mc.rebalance(initialOffsetMap.keySet());

                // Add the records
                IntStream.range(0, msgCount+5).forEach((i) -> {
                        ConsumerRecord<String, String> cr = new ConsumerRecord<>("topic", 0, i, "key", "value");
                        mc.addRecord(cr);
                    });

                // Add the records
                IntStream.range(0, msgCount).forEach((i) -> {
                        ConsumerRecord<String, String> cr = new ConsumerRecord<>("topic", 1, i, "key", "value");
                        mc.addRecord(cr);
                    });

                // Add the records
                IntStream.range(0, msgCount-5).forEach((i) -> {
                        ConsumerRecord<String, String> cr = new ConsumerRecord<>("topic", 2, i, "key", "value");
                        mc.addRecord(cr);
               });


        });

        Random r = new Random();

        // Setup and run the KafkaProcessor
        Consumer<ConsumerRecords<String, String>> consumer = (crs) -> {
            try {
                Thread.sleep(r.nextInt(10));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            int offset = (int) crs.iterator().next().offset();
            if (crs.iterator().next().partition() == 1) {
                SynchronousQueue<ConsumerRecords<String, String>> sq = s.get(offset);
                try {
                    sq.put(crs);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        KafkaProcessor<String, String> kafkaProcessor = new KafkaProcessor<>(testEventLogger,
                                                                             mc,
                                                                             Duration.ZERO,
                                                                             consumer,
                                                                             Collections.singleton("topic"))
            .withPipelining("complexManyRecordTest",
                            3, 3, 0,
                            80);
        Thread kpRunner = new Thread(kafkaProcessor::run, "complexManyRecordKpRunner");
        kpRunner.start();

        try (Closeable _tpCleanup = kpRunner::stop) {
            // We should be able to pull out the record we put in,
            // within 10 seconds, or something's gone terribly wrong.
            IntStream.range(0, msgCount).forEach((i) -> {
                    try {
                        SynchronousQueue<ConsumerRecords<String, String>> sq = s.get(i);
                        ConsumerRecords<String, String> crs = sq.poll(1, TimeUnit.MINUTES);
                        ConsumerRecord<String, String> cr = crs.iterator().next();
                        assertEquals(i,
                                     cr.offset());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });
            // A hack in lieu of implementing separate drain +
            // shutdown methods so we get that last round to commit
            // the last records to cross the finish line. Without this
            // `Thread.sleep`, the last few records won't "commit".
            Thread.sleep(500);
            kafkaProcessor.stayingAlive.set(false);
            kpRunner.join(Duration.of(1, ChronoUnit.MINUTES).toMillis());
        }

        Map<TopicPartition, OffsetAndMetadata> committed = mc.committed(initialOffsetMap.keySet());
        OffsetAndMetadata committedTpOffset = committed.get(tp1);
        assertEquals(msgCount, committedTpOffset.offset());
    }

    @Ignore
    @Test
    public void testComplexTtpManyMessages() throws InterruptedException, IOException {
        int msgCount = 100;
        CopyOnWriteArraySet<ConsumerRecords<String, String>> completedWork = new CopyOnWriteArraySet<>();
        CopyOnWriteArraySet<ConsumerRecords<String, String>> ttpWork = new CopyOnWriteArraySet<>();

        // Setup the MockConsumer
        MockConsumer<String, String> mc = new MockConsumer<>(OffsetResetStrategy.EARLIEST);
        TopicPartition tp = new TopicPartition("topic", 0);
        mc.schedulePollTask(() -> {
                mc.updateBeginningOffsets(Collections.singletonMap(tp, 0l));
                mc.rebalance(Collections.singleton(tp));

                // Add the records
                IntStream.range(0, msgCount).forEach((i) -> {
                        ConsumerRecord<String, String> cr = new ConsumerRecord<>("topic", 0, i, "key", "value");
                        mc.addRecord(cr);
                    });
            });

        // Setup and run the KafkaProcessor
        Consumer<ConsumerRecords<String, String>> consumer = (crs) -> {
            try {
                Thread.sleep(500);
                completedWork.add(crs);
            } catch (InterruptedException e) { }
        };
        KafkaProcessor<String, String> kafkaProcessor = new KafkaProcessor<>(testEventLogger,
                                                                             mc,
                                                                             Duration.ZERO,
                                                                             consumer,
                                                                             Collections.singleton("topic"))
            .withPipelining("complexManyRecordTest",
                            3, 3, 0,
                            80)
            .withTTP(Duration.of(-5, ChronoUnit.SECONDS),
                     ttpWork::add);
        Thread kpRunner = new Thread(kafkaProcessor::run, "complexManyRecordKpRunner");
        kpRunner.start();

        try (Closeable _tpCleanup = kpRunner::stop) {
            Thread.sleep(500);
            //
            assertEquals(0, completedWork.size());
            kafkaProcessor.stayingAlive.set(false);
            kpRunner.join(Duration.of(1, ChronoUnit.MINUTES).toMillis());
        }

        assertEquals(msgCount, ttpWork.size());
    }

    @Ignore
    @Test
    public void testComplexWithinTtpManyMessages() throws InterruptedException, IOException {
        int msgCount = 100;
        CopyOnWriteArraySet<ConsumerRecords<String, String>> completedWork = new CopyOnWriteArraySet<>();
        CopyOnWriteArraySet<ConsumerRecords<String, String>> ttpWork = new CopyOnWriteArraySet<>();

        // Setup the MockConsumer
        MockConsumer<String, String> mc = new MockConsumer<>(OffsetResetStrategy.EARLIEST);
        TopicPartition tp = new TopicPartition("topic", 0);
        mc.schedulePollTask(() -> {
                mc.updateBeginningOffsets(Collections.singletonMap(tp, 0l));
                mc.rebalance(Collections.singleton(tp));

                // Add the records
                IntStream.range(0, msgCount).forEach((i) -> {
                        ConsumerRecord<String, String> cr = new ConsumerRecord<>("topic", 0, i, "key", "value");
                        mc.addRecord(cr);
                    });
            });

        // Setup and run the KafkaProcessor
        KafkaProcessor<String, String> kafkaProcessor = new KafkaProcessor<>(testEventLogger,
                                                                             mc,
                                                                             Duration.ZERO,
                                                                             completedWork::add,
                                                                             Collections.singleton("topic"))
            .withPipelining("complexManyRecordTest",
                            3, 3, 0,
                            80)
            .withTTP(Duration.of(5, ChronoUnit.HOURS),
                     ttpWork::add);
        Thread kpRunner = new Thread(kafkaProcessor::run, "complexManyRecordKpRunner");
        kpRunner.start();

        try (Closeable _tpCleanup = kpRunner::stop) {
            Thread.sleep(500);
            //
            assertEquals(msgCount, completedWork.size());
            kafkaProcessor.stayingAlive.set(false);
            kpRunner.join(Duration.of(1, ChronoUnit.MINUTES).toMillis());
        }

        assertEquals(0, ttpWork.size());
    }

    @Test
    public void testComplexConsumeMessagesWithExHandler() throws InterruptedException, IOException {
        Random r = new Random();

        int msgCount = 100;
        int failingMessage = r.nextInt(msgCount);
        List<SynchronousQueue<ConsumerRecords<String, String>>> s = Stream
            .iterate(0, (i) -> i+1).limit(msgCount)
            .map((i) -> new SynchronousQueue<ConsumerRecords<String, String>>())
            .collect(Collectors.toList());

        // Setup the MockConsumer
        MockConsumer<String, String> mc = new MockConsumer<>(OffsetResetStrategy.EARLIEST);
        TopicPartition tp = new TopicPartition("topic", 0);
        mc.schedulePollTask(() -> {
                mc.updateBeginningOffsets(Collections.singletonMap(tp, 0l));
                mc.rebalance(Collections.singleton(tp));

                // Add the records
                IntStream.range(0, msgCount).forEach((i) -> {
                        ConsumerRecord<String, String> cr = new ConsumerRecord<>("topic", 0, i, "key", "value");
                        mc.addRecord(cr);
                    });
            });

        // Setup and run the KafkaProcessor
        Consumer<ConsumerRecords<String, String>> consumer = (crs) -> {
            try {
                Thread.sleep(r.nextInt(10));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            int offset = (int) crs.iterator().next().offset();
            if (offset == failingMessage)
                new Double(offset / 0);
            SynchronousQueue<ConsumerRecords<String, String>> sq = s.get(offset);
            try {
                sq.put(crs);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };
        HashSet<Map<Long, Throwable>> errorQueue = new HashSet<>();
        BiConsumer<ConsumerRecords<String, String>, Throwable> errorHandler = (crs, t) -> {
            errorQueue.add(Collections.singletonMap(crs.iterator().next().offset(), t));
        };
        KafkaProcessor<String, String> kafkaProcessor = new KafkaProcessor<>(testEventLogger,
                                                                             mc,
                                                                             Duration.ZERO,
                                                                             consumer,
                                                                             Collections.singleton("topic"))
            .withPipelining("complexManyRecordTest",
                            3, 3, 0,
                            80)
            .withErrorHandler(errorHandler);
        Thread kpRunner = new Thread(kafkaProcessor::run, "complexManyRecordKpRunner");
        kpRunner.start();

        try (Closeable _tpCleanup = kpRunner::stop) {
            // We should be able to pull out the record we put in,
            // within 10 seconds, or something's gone terribly wrong.
            IntStream.range(0, msgCount).forEach((i) -> {
                    try {
                        SynchronousQueue<ConsumerRecords<String, String>> sq = s.get(i);
                        ConsumerRecords<String, String> crs = sq.poll(10, TimeUnit.SECONDS);
                        if (i != failingMessage) {
                            ConsumerRecord<String, String> cr = crs.iterator().next();
                            assertEquals(i,
                                         cr.offset());
                        } else {
                            assertEquals(null, crs);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });
            // A hack in lieu of implementing separate drain +
            // shutdown methods so we get that last round to commit
            // the last records to cross the finish line. Without this
            // `Thread.sleep`, the last few records won't "commit".
            Thread.sleep(50);
            kafkaProcessor.stayingAlive.set(false);
            kpRunner.join(Duration.of(10, ChronoUnit.SECONDS).toMillis());
        }

        assertEquals(ArithmeticException.class, errorQueue.iterator().next().get((long) failingMessage).getClass());

        Map<TopicPartition, OffsetAndMetadata> committed = mc.committed(Collections.singleton(tp));
        OffsetAndMetadata committedTpOffset = committed.get(tp);
        assertEquals(msgCount, committedTpOffset.offset());
    }

    @Test
    public void testComplexConsumeMessagesWithErrorQueue() throws InterruptedException, IOException {
        Random r = new Random();

        int msgCount = 100;
        int failingMessage = r.nextInt(msgCount);
        List<SynchronousQueue<ConsumerRecords<String, String>>> s = Stream
            .iterate(0, (i) -> i+1).limit(msgCount)
            .map((i) -> new SynchronousQueue<ConsumerRecords<String, String>>())
            .collect(Collectors.toList());

        // Setup the MockConsumer
        MockConsumer<String, String> mc = new MockConsumer<>(OffsetResetStrategy.EARLIEST);
        TopicPartition tp = new TopicPartition("topic", 0);
        mc.schedulePollTask(() -> {
                mc.updateBeginningOffsets(Collections.singletonMap(tp, 0l));
                mc.rebalance(Collections.singleton(tp));

                // Add the records
                IntStream.range(0, msgCount).forEach((i) -> {
                        ConsumerRecord<String, String> cr = new ConsumerRecord<>("topic", 0, i, "key", "value");
                        mc.addRecord(cr);
                    });
            });

        // Setup the MockProducer
        MockProducer<String, String> mp = new MockProducer<>();

        // Setup and run the KafkaProcessor
        Consumer<ConsumerRecords<String, String>> consumer = (crs) -> {
            try {
                Thread.sleep(r.nextInt(10));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            int offset = (int) crs.iterator().next().offset();
            if (offset == failingMessage)
                new Double(offset / 0);
            SynchronousQueue<ConsumerRecords<String, String>> sq = s.get(offset);
            try {
                sq.put(crs);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };
        HashSet<Map<Long, Throwable>> errorQueue = new HashSet<>();
        BiConsumer<ConsumerRecords<String, String>, Throwable> errorHandler = (crs, t) -> {
            errorQueue.add(Collections.singletonMap(crs.iterator().next().offset(), t));
        };
        KafkaProcessor<String, String> kafkaProcessor = new KafkaProcessor<>(testEventLogger,
                                                                             mc,
                                                                             Duration.ZERO,
                                                                             consumer,
                                                                             Collections.singleton("topic"))
            .withPipelining("complexManyRecordTest",
                            3, 3, 0,
                            80)
            .withErrorHandler(errorHandler)
            .withErrorQueue(mp, "errorQueueTopic");
        Thread kpRunner = new Thread(kafkaProcessor::run, "complexManyRecordKpRunner");
        kpRunner.start();

        try (Closeable _tpCleanup = kpRunner::stop) {
            // We should be able to pull out the record we put in,
            // within 10 seconds, or something's gone terribly wrong.
            IntStream.range(0, msgCount).forEach((i) -> {
                    try {
                        SynchronousQueue<ConsumerRecords<String, String>> sq = s.get(i);
                        ConsumerRecords<String, String> crs = sq.poll(10, TimeUnit.SECONDS);
                        if (i != failingMessage) {
                            ConsumerRecord<String, String> cr = crs.iterator().next();
                            assertEquals(i,
                                         cr.offset());
                        } else {
                            assertEquals(null, crs);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });
            // A hack in lieu of implementing separate drain +
            // shutdown methods so we get that last round to commit
            // the last records to cross the finish line. Without this
            // `Thread.sleep`, the last few records won't "commit".
            Thread.sleep(50);
            kafkaProcessor.stayingAlive.set(false);
            kpRunner.join(Duration.of(10, ChronoUnit.SECONDS).toMillis());
        }

        assertEquals(errorQueue.iterator().next().get((long) failingMessage).getClass(), ArithmeticException.class);

        ProducerRecord<String, String> pr = mp.history().get(0);
        assertEquals("key", pr.key());
        assertEquals("value", pr.value());
        assertEquals(ArithmeticException.class.getCanonicalName(), new String(pr.headers().lastHeader("processExceptionType").value()));

        Map<TopicPartition, OffsetAndMetadata> committed = mc.committed(Collections.singleton(tp));
        OffsetAndMetadata committedTpOffset = committed.get(tp);
        assertEquals(msgCount, committedTpOffset.offset());
    }

    @Ignore
    @Test
    public void testConfluentConsumeManyMessages() throws InterruptedException, IOException {
        int msgCount = 100;
        List<SynchronousQueue<ConsumerRecord<String, String>>> s = Stream
            .iterate(0, (i) -> i+1).limit(msgCount)
            .map((i) -> new SynchronousQueue<ConsumerRecord<String, String>>())
            .collect(Collectors.toList());

        // Setup the MockConsumer
        MockConsumer<String, String> mc = new MockConsumer<>(OffsetResetStrategy.EARLIEST);
        TopicPartition tp = new TopicPartition("topic", 0);
        mc.schedulePollTask(() -> {
                mc.updateBeginningOffsets(Collections.singletonMap(tp, 0l));
                mc.rebalance(Collections.singleton(tp));

                // Add the records
                IntStream.range(0, msgCount).forEach((i) -> {
                        ConsumerRecord<String, String> cr = new ConsumerRecord<>("topic", 0, i, "key", "value");
                        mc.addRecord(cr);
                    });
            });

        Random r = new Random();

        // Setup and run the KafkaProcessor
        Consumer<ConsumerRecord<String, String>> consumer = (cr) -> {
            try {
                Thread.sleep(r.nextInt(10));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            int offset = (int) cr.offset();
            SynchronousQueue<ConsumerRecord<String, String>> sq = s.get(offset);
            try {
                sq.put(cr);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };
        KafkaProcessor<String, String> kafkaProcessor = PowerMockito.spy(new KafkaProcessor<>(testEventLogger,
                                                                                              mc,
                                                                                              Duration.ZERO,
                                                                                              null,
                                                                                              Collections.singleton("topic")))
            .withPipelining("confluentManyRecordTest",
                            3, 3, 0,
                            80)
            .withConfluentMagic(consumer);

        try {
            PowerMockito
                .doAnswer(invocationOnMock -> {
                        ((ParallelEoSStreamProcessor) kafkaProcessor.psp).onPartitionsAssigned(Collections.singleton(tp));
                        return null;
                    })
                .when(kafkaProcessor, "unitTestHack");
        } catch (Exception e) {
            e.printStackTrace();
        }

        Thread kpRunner = new Thread(kafkaProcessor::run, "confluentManyRecordKpRunner");
        kpRunner.start();

        Thread.sleep(500);
        ParallelEoSStreamProcessor psp = (ParallelEoSStreamProcessor) kafkaProcessor.psp;
        assertNotNull(psp);
        assertFalse(psp.isClosedOrFailed());
        psp.requestCommitAsap();

        try (Closeable _tpCleanup = kpRunner::stop) {
            // We should be able to pull out the record we put in,
            // within 10 seconds, or something's gone terribly wrong.
            IntStream.range(0, msgCount).forEach((i) -> {
                    try {
                        SynchronousQueue<ConsumerRecord<String, String>> sq = s.get(i);
                        ConsumerRecord<String, String> cr = sq.poll(1, TimeUnit.MINUTES);
                        assertNotNull(cr);
                        assertEquals(i,
                                     cr.offset());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });

            // The Confluent stuff complains if you try and grab the
            // offsets on an already closed instance.
            Thread.sleep(50);
            Map<TopicPartition, OffsetAndMetadata> committed = mc.committed(Collections.singleton(tp));
            OffsetAndMetadata committedTpOffset = committed.get(tp);
            assertEquals(msgCount, committedTpOffset.offset());

            kafkaProcessor.stayingAlive.set(false);
            kpRunner.join(Duration.of(1, ChronoUnit.MINUTES).toMillis());
        }
    }
}

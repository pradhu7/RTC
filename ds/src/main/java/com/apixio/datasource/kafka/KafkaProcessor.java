package com.apixio.datasource.kafka;

import com.apixio.logger.EventLogger;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.confluent.parallelconsumer.ParallelEoSStreamProcessor;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import io.confluent.parallelconsumer.ParallelConsumerOptions;
import io.confluent.parallelconsumer.ParallelStreamProcessor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Headers;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * This is a sparse implementation of a Kafka Processor to shim other
 * Kafka Consumers in Patient Service. The immediate goal is to
 * provide an implementation which uses the base Apache Kafka Java
 * libraries, as they have shown not to suffer from the "rewind"
 * problem.

 * It provides:
 * - The "simple" poll-process-commit loop
 * - A pipelining processor
 * - "Time to process" (TTP) support for processing record(s)
 * - A user configurable error handler callback method
 * - An error queue implementation

 * As time allows, additional features should be added. A quick, loose
 * list in order of priority for Patient Service:

 * 1. Retry support for transient failures.

 * 2. Expose metrics (Dropwizard kind) to avoid spamming Graylog, but
 *    still be able to observe behaviour.
 */
public class KafkaProcessor<K, V> {
    // Basic fields.
    private final EventLogger eventLogger;
    private final Consumer<K, V> kafkaConsumer;
    private final Duration pollInterval;
    private final java.util.function.Consumer<ConsumerRecords<K, V>> consumer;
    // Kafka can accept either a `Collection` or a `Pattern`.
    private final Collection<String> topics;
    private final Pattern topicsPattern;
    // TODO: Expose a "run in thread" helper function, along with a
    // "shutdown" so this field may be changed to `private`, like the
    // rest of them.
    public AtomicBoolean stayingAlive = new AtomicBoolean(true);

    // Attributes for the multi-threaded pipelining consumer. The
    // values here should be fallback defaults and overwritten when
    // the `.withPipelining` method is called.
    private boolean withPipelining = false;
    private String tpName = "unnamedPipelineConsumer";
    private int corePoolSize = 4;
    private int maxPoolSize = 4;
    private long keepAliveTime = 0;
    private int perPartitionBufferSize = 20;

    // For implementing TTPs on record processing
    private Duration ttp;
    private java.util.function.Consumer<ConsumerRecords<K, V>> ttpHandler;

    // For providing bizlogic defined error handling
    private java.util.function.BiConsumer<ConsumerRecords<K, V>, Throwable> errorHandler;
    private Producer<K, V> producer;
    private String errorQueueTopic;

    // Use the fancy
    // https://github.com/confluentinc/parallel-consumer/ library?
    private boolean confluentMagic = false;
    private java.util.function.Consumer<ConsumerRecord<K, V>> singleConsumer;
    public ParallelStreamProcessor<K, V> psp;

    public KafkaProcessor(EventLogger eventLogger,
                          Consumer<K, V> kafkaConsumer,
                          Duration pollInterval,
                          java.util.function.Consumer<ConsumerRecords<K, V>> consumer,
                          Collection<String> topics) {
        this.eventLogger = eventLogger;
        this.kafkaConsumer = kafkaConsumer;
        this.pollInterval = pollInterval;
        this.consumer = consumer;
        this.topics = topics;
        this.topicsPattern = null;
    }

    public KafkaProcessor(EventLogger eventLogger,
                          Consumer<K, V> kafkaConsumer,
                          Duration pollInterval,
                          java.util.function.Consumer<ConsumerRecords<K, V>> consumer,
                          Pattern topicsPattern) {
        this.eventLogger = eventLogger;
        this.kafkaConsumer = kafkaConsumer;
        this.pollInterval = pollInterval;
        this.consumer = consumer;
        this.topicsPattern = topicsPattern;
        this.topics = null;
    }

    public KafkaProcessor<K, V> withPipelining(String name,
                                               int corePoolSize,
                                               int maxPoolSize,
                                               long keepAliveTime,
                                               int perPartitionBufferSize) {
        this.withPipelining = true;
        this.tpName = name;
        this.corePoolSize = corePoolSize;
        this.maxPoolSize = maxPoolSize;
        this.keepAliveTime = keepAliveTime;
        this.perPartitionBufferSize = perPartitionBufferSize;
        return this;
    }

    public KafkaProcessor<K, V> withTTP(Duration ttp,
                                        java.util.function.Consumer<ConsumerRecords<K, V>> ttpHandler) {
        this.ttp = ttp;
        this.ttpHandler = ttpHandler;
        return this;
    }

    public KafkaProcessor<K, V> withErrorHandler(java.util.function.BiConsumer<ConsumerRecords<K, V>, Throwable> errorHandler) {
        this.errorHandler = errorHandler;
        return this;
    }

    public KafkaProcessor<K, V> withConfluentMagic(java.util.function.Consumer<ConsumerRecord<K, V>> singleConsumer) {
        this.confluentMagic = true;
        this.singleConsumer = singleConsumer;
        return this;
    }

    public KafkaProcessor<K, V> withErrorQueue(Producer<K, V> producer, String errorQueueTopic) {
        this.producer = producer;
        this.errorQueueTopic = errorQueueTopic;
        return this;
    }

    public void run() {
        if (withPipelining && confluentMagic) {
            runConfluentConsumer();
        } else if (withPipelining) {
            runPipeliningConsumer();
        } else {
            runSimpleConsumer();
        }
    }

    /**
     * Run the simplest Kafka consumer loop. This implementation
     * should not be neglected in favor of the "complex" consumer loop
     * code, as there is no reason to not also implement error
     * topics/retries here as well.
     */
    private void runSimpleConsumer() {
        if (this.topics != null) {
            this.kafkaConsumer.subscribe(this.topics);
        } else if (this.topicsPattern != null) {
            this.kafkaConsumer.subscribe(this.topicsPattern);
        } else {
            throw new RuntimeException("Topics for Kafka must be specified");
        }
        while (this.stayingAlive.get()) {
            ConsumerRecords<K, V> consumerRecords = kafkaConsumer.poll(pollInterval);
            if (!consumerRecords.isEmpty()) {
                // TODO: Use `PipelineJob` here for TTP checks?
                this.consumer.accept(consumerRecords);
                Set<TopicPartition> topicPartitions = consumerRecords.partitions();
                Map<TopicPartition, OffsetAndMetadata> topicPartitionOffsetMap = topicPartitions
                    .stream()
                    .collect(Collectors.toMap(Function.identity(),
                                              topicPartition -> {
                                                  List<ConsumerRecord<K, V>> records = consumerRecords.records(topicPartition);
                                                  return new OffsetAndMetadata(records.get(records.size() - 1).offset() + 1);
                                              })
                             );
                kafkaConsumer.commitSync(topicPartitionOffsetMap);
            }
        }
    }

    // This class intends to encapsulate a group of consumer records
    // (even if it is a singular record) belonging to the same
    // TopicPartition. In addition to the record(s) itself, it also
    // manages the associated metadata in processing the record, such
    // as the processing callback `Future`, start time, exceptions in
    // processing (if any), and more. In this way, those features are
    // not bound to the specific Kafka library, but to any library
    // that uses this "envelope" for tracking the processing of Kafka
    // messages.
    static class PipelineJob<K, V> {
        private final EventLogger eventLogger;
        private final KafkaProcessor<K, V> kafkaProcessor;
        private final ConsumerRecords<K, V> consumerRecords;
        private final Future<?> completedWork;
        private volatile Instant started = null;

        public PipelineJob(ConsumerRecords<K, V> consumerRecords,
                           ThreadPoolExecutor threadPoolExecutor,
                           KafkaProcessor<K, V> kafkaProcessor) {
            this.consumerRecords = consumerRecords;
            this.kafkaProcessor = kafkaProcessor;
            this.eventLogger = this.kafkaProcessor.eventLogger;
            this.completedWork = threadPoolExecutor.submit(this::run);
        }

        private void run() {
            this.started = Instant.now();
            try {
                this.kafkaProcessor.consumer.accept(this.consumerRecords);
            } catch (Throwable t) {
                eventLogger.error(t);
                Throwable errorHandlerT = null;
                if (this.kafkaProcessor.errorHandler != null) {
                    try {
                        this.kafkaProcessor.errorHandler.accept(this.consumerRecords, t);
                    } catch (Throwable anotherT) {
                        eventLogger.error(anotherT);
                        errorHandlerT = anotherT;
                    }
                }
                errorQueueLog(t, errorHandlerT);
                throw t;
            }
        }

        private void errorQueueLog(Throwable processThrew, Throwable handlerThrew) {
            if (this.kafkaProcessor.errorQueueTopic != null) {
                consumerRecords.forEach((cr) -> {
                        ProducerRecord<K, V> pr = new ProducerRecord<>(this.kafkaProcessor.errorQueueTopic, cr.key(), cr.value());
                        Headers headers = pr.headers();
                        headers.add("thread", Thread.currentThread().getName().getBytes());

                        headers.add("processExceptionType", processThrew.getClass().getCanonicalName().getBytes());
                        headers.add("processExceptionMessage", processThrew.getMessage().getBytes());
                        headers.add("processExceptionStackTrace", ExceptionUtils.getStackTrace(processThrew).getBytes());
                        if (handlerThrew != null) {
                            headers.add("handlerExceptionType", handlerThrew.getClass().getCanonicalName().getBytes());
                            headers.add("handlerExceptionMessage", handlerThrew.getMessage().getBytes());
                            headers.add("handlerExceptionStackTrace", ExceptionUtils.getStackTrace(handlerThrew).getBytes());
                        }
                        this.kafkaProcessor.producer.send(pr);
                    });
            }
        }

        private void enforceTTP(Instant now) {
            if (this.started != null
                && this.kafkaProcessor.ttp
                .minus(Duration.between(this.started, now))
                .isNegative()) {
                if (!this.isDone())
                    this.completedWork.cancel(true);
                this.kafkaProcessor.ttpHandler.accept(this.consumerRecords);

            }
        }

        public boolean isDone() {
            return this.completedWork.isDone();
        }

        public OffsetAndMetadata getOAM() {
            TopicPartition tp = this.consumerRecords.partitions().iterator().next();
            List<ConsumerRecord<K, V>> records = this.consumerRecords.records(tp);
            ConsumerRecord<K, V> lastRecord = records.get(records.size() - 1);
            return new OffsetAndMetadata(lastRecord.offset() + 1);
        }

        public void cancel() {
            this.completedWork.cancel(true);
        }
    }

    static class PipelineRebalanceListener implements ConsumerRebalanceListener {
        ArrayBlockingQueue<Collection<TopicPartition>> onRevoke = new ArrayBlockingQueue<>(20);
        ArrayBlockingQueue<Collection<TopicPartition>> onAssign = new ArrayBlockingQueue<>(20);

        @Override
        public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
            boolean putComplete = false;
            while (putComplete != true) {
                try {
                    this.onRevoke.put(partitions);
                    putComplete = true;
                } catch (InterruptedException e) {
                }
            }
        }

        @Override
        public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
            boolean putComplete = false;
            while (putComplete != true) {
                try {
                    this.onAssign.put(partitions);
                    putComplete = true;
                } catch (InterruptedException e) {
                }
            }
        }
    }

    /**
     * The idea here, is that we're desiring concurrency within the
     * Kafka client, beyond the parallelism Kafka grants us by
     * allowing each partition to be processed by a separate Kafka
     * client. This is probably because whatever processing we're
     * performing is "slow" or, we're not fully utilizing the
     * resources the JVM has access to (often CPU or IO). By using
     * multiple `Thread`s, we can mitigate this.
     *
     * However, we cannot achieve direct parallelism, because by
     * design, acknowledging a record within a single partition
     * acknowledges all records prior (similar to how TCP congestion
     * control behaves). So, this means as a thread completes, the
     * offset for the completed record should only be commited if all
     * records *prior* to that offset in the same partition are also
     * marked as completed. In fact, a completed record does not need
     * its offset commited if it is not the largest offset in a
     * contiguous block of completed records.
     *
     * TODO: implement rate limit for offset commit, either
     * config-driven or dynamic, and consider cross-partition bulk
     * updates.
     *
     * TODO: Consider rebalance events, where I'll want to handle
     * local state bookkeeping for "paused" TopicPartitions, as well
     * as tasks enqueued to the ThreadPool which this consumer cannot
     * commit offsets for.
     */
    private void runPipeliningConsumer() {
        // Rather than typically `poll`, process, and `commit`, as per
        // the "simple" consumer, the loop is now: "`poll` and
        // enqueue", "pause topics we're at capacity for", "check for
        // completed work", "if any, commit", and "unpause topics we
        // may now have room for".

        int partitionQueueCapacity = this.perPartitionBufferSize;

        // TODO: Expose metrics on this queue.
        BlockingQueue<Runnable> tpQueue = new LinkedBlockingQueue<>();

        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat(this.tpName + "-%d").build();

        ThreadPoolExecutor tp = new ThreadPoolExecutor(this.corePoolSize,
                                                       this.maxPoolSize,
                                                       this.keepAliveTime,
                                                       TimeUnit.SECONDS,
                                                       tpQueue,
                                                       threadFactory);

        Map<TopicPartition, Queue<PipelineJob<K, V>>> partitionQueueMap = new ConcurrentHashMap<>();

        PipelineRebalanceListener pipelineRebalanceListener = new PipelineRebalanceListener();
        if (this.topics != null) {
            this.kafkaConsumer.subscribe(this.topics, pipelineRebalanceListener);
        } else if (this.topicsPattern != null) {
            this.kafkaConsumer.subscribe(this.topicsPattern, pipelineRebalanceListener);
        } else {
            throw new RuntimeException("Topics for Kafka must be specified");
        }

        while (this.stayingAlive.get()) {
            pollAndEnqueueRecords(tp, partitionQueueMap, pipelineRebalanceListener);

            if (ttp != null)
                enforceTTPs(partitionQueueMap);

            // Check: Are any topic-partitions ready for committing
            // offsets? If so, commit them.
            Map<TopicPartition, PipelineJob<K, V>> tpCompletedJobMap = getLatestCompletedRecordsMap(partitionQueueMap);
            if (!tpCompletedJobMap.isEmpty()) {
                commitOffsets(kafkaConsumer, tpCompletedJobMap);
                cleanupQueues(partitionQueueMap, tpCompletedJobMap);
            }

            // At this point, we've (possibly) added work to the
            // threadpool, and pruned out "old" work from last round
            // which completed! So this is the correct point to check
            // queue capacities and pause topic-partitions over
            // capacity, and unpause those we can to populate next
            // round.
            managePausedTopicPartitions(partitionQueueMap, partitionQueueCapacity);
        }
    }

    // This is called every loop of `runPipeliningConsumer`. A
    // potential future optimization would be to rate limit this call.
    private void enforceTTPs(Map<TopicPartition, Queue<PipelineJob<K, V>>> partitionQueueMap) {
        Instant now = Instant.now();
        for (Queue<PipelineJob<K, V>> queue : partitionQueueMap.values()) {
            for (PipelineJob<K, V> job : queue) {
                job.enforceTTP(now);
            }
        }
    }

    private void pollAndEnqueueRecords(ThreadPoolExecutor tp,
                                       Map<TopicPartition, Queue<PipelineJob<K, V>>> partitionQueueMap,
                                       PipelineRebalanceListener pipelineRebalanceListener) {
        // Get records. Last round will have paused any topics we
        // don't have capacity for.
        ConsumerRecords<K, V> consumerRecords = kafkaConsumer.poll(pollInterval);
        try {
            for (Collection<TopicPartition> addedTPs = pipelineRebalanceListener.onAssign.poll(0, TimeUnit.MILLISECONDS);
                 addedTPs != null;
                 addedTPs = pipelineRebalanceListener.onAssign.poll(0, TimeUnit.MILLISECONDS)) {
                for (TopicPartition addedTP: addedTPs) {
                    // Currently don't update map to add empty queues
                    // per `addedTP`, but use `computeIfAbsent`
                    // instead.
                }
            }
        } catch (InterruptedException e) {
            eventLogger.error(e);
        }
        try {
            for (Collection<TopicPartition> removedTPs = pipelineRebalanceListener.onRevoke.poll(0, TimeUnit.MILLISECONDS);
                 removedTPs != null;
                 removedTPs = pipelineRebalanceListener.onRevoke.poll(0, TimeUnit.MILLISECONDS)) {
                for (TopicPartition removedTP: removedTPs) {
                    Queue<PipelineJob<K, V>> q = partitionQueueMap.remove(removedTP);
                    if (q != null)
                        q.forEach(PipelineJob::cancel);
                }
            }
        } catch (InterruptedException e) {
            eventLogger.error(e);
        }
        if (consumerRecords.isEmpty()) {
            // If empty, maybe sleep? But I don't know poll.ms, so
            // that could be risky and cause rebalance.

            // Wait, I don't need to. I'm gonna be waiting on the
            // partition queue of Futures to resolve anyway!

            // Or should I? Need to think here.
        } else {
            // Queue everything, then figure which topics are at
            // capacity and need to be paused.

            // Let's grab the partitions
            Set<TopicPartition> partitions = new HashSet<>(consumerRecords.partitions());

            // The desired behaviour from the `ThreadPoolExecutor`
            // (TPE) is to FIFO queue tasks submitted to it for
            // execution on the next available `Thread.` As such, to
            // avoid biasing execution to any `TopicPartition` (TP) in
            // particular, records need to be extracted from each TP
            // in a round-robin. Otherwise, the TPE FIFO queue will be
            // front-loaded with a single TP's workload.

            for (int i = 0; !partitions.isEmpty(); i++) {
                int finalI = i;
                // Drop partitions from the round-robin if they have
                // no more records to provide.
                partitions = partitions.stream().filter((topicPartition) -> consumerRecords.records(topicPartition).size() > finalI).collect(Collectors.toSet());
                for (TopicPartition partition : partitions) {
                    List<ConsumerRecord<K, V>> partitionConsumerRecords = consumerRecords.records(partition);
                    Queue<PipelineJob<K, V>> partitionQueue = partitionQueueMap.computeIfAbsent(partition, (k) -> new LinkedList<>());
                    ConsumerRecord<K, V> r = partitionConsumerRecords.get(i);

                    // Construct a `ConsumerRecords`, even though this
                    // is only ever gonna be "just" this
                    // partition. This leaves the door open to future
                    // work to implement sub-batching when we want
                    // de-duplication or other batch operations (e.g.,
                    // bulk writes to DBs).
                    List<ConsumerRecord<K, V>> consumerRecordList = Collections.singletonList(r);
                    Map<TopicPartition, List<ConsumerRecord<K, V>>> m = Collections.singletonMap(partition, consumerRecordList);
                    ConsumerRecords<K, V> rs = new ConsumerRecords<>(m);

                    partitionQueue.add(new PipelineJob<>(rs, tp, this));
                }
            }
        }
    }

    // For each queue in the map, we want the largest committable
    // offset.
    //
    // For example, a queue with "XYYYYY", where X is an incomplete
    // job, and Y is a completed job, then there is no committable
    // offset.
    //
    // However, if a queue with "YYYYYXZZZZZ", where X is still an
    // incomplete job, and Y and Z are completed jobs, then the latest
    // commitable offset is the last Y.
    private Map<TopicPartition, PipelineJob<K, V>> getLatestCompletedRecordsMap(Map<TopicPartition, Queue<PipelineJob<K, V>>> partitionQueueMap) {
        Map<TopicPartition, PipelineJob<K, V>> lastCompletedRecordsMap = new HashMap<>();

        partitionQueueMap.forEach((tp, queue) -> {
                Iterator<PipelineJob<K, V>> queueIterator = queue.iterator();
                if (queueIterator.hasNext()) {
                    PipelineJob<K, V> lastCompletedRecords = null;
                    PipelineJob<K, V> i;
                    do {
                        i = queueIterator.next();
                        if (i.isDone()) {
                            lastCompletedRecords = i;
                        }
                        else
                            break;
                    } while(queueIterator.hasNext());
                    // Skip populating the return map for `null`s.
                    if (lastCompletedRecords != null)
                        lastCompletedRecordsMap.put(tp, lastCompletedRecords);
                }
            });

        return lastCompletedRecordsMap;
    }

    private void commitOffsets(Consumer<K, V> kafkaConsumer,
                               Map<TopicPartition, PipelineJob<K, V>> tpCompletedJobMap) {
        Map<TopicPartition, OffsetAndMetadata> commitMap = new HashMap<>();
        tpCompletedJobMap.forEach((tp, job) -> {
                OffsetAndMetadata oam = job.getOAM();
                commitMap.put(tp, oam);
            });
        kafkaConsumer.commitSync(commitMap);
    }

    private void cleanupQueues(Map<TopicPartition, Queue<PipelineJob<K, V>>> partitionQueueMap,
                               Map<TopicPartition, PipelineJob<K, V>> tpCompletedJobMap) {
        tpCompletedJobMap.forEach((tp, completedJob) -> {
                Queue<PipelineJob<K, V>> q = partitionQueueMap.get(tp);

                for (PipelineJob<K, V> queueHead;
                     (queueHead = q.peek()) != null
                         && queueHead != completedJob;
                     q.remove());
            });
    }

    private void managePausedTopicPartitions(Map<TopicPartition, Queue<PipelineJob<K, V>>> partitionQueueMap, int capacity) {
        Set<TopicPartition> tpsToResume = new HashSet<>();
        Set<TopicPartition> tpsToPause = new HashSet<>();

        partitionQueueMap.forEach((tp, q) -> {
                if (q.size() < capacity)
                    tpsToResume.add(tp);
                else
                    tpsToPause.add(tp);
            });

        this.kafkaConsumer.pause(tpsToPause);
        this.kafkaConsumer.resume(tpsToResume);
    }

    private void unitTestHack() {
        // This method left intentionally blank.
    }

    private void runConfluentConsumer() {
        ParallelConsumerOptions<K, V> opts = ParallelConsumerOptions
            .<K, V>builder()
            .consumer(kafkaConsumer)
            .ordering(ParallelConsumerOptions.ProcessingOrder.UNORDERED)
            .commitMode(ParallelConsumerOptions.CommitMode.PERIODIC_CONSUMER_SYNC)
            .maxConcurrency(maxPoolSize)
            .build();

        try (ParallelStreamProcessor<K, V> psp = ParallelStreamProcessor.createEosStreamProcessor(opts)) {
            this.psp = psp;

            psp.subscribe(topics);

            unitTestHack();

            psp.poll(this.singleConsumer);

            while (this.stayingAlive.get()) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    // Do nothing! This thread *should* wake up and
                    // check if the `psp` is closed, or if
                    // `stayingAlive` is false!
                }
                if (((ParallelEoSStreamProcessor) psp).isClosedOrFailed()) {
                    break;
                }
            }
        }
    }
}

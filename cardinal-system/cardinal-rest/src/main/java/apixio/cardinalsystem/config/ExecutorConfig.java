package apixio.cardinalsystem.config;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
public class ExecutorConfig {
    public int minPoolSize = 10;
    public int maxPoolSize = 100;
    public int queueSize = 1000;
    public int keepAliveSeconds = 2;

    public ThreadPoolExecutor getThreadPoolExecutor(String threadPoolName) {
        return new ThreadPoolExecutor(
                minPoolSize,
                maxPoolSize,
                keepAliveSeconds,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(
                        queueSize
                ),
                new BasicThreadFactory.Builder().daemon(true).namingPattern(threadPoolName).build()
        );
    }
}
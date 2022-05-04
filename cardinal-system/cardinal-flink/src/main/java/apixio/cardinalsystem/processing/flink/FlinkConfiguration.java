package apixio.cardinalsystem.processing.flink;

public class FlinkConfiguration {
    private String checkpointInterval = "5min";
    private String checkpointTimeout = "30min";
    private int tolerableCheckpointFailures = 3;
    private int paralellism = 0;

    public String getCheckpointInterval() {
        return checkpointInterval;
    }

    public void setCheckpointInterval(String checkpointInterval) {
        this.checkpointInterval = checkpointInterval;
    }

    public String getCheckpointTimeout() {
        return checkpointTimeout;
    }

    public void setCheckpointTimeout(String checkpointTimeout) {
        this.checkpointTimeout = checkpointTimeout;
    }

    public int getTolerableCheckpointFailures() {
        return tolerableCheckpointFailures;
    }

    public void setTolerableCheckpointFailures(int tolerableCheckpointFailures) {
        this.tolerableCheckpointFailures = tolerableCheckpointFailures;
    }

    public int getParalellism() {
        return paralellism;
    }

    public void setParalellism(int paralellism) {
        this.paralellism = paralellism;
    }
}

package combinerutils;

import com.apixio.model.nassembly.*;
import com.apixio.nassembly.model.Merger;

import java.util.Set;

// This Aggregator is Pre-Aggregated!
public class MergedAggregator implements Aggregator, PreAggregated {

    final TransformationMeta[] preProcesses;
    final Set<String> groupIds;
    final TransformationMeta[] postProcesses;


    public MergedAggregator(Merger<? extends Exchange> merger) {
        this.preProcesses = new TransformationMeta[]{};
        this.postProcesses = new TransformationMeta[]{};
        this.groupIds = merger.getGroupIds();
    }

    public MergedAggregator(GarbageCollector collector) {
        this.preProcesses = new TransformationMeta[]{};
        this.postProcesses = new TransformationMeta[]{};
        this.groupIds = collector.getGroupByFieldNames();
    }


    @Override
    public Set<String> getGroupIds() {
        return groupIds;
    }

    @Override
    public TransformationMeta[] getPostProcesses() {
        return postProcesses;
    }

    @Override
    public TransformationMeta[] getPreProcesses() {
        return preProcesses;
    }
}

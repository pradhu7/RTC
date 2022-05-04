package combinerutils;

import com.apixio.model.nassembly.Aggregator;
import com.apixio.model.nassembly.TransformationMeta;

import java.util.Set;

public class AggregatorImpl implements Aggregator {

    TransformationMeta[] preProcesses;

    Set<String> groupIds;

    TransformationMeta[] postProcesses;

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

    public AggregatorImpl(TransformationMeta[] preProcesses, Set<String> groupIds, TransformationMeta[] postProcesses) {
        this.preProcesses = preProcesses;
        this.groupIds = groupIds;
        this.postProcesses = postProcesses;
    }
}

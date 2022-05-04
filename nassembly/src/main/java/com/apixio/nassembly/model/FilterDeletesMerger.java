package com.apixio.nassembly.model;

import com.apixio.model.nassembly.Exchange;
import com.apixio.model.nassembly.TransformationMeta;
import com.apixio.nassembly.combinerutils.TransformationFactory;
import com.apixio.nassembly.model.Merger;

import java.util.Set;

public interface FilterDeletesMerger<T extends Exchange> extends Merger<T> {


    @Override default TransformationMeta[] getAggregatorPostProcesses() {
        TransformationMeta deleteIndicatorFilter = TransformationFactory
                .markAsDelete(getEditTypeColName(), "DELETE");

        return new TransformationMeta[]{deleteIndicatorFilter};
    }

    default String getEditTypeColName() {
        return "base.dataCatalogMeta.editType";
    }

}

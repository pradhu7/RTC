package com.apixio.nassembly.model;

import com.apixio.model.nassembly.GarbageCollector;
import com.apixio.model.nassembly.TransformationMeta;
import com.apixio.nassembly.combinerutils.TransformationFactory;

public interface FilterDeletesGarbageCollector extends GarbageCollector {

    default String getEditTypeColName(){return "base.dataCatalogMeta.editType";}


    @Override
    default TransformationMeta[] getPostProcesses() {
        TransformationMeta deleteIndicatorFilter = TransformationFactory
                .markAsDelete(getEditTypeColName(), "DELETE");

        return new TransformationMeta[]{deleteIndicatorFilter};
    }
}

package io.github.testlens.application.mapper;

import io.github.testlens.application.model.ApplicationModel;

import java.util.List;

/** Result of one explicit observation in the current browsing context. @since 0.5.0 */
public record MappingObservation(String observationId,String pageId,String stateId,
                                 List<String> elementIds, ApplicationModel.Completeness completeness,
                                 List<ApplicationModel.Limitation> limitations, MappingMetrics metrics) {
    public MappingObservation { elementIds=List.copyOf(elementIds);limitations=List.copyOf(limitations); }
}

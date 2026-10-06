package io.github.testlens.application.mapper;

/** Cumulative synchronous mapping measurements. @since 0.5.0 */
public record MappingMetrics(long discoveryNanos,long selectorAnalysisNanos,long mergeNanos,int discoveryScriptCalls,
                             int seleniumCommands,int observations,int discoveredNodes,int analyzedElements,
                             int skippedElements,int selectorAnalyses,int pages,int states,int transitions) { }

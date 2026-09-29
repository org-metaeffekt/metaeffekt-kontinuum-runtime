package org.metaeffekt.kontinuum.runtime.models.shared;

import java.util.List;

/**
 * A read-only catalog of the processors available to a pipeline.
 */
public interface ProcessorCatalog {

    /**
     * Returns all processors held by the catalog.
     *
     * @return the available processors
     */
    List<ProcessorDefinitions.Processor> getProcessors();

    /**
     * Returns the processor matching the given string id.
     *
     * @param processorId the processor id
     * @return the matching processor, or null if none
     */
    ProcessorDefinitions.Processor getProcessorById(String processorId);

    /**
     * Returns the processor matching the given processor id constant.
     *
     * @param processorId the processor id constant
     * @return the matching processor, or null if none
     */
    ProcessorDefinitions.Processor getProcessorById(DefaultProcessorCatalog.ProcessorIds processorId);

}

package org.metaeffekt.kontinuum.runtime.models.shared;

import java.util.List;

public interface ProcessorCatalog {

    List<ProcessorDefinitions.Processor> getProcessors();

    ProcessorDefinitions.Processor getProcessorById(String processorId);

    ProcessorDefinitions.Processor getProcessorById(DefaultProcessorCatalog.ProcessorIds processorId);

}

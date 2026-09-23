package org.metaeffekt.kontinuum.runtime.generator.shared;

import org.apache.commons.lang3.StringUtils;
import org.metaeffekt.kontinuum.runtime.generator.shared.stages.*;
import org.metaeffekt.kontinuum.runtime.models.shared.*;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration.ProjectProperties.Asset;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.Processor;

import java.util.*;

/**
 * Orchestrates pipeline generation.
 * <p>
 * Stage handlers are invoked at the scope they declare:
 * <ul>
 *   <li>{@link StageScope#PIPELINE} handlers run once for the whole pipeline,</li>
 *   <li>{@link StageScope#ASSET} handlers run once per asset,</li>
 *   <li>{@link StageScope#REPORT_GROUP} handlers run once per report group.</li>
 * </ul>
 * Report groups are built up-front from the configured reports; the asset-scoped group stage
 * registers the per-asset inventory contributions as prerequisites of the matching group so that
 * report generation depends on all member assets.
 */
public class Pipeline {

    private final PipelineConfiguration pipelineConfiguration;

    private final Workspace workspace;

    private final EnvironmentConfiguration environmentConfiguration;
    private final ProcessorCatalog processorCatalog = new DefaultProcessorCatalog();

    private final List<PipelineStageHandler> pipelineStageHandlers;
    private final List<AssetStageHandler> assetStageHandlers;
    private final List<ReportGroupStageHandler> reportGroupStageHandlers;

    private final Map<ReportGroupKey, ReportGroupExecutionContext> reportGroupContexts;

    public Pipeline(PipelineConfiguration pipelineConfiguration,
                    EnvironmentConfiguration environmentConfiguration) {

        new PipelineConfigurationLoader().validatePipelineConfigFile(pipelineConfiguration);

        this.environmentConfiguration = environmentConfiguration;
        this.pipelineConfiguration = pipelineConfiguration;
        this.workspace = new Workspace(pipelineConfiguration, environmentConfiguration);
        this.reportGroupContexts = createReportGroupContexts();

        this.pipelineStageHandlers = List.of(
                new PreStageHandler()
        );
        this.assetStageHandlers = List.of(
                new FetchStageHandler(),
                new ExtractStageHandler(),
                new PrepareStageHandler(),
                new AggregateStageHandler(),
                new ResolveStageHandler(),
                new ScanStageHandler(),
                new AdviseStageHandler(),
                new GroupStageHandler(reportGroupContexts),
                new DashboardStageHandler(),
                new SummarizeStageHandler(),
                new PostStageHandler()
        );
        this.reportGroupStageHandlers = List.of(
                new ReportStageHandler()
        );
    }

    public PipelineExecution generatePipeline() {
        PipelineExecutionContext pipelineContext = new PipelineExecutionContext(
                pipelineConfiguration, environmentConfiguration, processorCatalog);
        for (PipelineStageHandler handler : pipelineStageHandlers) {
            handler.process(pipelineContext);
        }

        Map<Asset, AssetExecutionContext> assetExecutionContextMap = new LinkedHashMap<>();
        for (Asset asset : pipelineConfiguration.getProjectProperties().getAllAssets()) {
            AssetExecutionContext context = new AssetExecutionContext(
                    asset,
                    pipelineConfiguration,
                    environmentConfiguration,
                    workspace,
                    processorCatalog
            );

            for (AssetStageHandler handler : assetStageHandlers) {
                handler.process(context);
            }

            assetExecutionContextMap.put(asset, context);
        }

        List<ReportGroupExecutionContext> groupContexts = new ArrayList<>(reportGroupContexts.values());
        for (ReportGroupExecutionContext groupContext : groupContexts) {
            for (ReportGroupStageHandler handler : reportGroupStageHandlers) {
                handler.process(groupContext);
            }
        }

        PipelineExecution execution = new PipelineExecution(pipelineContext, assetExecutionContextMap, groupContexts);
        appendPreScriptToProcessors(execution.getContexts());
        return execution;
    }

    private Map<ReportGroupKey, ReportGroupExecutionContext> createReportGroupContexts() {
        Map<ReportGroupKey, ReportGroupExecutionContext> groups = new LinkedHashMap<>();
        List<PipelineConfiguration.Report> reports = pipelineConfiguration.getReports();
        if (reports == null) {
            return groups;
        }

        for (int reportIndex = 0; reportIndex < reports.size(); reportIndex++) {
            PipelineConfiguration.Report report = reports.get(reportIndex);
            if (report == null || report.getTypes() == null) {
                continue;
            }
            List<Asset> memberAssets = resolveMemberAssets(report);
            for (String typeKey : report.getTypes()) {
                ReportType reportType = ReportType.fromKey(typeKey);
                ReportGroupKey key = new ReportGroupKey(reportIndex, reportType);
                groups.put(key, new ReportGroupExecutionContext(
                        reportIndex, report, reportType, memberAssets,
                        pipelineConfiguration, environmentConfiguration, workspace, processorCatalog));
            }
        }
        return groups;
    }

    private List<Asset> resolveMemberAssets(PipelineConfiguration.Report report) {
        List<Asset> members = new ArrayList<>();
        if (report.getAssetIds() == null) {
            return members;
        }
        for (Asset asset : pipelineConfiguration.getProjectProperties().getAllAssets()) {
            if (report.getAssetIds().contains(asset.getId())) {
                members.add(asset);
            }
        }
        return members;
    }

    private void appendPreScriptToProcessors(List<ExecutionContext> contexts) {
        if (StringUtils.isBlank(environmentConfiguration.SETUP_COMMAND)) {
            return;
        }

        for (ExecutionContext context : contexts) {
            for (Processor processor : context.getProcessors()) {
                String preScript = processor.getPreScript();

                if (StringUtils.isBlank(preScript)) {
                    processor.setPreScript(environmentConfiguration.SETUP_COMMAND);
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(environmentConfiguration.SETUP_COMMAND).append(System.lineSeparator()).append(preScript);
                    processor.setPreScript(stringBuilder.toString());
                }
            }
        }
    }
}

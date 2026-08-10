package org.metaeffekt.kontinuum.runtime.models.shared;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@Getter
public class AssetPlan {

    private boolean requireVulnerabilityMirror = false;

    private boolean requireFetch = false;

    private boolean requireContainerInspect = false;

    private boolean requireExtract = false;

    private boolean requireAggregation = false;

    private boolean requirePortfolioIntegration = false;

    private boolean requireResolve = false;

    private boolean requireLicenseScan = false;

    private boolean requireVulnerabilityEnrichment = false;

    private boolean requireSpdx;

    private boolean requireCycloneDx;

    private boolean requireDashboardGeneration = false;

    private boolean requireReportGeneration = false;

    private PipelineConfiguration.ProjectProperties.Asset asset;

    public AssetPlan(PipelineConfiguration.ProjectProperties.Asset asset, PipelineConfiguration pipelineConfiguration,
                     EnvironmentConfiguration environmentConfiguration) {
        this.asset = asset;
        requireSpdx = pipelineConfiguration.getOptions().getGlobal().getEnableSpdxBom();
        requireCycloneDx = pipelineConfiguration.getOptions().getGlobal().getEnableCycloneDxBom();

        if (pipelineConfiguration.getPortfolioManager() != null) {
            requirePortfolioIntegration = true;
            requireAggregation = true;
        }

        if (asset.getUrlResolver() != null) {
            try {
                URL assetUrl = new URL(asset.getUrlResolver().getUrl());

                if (!"file".equals(assetUrl.getProtocol())) {
                    requireFetch = true;
                    requireExtract = true;
                }
            } catch (MalformedURLException e) {
                throw new IllegalStateException( "The URLResolver for asset " + asset + " must contain a valid URL.",e);
            }
        }

        if (asset.getContainerResolver() != null) {
            requireContainerInspect = true;
            requireExtract = true;
        }

        if (asset.getMavenResolver() != null) {
            requireFetch = true;
            requireExtract = true;
        }

        evaluateOptions(pipelineConfiguration);
        evaluateDashboards(pipelineConfiguration);
        evaluateReports(asset.getId(), pipelineConfiguration);
        validateAssetPlan(pipelineConfiguration);
    }

    private void evaluateOptions(PipelineConfiguration pipelineConfiguration) {
        PipelineConfiguration.Options options = pipelineConfiguration.getOptions();
        if (options != null) {
            PipelineConfiguration.Options.GlobalOptions globalOptions = options.getGlobal();
            if (globalOptions != null) {
                if (globalOptions.getEnableResolve()) {
                    requireResolve = true;
                }
            }
        }
    }

    private void evaluateDashboards(PipelineConfiguration pipelineConfiguration) {
        Optional.ofNullable(pipelineConfiguration.getDashboards())
                .orElse(Collections.emptyList())
                .stream()
                .filter(d -> asset.getId().equals(d.getAssetId()))
                .findAny()
                .ifPresent(d -> {
                    requireVulnerabilityMirror = true;
                    requireVulnerabilityEnrichment = true;
                    requireDashboardGeneration = true;
                });
    }

    private void evaluateReports(String assetId, PipelineConfiguration pipelineConfiguration) {
        List<PipelineConfiguration.Report> reportsForAsset = Optional.ofNullable(pipelineConfiguration.getReports())
                .orElse(Collections.emptyList())
                .stream()
                .filter(r -> assetId.equals(r.getAssetId()))
                .toList();

        if (reportsForAsset.isEmpty()) {
            return;
        }

        requireReportGeneration = true;

        for (PipelineConfiguration.Report report : reportsForAsset) {
            List<String> types = report.getTypes();
            if (types == null) {
                continue;
            }
            for (String type : types) {
                if (type == null) {
                    continue;
                }
                if (ReportType.requiresVulnerabilityEnrichment(ReportType.fromKey(type))) {
                    requireVulnerabilityMirror = true;
                    requireVulnerabilityEnrichment = true;
                } else if (ReportType.requiresScan(ReportType.fromKey(type)) && pipelineConfiguration.getPortfolioManager() == null) {
                    requireLicenseScan = true;
                }
            }
        }
    }

    private void validateAssetPlan(PipelineConfiguration pipelineConfiguration) {

    }

    private static boolean portfolioManagerFlag(PipelineConfiguration pipelineConfiguration,
                                                Function<PipelineConfiguration.PortfolioManager, Boolean> flagAccessor) {
        return Optional.ofNullable(pipelineConfiguration.getPortfolioManager())
                .map(flagAccessor)
                .orElse(Boolean.FALSE);
    }
}

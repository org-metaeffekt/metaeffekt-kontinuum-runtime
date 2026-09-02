package org.metaeffekt.kontinuum.runtime.gitlab;

import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Test;

import java.io.File;

@MojoTest
public class GenerateGitlabPipelineMojoTest {

    @Test
    @InjectMojo(goal = "generate-gitlab-pipeline")
    void testMojoExecution(GenerateGitlabPipelineMojo mojo) throws MojoExecutionException {

    }

}

/*-
 * #%L
 * io.earcam.maven.plugin.sitemap
 * %%
 * Copyright (C) 2017 earcam
 * %%
 * SPDX-License-Identifier: (BSD-3-Clause OR EPL-1.0 OR Apache-2.0 OR MIT)
 *
 * You <b>must</b> choose to accept, in full - any individual or combination of
 * the following licenses:
 * <ul>
 * 	<li><a href="https://opensource.org/licenses/BSD-3-Clause">BSD-3-Clause</a></li>
 * 	<li><a href="https://www.eclipse.org/legal/epl-v10.html">EPL-1.0</a></li>
 * 	<li><a href="https://www.apache.org/licenses/LICENSE-2.0">Apache-2.0</a></li>
 * 	<li><a href="https://opensource.org/licenses/MIT">MIT</a></li>
 * </ul>
 * #L%
 */
package io.earcam.maven.plugin.sitemap;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.model.Reporting;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.earcam.unexceptional.Exceptional;
import io.earcam.utilitarian.site.sitemap.Sitemaps;

@Component(role = AbstractMavenLifecycleParticipant.class, hint = SitemapIndexMavenLifecycleParticipant.NAME, instantiationStrategy = "singleton")
public class SitemapIndexMavenLifecycleParticipant extends AbstractMavenLifecycleParticipant {

	private static final Logger LOG = LoggerFactory.getLogger(SitemapIndexMavenLifecycleParticipant.class);
	static final String NAME = "sitemapindex";
	private static final String CATEGORY = '[' + NAME + ']';

	private static volatile boolean shouldRun = false;


	@Override
	public void afterSessionEnd(MavenSession session) throws MavenExecutionException
	{
		if(session.getResult().hasExceptions()) {
			LOG.warn("{} extension: not running due to previous build errors", CATEGORY);
			return;
		}
		LOG.debug("{} extension: configured to run: {}", CATEGORY, shouldRun);
		if(shouldRun) {
			MavenProject project = session.getTopLevelProject();

			Stream<Path> targetDirs = project.getCollectedProjects().stream()
					.map(MavenProject::getModel)
					.map(Model::getReporting)
					.map(Reporting::getOutputDirectory)
					.map(Paths::get);

			Path targetDir = Paths.get(project.getModel().getReporting().getOutputDirectory());

			Path indexFile = Exceptional.apply(Sitemaps::index, targetDir, targetDirs);

			Exceptional.apply(Files::lines, indexFile)
					.forEach(f -> LOG.debug("{} Created {}", CATEGORY, f));
		}
	}


	static void shouldRun()
	{
		shouldRun = true;
	}
}

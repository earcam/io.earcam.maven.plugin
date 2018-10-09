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

import java.io.File;
import java.net.URI;
import java.util.List;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.codehaus.plexus.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.earcam.utilitarian.site.sitemap.Sitemaps;

@Component(role = AbstractMavenLifecycleParticipant.class, hint = SitemapSubmissionMavenLifecycleParticipant.NAME, instantiationStrategy = "singleton")
public class SitemapSubmissionMavenLifecycleParticipant extends AbstractMavenLifecycleParticipant {

	private static final Logger LOG = LoggerFactory.getLogger(SitemapIndexMavenLifecycleParticipant.class);
	static final String NAME = "sitemapsubmission";
	private static final String CATEGORY = '[' + NAME + ']';

	static volatile boolean run = false;
	static URI baseUrl;
	static File targetDir;
	static List<String> hosts;


	@Override
	public void afterSessionEnd(MavenSession session) throws MavenExecutionException
	{
		if(session.getResult().hasExceptions()) {
			LOG.warn("{} extension: not running due to previous build errors", CATEGORY);
			return;
		}
		LOG.debug("{} extension: configured to run: {}", CATEGORY, run);
		if(run) {
			String responses = Sitemaps.submit(targetDir.toPath(), baseUrl, hosts);
			LOG.info("{} extension ran, responses: {}", CATEGORY, responses);
		}
	}


	static void shouldRun(URI baseUrl, File targetDir, List<String> hosts)
	{
		run = true;
		SitemapSubmissionMavenLifecycleParticipant.baseUrl = baseUrl;
		SitemapSubmissionMavenLifecycleParticipant.targetDir = targetDir;
		SitemapSubmissionMavenLifecycleParticipant.hosts = hosts;
	}
}

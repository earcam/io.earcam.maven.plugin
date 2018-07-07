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

import static org.apache.maven.plugins.annotations.LifecyclePhase.DEPLOY;

import java.io.File;
import java.net.URI;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mojo for submitting sitemaps
 */
@Mojo(name = SitemapSubmissionMojo.NAME, requiresProject = true, threadSafe = true, inheritByDefault = false, defaultPhase = DEPLOY)
public class SitemapSubmissionMojo extends AbstractMojo {

	private static final Logger LOG = LoggerFactory.getLogger(SitemapIndexMavenLifecycleParticipant.class);
	static final String NAME = "submit";
	private static final String CATEGORY = '[' + NAME + ']';

	/**
	 * The site's base URL, defaults to ${project.distributionManagement.site.url}
	 */
	@Parameter(property = "sitemap.url.base", defaultValue = "${project.distributionManagement.site.url}")
	protected URI baseUrl;

	/**
	 * Location of where the sitemap/index should be written, defaults to ${project.reporting.outputDirectory}
	 */
	@Parameter(property = "sitemap.dir.target", defaultValue = "${project.reporting.outputDirectory}")
	protected File targetDir;

	/**
	 * Sitemap submission hosts. The host URI must include the protocol but no path, e.g. https://google.com
	 */
	@Parameter(property = "sitemap.submit.hosts", defaultValue = "https://google.com,https://bing.com,https://yahoo.com", readonly = true, required = true)
	protected List<String> hosts;

	/**
	 * Skip execution of this plugin
	 */
	@Parameter(property = "sitemap.skip", defaultValue = "false")
	protected boolean skip;


	@Override
	public void execute() throws MojoExecutionException, MojoFailureException
	{
		if(skip) {
			LOG.debug("{} skip == true, skipping execution", CATEGORY);
			return;
		}

		LOG.debug("{} extending lifecycle, will run at end", CATEGORY);
		SitemapSubmissionMavenLifecycleParticipant.shouldRun(baseUrl, targetDir, hosts);
	}
}

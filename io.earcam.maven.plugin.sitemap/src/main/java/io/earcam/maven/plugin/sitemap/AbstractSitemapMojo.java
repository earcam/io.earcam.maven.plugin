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
import java.util.regex.Pattern;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import io.earcam.utilitarian.site.sitemap.SitemapParameters;

public abstract class AbstractSitemapMojo extends AbstractMojo {

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	protected MavenProject project;

	/**
	 * Whether to GZIP the generated files
	 */
	@Parameter(property = "sitemap.gzip", defaultValue = "false")
	protected boolean gzip;

	/**
	 * Regular expression determining which file names to include
	 */
	@Parameter(property = "sitemap.include.regex", defaultValue = ".*\\.html?$")
	protected String include;

	/**
	 * The site's base URL, defaults to ${project.distributionManagement.site.url}
	 */
	@Parameter(property = "sitemap.url.base", defaultValue = "${project.distributionManagement.site.url}")
	protected URI baseUrl;

	/**
	 * Location of files to index, defaults to ${project.reporting.outputDirectory}
	 */
	@Parameter(property = "sitemap.dir.source", defaultValue = "${project.reporting.outputDirectory}")
	protected File sourceDir;

	/**
	 * Location of where the sitemap/index should be written, defaults to ${project.reporting.outputDirectory}
	 */
	@Parameter(property = "sitemap.dir.target", defaultValue = "${project.reporting.outputDirectory}")
	protected File targetDir;

	/**
	 * Skip execution of this plugin
	 */
	@Parameter(property = "sitemap.skip", defaultValue = "false")
	protected boolean skip;


	@SuppressWarnings("squid:S4784")  // SonarQube - questionable in context
	protected SitemapParameters parameters()
	{
		SitemapParameters parameters = new SitemapParameters(baseUrl, sourceDir.toPath(), targetDir.toPath());
		parameters.options().setGzip(gzip);
		parameters.options().setInclude(Pattern.compile(include));
		return parameters;
	}
}

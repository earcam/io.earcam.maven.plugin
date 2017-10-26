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

import static org.apache.maven.plugins.annotations.LifecyclePhase.SITE;

import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.earcam.unexceptional.Exceptional;
import io.earcam.utilitarian.site.sitemap.Sitemaps;

/**
 * Mojo for generating sitemaps
 */
@Mojo(name = SitemapMojo.NAME, requiresProject = true, threadSafe = true, inheritByDefault = true, defaultPhase = SITE, aggregator = false, requiresOnline = false)
public class SitemapMojo extends AbstractSitemapMojo {

	private static final Logger LOG = LoggerFactory.getLogger(SitemapMojo.class);
	static final String NAME = "sitemap";
	private static final String CATEGORY = '[' + NAME + ']';


	@Override
	public void execute() throws MojoExecutionException, MojoFailureException
	{
		if(skip) {
			LOG.debug("{} skip == true, skipping execution", CATEGORY);
			return;
		}

		LOG.debug("{} start", CATEGORY);

		Path file = Exceptional.apply(Sitemaps::create, parameters());

		Exceptional.apply(Files::lines, file)
				.forEach(f -> LOG.debug("{} Created {}", CATEGORY, f));

		LOG.debug("{} end", CATEGORY);
	}
}

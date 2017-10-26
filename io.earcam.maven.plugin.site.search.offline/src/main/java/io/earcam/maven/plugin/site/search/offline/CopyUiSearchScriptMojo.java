/*-
 * #%L
 * io.earcam.maven.plugin.site.search.offline
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
package io.earcam.maven.plugin.site.search.offline;

import static io.earcam.utilitarian.site.search.offline.Resources.UI_SCRIPT_SEARCH;
import static io.earcam.utilitarian.site.search.offline.Resources.UI_SCRIPT_SEARCH_FILE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.apache.maven.plugins.annotations.LifecyclePhase.SITE;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.earcam.unexceptional.Closing;
import io.earcam.utilitarian.site.search.offline.Resources;

/**
 * Copy a basic search script (Bootstrap friendly; requires JQuery, LunrJs and TypeAhead).
 *
 * For use with {@link JsSearchIndexMojo}
 */
@Mojo(name = CopyUiSearchScriptMojo.NAME, requiresProject = true, threadSafe = true, inheritByDefault = true, defaultPhase = SITE)
public class CopyUiSearchScriptMojo extends AbstractMojo {

	private static final Logger LOG = LoggerFactory.getLogger(CopyUiSearchScriptMojo.class);
	static final String NAME = "copy-ui-script";
	private static final String CATEGORY = "[search-" + NAME + ']';

	/**
	 * Where to write the copied ui search script
	 */
	@Parameter(property = "output.file", defaultValue = "${project.reporting.outputDirectory}/js/" + UI_SCRIPT_SEARCH_FILE)
	protected File outputFile;

	/**
	 * Skip execution of this plugin
	 */
	@Parameter(property = "skip", defaultValue = "false")
	protected boolean skip;


	@Override
	public void execute() throws MojoExecutionException, MojoFailureException
	{
		if(skip) {
			LOG.debug("{} skip == true, skipping execution", CATEGORY);
			return;
		}
		Closing.closeAfterAccepting(Resources::getResource, UI_SCRIPT_SEARCH, this::copy);
	}


	private void copy(InputStream resourceAsStream) throws IOException
	{
		outputFile.getParentFile().mkdirs();
		Files.copy(resourceAsStream, outputFile.toPath(), REPLACE_EXISTING);
	}
}

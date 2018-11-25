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

import static io.earcam.utilitarian.site.search.offline.Resources.PROPERTY_USE_SCRIPT_ENGINE;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.earcam.utilitarian.site.search.offline.ConfigurationModel;
import io.earcam.utilitarian.site.search.offline.ConfigurationModel.Crawling;
import io.earcam.utilitarian.site.search.offline.ConfigurationModel.Indexing;
import io.earcam.utilitarian.site.search.offline.Resources;
import io.earcam.utilitarian.site.search.offline.jsonb.JsonBind;

/**
 * Generate a <a href="https://lunrjs.com">lunr.js</a> index for offline/static web search.
 */
@Mojo(name = JsSearchIndexMojo.NAME, requiresProject = true, threadSafe = true, inheritByDefault = true, defaultPhase = LifecyclePhase.SITE)
public class JsSearchIndexMojo extends AbstractMojo {

	private static final Logger LOG = LoggerFactory.getLogger(JsSearchIndexMojo.class);

	static final String NAME = "index";

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	MavenProject project;

	/**
	 * Use the default settings for {@link #crawler} and {@link #indexer} (if true then
	 * {@link #crawler} or {@link #indexer} must not be specified)
	 */
	@Parameter(name = "useDefaultConfiguration", required = false, defaultValue = "true")
	boolean useDefaultConfiguration;

	/**
	 * The crawler configuration (({@link #useDefaultConfiguration} must be false)
	 */
	@Parameter(name = "crawler", required = false)
	ConfigurationModel.Crawling crawler;

	/**
	 * The indexer configuration (({@link #useDefaultConfiguration} must be false)
	 */
	@Parameter(name = "indexer", required = false)
	ConfigurationModel.Indexing indexer;

	/**
	 * Skip execution of this plugin
	 */
	@Parameter(property = "skip", defaultValue = "false")
	protected boolean skip;

	/**
	 * THe character encoding to use
	 */
	@Parameter(name = "outputCharset", required = true, defaultValue = "${project.reporting.outputEncoding}")
	String outputCharset;


	@Override
	public void execute() throws MojoExecutionException, MojoFailureException
	{
		if(skip) {
			LOG.warn("search index - skipping execution, as configured");
			return;
		}
		workaroundForMavenVersusJdk9AndNashorn();
		validate();

		if(useDefaultConfiguration) {
			configureDefaults();

		}
		JsSearchLifecycleParticipant.indexer(indexer);
		JsSearchLifecycleParticipant.addDocuments(crawler.build().documents());
	}


	/**
	 * Workaround for Maven vs JDK>=9 issue https://issues.apache.org/jira/browse/MNG-6275
	 * 
	 * Where Nashorn cannot be loaded via SPI (had previously tried a zero-code
	 * workaround of defining {@code META-INF/services/javax.script.ScriptEngineFactory}
	 * in this module - but that wasn't picked up).
	 * 
	 * It's public as we need to call it in the verify.groovy integration tests
	 */
	public static void workaroundForMavenVersusJdk9AndNashorn()
	{
		System.setProperty(PROPERTY_USE_SCRIPT_ENGINE, "jdk.nashorn.api.scripting.NashornScriptEngineFactory");
	}


	private void validate()
	{
		if(useDefaultConfiguration && (crawler != null || indexer != null)) {
			throw new IllegalStateException(
					"Either specifiy the 'crawler' AND 'indexer' configuration OR set 'useDefaultConfiguration' to 'true'");
		}
	}


	private void configureDefaults()
	{
		Map<String, String> searchReplace = searchReplaceMap();

		Charset charset = Charset.forName(outputCharset);
		String crawlerJson = Resources.getResource(Resources.DEFAULT_CRAWLER_JSON, charset, searchReplace);
		String indexerJson = Resources.getResource(Resources.DEFAULT_INDEXER_JSON, charset, searchReplace);
		crawler = JsonBind.readJson(crawlerJson, Crawling.class);
		indexer = JsonBind.readJson(indexerJson, Indexing.class);
	}


	private Map<String, String> searchReplaceMap()
	{
		Map<String, String> searchReplace = new HashMap<>();
		searchReplace.put("${outputCharset}", outputCharset);
		searchReplace.put("${jsonDir}", project.getModel().getReporting().getOutputDirectory());
		searchReplace.put("${baseDir}", project.getModel().getReporting().getOutputDirectory());
		searchReplace.put("${baseUri}", project.getModel().getDistributionManagement().getSite().getUrl());
		return searchReplace;
	}
}

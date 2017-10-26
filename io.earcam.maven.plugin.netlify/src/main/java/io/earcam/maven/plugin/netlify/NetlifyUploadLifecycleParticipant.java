/*-
 * #%L
 * io.earcam.maven.plugin.netlify
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
package io.earcam.maven.plugin.netlify;

import static io.earcam.unexceptional.Exceptional.uri;
import static java.util.stream.Collectors.toMap;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.earcam.utilitarian.site.deploy.netlify.Netlify;
import io.earcam.utilitarian.site.deploy.netlify.Site;

@Component(role = AbstractMavenLifecycleParticipant.class, hint = "upload", instantiationStrategy = "singleton")
public class NetlifyUploadLifecycleParticipant extends AbstractMavenLifecycleParticipant {

	private static final String SYSTEM_PROPERTY_TEST_URL = "netlify.test.url";
	private static final String CATEGORY = "[netlify.upload] ";
	private static final Logger LOG = LoggerFactory.getLogger(NetlifyUploadLifecycleParticipant.class);
	private static volatile boolean shouldRun = false;
	private static NetlifyUploadMojo mojo;


	@Override
	public void afterSessionEnd(MavenSession session) throws MavenExecutionException
	{
		if(session.getResult().hasExceptions()) {
			warn("{} extension: not running due to previous build errors", CATEGORY);
			return;
		}
		LOG.debug("{} extension: configured to run: {}", CATEGORY, shouldRun);
		if(shouldRun) {
			List<MavenProject> projects = session.getTopLevelProject().getCollectedProjects();
			projects.add(session.getTopLevelProject());
			uploads(projects);
		}
	}


	static void shouldRun()
	{
		shouldRun = true;
	}


	private void uploads(List<MavenProject> projects)
	{
		process(mojo, projects);
	}


	private void process(NetlifyUploadMojo mojo, List<MavenProject> projects)
	{
		Netlify netlify = createNetlify(mojo);
		Site site = siteForMojo(mojo, netlify);

		Map<String, Path> baseDirs = projects.stream()
				.collect(toMap(
						this::uriPath,
						p -> Paths.get(p.getModel().getReporting().getOutputDirectory())));

		debug("site.name: {}, baseDirs: {}", site.name(), baseDirs);

		netlify.deployZip(site.name(), baseDirs);
	}


	private Netlify createNetlify(NetlifyUploadMojo mojo)
	{
		String testUrl = System.getProperty(SYSTEM_PROPERTY_TEST_URL);
		return (isEmpty(testUrl)) ? new Netlify(mojo.accessToken()) : new Netlify(mojo.accessToken(), testUrl);
	}


	private String uriPath(MavenProject project)
	{
		String path = uri(project.getModel().getDistributionManagement().getSite().getUrl()).getPath();
		debug("project {} has path {}", project, path);
		return "//".equals(path) ? "" : path;
	}


	private Site siteForMojo(NetlifyUploadMojo mojo, Netlify netlify)
	{
		Site site = new Site();

		if(isEmpty(mojo.siteName)) {
			site = netlify.create(site);
			debug("The newly created site name is : {}", site.name());
		} else {
			Optional<Site> existingSite = netlify.findSiteForName(mojo.siteName);
			if(existingSite.isPresent()) {
				site = existingSite.get();
			} else {
				site.setName(mojo.siteName);
				site.setCustomDomain(mojo.customDomain);
				site = netlify.create(site);
			}
		}
		debug("The site ID is: {}", site.id());
		return site;
	}


	private static boolean isEmpty(String string)
	{
		return string == null || "".equals(string);
	}


	public static void addPlugin(NetlifyUploadMojo plugin)
	{
		debug("Adding {} to netlify upload", plugin.project);
		mojo = plugin;
	}


	static void debug(String msg, Object... parameters)
	{
		String format = CATEGORY + msg;
		LOG.debug(format, parameters);
	}


	static void warn(String msg, Object... parameters)
	{
		String format = CATEGORY + msg;
		LOG.warn(format, parameters);
	}
}

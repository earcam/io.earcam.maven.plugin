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

import static org.apache.maven.plugins.annotations.InstantiationStrategy.SINGLETON;
import static org.apache.maven.plugins.annotations.LifecyclePhase.SITE;

import java.util.Objects;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;

/**
 * <a href="https://netlify.com">Netlify</a> site deploy plugin
 */
@Mojo(name = "upload", requiresProject = true, threadSafe = true, inheritByDefault = false, defaultPhase = SITE, instantiationStrategy = SINGLETON)
public class NetlifyUploadMojo extends AbstractMojo {

	@Parameter(defaultValue = "${settings}", readonly = true, required = true)
	Settings settings;

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	MavenProject project;

	/**
	 * Server ID, references a {@code <server>} in your {@code ~/.m2/settings.xml}
	 */
	@Parameter(property = "deploy.netlify.server.id", defaultValue = "${project.distributionManagement.site.id}", required = true)
	String serverId;

	/**
	 * The unique site name, used as a Netlify identifier and subdomain, e.g.
	 * given {@code <siteName>foo</siteName>}, then {@code URL == //foo.netlify.com}
	 */
	@Parameter(property = "deploy.netlify.site.name", required = false)
	String siteName;

	/**
	 * Configure a custom domain, e.g. given {@code <siteName>foo.acme.com</siteName>} configure a
	 * DNS CNAME entry for your subdomain <b>foo.acme.com</b> to point to <b>acme-foo.netlify.com.</b>
	 * and set this to {@code <customDomain>foo.acme.com</customDomain>}
	 */
	@Parameter(property = "deploy.netlify.domain.custom", required = false)
	String customDomain;

	/**
	 * Fails build if site (as identified by {@code <siteName>}) does not already exist and this
	 * property is set to {@code false}
	 */
	@Parameter(property = "deploy.netlify.site.create", defaultValue = "true", required = false)
	boolean createSiteIfNotExists;

	/**
	 * Skip execution of this plugin
	 */
	@Parameter(property = "deploy.netlify.skip", defaultValue = "false")
	boolean skip;


	@Override
	public void execute() throws MojoExecutionException, MojoFailureException
	{
		if(skip) {
			return;
		}
		NetlifyUploadLifecycleParticipant.shouldRun();
		NetlifyUploadLifecycleParticipant.addPlugin(this);
	}


	String accessToken()
	{
		String token = server().getPassword();
		Objects.requireNonNull(token, "Did not find Netlify access token as the '" + serverId + "' server password in settings.xml");
		return token;
	}


	private Server server()
	{
		Server server = settings.getServer(serverId);
		Objects.requireNonNull(server, "server with id '" + serverId + "' not found in settings.xml");
		return server;
	}


	/**
	 * @deprecated unused
	 * @return {@code null}
	 */
	// Scheduled for delete on next major release, revapi seems overzealous
	// in applying semver rules to projects versioned less than 1.0.0
	@SuppressWarnings("squid:S1133")
	@Deprecated
	public MavenProject getProject()
	{
		throw new UnsupportedOperationException("deprecated; never used");
	}
}

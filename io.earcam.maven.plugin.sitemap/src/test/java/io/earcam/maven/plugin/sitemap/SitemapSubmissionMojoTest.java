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

import static io.earcam.utilitarian.net.FreePortFinder.findFreePort;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.Before;
import org.junit.Test;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import io.earcam.unexceptional.Exceptional;
import io.earcam.utilitarian.net.FreePortFinder;

@SuppressWarnings("restriction")
public class SitemapSubmissionMojoTest {

	private static final MavenSession MOCK_MAVEN_SESSION = mock(MavenSession.class, RETURNS_DEEP_STUBS);


	@Before
	public void before()
	{
		SitemapSubmissionMavenLifecycleParticipant.run = false;
		SitemapSubmissionMavenLifecycleParticipant.baseUrl = null;
		SitemapSubmissionMavenLifecycleParticipant.targetDir = null;
		SitemapSubmissionMavenLifecycleParticipant.hosts = null;
	}


	@Test
	public void skip() throws URISyntaxException, MojoExecutionException, MojoFailureException, MavenExecutionException
	{
		SitemapSubmissionMojo mojo = new SitemapSubmissionMojo();
		mojo.baseUrl = new URI("http://acme.com");
		mojo.targetDir = Paths.get(".", "target").toFile();
		mojo.hosts = Arrays.asList("http://google.com");
		mojo.skip = true;

		mojo.execute();
		new SitemapSubmissionMavenLifecycleParticipant().afterSessionEnd(MOCK_MAVEN_SESSION);

		assertThat(SitemapSubmissionMavenLifecycleParticipant.run, is(false));
		assertThat(SitemapSubmissionMavenLifecycleParticipant.baseUrl, is(nullValue()));
		assertThat(SitemapSubmissionMavenLifecycleParticipant.targetDir, is(nullValue()));
		assertThat(SitemapSubmissionMavenLifecycleParticipant.hosts, is(nullValue()));
	}


	@Test
	public void doesNotRunOnError() throws URISyntaxException, MojoExecutionException, MojoFailureException, MavenExecutionException
	{
		MavenSession mavenSession = mock(MavenSession.class);
		MavenExecutionResult result = mock(MavenExecutionResult.class);

		given(mavenSession.getResult()).willReturn(result);
		given(result.hasExceptions()).willReturn(true);

		SitemapSubmissionMojo mojo = new SitemapSubmissionMojo();
		mojo.baseUrl = new URI("http://acme.com");
		mojo.targetDir = Paths.get(".", "target").toFile();
		mojo.hosts = Arrays.asList("http://localhost:" + findFreePort());

		mojo.execute();
		new SitemapSubmissionMavenLifecycleParticipant().afterSessionEnd(mavenSession);

		assertThat(SitemapSubmissionMavenLifecycleParticipant.run, is(true));
	}


	@Test
	public void submit() throws IOException, MojoExecutionException, MojoFailureException, MavenExecutionException
	{
		int port = FreePortFinder.findFreePort();

		List<String> submittedPaths = new ArrayList<>();

		HttpServer server = createHttpServer(port, submittedPaths);
		server.start();

		Path dir = Paths.get(".", "test", "resources", "dir");
		dir.toFile().mkdirs();
		Path sitemap = dir.resolve("sitemap.xml");
		Files.write(sitemap, "irrelevant ATM".getBytes(UTF_8));

		Files.write(dir.resolve(".io.earcam.utilitarian.site.sitemap.index.list"), sitemap.toAbsolutePath().toString().getBytes(UTF_8));

		SitemapSubmissionMojo mojo = new SitemapSubmissionMojo();
		mojo.baseUrl = Exceptional.uri("https://domain.acme.com/module/");
		mojo.targetDir = dir.toFile();
		mojo.hosts = Arrays.asList("http://localhost:" + port);

		mojo.execute();
		new SitemapSubmissionMavenLifecycleParticipant().afterSessionEnd(MOCK_MAVEN_SESSION);

		assertThat(submittedPaths, contains(endsWith(":" + port + "/ping?sitemap=https://domain.acme.com/module/sitemap.xml")));

		server.stop(0);
	}


	static HttpServer createHttpServer(int port, List<String> submittedPaths) throws IOException
	{
		HttpServer server = HttpServer.create(new InetSocketAddress("localhost", port), port);
		HttpContext context = server.createContext("/");
		context.setHandler(new HttpHandler() {

			@Override
			public void handle(HttpExchange exchange) throws IOException
			{
				String pfft = exchange.getProtocol() + "://" + exchange.getLocalAddress().getHostString() + ':'
						+ exchange.getLocalAddress().getPort() + exchange.getRequestURI().getPath()
						+ '?'
						+ exchange.getRequestURI().getQuery();

				submittedPaths.add(pfft);
				drain(exchange.getRequestBody());

				exchange.sendResponseHeaders(200, 0);
				exchange.getResponseBody().close();
			}


			private void drain(InputStream input) throws IOException
			{
				while(input.read() != -1);
			}
		});
		return server;
	}
}

/*-
 * #%L
 * io.earcam.maven.plugin.ramdisk
 * %%
 * Copyright (C) 2018 earcam
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
package io.earcam.maven.plugin.ramdisk;

import static io.earcam.maven.plugin.ramdisk.RamdiskMojo.PROPERTY_SKIP;
import static java.time.ZoneId.systemDefault;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.io.FileMatchers.anExistingFileOrDirectory;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Properties;

import org.apache.maven.model.Build;
import org.apache.maven.project.MavenProject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.earcam.utilitarian.io.file.RecursiveFiles;

public class CleanUpMojoTest {

	private Path baseDir;

	private Build build;

	private Properties properties;
	private MavenProject project;
	private CleanUpMojo mojo;


	@Before
	public void setUp()
	{
		baseDir = Paths.get(".", "target", "fakeModuleCleanUpRoot_" + LocalDateTime.now(systemDefault()) + "_" + randomUUID());

		build = mock(Build.class);
		project = mock(MavenProject.class);

		baseDir.toFile().mkdirs();
		mojo = new CleanUpMojo();

		mojo.project = project;

		when(project.getBuild()).thenReturn(build);
		when(build.getDirectory()).thenReturn(baseDir.resolve("target").toAbsolutePath().toString());

		properties = new Properties();
		when(project.getProperties()).thenReturn(properties);
	}


	@After
	public void tearDown() throws IOException
	{
		if(baseDir != null && baseDir.toFile().exists()) {
			RecursiveFiles.delete(baseDir);
		}
	}


	@Test
	public void removesLiveSymLinkAndDestination() throws IOException
	{
		Path fakeTmpFs = baseDir.resolve("fake_tmpfs");
		Path targetDir = baseDir.resolve("target");
		Files.createDirectories(fakeTmpFs);
		Files.createSymbolicLink(targetDir, fakeTmpFs);

		mojo.execute();

		assertThat(fakeTmpFs.toFile(), is(not(anExistingFileOrDirectory())));
		assertThat(targetDir.toFile(), is(not(anExistingFileOrDirectory())));
		assertThat(properties, hasEntry(PROPERTY_SKIP, "true"));
	}


	@Test
	public void removesDeadSymLinkAndDestination() throws IOException
	{
		Path fakeTmpFs = baseDir.resolve("fake_tmpfs");
		Path targetDir = baseDir.resolve("target");
		Files.createDirectories(fakeTmpFs);
		Files.createSymbolicLink(targetDir, fakeTmpFs);
		RecursiveFiles.delete(fakeTmpFs);

		mojo.execute();

		assertThat(fakeTmpFs.toFile(), is(not(anExistingFileOrDirectory())));
		assertThat(targetDir.toFile(), is(not(anExistingFileOrDirectory())));
		assertThat(properties, hasEntry(PROPERTY_SKIP, "true"));
	}


	@Test
	public void doesNotRemoveTargetWhenDirectory() throws IOException
	{
		Path targetDir = baseDir.resolve("target");
		Files.createDirectories(targetDir.resolve("classes"));

		mojo.execute();

		assertThat(targetDir.toFile(), is(anExistingFileOrDirectory()));
		assertThat(properties, hasEntry(PROPERTY_SKIP, "true"));
	}
}

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
import static io.earcam.maven.plugin.ramdisk.RamdiskBuildExtension.relativePathFor;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.ZoneId.systemDefault;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.io.FileMatchers.anExistingDirectory;
import static org.hamcrest.io.FileMatchers.anExistingFile;
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
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.earcam.unexceptional.Exceptional;
import io.earcam.utilitarian.io.file.RecursiveFiles;

public class RamdiskMojoTest {

	static {
		System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "trace");
	}

	private final Path baseDir = Paths.get(".", "target", "fakeModuleRoot_" + LocalDateTime.now(systemDefault()) + "_" + randomUUID());

	private final Path buildDir = baseDir.resolve("target");
	private MavenProject project;
	private final Build build = mock(Build.class);

	private RamdiskMojo mojo;

	private Path moduleTargetDir;


	@Before
	public void setUp()
	{
		project = mock(MavenProject.class);

		baseDir.toFile().mkdirs();
		mojo = new RamdiskMojo();

		mojo.project = project;

		when(project.getGroupId()).thenReturn(randomUUID().toString());
		when(project.getArtifactId()).thenReturn(randomUUID().toString());
		when(project.getVersion()).thenReturn("0.1.0");
		when(project.getPackaging()).thenReturn("jar");

		when(project.getBuild()).thenReturn(build);
		when(project.getBasedir()).thenReturn(baseDir.toFile());
		when(build.getDirectory()).thenReturn(buildDir.toAbsolutePath().toString());

		when(build.getOutputDirectory()).thenReturn(buildDir.resolve("classes").toAbsolutePath().toString());
		when(build.getTestOutputDirectory()).thenReturn(buildDir.resolve("test-classes").toAbsolutePath().toString());

		when(project.getProperties()).thenReturn(new Properties());
	}


	@After
	public void tearDown() throws IOException
	{
		if(moduleTargetDir != null && moduleTargetDir.toFile().exists()) {
			RecursiveFiles.delete(moduleTargetDir);
		}
	}


	@Test
	public void withNoScm()
	{
		mojo.execute();

		moduleTargetDir = moduleTargetDir();

		assertThat(moduleTargetDir.toFile(), is(anExistingDirectory()));

		assertThat(buildDir, is(aSoftlinkPointingTo(moduleTargetDir)));
	}


	private static Matcher<Path> aSoftlinkPointingTo(Path linkTarget)
	{
		return new TypeSafeMatcher<Path>(Path.class) {

			@Override
			public void describeTo(Description description)
			{
				description.appendText("a softlink pointing to ")
						.appendValue(linkTarget);
			}


			@Override
			protected boolean matchesSafely(Path item)
			{
				return Files.isSymbolicLink(item)
						&& Exceptional.apply(Files::readSymbolicLink, item).equals(linkTarget);
			}
		};
	}


	private Path moduleTargetDir()
	{
		return RamdiskBuildExtension.findTmpFs().resolve(relativePathFor(project));
	}


	@Test
	public void whenTargetDirectoryAlreadyExistsThenContentsAreCopiedToSoftLink() throws IOException
	{
		buildDir.toFile().mkdirs();
		Files.write(buildDir.resolve("some.file"), "some.content".getBytes(UTF_8));

		mojo.execute();

		moduleTargetDir = moduleTargetDir();

		assertThat(moduleTargetDir.toFile(), is(anExistingDirectory()));

		assertThat(buildDir, is(aSoftlinkPointingTo(moduleTargetDir)));

		assertThat(moduleTargetDir.resolve("some.file").toFile(), is(anExistingFile()));
	}


	@Test
	public void whenTargetDirectoryAndSoftLinkTargetAlreadyExistThenContentsAreCopied() throws IOException
	{
		buildDir.toFile().mkdirs();
		Files.write(buildDir.resolve("some.file"), "some.content".getBytes(UTF_8));
		moduleTargetDir = moduleTargetDir();
		moduleTargetDir.toFile().mkdirs();

		mojo.execute();

		assertThat(moduleTargetDir.toFile(), is(anExistingDirectory()));

		assertThat(buildDir, is(aSoftlinkPointingTo(moduleTargetDir)));

		assertThat(moduleTargetDir.resolve("some.file").toFile(), is(anExistingFile()));
	}


	@Test
	public void whenTargetDirectoryIsASoftLinkThenNothingIsChanged() throws IOException
	{
		moduleTargetDir = moduleTargetDir();
		moduleTargetDir.toFile().mkdirs();

		Files.createSymbolicLink(buildDir, moduleTargetDir);

		mojo.execute();

		assertThat(moduleTargetDir.toFile(), is(anExistingDirectory()));

		assertThat(buildDir, is(aSoftlinkPointingTo(moduleTargetDir)));
	}


	@Test
	public void whenProjectSkipPropertyIsTrueThenSkipsExecution()
	{
		Properties properties = new Properties();
		properties.put(PROPERTY_SKIP, "true");
		when(project.getProperties()).thenReturn(properties);

		mojo.execute();

		moduleTargetDir = moduleTargetDir();

		assertThat(moduleTargetDir.toFile(), is(not(anExistingDirectory())));

		assertThat(buildDir.toFile(), is(not(anExistingFileOrDirectory())));
	}
}

/*-
 * #%L
 * io.earcam.maven.plugin.jlinkage
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
package io.earcam.maven.plugin.jlinkage;

import static io.earcam.instrumental.archive.Archive.archive;
import static io.earcam.instrumental.archive.AsJar.asJar;
import static io.earcam.maven.plugin.jlinkage.JlinkageMojo.JDK_MODULE_LOCALEDATA;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.ToolchainManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.acme.Hello;
import com.acme.IDependOnInstrumentation;

import io.earcam.instrumental.reflect.Resources;
import io.earcam.utilitarian.io.IoStreams;

public class JlinkageMojoTest {

	// Report possible JDK bug? If a library path contains ":" (the Unix path separator char but as a literal) then
	// private final Path testRoot = Paths.get(".", "target", "generate_" + LocalDateTime.now(systemDefault()) + "_" +
	// UUID.randomUUID());
	private final Path testRoot = Paths.get(".", "target", "generate_" + UUID.randomUUID());

	private MavenProject project = mock(MavenProject.class);
	private Build build = mock(Build.class);
	private ToolchainManager toolchainManager = mock(ToolchainManager.class);
	private Toolchain toolchain = mock(Toolchain.class);
	private Artifact dependency = mock(Artifact.class);

	private JlinkageMojo mojo = new JlinkageMojo();
	private Path dependencyJar;


	@Before
	public void before()
	{
		testRoot.toFile().mkdirs();

		dependencyJar = archive()
				.configured(asJar())
				.with(Hello.class)
				.to(testRoot.resolve(Paths.get("fake-repo", "dependency.jar")));

		when(project.getBuild()).thenReturn(build);

		// when(toolchainManager.getToolchainFromBuildContext(Mockito.eq("jdk"), Mockito.any())).thenReturn(toolchain);
		when(toolchainManager.getToolchains(any(), eq("jdk"), any())).thenReturn(singletonList(toolchain));

		when(toolchain.findTool("jlink")).thenReturn("/usr/lib/jvm/java-11-oracle/bin/jlink");

		when(dependency.getFile()).thenReturn(dependencyJar.toFile());
		when(project.getArtifacts()).thenReturn(Collections.singleton(dependency));

		mojo.project = project;
		mojo.toolchainManager = toolchainManager;

		mojo.outputDirectory = testRoot.toFile();
		mojo.outputName = "jlinked-jvm";
		mojo.compress = Compress.ZIP;
	}


	@Test
	public void throwsWhenJdkToolchainUnavailable() throws Exception
	{
		when(build.getOutputDirectory()).thenReturn("/dev/null/nothing/to/see/here");
		when(toolchainManager.getToolchains(any(), eq("jdk"), any())).thenReturn(emptyList());
		try {
			mojo.execute();
			fail();
		} catch(IllegalStateException e) {}
	}


	@Test
	public void whenVendorSpecifiedThenIsAddedToToolchainRequirements() throws Exception
	{
		when(build.getOutputDirectory()).thenReturn("/dev/null/nothing/to/see/here");

		mojo.toolchainJdkVendor = "skynet-jdk";

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, String>> requirements = ArgumentCaptor.forClass(Map.class);

		when(toolchainManager.getToolchains(any(), eq("jdk"), requirements.capture())).thenReturn(singletonList(toolchain));
		mojo.execute();

		Map<String, String> required = requirements.getValue();
		assertThat(required, hasEntry("vendor", "skynet-jdk"));
	}


	@Test
	public void whenVendorIsNotSpecifiedThenIsNotAddedToToolchainRequirements() throws Exception
	{
		when(build.getOutputDirectory()).thenReturn("/dev/null/nothing/to/see/here");

		mojo.toolchainJdkVendor = null;

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, String>> requirements = ArgumentCaptor.forClass(Map.class);

		when(toolchainManager.getToolchains(any(), eq("jdk"), requirements.capture())).thenReturn(singletonList(toolchain));
		mojo.execute();

		Map<String, String> required = requirements.getValue();
		assertThat(required, not(hasKey("vendor")));
	}


	@Test
	public void singleSimpleDependency() throws Exception
	{
		when(build.getOutputDirectory()).thenReturn("/dev/null/nothing/to/see/here");

		mojo.endian = Endian.LITTLE;  // Linux x86

		mojo.execute();

		String javaExec = testRoot.resolve(Paths.get(mojo.outputName, "bin", "java")).toString();
		Process process = new ProcessBuilder(javaExec, "--class-path", dependencyJar.toAbsolutePath().toString(), Hello.class.getCanonicalName())
				.inheritIO()
				.start();

		int exitCode = process.waitFor();

		assertThat(exitCode, is(equalTo(Hello.EXIT_CODE)));
	}


	@Test
	public void simpleDependencyAndModuleArtifact() throws Exception
	{
		Path projectClasses = testRoot
				.resolve("classes" + File.separatorChar + IDependOnInstrumentation.class.getPackage().getName().replace('.', File.separatorChar));
		projectClasses.toFile().mkdirs();
		Files.write(projectClasses.resolve(IDependOnInstrumentation.class.getSimpleName() + ".class"), Resources.classAsBytes(IDependOnInstrumentation.class));

		when(build.getOutputDirectory()).thenReturn(projectClasses.toString());

		mojo.execute();

		String javaExec = testRoot.resolve(Paths.get(mojo.outputName, "bin", "java")).toString();
		Process process = new ProcessBuilder(javaExec, "--list-modules")
				.start();

		int exitCode = process.waitFor();
		String output = new String(IoStreams.readAllBytes(process.getInputStream()), UTF_8);

		assertThat(output, allOf(containsString("java.base"), containsString("java.instrument")));

		assertThat(exitCode, is(0));
	}


	@Test
	public void testDependencyIsIgnored() throws Exception
	{
		dependencyJar = archive()
				.configured(asJar())
				.with(IDependOnInstrumentation.class)
				.to(testRoot.resolve(Paths.get("fake-repo", "dependency.jar")));

		when(dependency.getScope()).thenReturn("test");

		Path projectClasses = testRoot.resolve("classes" + File.separatorChar + Hello.class.getPackage().getName().replace('.', File.separatorChar));
		projectClasses.toFile().mkdirs();
		Files.write(projectClasses.resolve(Hello.class.getSimpleName() + ".class"), Resources.classAsBytes(Hello.class));

		when(build.getOutputDirectory()).thenReturn(projectClasses.toString());

		mojo.execute();

		String javaExec = testRoot.resolve(Paths.get(mojo.outputName, "bin", "java")).toString();
		Process process = new ProcessBuilder(javaExec, "--list-modules")
				.start();

		int exitCode = process.waitFor();
		String output = new String(IoStreams.readAllBytes(process.getInputStream()), UTF_8);

		assertThat(output, allOf(containsString("java.base"), not(containsString("java.instrument"))));

		assertThat(exitCode, is(0));
	}


	@Test
	public void addLocalesAddsLocaleJdkModule() throws Exception
	{
		when(build.getOutputDirectory()).thenReturn("/dev/null/nothing/to/see/here");

		mojo.locales = new String[] { "en", "ja" };

		mojo.execute();

		String javaExec = testRoot.resolve(Paths.get(mojo.outputName, "bin", "java")).toString();
		Process process = new ProcessBuilder(javaExec, "--list-modules")
				.start();

		int exitCode = process.waitFor();
		String output = new String(IoStreams.readAllBytes(process.getInputStream()), UTF_8);

		assertThat(output, allOf(containsString("java.base"), containsString(JDK_MODULE_LOCALEDATA)));

		assertThat(exitCode, is(0));
	}


	@Test
	public void borksWhenGivenNonsenseAdditionalArguments() throws Exception
	{
		when(build.getOutputDirectory()).thenReturn("/dev/null/nothing/to/see/here");

		mojo.additionalArguments = Arrays.asList("--this-should-make-jlink-sick", "--enough-to-exit-with-non-zero");

		try {
			mojo.execute();
			Assert.fail();
		} catch(MojoExecutionException e) {
			assertThat(e.getMessage(), allOf(containsString("jlink"), containsString("exitcode")));
		}
	}

}

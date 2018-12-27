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

import static io.earcam.instrumental.module.auto.Reader.reader;
import static io.earcam.maven.plugin.jlinkage.Compress.NONE;
import static io.earcam.maven.plugin.jlinkage.Endian.NATIVE;
import static java.io.File.pathSeparator;
import static java.io.File.separator;
import static java.util.Locale.ROOT;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import static org.apache.maven.artifact.Artifact.SCOPE_TEST;
import static org.apache.maven.plugins.annotations.LifecyclePhase.PACKAGE;
import static org.apache.maven.plugins.annotations.ResolutionScope.RUNTIME_PLUS_SYSTEM;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.ToolchainManager;

import io.earcam.instrumental.archive.jpms.auto.JdkModules;
import io.earcam.instrumental.module.auto.Reader;
import io.earcam.instrumental.module.jpms.ModuleInfo;
import io.earcam.unexceptional.Exceptional;

@Mojo(name = JlinkageMojo.NAME, requiresProject = true, threadSafe = true, inheritByDefault = false, defaultPhase = PACKAGE, requiresDependencyResolution = RUNTIME_PLUS_SYSTEM)
public class JlinkageMojo extends AbstractJlinkMojo {

	static final String JDK_MODULE_LOCALEDATA = "jdk.localedata";

	static final String JLINK = "jlink";
	static final String NAME = JLINK;

	@Parameter(required = false, defaultValue = "NONE")
	protected Compress compress = NONE;

	@Parameter(required = false, defaultValue = "NATIVE")
	protected Endian endian = NATIVE;

	@Parameter(required = false, defaultValue = "false")
	protected boolean excludeHeaderFiles = false;

	@Parameter(required = false, defaultValue = "true")
	protected boolean excludeManPages = true;

	@Parameter(required = false, defaultValue = "false")
	protected boolean stripDebug = false;

	@Parameter(required = false)
	protected List<String> additionalArguments = new ArrayList<>();

	@Parameter(required = false, defaultValue = "true")
	protected boolean ignoreSigningInformation = true;

	@Parameter(required = false)
	protected String[] locales = {};

	@Parameter(required = false, defaultValue = "${project.build.directory}")
	protected File outputDirectory;

	@Parameter(required = false, defaultValue = "${project.artifactId}")
	protected String outputName;

	@Parameter(required = false)
	protected List<String> modulePaths = Collections.emptyList();

	@Parameter(required = false, defaultValue = "11")
	protected int toolchainJdkVersion = 11;

	/**
	 * 
	 */
	@Parameter(required = false)
	protected String toolchainJdkVendor;

	@Parameter(required = true, defaultValue = "${session}", readonly = true)
	protected MavenSession session;

	@Component
	protected ToolchainManager toolchainManager;


	protected JlinkageMojo()
	{
		super(JLINK);
	}


	@Override
	protected void exec() throws MojoExecutionException, MojoFailureException
	{
		List<String> jlinkCommand = new ArrayList<>();

		appendRequiredJdkModules(jlinkCommand);
		appendOptions(jlinkCommand);
		prependJlinkExecutable(jlinkCommand);

		runJlink(jlinkCommand);
	}


	private void appendRequiredJdkModules(List<String> jlinkCommand)
	{
		Set<String> imports = new HashSet<>();
		process(imports);

		JdkModules jdkModuleMapper = new JdkModules(toolchainJdkVersion);
		Set<ModuleInfo> moduleInfos = jdkModuleMapper.moduleRequiredFor("anon!", imports.iterator());

		Set<String> jdkModules = moduleInfos.stream()
				.map(ModuleInfo::name)
				.collect(toSet());

		if(locales.length != 0) {
			jdkModules.add(JDK_MODULE_LOCALEDATA);
		}

		logDebug("Adding JDK modules: {}", jdkModules);

		jlinkCommand.add("--add-modules");
		jlinkCommand.add(jdkModules.stream()
				.collect(joining(",")));
	}


	private void process(Set<String> imports)
	{
		Reader reader = createReader(imports);
		processThisArtifact(reader);
		processDependencyArtifacts(reader);
	}


	private Reader createReader(Set<String> imports)
	{
		return reader()
				.ignoreAnnotations()
				.setImportedTypeReducer(Reader::typeToPackageReducer)
				.setImportingTypeReducer(Reader::typeToPackageReducer)
				.addImportListener((importer, importee) -> imports.addAll(importee));
	}


	private void processThisArtifact(Reader reader)
	{
		File thisArtifact = new File(project.getBuild().getOutputDirectory());
		if(thisArtifact.isDirectory()) {
			logDebug("Will analyse this project: {}", thisArtifact);
			Exceptional.accept(reader::processJar, thisArtifact.toPath());
		}
	}


	private void processDependencyArtifacts(Reader reader)
	{
		logDebug("Will analyse these dependencies: {}", project.getArtifacts());

		project.getArtifacts().stream()
				.filter(JlinkageMojo::isNotTestScoped)
				.map(Artifact::getFile)
				.map(File::toPath)
				.peek(f -> logDebug("Analysing {}", f))
				.forEach(Exceptional.uncheckConsumer(reader::processJar));
	}


	private static boolean isNotTestScoped(Artifact artifact)
	{
		return !SCOPE_TEST.equals(artifact.getScope());
	}


	private void appendOptions(List<String> jlinkCommand)
	{
		if(!NATIVE.equals(endian)) {
			jlinkCommand.add("--endian");
			jlinkCommand.add(endian.name().toLowerCase(ROOT));
		}
		jlinkCommand.add("--compress=" + compress.ordinal());

		booleanOption(jlinkCommand, ignoreSigningInformation, "--ignore-signing-information");
		booleanOption(jlinkCommand, excludeHeaderFiles, "--no-header-files");
		booleanOption(jlinkCommand, excludeManPages, "--no-man-pages");
		booleanOption(jlinkCommand, stripDebug, "--strip-debug");
		booleanOption(jlinkCommand, verbose, "--verbose");

		if(locales.length != 0) {
			jlinkCommand.add("--include-locales=" + String.join(",", locales));
		}
		jlinkCommand.addAll(additionalArguments);

		jlinkCommand.add("--output");
		jlinkCommand.add(outputPath(outputDirectory, outputName));
	}


	private static void booleanOption(List<String> command, boolean choice, String option)
	{
		if(choice) {
			command.add(option);
		}
	}


	private static String outputPath(File directory, String fileName)
	{
		return directory + separator + fileName;
	}


	private void prependJlinkExecutable(List<String> jlinkCommand)   // badly named - also slaps on the module-path
	{
		logDebug("session: {}", session);

		String jlinkExecutable = findJlinkExecutable();

		jlinkCommand.add(0, jlinkExecutable);

		jlinkCommand.add(1, "--module-path");
		String jdkJmods = Paths.get(jlinkExecutable).getParent().getParent().resolve("jmods").toString();
		String jmods = Stream.concat(Stream.of(jdkJmods), modulePaths.stream()).collect(joining(pathSeparator));
		jlinkCommand.add(2, jmods);
	}


	private String findJlinkExecutable()
	{
		Toolchain toolchain = lookupToolchain();
		String jlinkExecutable = toolchain.findTool(JLINK);
		logDebug("jlinkExecutable: {}", jlinkExecutable);
		return jlinkExecutable;
	}


	private Toolchain lookupToolchain()
	{
		Map<String, String> requirements = new HashMap<>();
		requirements.put("version", Integer.toString(toolchainJdkVersion));
		if(toolchainJdkVendor != null) {
			requirements.put("vendor", toolchainJdkVendor);
		}
		List<Toolchain> toolchains = toolchainManager.getToolchains(session, "jdk", requirements);
		if(toolchains.isEmpty()) {
			throw new IllegalStateException("Toolchains not present");
		}
		logDebug("selecting first available from toolchains: {}", toolchains);
		return toolchains.get(0);
	}


	private void runJlink(List<String> jlinkCommand) throws MojoExecutionException
	{
		logInfo("will run: {}", jlinkCommand);

		ProcessBuilder procBuilder = new ProcessBuilder(jlinkCommand)
				.inheritIO();
		Process process = Exceptional.get(procBuilder::start);

		int exitCode = Exceptional.get(process::waitFor);
		if(exitCode != 0) {
			throw new MojoExecutionException("non zero exitcode for jlink: " + exitCode);
		}
		logDebug("jlink command completed successfully");
	}
}

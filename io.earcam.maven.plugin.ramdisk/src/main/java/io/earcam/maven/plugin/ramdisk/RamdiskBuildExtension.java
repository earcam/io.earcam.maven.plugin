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

import static io.earcam.maven.plugin.ramdisk.RamdiskBuildExtension.NAME;
import static io.earcam.maven.plugin.ramdisk.RamdiskMojo.PROPERTY_DIRECTORY;
import static io.earcam.maven.plugin.ramdisk.RamdiskMojo.PROPERTY_SKIP;
import static java.nio.charset.Charset.defaultCharset;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Scanner;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.Scm;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.earcam.unexceptional.Exceptional;
import io.earcam.utilitarian.io.IoStreams;
import io.earcam.utilitarian.io.file.RecursiveFiles;

@SuppressWarnings("squid:S4797")
@Component(role = AbstractMavenLifecycleParticipant.class, hint = NAME, instantiationStrategy = "singleton")
public class RamdiskBuildExtension extends AbstractMavenLifecycleParticipant {

	private static final String TMPFS_NOT_FOUND = "Could not determine tmpfs directory to use for target";
	private static final Logger LOG = LoggerFactory.getLogger(RamdiskBuildExtension.class);

	static final String NAME = "ramdisk";
	static final String LOG_CATEGORY = "[ramdisk-extension]";

	Path tmpFsRoot;
	private Plugin plugin;


	@Override
	public void afterProjectsRead(MavenSession session)
	{
		Properties userProperties = session.getUserProperties();
		if(shouldSkip(userProperties, LOG_CATEGORY)) {
			return;
		}
		userProperties.put("clean.followSymLinks", "true");
		userProperties.put("maven.clean.followSymLinks", "true");

		tmpFsRoot = selectTmpFs(session.getCurrentProject());
		if(tmpFsRoot == null) {
			LOG.warn("{} {}", LOG_CATEGORY, TMPFS_NOT_FOUND);
			return;
		}
		List<MavenProject> modules = session.getAllProjects();
		LOG.debug("{} Applying ramdisk for: {}", LOG_CATEGORY, modules);

		plugin = createPlugin();

		modules.forEach(this::createTmpFsBuildDir);
	}


	static boolean shouldSkip(Properties properties, String logCategory)
	{
		boolean skip = "true".equals(properties.getOrDefault(PROPERTY_SKIP, "false"));
		if(skip) {
			LOG.debug("{} configured to skip execution", logCategory);
		}
		return skip;
	}


	protected Path selectTmpFs(MavenProject project)
	{
		return tmpFsFor(project);
	}


	static Path tmpFsFor(MavenProject project)
	{
		Optional<Path> fromProperty = fromProperty(project.getProperties());
		return fromProperty.orElse(findTmpFs());
	}


	private static Optional<Path> fromProperty(Properties properties)
	{
		return Optional.ofNullable(properties.getProperty(PROPERTY_DIRECTORY, System.getProperty(PROPERTY_DIRECTORY)))
				.map(File::new)
				.map(File::toPath);
	}


	static Path findTmpFs()
	{
		String uid = extractUid();

		List<Path> possibilities = new ArrayList<>();
		if(uid != null) {
			possibilities.add(Paths.get("/", "run", uid));
			possibilities.add(Paths.get("/", "run", "user", uid));
			possibilities.add(Paths.get("/", "var", "run", "user", uid));
		}
		possibilities.add(Paths.get("/", "dev", "shm"));
		possibilities.add(Paths.get("/", "tmp"));

		return possibilities.stream()
				.sequential()
				.filter(Files::isWritable)
				.findFirst()
				.orElse(null);
	}


	private static String extractUid()
	{
		try {
			// SonarQube should really consider constant args as explicit and therefore OK
			@SuppressWarnings("squid:S4721")
			Process process = new ProcessBuilder("/usr/bin/id", "-u", System.getProperty("user.name")).redirectErrorStream(true).start();

			try(Scanner scanner = new Scanner(process.getInputStream(), defaultCharset().toString())) {
				return Integer.toString(scanner.nextInt());
			}
		} catch(Exception e) {
			LOG.debug("{} Unable to get UID", LOG_CATEGORY, e);
			return null;
		}
	}


	private Plugin createPlugin()
	{
		String scrapeDeets = "META-INF/maven/io.earcam.maven.plugin/io.earcam.maven.plugin.ramdisk/plugin-help.xml";
		String xml = new String(IoStreams.readAllBytes(getClass().getClassLoader().getResourceAsStream(scrapeDeets)), UTF_8);

		Plugin dynamicPlugin = new Plugin();
		dynamicPlugin.setGroupId(extractFromXml(xml, "<groupId>", "</groupId>"));
		dynamicPlugin.setArtifactId(extractFromXml(xml, "<artifactId>", "</artifactId>"));
		dynamicPlugin.setVersion(extractFromXml(xml, "<version>", "</version>"));

		PluginExecution exec = new PluginExecution();
		exec.setId("post-clean");
		exec.setGoals(Collections.singletonList(NAME));
		exec.setPhase("post-clean");
		dynamicPlugin.addExecution(exec);

		exec = new PluginExecution();
		exec.setId("validate");
		exec.setGoals(Collections.singletonList(NAME));
		exec.setPhase("validate");
		dynamicPlugin.addExecution(exec);
		return dynamicPlugin;
	}


	private String extractFromXml(String xml, String openTag, String closeTag)
	{
		int start = xml.indexOf(openTag);
		int end = xml.indexOf(closeTag);
		return xml.substring(start + openTag.length(), end);
	}


	private void createTmpFsBuildDir(MavenProject project)
	{
		if(shouldSkip(project.getProperties(), LOG_CATEGORY)) {
			return;
		}
		project.getBuild().addPlugin(plugin);
		Exceptional.accept(RamdiskBuildExtension::createTmpFsBuildDir, project, tmpFsRoot);
	}


	static void createTmpFsBuildDir(MavenProject project, Path tmpFs) throws IOException
	{
		Objects.requireNonNull(tmpFs, TMPFS_NOT_FOUND);

		Path linkTarget = tmpFs.resolve(relativePathFor(project));

		if(!linkTarget.toFile().exists()) {
			LOG.debug("link target does not exist, creating: {}", linkTarget);
			Files.createDirectories(linkTarget);
		}

		Path link = Paths.get(project.getBuild().getDirectory());

		if(link.toFile().exists() && !Files.isSymbolicLink(link)) {
			LOG.debug("{} local target directory exists but is not a symbolic link, moving contents: {}", LOG_CATEGORY, link);
			RecursiveFiles.move(link, linkTarget, REPLACE_EXISTING);

			// TODO what-if: link exists but points somewhere else ...
			// use-case, someone switches branches
			// we'll need to check: (!Files.readSymbolicLink(linkSource).equals(linkTarget.toAbsolutePath()))
		}
		if(!link.toFile().exists()) {
			LOG.debug("{} link does not exist, creating: {}", LOG_CATEGORY, link);
			Files.createSymbolicLink(link, linkTarget);
		}
	}


	static Path relativePathFor(MavenProject project)
	{
		Path linkTarget = Paths.get("maven", project.getGroupId(), project.getArtifactId(), project.getVersion());
		return appendScmTag(project, linkTarget);
	}


	private static Path appendScmTag(MavenProject project, Path linkTarget)
	{
		Scm scm = project.getScm();
		if(scm != null) {
			String tag = scm.getTag();
			if(tag != null && !tag.isEmpty()) {
				linkTarget = linkTarget.resolve(tag);
			}
		}
		return linkTarget;
	}


	/**
	 * This ensures that softlinks are recreated to avoid things like IDEs (that
	 * may be oblivious to extensions) creating "target" directories after a clean.
	 */
	@Override
	public void afterSessionEnd(MavenSession session)
	{
		session.getAllProjects().forEach(this::createTmpFsBuildDir);
	}
}

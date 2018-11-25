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

import static io.earcam.maven.plugin.ramdisk.RamdiskBuildExtension.shouldSkip;
import static org.apache.maven.plugins.annotations.LifecyclePhase.VALIDATE;

import java.nio.file.Path;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.earcam.unexceptional.Exceptional;

/**
 * <p>
 * To use this plugin you should just enable extensions {@code <extensions>true</extensions>}
 * (there's no need to define {@code executions} or {@code goals}).
 * </p>
 * 
 * <p>
 * Configuration is achieved with the following properties:
 * <ul>
 * <li><b>earcam.ramdisk.dir</b> to define a base path for tmpfs</li>
 * <li><b>earcam.ramdisk.skip</b> to skip execution</li>
 * </ul>
 * </p>
 * 
 */
@Mojo(name = RamdiskMojo.NAME, requiresProject = true, threadSafe = true, inheritByDefault = true, defaultPhase = VALIDATE)
public class RamdiskMojo extends AbstractMojo {

	private static final Logger LOG = LoggerFactory.getLogger(RamdiskMojo.class);

	public static final String PROPERTY_DIRECTORY = "earcam.ramdisk.dir";
	public static final String PROPERTY_SKIP = "earcam.ramdisk.skip";

	static final String NAME = "ramdisk";
	static final String LOG_CATEGORY = "[ramdisk-mojo]";

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	MavenProject project;


	@Override
	public void execute()
	{
		if(shouldSkip(project.getProperties(), LOG_CATEGORY)) {
			return;
		}

		LOG.debug("{} executing...", LOG_CATEGORY);
		Path tmpFs = RamdiskBuildExtension.tmpFsFor(project);

		Exceptional.accept(RamdiskBuildExtension::createTmpFsBuildDir, project, tmpFs);
	}
}
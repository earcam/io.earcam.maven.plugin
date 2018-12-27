/*-
 * #%L
 * io.earcam.maven.plugin.jigsaw
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
package io.earcam.maven.plugin.jigsaw;

import java.io.File;

import org.apache.maven.plugins.annotations.Parameter;

import io.earcam.maven.plugin.base.LessAbstractMojo;

abstract class AbstractJigsawMojo extends LessAbstractMojo {

	static final String CATEGORY = "jigsaw";

	/**
	 * Add <b>provides</b> clauses to the <b>module-info</b> definition for entries found in
	 * <b>META-INF/services</b>.
	 * 
	 * @see <a href="https://docs.oracle.com/javase/tutorial/ext/basics/spi.html">SPI</a>
	 */
	@Parameter(property = "addMetaInfServices", defaultValue = "true")
	protected boolean addMetaInfServices = true;

	/**
	 * The <b>module-info.class</b> file. Default value is recommended; <b>module-info.class</b> must appear in the root
	 * of the JAR, with the exception of Multi-Release JARs.
	 */
	@Parameter(property = "targetFile", defaultValue = "${project.build.outputDirectory}/module-info.class", required = true)
	protected File targetFile;

	/**
	 * <p>
	 * The JDK version to use (must be &ge; 9)
	 * </p>
	 * 
	 * <p>
	 * For <b>compilation</b> this will simply determine the module-info class file's major version number.
	 * </p>
	 * <p>
	 * For <b>generation</b> this will determine the set of modules deemed available from the JDK, and
	 * the module-info class file's major version number.
	 * </p>
	 */
	@Parameter(property = "jdkVersion", required = true, defaultValue = "9")
	protected int jdkVersion = 9;


	protected AbstractJigsawMojo(String name)
	{
		super(logIdentifierName(name));
	}


	protected static final String logIdentifierName(String name)
	{
		return CATEGORY + '.' + name;
	}
}

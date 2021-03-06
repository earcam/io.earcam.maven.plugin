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

import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import io.earcam.maven.plugin.base.LessAbstractMojo;

public abstract class AbstractJlinkMojo extends LessAbstractMojo {

	static final String CATEGORY = "jlinkage";

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	protected MavenProject project;


	protected AbstractJlinkMojo(String name)
	{
		super(logIdentifierName(name));
	}


	protected static final String logIdentifierName(String name)
	{
		return CATEGORY + '.' + name;
	}

}

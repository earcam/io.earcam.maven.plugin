/*-
 * #%L
 * io.earcam.maven.plugin.base
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
package io.earcam.maven.plugin.base;

import java.util.function.BiConsumer;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.utils.logging.MessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class LessAbstractMojo extends AbstractMojo {

	private static final Logger LOG = LoggerFactory.getLogger(LessAbstractMojo.class);

	/**
	 * Skip execution of this plugin
	 */
	@Parameter(property = "skip", defaultValue = "false")
	protected boolean skip;

	/**
	 * Log more information from this plugin (debug-level messages <i>promoted</i> to info-level)
	 */
	@Parameter(property = "verbose", defaultValue = "false")
	protected boolean verbose;

	final String logPrefix;


	protected LessAbstractMojo(String logIdentifierName)
	{
		String preprefix = '[' + logIdentifierName + "] ";
		this.logPrefix = MessageUtils.isColorEnabled() ? MessageUtils.buffer().strong(preprefix).toString() : preprefix;
	}


	@Override
	public final void execute() throws MojoExecutionException, MojoFailureException
	{
		if(skip) {
			logInfo("Skipping execution as configured");
			return;
		}
		exec();
	}


	protected abstract void exec() throws MojoExecutionException, MojoFailureException;


	protected void logInfo(String message, Object... parameters)
	{
		logPrefixed(LOG::info, message, parameters);
	}


	private void logPrefixed(BiConsumer<String, Object[]> log, String message, Object... parameters)
	{
		log.accept(prefix(message), parameters);
	}


	private String prefix(String message)
	{
		return logPrefix + message;
	}


	protected final void logDebug(String message, Object... parameters)
	{
		if(verbose) {
			logInfo(message, parameters);
		} else {
			logPrefixed(LOG::debug, message, parameters);
		}
	}
}

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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.shared.utils.logging.MessageUtils;
import org.junit.Test;

import io.earcam.maven.plugin.base.LessAbstractMojo;

public class LessAbstractMojoTest {

	private class ExecDummyMojo extends LessAbstractMojo {

		private boolean executed;


		public ExecDummyMojo()
		{
			super("exec");
		}


		@Override
		public void exec() throws MojoExecutionException, MojoFailureException
		{
			executed = true;
		}
	}

	private class LogDummyMojo extends LessAbstractMojo {

		static final String MESSAGE = "logged at debug";
		String lastInfoMessage;


		public LogDummyMojo()
		{
			super("log");
		}


		@Override
		public void exec() throws MojoExecutionException, MojoFailureException
		{
			logDebug(MESSAGE);
		}


		@Override
		protected void logInfo(String message, Object... parameters)
		{
			this.lastInfoMessage = message;
		}
	}


	@Test
	public void whenConfiguredThenSkipsExecution() throws Exception
	{
		ExecDummyMojo mojo = new ExecDummyMojo();

		mojo.skip = true;

		mojo.execute();

		assertThat(mojo.executed, is(false));
	}


	@Test
	public void whenNotConfiguredThenDoesNotSkipExecution() throws Exception
	{
		ExecDummyMojo mojo = new ExecDummyMojo();

		mojo.execute();

		assertThat(mojo.executed, is(true));
	}


	@Test
	public void whenVerboseIsFalseThenDebugNotLoggedAsInfo() throws Exception
	{
		LogDummyMojo mojo = new LogDummyMojo();

		mojo.execute();

		assertThat(mojo.lastInfoMessage, not(endsWith(LogDummyMojo.MESSAGE)));
	}


	@Test
	public void whenVerboseIsTrueThenDebugLoggedAsInfo() throws Exception
	{
		LogDummyMojo mojo = new LogDummyMojo();

		mojo.verbose = true;

		mojo.execute();

		assertThat(mojo.lastInfoMessage, endsWith(LogDummyMojo.MESSAGE));
	}


	@Test
	public void whenJansiIsEnableThenLogPrefixIsColourized() throws Exception
	{
		MessageUtils.setColorEnabled(true);
		LogDummyMojo mojo = new LogDummyMojo();

		assertThat(mojo.logPrefix, containsString("[1m[log] "));
	}


	@Test
	public void whenJansiNotEnableThenLogPrefixNotColourized() throws Exception
	{
		MessageUtils.setColorEnabled(false);
		LogDummyMojo mojo = new LogDummyMojo();

		assertThat(mojo.logPrefix, is(equalTo("[log] ")));
		MessageUtils.setColorEnabled(true);
	}
}

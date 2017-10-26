/*-
 * #%L
 * io.earcam.maven.plugin.site.search.offline
 * %%
 * Copyright (C) 2017 earcam
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
package io.earcam.maven.plugin.sitemap;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.junit.Test;

public class SitemapIndexMavenLifecycleParticipantTest {

	@Test
	public void doesNotRunWhenThereAreBuildErrors() throws Exception
	{
		SitemapIndexMavenLifecycleParticipant participant = new SitemapIndexMavenLifecycleParticipant();

		MavenSession session = mock(MavenSession.class);
		MavenExecutionResult result = mock(MavenExecutionResult.class);

		given(session.getResult()).willReturn(result);
		given(result.hasExceptions()).willReturn(true);

		participant.afterSessionEnd(session);

		verify(result, atLeastOnce()).hasExceptions();
		verify(session, never()).getTopLevelProject();
	}
}

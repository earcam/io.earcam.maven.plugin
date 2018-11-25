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
package io.earcam.maven.plugin.site.search.offline;

import java.util.stream.Stream;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.codehaus.plexus.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.earcam.utilitarian.site.search.offline.ConfigurationModel.Indexing;
import io.earcam.utilitarian.site.search.offline.Document;
import io.earcam.utilitarian.site.search.offline.Indexer;

@Component(role = AbstractMavenLifecycleParticipant.class, hint = JsSearchIndexMojo.NAME, instantiationStrategy = "singleton")
public class JsSearchLifecycleParticipant extends AbstractMavenLifecycleParticipant {

	private static final Logger LOG = LoggerFactory.getLogger(JsSearchLifecycleParticipant.class);

	private static Indexer indexer;


	synchronized static void indexer(Indexing indexing)
	{
		if(JsSearchLifecycleParticipant.indexer == null) {
			JsSearchLifecycleParticipant.indexer = indexing.build();
		}
	}


	@Override
	public void afterSessionEnd(MavenSession session) throws MavenExecutionException
	{
		if(session.getResult().hasExceptions()) {
			LOG.warn("{} not running due to previous build errors", getClass().getSimpleName());
			return;
		}
		if(indexer != null) {
			indexer.writeJson();
		}
	}


	public static void addDocuments(Stream<Document> documents)
	{
		indexer.add(documents);
	}
}

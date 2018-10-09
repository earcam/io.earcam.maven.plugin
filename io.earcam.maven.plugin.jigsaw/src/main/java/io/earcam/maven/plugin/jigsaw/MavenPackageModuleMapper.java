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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import io.earcam.instrumental.archive.jpms.auto.FilesystemPackageModuleMapper;

class MavenPackageModuleMapper extends FilesystemPackageModuleMapper {

	private final List<Path> artifactsInLocalRepo = new ArrayList<>();


	public void add(Path localArtifact)
	{
		artifactsInLocalRepo.add(localArtifact);
	}


	@Override
	protected Stream<Path> paths()
	{
		return artifactsInLocalRepo.stream();
	}
}

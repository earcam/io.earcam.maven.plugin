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

import java.util.Arrays;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.apache.maven.plugins.annotations.Parameter;

import io.earcam.utilitarian.charstar.CharSequences;

public final class Export {

	private static final String[] EMPTY = new String[0];

	@Parameter(name = "packageRegex", required = true)
	private String packageRegex;

	@Parameter(name = "to", required = false)
	private String[] to = EMPTY;


	@SuppressWarnings("squid:S4784")  // SonarQube - questionable in context
	public Predicate<String> packages()
	{
		return Pattern.compile(getPackageRegex()).asPredicate();
	}


	public String[] trimmedTo()
	{
		return Arrays.stream(getTo())
				.map(CharSequences::trim)
				.toArray(s -> new String[s]);
	}


	public String getPackageRegex()
	{
		return packageRegex;
	}


	public void setPackageRegex(String packageRegex)
	{
		this.packageRegex = packageRegex;
	}


	public String[] getTo()
	{
		return to;
	}


	public void setTo(String[] to)
	{
		this.to = to;
	}
}

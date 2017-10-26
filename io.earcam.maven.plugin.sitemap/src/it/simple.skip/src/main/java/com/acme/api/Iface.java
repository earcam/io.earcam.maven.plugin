/*-
 * #%L
 * io.earcam.maven.plugin.sitemap
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
package com.acme.api;

import java.io.Serializable;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.management.remote.rmi.RMIConnection;

/**
 * Just temp - as ..incept only processing interfaces not classes
 */
@ParametersAreNonnullByDefault
public interface Iface extends Serializable, Comparable<Iface> {

	public static RMIConnection eh()
	{
		return null;
	}
}
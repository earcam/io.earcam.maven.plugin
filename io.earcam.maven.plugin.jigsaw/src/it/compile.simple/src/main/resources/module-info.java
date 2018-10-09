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
/**
 * @version 0.0.1-SNAPSHOT
 */
module compile.simple {
	/**
	 * @version 10.0.2
	 * @modifiers mandated
	 */
	requires java.base;
	/**
	 * @modifiers mandated
	 */
	requires org.slf4j;
	/**
	 * @modifiers mandated
	 */
	opens com.acme.foo.bar to 
		com.acme.wawa,
		com.acme.bah.humbug,
		com.acme.meh;
	uses com.acme.magic.Service;
	provides java.util.Comparator with 
		com.acme.foo.bar.DumbComparator;
}
/*
 * Copyright (c) 2004-2008 Marco Maccaferri and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Marco Maccaferri - initial API and implementation
 */

package org.eclipsetrader.core.instruments;

import java.util.Currency;

import org.eclipsetrader.core.feed.IDividend;

/**
 * Interface implemented by common stock instruments.
 *
 * @since 1.0
 */
public interface IStock extends ISecurity {

	public Currency getCurrency();

	public IDividend[] getDividends();
}
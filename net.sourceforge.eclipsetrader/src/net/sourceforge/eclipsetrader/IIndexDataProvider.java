/*******************************************************************************
 * Copyright (c) 2004 Marco Maccaferri and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     Marco Maccaferri - initial API and implementation
 *******************************************************************************/
package net.sourceforge.eclipsetrader;


/**
 */
public interface IIndexDataProvider
{
  public void addUpdateListener(IIndexUpdateListener listener);
  public void removeUpdateListener(IIndexUpdateListener listener);
  
  public void setSymbols(String[] symbols);
  public IExtendedData[] getIndexData();
}

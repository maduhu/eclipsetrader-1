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
package net.sourceforge.eclipsetrader.ui.views.indices;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Vector;

import net.sourceforge.eclipsetrader.IExtendedData;
import net.sourceforge.eclipsetrader.IIndexDataProvider;
import net.sourceforge.eclipsetrader.IIndexUpdateListener;
import net.sourceforge.eclipsetrader.TraderPlugin;
import net.sourceforge.eclipsetrader.ui.internal.views.Images;
import net.sourceforge.eclipsetrader.ui.internal.views.ViewsPlugin;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

/**
 */
public class IndexView extends ViewPart implements IPropertyChangeListener, IIndexUpdateListener
{
  private static String EXTENSION_POINT_ID = "net.sourceforge.eclipsetrader.indexProvider";
  private SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
  private SimpleDateFormat df_us = new SimpleDateFormat("MM/dd/yyyy h:mma");
  private SimpleDateFormat tf = new SimpleDateFormat("HH:mm:ss");
  private NumberFormat pf = NumberFormat.getInstance();
  private NumberFormat nf = NumberFormat.getInstance();
  private NumberFormat pcf = NumberFormat.getInstance();
  private Composite parent;
  private Image up = Images.ICON_UP.createImage();
  private Image down = Images.ICON_DOWN.createImage();
  private Image equal = Images.ICON_EQUAL.createImage();
  private Vector widgets = new Vector();
  private HashMap map = new HashMap();
  
  public IndexView()
  {
    pf.setGroupingUsed(true);
    pf.setMaximumFractionDigits(2);
    pf.setMinimumFractionDigits(2);

    pcf.setGroupingUsed(true);
    pcf.setMaximumFractionDigits(2);
    pcf.setMinimumFractionDigits(2);

    nf.setGroupingUsed(true);
    nf.setMaximumFractionDigits(0);
    nf.setMinimumFractionDigits(0);
  }

  /* (non-Javadoc)
   * @see org.eclipse.ui.IWorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
   */
  public void createPartControl(Composite parent)
  {
    this.parent = parent;
    RowLayout rowLayout = new RowLayout();
    rowLayout.wrap = true;
    rowLayout.pack = false;
    rowLayout.justify = false;
    rowLayout.type = SWT.HORIZONTAL;
    rowLayout.marginLeft = 2;
    rowLayout.marginTop = 2;
    rowLayout.marginRight = 2;
    rowLayout.marginBottom = 2;
    rowLayout.spacing = 3;
    parent.setLayout(rowLayout);
    
    IPreferenceStore pref = ViewsPlugin.getDefault().getPreferenceStore();
    String[] providers = pref.getString("index.providers").split(",");
    for (int i = 0; i < providers.length; i++)
    {
      String[] symbols = pref.getString("index." + providers[i]).split(",");
      IIndexDataProvider ip = (IIndexDataProvider)TraderPlugin.getExtensionInstance(EXTENSION_POINT_ID, providers[i]);
      if (ip != null)
      {
        for (int ii = 0; ii < symbols.length; ii++)
        {
          IndexWidget w = new IndexWidget(parent, SWT.NONE);
          w.setSymbol(symbols[ii]);
          w.setLayoutData(new RowData());
          widgets.add(w);
        }
        ip.setSymbols(symbols);
        ip.addUpdateListener(this);
      }
    }
    
    // Sets the default data from the preference store
    restoreSavedData();

    // Listener for changes in property settings
    pref.addPropertyChangeListener(this);
  }

  /* (non-Javadoc)
   * @see org.eclipse.ui.IWorkbenchPart#setFocus()
   */
  public void setFocus()
  {
  }

  /* (non-Javadoc)
   * @see org.eclipse.ui.IWorkbenchPart#dispose()
   */
  public void dispose()
  {
    IPreferenceStore pref = ViewsPlugin.getDefault().getPreferenceStore();
    pref.removePropertyChangeListener(this);

    String[] providers = pref.getString("index.providers").split(",");
    for (int i = 0; i < providers.length; i++)
    {
      String[] symbols = pref.getString("index." + providers[i]).split(",");
      IIndexDataProvider ip = (IIndexDataProvider)TraderPlugin.getExtensionInstance(EXTENSION_POINT_ID, providers[i]);
      if (ip != null)
        ip.removeUpdateListener(this);
    }

    // Saves the latest received data
    if (map.values().size() != 0)
    {
      IExtendedData[] data = new IExtendedData[map.values().size()];
      map.values().toArray(data);
      TraderPlugin.getDataStore().storeIndexData(data);
    }

    up.dispose();
    down.dispose();
    
    super.dispose();
  }
  
  private void restoreSavedData()
  {
    IExtendedData[] data = TraderPlugin.getDataStore().loadIndexData();
    for (int i = 0; i < data.length; i++)
    {
      for (int ii = 0; ii < widgets.size(); ii++)
      {
        IndexWidget widget = (IndexWidget)widgets.get(ii);
        if (widget.getSymbol().equalsIgnoreCase(data[i].getSymbol()) == true)
        {
          Image image = (data[i].getLastPrice() == data[i].getClosePrice()) ? equal : ((data[i].getLastPrice() < data[i].getClosePrice()) ? down : up);
          double change = data[i].getLastPrice() - data[i].getClosePrice();
          widget.setValues(data[i].getDescription(), pf.format(data[i].getLastPrice()), pf.format(change) + " (" + pcf.format(change / data[i].getClosePrice() * 100) + ")", tf.format(data[i].getDate()), image);
          widget.setTimeStamp(System.currentTimeMillis());
          widget.setPrice(data[i].getLastPrice());
          map.put(data[i].getSymbol(), data[i]);
        }
      }
    }
  }
  
  /* (non-Javadoc)
   * @see net.sourceforge.eclipsetrader.ui.views.indices.IIndexUpdateListener#indexUpdate(net.sourceforge.eclipsetrader.ui.views.indices.IIndexProvider)
   */
  public void indexUpdate(IIndexDataProvider provider)
  {
    final IExtendedData[] data = provider.getIndexData();
    parent.getDisplay().asyncExec(new Runnable() {
      public void run() {
        parent.setRedraw(false);
        for (int i = 0; i < data.length; i++)
        {
          map.put(data[i].getSymbol(), data[i]);
          for (int ii = 0; ii < widgets.size(); ii++)
          {
            IndexWidget widget = (IndexWidget)widgets.get(ii);
            if (widget.getSymbol().equalsIgnoreCase(data[i].getSymbol()) == true)
            {
              Image image = null;
              if (widget.getPrice() == 0)
                image = (data[i].getLastPrice() == data[i].getClosePrice()) ? equal : ((data[i].getLastPrice() < data[i].getClosePrice()) ? down : up);
              else if (widget.getPrice() != data[i].getLastPrice())
                image = (data[i].getLastPrice() == widget.getPrice()) ? equal : ((data[i].getLastPrice() < widget.getPrice()) ? down : up);
              else if ((System.currentTimeMillis() - widget.getTimeStamp()) >= 60000)
                image = equal;
              double change = data[i].getLastPrice() - data[i].getClosePrice();
              if (image != null)
                widget.setValues(data[i].getDescription(), pf.format(data[i].getLastPrice()), pf.format(change) + " (" + pcf.format(change / data[i].getClosePrice() * 100) + ")", tf.format(data[i].getDate()), image);
              else
                widget.setValues(data[i].getDescription(), pf.format(data[i].getLastPrice()), pf.format(change) + " (" + pcf.format(change / data[i].getClosePrice() * 100) + ")", tf.format(data[i].getDate()));
              
              if (widget.getPrice() != data[i].getLastPrice())
                widget.setTimeStamp(System.currentTimeMillis());
              widget.setPrice(data[i].getLastPrice());
            }
          }
        }
        parent.setRedraw(true);
        parent.layout();
      }
    });
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
   */
  public void propertyChange(PropertyChangeEvent event)
  {
    boolean updateWidgets = false;
    IPreferenceStore pref = ViewsPlugin.getDefault().getPreferenceStore();
    
    if (event.getProperty().equalsIgnoreCase("net.sourceforge.eclipsetrader.streaming") == true)
    {
      if (TraderPlugin.isStreaming() == true)
        map.clear();
    }
    else if (event.getProperty().equalsIgnoreCase("index.providers") == true)
    {
      // Remove this listener from old providers
      String[] providers = ((String)event.getOldValue()).split(",");
      for (int i = 0; i < providers.length; i++)
      {
        IIndexDataProvider ip = (IIndexDataProvider)TraderPlugin.getExtensionInstance(EXTENSION_POINT_ID, providers[i]);
        if (ip != null)
          ip.removeUpdateListener(this);
      }
      updateWidgets = true;
    }
    else if (event.getProperty().startsWith("index.") == true)
    {
      String[] providers = pref.getString("index.providers").split(",");
      for (int i = 0; i < providers.length; i++)
      {
        if (event.getProperty().equalsIgnoreCase("index." + providers[i]) == true)
          updateWidgets = true;
      }
    }
    
    if (updateWidgets == true)
    {
      // Saves the latest received data
      if (map.values().size() != 0)
      {
        IExtendedData[] data = new IExtendedData[map.values().size()];
        map.values().toArray(data);
        TraderPlugin.getDataStore().storeIndexData(data);
      }

      // Get a count of new symbols
      int totalWidgets = 0;
      String[] providers = pref.getString("index.providers").split(",");
      for (int i = 0; i < providers.length; i++)
      {
        String[] symbols = pref.getString("index." + providers[i]).split(",");
        totalWidgets += symbols.length;
      }
      // Remove widgets that are no longer needed
      while (totalWidgets < widgets.size())
      {
        ((IndexWidget)widgets.lastElement()).dispose();
        widgets.removeElement(widgets.lastElement());
      }
      // Creates new widgets for new symbols
      while (totalWidgets > widgets.size())
      {
        IndexWidget w = new IndexWidget(parent, SWT.NONE);
        w.setLayoutData(new RowData());
        widgets.add(w);
      }
      
      // Updates the widget symbols and listeners
      int index = 0;
      for (int i = 0; i < providers.length; i++)
      {
        String[] symbols = pref.getString("index." + providers[i]).split(",");
        IIndexDataProvider ip = (IIndexDataProvider)TraderPlugin.getExtensionInstance(EXTENSION_POINT_ID, providers[i]);
        if (ip != null)
        {
          for (int ii = 0; ii < symbols.length; ii++)
          {
            IndexWidget w = (IndexWidget)widgets.get(index);
            w.setValues("", "", "", "", null);
            w.setSymbol(symbols[ii]);
            w.setPrice(0);
            w.setTimeStamp(0);
            index++;
          }
          ip.setSymbols(symbols);
          ip.addUpdateListener(this);
        }
      }
      
      // Updates the parent container
      restoreSavedData();
      parent.layout(true);
    }
  }
}

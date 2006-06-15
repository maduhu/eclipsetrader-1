/*
 * Copyright (c) 2004-2006 Marco Maccaferri and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Marco Maccaferri - initial API and implementation
 */

package net.sourceforge.eclipsetrader.trading.internal;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;

import net.sourceforge.eclipsetrader.core.CurrencyConverter;
import net.sourceforge.eclipsetrader.core.FeedMonitor;
import net.sourceforge.eclipsetrader.core.db.Security;
import net.sourceforge.eclipsetrader.core.db.WatchlistItem;
import net.sourceforge.eclipsetrader.core.db.feed.Quote;
import net.sourceforge.eclipsetrader.trading.TradingPlugin;
import net.sourceforge.eclipsetrader.trading.views.WatchlistView;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.themes.ITheme;
import org.eclipse.ui.themes.IThemeManager;

public class WatchlistBoxViewer extends AbstractLayout
{
    public static final String FOREGROUND = "BOX_FOREGROUND";
    public static final String BACKGROUND = "BOX_BACKGROUND";
    public static final String POSITIVE_FOREGROUND = "BOX_POSITIVE_FOREGROUND";
    public static final String NEGATIVE_FOREGROUND = "BOX_NEGATIVE_FOREGROUND";
    private Image up = TradingPlugin.getImageDescriptor("icons/higher.gif").createImage();
    private Image down = TradingPlugin.getImageDescriptor("icons/lower.gif").createImage();
    private Image equal = TradingPlugin.getImageDescriptor("icons/equal.gif").createImage();
    private Color foreground;
    private Color background;
    private Color negativeForeground;
    private Color positiveForeground;
    private NumberFormat numberFormatter = NumberFormat.getInstance();
    private NumberFormat percentageFormatter = NumberFormat.getInstance();
    private SimpleDateFormat formatter = new SimpleDateFormat("HH:mm");
    private Composite content;
    private IPropertyChangeListener themeChangeListener = new IPropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent event)
        {
            if (event.getProperty().equals(IThemeManager.CHANGE_CURRENT_THEME))
            {
                ((ITheme) event.getOldValue()).removePropertyChangeListener(this);
                setTheme((ITheme) event.getNewValue());
            }
            else
            {
                IThemeManager themeManager = PlatformUI.getWorkbench().getThemeManager();
                ITheme theme = themeManager.getCurrentTheme();

                if (event.getProperty().equals(BACKGROUND))
                    background = theme.getColorRegistry().get(event.getProperty());
                else if (event.getProperty().equals(FOREGROUND))
                    foreground = theme.getColorRegistry().get(event.getProperty());
                else if (event.getProperty().equals(POSITIVE_FOREGROUND))
                    positiveForeground = theme.getColorRegistry().get(event.getProperty());
                else if (event.getProperty().equals(NEGATIVE_FOREGROUND))
                    negativeForeground = theme.getColorRegistry().get(event.getProperty());
            }

            updateView();
        }
    };
    
    public WatchlistBoxViewer(WatchlistView view)
    {
        super(view);

        numberFormatter.setGroupingUsed(true);
        numberFormatter.setMinimumIntegerDigits(1);
        numberFormatter.setMinimumFractionDigits(0);
        numberFormatter.setMaximumFractionDigits(4);

        percentageFormatter.setGroupingUsed(true);
        percentageFormatter.setMinimumIntegerDigits(1);
        percentageFormatter.setMinimumFractionDigits(2);
        percentageFormatter.setMaximumFractionDigits(2);
    }

    public Composite createPartControl(Composite parent)
    {
        content = new Composite(parent, SWT.NONE);
        RowLayout rowLayout = new RowLayout();
        rowLayout.wrap = true;
        rowLayout.pack = true;
        rowLayout.fill = true;
        rowLayout.justify = false;
        rowLayout.type = SWT.HORIZONTAL;
        rowLayout.marginLeft = 2;
        rowLayout.marginTop = 2;
        rowLayout.marginRight = 2;
        rowLayout.marginBottom = 2;
        rowLayout.spacing = 3;
        content.setLayout(rowLayout);
        
        IThemeManager themeManager = PlatformUI.getWorkbench().getThemeManager();
        themeManager.addPropertyChangeListener(themeChangeListener);
        ITheme theme = themeManager.getCurrentTheme();
        setTheme(theme);

        return content;
    }

    public void dispose()
    {
        IThemeManager themeManager = PlatformUI.getWorkbench().getThemeManager();
        themeManager.removePropertyChangeListener(themeChangeListener);
        ITheme theme = themeManager.getCurrentTheme();
        theme.removePropertyChangeListener(themeChangeListener);

        if (content != null)
            content.dispose();
        
        up.dispose();
        down.dispose();
        equal.dispose();
    }

    /* (non-Javadoc)
     * @see net.sourceforge.eclipsetrader.trading.views.AbstractWatchlistView#updateView()
     */
    public void updateView()
    {
        int index = 0;
        BoxViewItem viewItem;

        Control[] items = content.getChildren();
        for (Iterator iter = getView().getWatchlist().getItems().iterator(); iter.hasNext(); )
        {
            WatchlistItem watchlistItem = (WatchlistItem)iter.next();
            if (index < items.length)
            {
                viewItem = (BoxViewItem)items[index];
                viewItem.setWatchlistItem(watchlistItem);
            }
            else
                viewItem = new BoxViewItem(content, SWT.NONE, watchlistItem);

            viewItem.setBackground(background);
            viewItem.setForeground(foreground);
            viewItem.setPositiveForeground(positiveForeground);
            viewItem.setNegativeForeground(negativeForeground);

            Security security = watchlistItem.getSecurity();
            if (security != null && security.getQuoteFeed() != null)
                FeedMonitor.monitor(security);
            
            index++;
        }
        while(index < items.length)
            items[index++].dispose();
    }

    /* (non-Javadoc)
     * @see net.sourceforge.eclipsetrader.trading.views.AbstractWatchlistView#update(java.util.Observable, java.lang.Object)
     */
    public void update(Observable o, Object arg)
    {
        content.getDisplay().asyncExec(new Runnable() {
            public void run()
            {
                if (!content.isDisposed())
                    updateView();
            }
        });
    }

    /* (non-Javadoc)
     * @see net.sourceforge.eclipsetrader.trading.views.AbstractWatchlistView#itemAdded(java.lang.Object)
     */
    public void itemAdded(Object o)
    {
        if (o instanceof WatchlistItem)
        {
            WatchlistItem watchlistItem = (WatchlistItem)o;
            new BoxViewItem(content, SWT.NONE, watchlistItem);
        }
    }

    /* (non-Javadoc)
     * @see net.sourceforge.eclipsetrader.trading.views.AbstractWatchlistView#itemRemoved(java.lang.Object)
     */
    public void itemRemoved(Object o)
    {
        Control[] items = content.getChildren();
        for (int i = 0; i < items.length - 1; i++)
        {
            BoxViewItem tableItem = (BoxViewItem)items[i];
            if (tableItem.getWatchlistItem().equals(o))
            {
                tableItem.dispose();
                break;
            }
        }
    }
    
    protected void setTheme(ITheme theme)
    {
        positiveForeground = theme.getColorRegistry().get(POSITIVE_FOREGROUND);
        negativeForeground = theme.getColorRegistry().get(NEGATIVE_FOREGROUND);
        foreground = theme.getColorRegistry().get(FOREGROUND);
        background = theme.getColorRegistry().get(BACKGROUND);
        theme.addPropertyChangeListener(themeChangeListener);
    }

    /* (non-Javadoc)
     * @see net.sourceforge.eclipsetrader.trading.internal.AbstractLayout#getSelection()
     */
    public WatchlistItem[] getSelection()
    {
        return new WatchlistItem[0];
    }
    
    private class BoxViewItem extends Box implements DisposeListener, Observer
    {
        private WatchlistItem watchlistItem;
        private Double lastValue;

        public BoxViewItem(Composite parent, int style, WatchlistItem watchlistItem)
        {
            super(parent, style);
            addDisposeListener(this);
            setWatchlistItem(watchlistItem);
        }
        
        WatchlistItem getWatchlistItem()
        {
            return watchlistItem;
        }

        void setWatchlistItem(WatchlistItem watchlistItem)
        {
            if (this.watchlistItem != null)
            {
                this.watchlistItem.deleteObserver(this);
                Security security = this.watchlistItem.getSecurity();
                if (security != null)
                    FeedMonitor.cancelMonitor(security);
            }
            this.watchlistItem = watchlistItem;

            updateDisplay();
            
            this.watchlistItem.addObserver(this);
        }
        
        private void updateDisplay()
        {
            Security security = watchlistItem.getSecurity();
            setName(security.getDescription());
            Quote quote = security.getQuote();
            if (quote != null)
            {
                double last = CurrencyConverter.getInstance().convert(quote.getLast(), security.getCurrency(), watchlistItem.getParent().getCurrency());
                double close = CurrencyConverter.getInstance().convert(security.getClose(), security.getCurrency(), watchlistItem.getParent().getCurrency());
                setValue(numberFormatter.format(last));
                if (security.getClose() != null)
                {
                    double change = last - close;
                    double percentage = change / close * 100;
                    String prefix = "";
                    if (change > 0)
                        prefix = "+";
                    setChange(prefix + numberFormatter.format(change) + " (" + prefix + percentageFormatter.format(percentage) + "%)");
                }
                if (quote.getDate() != null)
                    setTime(formatter.format(quote.getDate()));
                if (lastValue != null)
                {
                    if (quote.getLast() > lastValue.doubleValue())
                        setImage(up);
                    else if (quote.getLast() < lastValue.doubleValue())
                        setImage(down);
                    else
                        setImage(equal);
                }
                lastValue = new Double(quote.getLast());
            }
            getParent().layout(true, true);
        }
        
        /* (non-Javadoc)
         * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
         */
        public void update(Observable o, Object arg)
        {
            getDisplay().asyncExec(new Runnable() {
                public void run()
                {
                    if (!isDisposed())
                        updateDisplay();
                }
            });
        }

        /* (non-Javadoc)
         * @see org.eclipse.swt.events.DisposeListener#widgetDisposed(org.eclipse.swt.events.DisposeEvent)
         */
        public void widgetDisposed(DisposeEvent e)
        {
            if (watchlistItem != null)
                watchlistItem.deleteObserver(this);
        }
    }
}

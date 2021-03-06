/*
 * Copyright (c) 2004-2013 Marco Maccaferri and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Marco Maccaferri - initial API and implementation
 */

package org.eclipsetrader.yahoo.internal.core.connector;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Status;
import org.eclipsetrader.core.feed.IBar;
import org.eclipsetrader.core.feed.IFeedIdentifier;
import org.eclipsetrader.core.feed.IFeedSubscription;
import org.eclipsetrader.core.feed.ILastClose;
import org.eclipsetrader.core.feed.IQuote;
import org.eclipsetrader.core.feed.ISubscriptionListener;
import org.eclipsetrader.core.feed.ITodayOHL;
import org.eclipsetrader.core.feed.ITrade;
import org.eclipsetrader.core.feed.LastClose;
import org.eclipsetrader.core.feed.Quote;
import org.eclipsetrader.core.feed.QuoteDelta;
import org.eclipsetrader.core.feed.QuoteEvent;
import org.eclipsetrader.core.feed.TodayOHL;
import org.eclipsetrader.core.feed.Trade;
import org.eclipsetrader.yahoo.internal.YahooActivator;
import org.eclipsetrader.yahoo.internal.core.repository.IdentifierType;

public class FeedSubscription implements IFeedSubscription {

    private SnapshotConnector connector;
    private IFeedIdentifier identifier;
    private ITrade trade;
    private IQuote quote;
    private ITodayOHL todayOHL;
    private ILastClose lastClose;
    private IBar bar;
    private ListenerList listeners = new ListenerList(ListenerList.IDENTITY);
    private IdentifierType identifierType;
    List<QuoteDelta> deltaList = new ArrayList<QuoteDelta>();
    private int instanceCount = 0;

    public FeedSubscription(SnapshotConnector connector, IdentifierType identifierType) {
        this.connector = connector;
        this.identifierType = identifierType;
        this.identifier = identifierType.getIdentifier();
        this.trade = identifierType.getTrade();
        this.quote = identifierType.getQuote();
        this.todayOHL = identifierType.getTodayOHL();
        this.lastClose = identifierType.getLastClose();
    }

    public IdentifierType getIdentifierType() {
        return identifierType;
    }

    public void setIdentifierType(IdentifierType identifierType) {
        this.identifierType = identifierType;
    }

    /* (non-Javadoc)
     * @see org.eclipsetrader.core.feed.IFeedSubscription#dispose()
     */
    @Override
    public void dispose() {
        connector.disposeSubscription(this);
    }

    protected void incrementInstanceCount() {
        instanceCount++;
    }

    protected int decrementInstanceCount() {
        instanceCount--;
        return instanceCount;
    }

    protected int getInstanceCount() {
        return instanceCount;
    }

    /* (non-Javadoc)
     * @see org.eclipsetrader.core.feed.IFeedSubscription#addSubscriptionListener(org.eclipsetrader.core.feed.ISubscriptionListener)
     */
    @Override
    public void addSubscriptionListener(ISubscriptionListener listener) {
        listeners.add(listener);
    }

    /* (non-Javadoc)
     * @see org.eclipsetrader.core.feed.IFeedSubscription#removeSubscriptionListener(org.eclipsetrader.core.feed.ISubscriptionListener)
     */
    @Override
    public void removeSubscriptionListener(ISubscriptionListener listener) {
        listeners.remove(listener);
    }

    /* (non-Javadoc)
     * @see org.eclipsetrader.core.feed.IFeedSubscription#getIdentifier()
     */
    @Override
    public IFeedIdentifier getIdentifier() {
        return identifier;
    }

    /* (non-Javadoc)
     * @see org.eclipsetrader.core.feed.IFeedSubscription#getSymbol()
     */
    @Override
    public String getSymbol() {
        return identifierType.getSymbol();
    }

    /* (non-Javadoc)
     * @see org.eclipsetrader.core.feed.IFeedSubscription#getQuote()
     */
    @Override
    public IQuote getQuote() {
        return quote;
    }

    public void setQuote(Double bid, Double ask, Long bidSize, Long askSize) {
        if (!equals(bid, quote.getBid()) || !equals(ask, quote.getAsk()) || !equals(bidSize, quote.getBidSize()) || !equals(askSize, quote.getAskSize())) {
            IQuote quote = new Quote(bid, ask, bidSize, askSize);
            addDelta(new QuoteDelta(identifierType.getIdentifier(), this.quote, quote));
            this.quote = quote;
            this.identifierType.setQuote(quote);
        }
    }

    /* (non-Javadoc)
     * @see org.eclipsetrader.core.feed.IFeedSubscription#getTodayOHL()
     */
    @Override
    public ITodayOHL getTodayOHL() {
        return todayOHL;
    }

    public void setTodayOHL(Double open, Double high, Double low) {
        if (todayOHL == null || !equals(open, todayOHL.getOpen()) || !equals(high, todayOHL.getHigh()) || !equals(low, todayOHL.getLow())) {
            ITodayOHL todayOHL = new TodayOHL(open, high, low);
            addDelta(new QuoteDelta(identifierType.getIdentifier(), this.todayOHL, todayOHL));
            this.todayOHL = todayOHL;
            this.identifierType.setTodayOHL(todayOHL);
        }
    }

    /* (non-Javadoc)
     * @see org.eclipsetrader.core.feed.IFeedSubscription#getTrade()
     */
    @Override
    public ITrade getTrade() {
        return trade;
    }

    public void setTrade(Date time, Double price, Long size, Long volume) {
        if (!equals(time, trade.getTime()) || !equals(price, trade.getPrice()) || !equals(size, trade.getSize()) || !equals(volume, trade.getVolume())) {
            ITrade trade = new Trade(time, price, size, volume);
            addDelta(new QuoteDelta(identifierType.getIdentifier(), this.trade, trade));
            this.trade = trade;
            this.identifierType.setTrade(trade);
        }
    }

    /* (non-Javadoc)
     * @see org.eclipsetrader.core.feed.IFeedSubscription#getLastClose()
     */
    @Override
    public ILastClose getLastClose() {
        return lastClose;
    }

    public void setLastClose(Double price, Date date) {
        if (!equals(price, lastClose.getPrice()) || !equals(date, lastClose.getDate())) {
            LastClose lastClose = new LastClose(price, date);
            addDelta(new QuoteDelta(identifierType.getIdentifier(), this.lastClose, lastClose));
            this.lastClose = lastClose;
            this.identifierType.setLastClose(lastClose);
        }
    }

    public void addDelta(QuoteDelta delta) {
        synchronized (deltaList) {
            deltaList.add(delta);
        }
    }

    public boolean hasListeners() {
        return listeners.size() != 0;
    }

    public void fireNotification() {
        QuoteDelta[] deltas;
        synchronized (deltaList) {
            if (deltaList.isEmpty()) {
                return;
            }
            deltas = deltaList.toArray(new QuoteDelta[deltaList.size()]);
            deltaList.clear();
        }
        QuoteEvent event = new QuoteEvent(connector, getIdentifier(), deltas);
        Object[] l = listeners.getListeners();
        for (int i = 0; i < l.length; i++) {
            try {
                ((ISubscriptionListener) l[i]).quoteUpdate(event);
            } catch (Exception e) {
                Status status = new Status(IStatus.ERROR, YahooActivator.PLUGIN_ID, 0, "Error notifying a quote update", e);
                YahooActivator.log(status);
            } catch (LinkageError e) {
                Status status = new Status(IStatus.ERROR, YahooActivator.PLUGIN_ID, 0, "Error notifying a quote update", e);
                YahooActivator.log(status);
            }
        }
    }

    protected boolean equals(Object o1, Object o2) {
        return o1 == o2 || o1 != null && o1.equals(o2);
    }
}

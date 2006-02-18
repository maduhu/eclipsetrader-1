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

package net.sourceforge.eclipsetrader.directaworld;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import sun.misc.BASE64Encoder;

import net.sourceforge.eclipsetrader.core.CorePlugin;
import net.sourceforge.eclipsetrader.core.IFeed;
import net.sourceforge.eclipsetrader.core.db.Security;
import net.sourceforge.eclipsetrader.core.db.feed.Quote;

public class Feed implements IFeed, Runnable
{
    private Map map = new HashMap();
    private Thread thread;
    private boolean stopping = false;
    private String userName = "";
    private String password = "";
    private NumberFormat nf = NumberFormat.getInstance();
    private NumberFormat pf = NumberFormat.getInstance();
    private SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

    public Feed()
    {
    }

    /* (non-Javadoc)
     * @see net.sourceforge.eclipsetrader.core.IFeed#subscribe(net.sourceforge.eclipsetrader.core.db.Security)
     */
    public void subscribe(Security security)
    {
        String symbol = security.getQuoteFeed().getSymbol();
        if (symbol == null || symbol.length() == 0)
            symbol = security.getCode();
        map.put(security, symbol);
    }

    /* (non-Javadoc)
     * @see net.sourceforge.eclipsetrader.core.IFeed#unSubscribe(net.sourceforge.eclipsetrader.core.db.Security)
     */
    public void unSubscribe(Security security)
    {
        map.remove(security);
    }

    /* (non-Javadoc)
     * @see net.sourceforge.eclipsetrader.core.IFeed#start()
     */
    public void start()
    {
        if (thread == null)
        {
            userName = DirectaWorldPlugin.getDefault().getPreferenceStore().getString(DirectaWorldPlugin.USERNAME_PREFS);
            password = DirectaWorldPlugin.getDefault().getPreferenceStore().getString(DirectaWorldPlugin.PASSWORD_PREFS);

            if (userName.length() == 0 || password.length() == 0)
            {
                LoginDialog dlg = new LoginDialog(userName, password);
                if (dlg.open() != LoginDialog.OK)
                    return;
                
                userName = dlg.getUserName();
                password = dlg.getPassword();
            }
            
            stopping = false;
            thread = new Thread(this);
            thread.start();
        }
    }

    /* (non-Javadoc)
     * @see net.sourceforge.eclipsetrader.core.IFeed#stop()
     */
    public void stop()
    {
        stopping = true;
        if (thread != null)
        {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            thread = null;
        }
    }

    /* (non-Javadoc)
     * @see net.sourceforge.eclipsetrader.core.IFeed#snapshot()
     */
    public void snapshot()
    {
        userName = DirectaWorldPlugin.getDefault().getPreferenceStore().getString(DirectaWorldPlugin.USERNAME_PREFS);
        password = DirectaWorldPlugin.getDefault().getPreferenceStore().getString(DirectaWorldPlugin.PASSWORD_PREFS);

        if (userName.length() == 0 || password.length() == 0)
        {
            LoginDialog dlg = new LoginDialog(userName, password);
            if (dlg.open() != LoginDialog.OK)
                return;
            
            userName = dlg.getUserName();
            password = dlg.getPassword();
        }

        update();
    }

    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    public void run()
    {
        long nextRun = System.currentTimeMillis() + 2 * 1000;

        while(!stopping)
        {
            if (System.currentTimeMillis() >= nextRun)
            {
                int requiredDelay = update();
                if (requiredDelay > 0)
                    nextRun = System.currentTimeMillis() + requiredDelay * 1000;
                else
                    nextRun = System.currentTimeMillis() + 16 * 1000;
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }
        }
        
        thread = null;
    }

    private int update()
    {
        int i, requiredDelay = -1;
        String inputLine;

        nf.setGroupingUsed(true);
        nf.setMinimumFractionDigits(0);
        nf.setMaximumFractionDigits(0);

        pf.setGroupingUsed(true);
        pf.setMinimumFractionDigits(4);
        pf.setMaximumFractionDigits(4);

        try
        {
            // Legge la pagina contenente gli ultimi prezzi
            StringBuffer url = new StringBuffer("http://registrazioni.directaworld.it/cgi-bin/qta?idx=alfa&modo=t&appear=n");
            i = 0;
            for (Iterator iter = map.values().iterator(); iter.hasNext(); )
                url.append("&id" + (++i) + "=" + (String)iter.next());
            for (; i < 30; i++)
                url.append("&id" + (i + 1) + "=");
            url.append("&u=" + userName + "&p=" + password);

            HttpURLConnection.setFollowRedirects(false);
            HttpURLConnection con = (HttpURLConnection) new URL(url.toString()).openConnection();
            String proxyHost = (String) System.getProperties().get("http.proxyHost");
            String proxyUser = (String) System.getProperties().get("http.proxyUser");
            String proxyPassword = (String) System.getProperties().get("http.proxyPassword");
            if (proxyHost != null && proxyHost.length() != 0 && proxyUser != null && proxyUser.length() != 0 && proxyPassword != null)
            {
                String login = proxyUser + ":" + proxyPassword;
                String encodedLogin = new BASE64Encoder().encodeBuffer(login.getBytes());
                con.setRequestProperty("Proxy-Authorization", "Basic " + encodedLogin.trim());
            }
            con.setAllowUserInteraction(true);
            con.setRequestMethod("GET");
            con.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows 98; QuoteTracker-DirectaWorld)");
            con.connect();
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            while ((inputLine = in.readLine()) != null)
            {
                if (inputLine.indexOf("<!--QT START HERE-->") != -1)
                {
                    while ((inputLine = in.readLine()) != null)
                    {
                        if (inputLine.indexOf("<!--QT STOP HERE-->") != -1)
                            break;
                        parseLine(inputLine);
                    }
                }
                else if (inputLine.indexOf("Sara' possibile ricaricare la pagina tra") != -1)
                {
                    int beginIndex = inputLine.indexOf("tra ") + 4;
                    int endIndex = inputLine.indexOf("sec") - 1;
                    try {
                        requiredDelay = Integer.parseInt(inputLine.substring(beginIndex, endIndex)) + 1;
                    } catch (Exception e) {
                        CorePlugin.logException(e);
                    }
                }
            }
            in.close();
        }
        catch (Exception e) {
            CorePlugin.logException(e);
        }
        
        return requiredDelay;
    }

    public void parseLine(String line) throws ParseException
    {
        String[] item = line.split(";");

        for (Iterator iter = map.keySet().iterator(); iter.hasNext(); )
        {
            Security security = (Security)iter.next();
            if (item[0].equalsIgnoreCase((String)map.get(security)))
            {
                Double open = null, high = null, low = null, close = null;
                Quote quote = new Quote();
                
                // item[1] - Nome
                quote.setLast(pf.parse(item[2]).doubleValue());
                // item[3] - Variazione
                quote.setVolume(nf.parse(item[4]).intValue());
                try {
                    if (item[5].length() == 7)
                        item[5] = item[5].charAt(0) + ":" + item[5].charAt(1) + item[5].charAt(3) + ":" + item[5].charAt(4) + item[5].charAt(6);
                    quote.setDate(df.parse(item[6] + " " + item[5]));
                }
                catch (Exception e) {
                }

                quote.setBid(pf.parse(item[7]).doubleValue());
                quote.setBidSize(nf.parse(item[8]).intValue());
                quote.setAsk(pf.parse(item[9]).doubleValue());
                quote.setAskSize(nf.parse(item[10]).intValue());
                // item[11] - ???
                open = new Double(pf.parse(item[12]).doubleValue());
                close = new Double(pf.parse(item[13]).doubleValue());
                low = new Double(pf.parse(item[14]).doubleValue());
                high = new Double(pf.parse(item[15]).doubleValue());

                security.setQuote(quote, open, high, low, close);
                break;
            }
        }
    }
}
package com.lenderman.nabu.adapter.utilities;

import java.net.URL;
import java.security.SecureRandom;
import java.security.Security;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import org.apache.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;
import com.lenderman.nabu.adapter.model.settings.Settings;

/*
 * Copyright(c) 2023 "RetroTech" Chris Lenderman
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

public class WebUtils
{
    /**
     * Class Logger
     */
    private static final Logger logger = Logger.getLogger(WebUtils.class);

    private static boolean httpsInitialized = false;

    // Bring in BouncyCastle and disable hostname verification for known URLs
    public static void initializeHttps()
    {
        if (!httpsInitialized)
        {
            Security.insertProviderAt(new BouncyCastleJsseProvider(), 1);
            Security.insertProviderAt(new BouncyCastleProvider(), 1);
            javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier(
                    new javax.net.ssl.HostnameVerifier()
                    {
                        public boolean verify(String hostname,
                                javax.net.ssl.SSLSession sslSession)
                        {
                            return Settings.allowedUri.contains(hostname);
                        }
                    });
            httpsInitialized = true;
        }
    }

    /**
     * Validate the URI
     * 
     * @param String URI to validate
     * @return true if allowed URL
     */
    public static boolean validateUri(String uri)
    {
        try
        {
            URL url = new URL(uri);
            if (!url.getProtocol().equals("http")
                    && !url.getProtocol().equals("https"))
            {
                return false;
            }
            return Settings.allowedUri.contains(url.getHost());
        }
        catch (Exception e)
        {
            return false;
        }
    }

    /**
     * Get a "latest TLS protocol" compatible factory
     * 
     * @param String URI to validate
     * @return true if allowed URL
     */
    public static SSLSocketFactory getFactory()
    {
        SSLContext clientContext;
        try
        {
            TrustManager[] trustArray = new TrustManager[1];
            trustArray[0] = new TrustAllTrustManager();
            clientContext = SSLContext.getInstance("TLS");
            clientContext.init(null, trustArray, new SecureRandom());
            return clientContext.getSocketFactory();
        }
        catch (Exception e)
        {
            logger.error("Could not get SSL factory", e);
        }
        return null;
    }
}

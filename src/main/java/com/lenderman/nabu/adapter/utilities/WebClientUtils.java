package com.lenderman.nabu.adapter.utilities;

import java.net.URL;
import java.net.URLConnection;
import javax.net.ssl.HttpsURLConnection;

/*
 * Copyright(c) 2024 "RetroTech" Chris Lenderman
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

// NOTE: This class was broken out from WebUtils to prevent static instantiation of
// BouncyCastle classes which are very slow to instantiate on older machines
public class WebClientUtils
{
    /**
     * Helper method to open a Web Client
     * 
     * @param String url
     * @return URLConnection
     */
    public static URLConnection openWebClient(String path) throws Exception
    {
        URL myURL = new URL(path);
        URLConnection connection = myURL.openConnection();
        connection.addRequestProperty("user-agent", "JavaNabuAdapter");
        connection.addRequestProperty("Content-Type",
                "application/octet-stream");
        connection.addRequestProperty("Content-Transfer-Encoding", "binary");

        if (path.startsWith("https"))
        {
            WebUtils.initializeHttps();
            ((HttpsURLConnection) connection)
                    .setSSLSocketFactory(WebUtils.getFactory());
        }
        return connection;
    }
}

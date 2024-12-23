package com.lenderman.nabu.adapter.utilities;

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

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.X509TrustManager;
import com.lenderman.nabu.adapter.model.settings.Settings;

public class TrustAllTrustManager implements X509TrustManager
{

    public void checkClientTrusted(X509Certificate[] x509Certificates, String s)
            throws CertificateException
    {
        // Do nothing
    }

    public void checkServerTrusted(X509Certificate[] x509Certificates, String s)
            throws CertificateException
    {
        for (int i = 0; i < x509Certificates.length; i++)
        {
            for (String uri : Settings.allowedUri)
            {
                if (x509Certificates[i].getSubjectX500Principal().getName()
                        .contains(uri))
                {
                    return;
                }
            }
        }
        throw new CertificateException(
                "Certificate not found in the allowed list.");
    }

    public X509Certificate[] getAcceptedIssuers()
    {
        return new X509Certificate[0];
    }
}

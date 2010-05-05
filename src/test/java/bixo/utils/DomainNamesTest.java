/*
 * Copyright (c) 1997-2009 101tec Inc.
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
 *
 */
package bixo.utils;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

import org.junit.Test;

import bixo.utils.DomainNames;

import junit.framework.TestCase;

public class DomainNamesTest extends TestCase {
    
    @Test
    public final void testIPv4() throws MalformedURLException {
        assertEquals("1.2.3.4", DomainNames.getPLD("1.2.3.4"));

        URL url = new URL("http://1.2.3.4:8080/a/b/c?_queue=1");
        assertEquals("1.2.3.4", DomainNames.getPLD(url));
    }

    public final void testIPv6() throws MalformedURLException, UnknownHostException {
        InetAddress inet = InetAddress.getByName("1080:0:0:0:8:800:200c:417a");
        URL url = new URL("http", inet.getHostAddress(), 8080, "a/b/c");
        assertEquals("[1080:0:0:0:8:800:200c:417a]", DomainNames.getPLD(url));
    }

    public final void testStandardDomains() throws MalformedURLException {
        assertEquals("xxx.com", DomainNames.getPLD("xxx.com"));
        assertEquals("xxx.com", DomainNames.getPLD("www.xxx.com"));
        assertEquals("xxx.com", DomainNames.getPLD("www.zzz.xxx.com"));
        assertEquals("xxx.com", DomainNames.getPLD(new URL("https://www.zzz.xxx.com:9000/a/b?c=d")));
    }

    public final void testBizDomains() {
        assertEquals("xxx.biz", DomainNames.getPLD("xxx.biz"));
        assertEquals("xxx.biz", DomainNames.getPLD("www.xxx.biz"));
    }

    // Japan (and uk) have shortened gTLDs before the country code.
    public final void testJapaneseDomains() {
        assertEquals("xxx.co.jp", DomainNames.getPLD("xxx.co.jp"));
        assertEquals("xxx.co.jp", DomainNames.getPLD("www.xxx.co.jp"));
        assertEquals("xxx.ne.jp", DomainNames.getPLD("www.xxx.ne.jp"));
    }

    // In Germany you can have xxx.de.com
    public final void testGermanDomains() {
        assertEquals("xxx.de.com", DomainNames.getPLD("xxx.de.com"));
        assertEquals("xxx.de.com", DomainNames.getPLD("www.xxx.de.com"));
    }

    // Typical international domains look like xxx.com.it
    public final void testItalianDomains() {
        assertEquals("xxx.com.it", DomainNames.getPLD("xxx.com.it"));
        assertEquals("xxx.com.it", DomainNames.getPLD("www.xxx.com.it"));
    }
    
    public final void testSafeGetHost() {
        assertEquals("domain.com", DomainNames.safeGetHost("http://domain.com"));
        assertTrue(!DomainNames.safeGetHost("mailto:ken@domain.com").equals("domain.com"));
    }

    public final void testGetSuperDomain() {
        assertEquals("domain.com", DomainNames.getSuperDomain("www.domain.com"));
        assertEquals("subdomain.domain.com", DomainNames.getSuperDomain("subsubdomain.subdomain.domain.com"));
        assertNull(DomainNames.getSuperDomain("domain.com"));
        assertEquals("xxx.de.com", DomainNames.getSuperDomain("www.xxx.de.com"));
        assertNull(DomainNames.getSuperDomain("xxx.com.it"));
    }
    
    public final void testIsUrlWithinSubdomain() {
        assertTrue(DomainNames.isUrlWithinDomain("http://domain.com", "domain.com"));
        assertFalse(DomainNames.isUrlWithinDomain("http://domain.ru", "domain.com"));
        assertTrue(DomainNames.isUrlWithinDomain("http://www.domain.com", "domain.com"));
        assertFalse(DomainNames.isUrlWithinDomain("http://subdomain.domain.com", "www.domain.com"));
        assertTrue(DomainNames.isUrlWithinDomain("http://subsubdomain.subdomain.domain.com", "subdomain.domain.com"));
        assertTrue(DomainNames.isUrlWithinDomain("http://subsubdomain.subdomain.domain.com", "domain.com"));
    }
}

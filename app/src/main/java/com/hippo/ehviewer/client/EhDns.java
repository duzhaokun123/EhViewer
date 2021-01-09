/*
 * Copyright 2018 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer.client;

/*
 * Created by Hippo on 2018/3/23.
 */

import android.content.Context;

import androidx.annotation.NonNull;

import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.Hosts;
import com.hippo.ehviewer.Settings;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Dns;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.dnsoverhttps.DnsOverHttps;

public class EhDns implements Dns {

    private static final Map<String, List<InetAddress>> builtInHosts;

    static {
        Map<String, List<InetAddress>> map = new HashMap<>();
        put(map, "e-hentai.org", "104.20.26.25", "104.20.26.25");
        put(map, "exhentai.org", "178.175.128.252", "178.175.129.252", "178.175.129.254", "178.175.128.254",
                "178.175.132.20", "178.175.132.22");
        put(map, "repo.e-hentai.org", "94.100.28.57", "94.100.29.73");
        put(map, "forums.e-hentai.org", "94.100.18.243");
        put(map, "ehgt.org", "37.48.89.44", "81.171.10.48", "178.162.139.24", "178.162.140.212"
                , "2001:1af8:4700:a062:8::47de", "2001:1af8:4700:a062:9::47de", "2001:1af8:4700:a0c9:4::47de", "2001:1af8:4700:a0c9:3::47de");
        put(map, "ul.ehgt.org", "94.100.24.82", "94.100.24.72");
        put(map, "raw.githubusercontent.com", "151.101.0.133", "151.101.64.133", "151.101.128.133", "151.101.192.133");
        builtInHosts = map;
    }

    private final Hosts hosts;
    private static DnsOverHttps dnsOverHttps;

    public EhDns(Context context) {
        hosts = EhApplication.getHosts(context);
        DnsOverHttps.Builder builder = new DnsOverHttps.Builder()
                .client(new OkHttpClient.Builder().cache(EhApplication.getOkHttpCache(context)).build())
                .url(HttpUrl.get("https://cloudflare-dns.com/dns-query"));
        try {
            builder.bootstrapDnsHosts(InetAddress.getByName("162.159.36.1"),
                    InetAddress.getByName("162.159.46.1"),
                    InetAddress.getByName("1.1.1.1"),
                    InetAddress.getByName("1.0.0.1"),
                    InetAddress.getByName("162.159.132.53"),
                    InetAddress.getByName("2606:4700:4700::1111"),
                    InetAddress.getByName("2606:4700:4700::1001"),
                    InetAddress.getByName("2606:4700:4700::0064"),
                    InetAddress.getByName("2606:4700:4700::6400"));
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        dnsOverHttps = builder.post(true).build();
    }

    private static void put(Map<String, List<InetAddress>> map, String host, String... ips) {
        List<InetAddress> addresses = new ArrayList<>();
        for (String ip : ips) {
            addresses.add(Hosts.toInetAddress(host, ip));
        }
        map.put(host, addresses);
    }

    @NonNull
    @Override
    public List<InetAddress> lookup(@NonNull String hostname) throws UnknownHostException {
        List<InetAddress> inetAddresses = hosts.get(hostname);
        if (inetAddresses != null) {
            return inetAddresses;
        }
        if (Settings.getBuiltInHosts()) {
            inetAddresses = builtInHosts.get(hostname);
            if (inetAddresses != null) {
                return inetAddresses;
            }
        }
        if (Settings.getDoH()) {
            inetAddresses = dnsOverHttps.lookup(hostname);
            if (inetAddresses.size() > 0) {
                return inetAddresses;
            }
        }
        return SYSTEM.lookup(hostname);
    }
}

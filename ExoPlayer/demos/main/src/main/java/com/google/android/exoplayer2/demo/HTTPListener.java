package com.google.android.exoplayer2.demo;


import java.io.BufferedInputStream;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.net.ssl.SSLServerSocketFactory;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.IStatus;
import fi.iki.elonen.NanoHTTPD.Response.Status;

import android.content.Context;

import javax.net.ssl.KeyManagerFactory;

public class HTTPListener extends NanoHTTPD {

    protected WSListener.Reader theReader = null;

    private static final int SSL_PORT = 9441;
    private static final int PORT = 9440;
    private static boolean SECURE = false;
    private static SampleChooserActivity theActivity = null;

    public HTTPListener(SampleChooserActivity activity, Context context, WSListener.Reader r, boolean secure) throws IOException {
        super(secure?HTTPListener.SSL_PORT:HTTPListener.PORT);

        this.theActivity = activity;
        this.theReader = r;
        try {
            if(secure) {
                char[] passphrase = "123456789".toCharArray();
                KeyStore e = KeyStore.getInstance(KeyStore.getDefaultType());
                ClassLoader classloader = Thread.currentThread().getContextClassLoader();
                InputStream keystoreStream = classloader.getResourceAsStream("keystore.https.jks");
                // System.setProperty("javax.net.ssl.trustStore", classloader.getResource("keystore.https.jks").getPath());
                if (keystoreStream != null) {
                    e.load(keystoreStream, passphrase);
                    KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                    keyManagerFactory.init(e, passphrase);
                    SSLServerSocketFactory factory = NanoHTTPD.makeSSLSocketFactory(e, keyManagerFactory);
                    HTTPListener.SECURE = true;
                    makeSecure(factory, null);
                }
            }
        } catch (Exception ex) {
            android.util.Log.v("ADF","ADF "+ex);
        }


        // makeSecure(NanoHTTPD.makeSSLSocketFactory("keystore.jks", "123456789".toCharArray()), null);
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        System.out.println("\nRunning! Point your browsers to "+this.getBaseURL("localhost")+"\n");

        if(secure==true) {
            new HTTPListener(activity, context, r, false);
        }
    }

    public static String getBaseURL(String ip) {
        String ret = "http"+(HTTPListener.SECURE?"s":"")+"://"+ip+":"+HTTPListener.SSL_PORT+"/";
        android.util.Log.v("ADF","ADF "+ret);
        return ret;
    }

    @Override
    public Response serve(IHTTPSession session) {

        // Parse body to ensure POST data is parsed
        if(session.getMethod().equals(Method.POST)) {
            Map<String, String> files = new HashMap<String, String>();
            try {
                session.parseBody(files);
            } catch (Exception e) {
            }
        }
        Map<String, String> parms = session.getParms();
        String message = parms.get("msg");
        if (message != null && message.length()>0) {
            try {
                theReader.read(message);
            } catch(Exception e) {
                return getResponse(Status.INTERNAL_ERROR,"EX:"+e);
            }
        }
        return getResponse(Status.OK,this.theActivity.getLocalIpAddress(false, true));
    }

    private Response getResponse(IStatus status, String ret) {
        Response r =  newFixedLengthResponse(ret);
        r.setStatus(status);
        r.addHeader("Content-Length",""+ret.length());
        r.addHeader("Access-Control-Allow-Origin","*");
        // r.setUseGzip(false);
        r.setChunkedTransfer(false);
        return r;
    }
   
}
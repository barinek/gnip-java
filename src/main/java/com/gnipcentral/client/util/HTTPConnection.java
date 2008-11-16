package com.gnipcentral.client.util;

import com.gnipcentral.client.Config;
import com.gnipcentral.client.resource.Error;
import com.gnipcentral.client.resource.Translator;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;

import javax.xml.bind.JAXBException;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.zip.GZIPInputStream;
import java.util.Properties;
import java.util.Date;

public class HTTPConnection {

    private static final String USER_AGENT_STRING;

    static {
        String properties = "META-INF/maven/com.gnipcentral/gnip-client/pom.properties";
        InputStream is = null;
        String v = null;
        try {
            is = Thread.currentThread().getContextClassLoader().getResourceAsStream(properties);
            if(is != null) {
                Properties p = new Properties();
                p.load(is);
                v = p.getProperty("version");
            }
        }
        catch(Exception e) {
            System.err.println("Unable to load version number for the Gnip client library.  Exception: " + e);
        }
        finally {
            try{if(is != null) is.close();}catch(IOException ignore) {} 
            USER_AGENT_STRING = "Gnip-Client-Java/" + (v != null ? v : "build");
        }
    }

    private final Logger LOG = LoggerFactory.getInstance();
    private final Config config;

    public HTTPConnection(Config config) {
        this.config = config;
    }

    public InputStream doGet(String urlString) throws IOException {
        HttpURLConnection urlConnection = getConnection(urlString, HTTPMethod.GET);
        LOG.log("HTTP GET to %s\n", urlString);
        return getData(urlConnection);
    }

    public InputStream doPost(String urlString, byte[] data) throws IOException {
        HttpURLConnection urlConnection = getConnection(urlString, HTTPMethod.POST);
        LOG.log("HTTP POST to %s\n", urlString);
        if(!config.isUseGzip())
            LOG.log("with data\n  %s\n", data != null ? new String(data) : "");
        return transferData(data, urlConnection);
    }

    public InputStream doPut(String urlString, byte[] data) throws IOException {
        HttpURLConnection urlConnection = getConnection(urlString, HTTPMethod.PUT);
        LOG.log("HTTP PUT to %s\n", urlString);
        if(!config.isUseGzip())
            LOG.log("with data\n  %s\n", new String(data));
        return transferData(data, urlConnection);
    }

    public InputStream doDelete(String urlString) throws IOException {
        HttpURLConnection urlConnection = getConnection(urlString, HTTPMethod.DELETE);
        LOG.log("HTTP DELETE to %s\n", urlString);
        return getData(urlConnection);
    }

    private InputStream transferData(byte[] data, HttpURLConnection urlConnection) throws IOException {
        urlConnection.setDoOutput(true);
        urlConnection.setFixedLengthStreamingMode(data == null ? 0 : data.length);

        LOG.log("Starting data transfer at %s\n", (new Date()).toString());
        urlConnection.connect();

        if(data != null) {
            OutputStream out = urlConnection.getOutputStream();
            IOUtils.copy(new ByteArrayInputStream(data), out);
            out.flush();
        }
        LOG.log("Finished data transfer at %s\n", (new Date()).toString());
        LOG.log("Awaiting server response...\n");

        int responseCode = urlConnection.getResponseCode();
        LOG.log("Received response with response code %d\n", responseCode);

        if (responseCode != HttpURLConnection.HTTP_OK) {
            String responseMessage = urlConnection.getResponseMessage();
            String errorMessage = "";
            try {
                Error error = Translator.parseError(urlConnection.getErrorStream());
                errorMessage = error.getMessage();
            }
            catch(JAXBException e) {
                LOG.log("Exception occurred unmarshalling error message %s", e.getMessage());
            }
            throw new IOException("Error with request code: " + responseCode + " message: " + responseMessage + " " + errorMessage);
        }
        
        LOG.log("Starting data read at %s\n", (new Date()).toString());
        InputStream resultStream;
        InputStream stream;
        String contentEncoding = urlConnection.getHeaderField("Content-Encoding");
        if ("gzip".equalsIgnoreCase(contentEncoding)) {
            stream = new GZIPInputStream(urlConnection.getInputStream());
        } else {
            stream = urlConnection.getInputStream();
        }
        ByteArrayOutputStream resultData = new ByteArrayOutputStream();
        IOUtils.copy(stream, resultData);
        resultStream = new ByteArrayInputStream(resultData.toByteArray());
        urlConnection.disconnect();
        LOG.log("Finished data read at %s\n", (new Date()).toString());
        
        return resultStream;
    }

    private InputStream getData(HttpURLConnection urlConnection) throws IOException {
        urlConnection.connect();
        int responseCode = urlConnection.getResponseCode();
        String responseMessage = urlConnection.getResponseMessage();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("Error with request code:" + responseCode + " message: " + responseMessage);
        }
        InputStream resultStream;
        InputStream stream;
        String contentEncoding = urlConnection.getHeaderField("Content-Encoding");
        if ("gzip".equalsIgnoreCase(contentEncoding)) {
            stream = new GZIPInputStream(urlConnection.getInputStream());
        } else {
            stream = urlConnection.getInputStream();
        }
        ByteArrayOutputStream resultData = new ByteArrayOutputStream();
        IOUtils.copy(stream, resultData);
        resultStream = new ByteArrayInputStream(resultData.toByteArray());

        urlConnection.disconnect();
        return resultStream;
    }

    private HttpURLConnection getConnection(String urlString, HTTPMethod method) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestMethod(method.name());
        urlConnection.addRequestProperty("Content-Type", "application/xml");
        urlConnection.addRequestProperty("Authorization", "Basic " + new String(Base64.encodeBase64(getGnipCredentials()), Charset.forName("UTF-8")));
        urlConnection.addRequestProperty("User-Agent", USER_AGENT_STRING);
        urlConnection.setConnectTimeout(2000);
        urlConnection.setReadTimeout(config.getReadTimeout());
        if (config.isUseGzip()) {
            urlConnection.addRequestProperty("Accept-Encoding", "gzip");
            urlConnection.addRequestProperty("Content-Encoding", "gzip");
        }
        return urlConnection;
    }

    private byte[] getGnipCredentials() {
        String credentials = config.getUsername() + ":" + config.getPassword();
        return credentials.getBytes(Charset.forName("UTF-8"));
    }
}

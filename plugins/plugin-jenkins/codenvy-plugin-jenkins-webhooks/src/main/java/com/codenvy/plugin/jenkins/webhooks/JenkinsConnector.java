/*
 *  [2012] - [2017] Codenvy, S.A.
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.plugin.jenkins.webhooks;

import com.google.common.io.CharStreams;

import org.apache.commons.io.IOUtils;
import org.eclipse.che.api.core.ServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Base64;

import static com.google.common.base.Strings.isNullOrEmpty;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.xml.transform.TransformerFactory.newInstance;
import static org.eclipse.che.commons.lang.IoUtil.readAndCloseQuietly;

/**
 * Client for Jenkins API.
 * One {@link JenkinsConnector} is configured for one Jenkins job.
 *
 * @author Stephane Tournie
 * @author Igor Vinokur
 */
public class JenkinsConnector {

    private static final Logger LOG = LoggerFactory.getLogger(JenkinsConnector.class);

    // The name of the Jenkins job
    private final String jobName;
    // The URL of the XML configuration of the Jenkins job
    private final String url;

    /**
     * Constructor
     *
     * @param url
     *         the URL of the Jenkins instance to connect to
     * @param jobName
     *         the name of the Jenkins job
     */
    public JenkinsConnector(String url, String jobName) {
        this.jobName = jobName;
        this.url = url;
    }

    public void addFactoryLink(String factoryUrl) throws IOException, ServerException {
        Document configDocument = xmlToDocument(getCurrentJenkinsJobConfiguration());
        Node descriptionNode = configDocument.getDocumentElement().getElementsByTagName("description").item(0);

        if (!descriptionNode.getTextContent().contains(factoryUrl)) {
            updateJenkinsJobDescription(factoryUrl, configDocument);
        } else {
            LOG.debug("factory link {} already displayed in description of Jenkins job {}", factoryUrl, jobName);
        }
    }

    public void addBuildFailedFactoryLink(String factoryUrl) throws IOException, ServerException {
        Document configDocument = xmlToDocument(getCurrentJenkinsJobConfiguration());
        Node descriptionNode = configDocument.getDocumentElement().getElementsByTagName("description").item(0);
        String content = descriptionNode.getTextContent();
        if (!content.contains(factoryUrl)) {
            content = content.substring(0, content.indexOf("\n") > 0 ? content.indexOf("\n") : content.length());
            descriptionNode.setTextContent(content + "\n" + "build brake factory: <a href=\"" + factoryUrl + "\">" + factoryUrl + "</a>");
            updateJenkinsJobDescription(factoryUrl, configDocument);
        } else {
            LOG.debug("factory link {} already displayed in description of Jenkins job {}", factoryUrl, jobName);
        }
    }

    public String getBuildInfo(int buildId) throws IOException, ServerException {
//        URL url = new URL(this.url + "/job/" + jobName + "/" + buildId + "/api/json");
//        HttpURLConnection http = (HttpURLConnection)url.openConnection();
//        try {
//
//            http = (HttpURLConnection)url.openConnection();
//            http.setInstanceFollowRedirects(false);
//            http.setRequestMethod("GET");
//
//            if (url.getUserInfo() != null) {
//                String basicAuth = "Basic " + new String(Base64.getEncoder().encode(url.getUserInfo().getBytes()));
//                http.setRequestProperty("Authorization", basicAuth);
//            }
//
//            if (http.getResponseCode() / 100 != 2) {
//                throw new ServerException("");
//            }
//
//            String result;
//            try (InputStream input = http.getInputStream()) {
//                result = IOUtils.toString(input);
//            }
//
//            return result;
//
//        } finally {
//            if (http != null) {
//                http.disconnect();
//            }
//        }
        String requestUrl = url + "/job/" + jobName + "/" + buildId + "/api/json";
        return doRequest("GET", requestUrl, APPLICATION_XML, null);
    }

    private String getCurrentJenkinsJobConfiguration() throws IOException, ServerException {
//        URL url = new URL(this.url + "/job/" + jobName + "/config.xml");
//        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
//        if (url.getUserInfo() != null) {
//            String basicAuth = "Basic " + new String(Base64.getEncoder().encode(url.getUserInfo().getBytes()));
//            connection.setRequestProperty("Authorization", basicAuth);
//        }
//        connection.setRequestMethod("GET");
//        connection.addRequestProperty(HttpHeaders.CONTENT_TYPE, APPLICATION_XML);
//        final int responseCode = connection.getResponseCode();
//        if ((responseCode / 100) != 2) {
//            InputStream in = connection.getErrorStream();
//            if (in == null) {
//                in = connection.getInputStream();
//            }
//            final String str;
//            try (Reader reader = new InputStreamReader(in)) {
//                str = CharStreams.toString(reader);
//            }
//            throw new IOException(str);
//        }
//        return readAndCloseQuietly(connection.getInputStream());
        String requestUrl = url + "/job/" + jobName + "/config.xml";
        return doRequest("GET", requestUrl, APPLICATION_XML, null);
    }

    private void updateJenkinsJobDescription(String factoryUrl, Document configDocument) throws IOException, ServerException {
//        URL url = new URL(this.url + "/job/" + jobName + "/config.xml");
//        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
//        try {
//            if (url.getUserInfo() != null) {
//                String basicAuth = "Basic " + new String(Base64.getEncoder().encode(url.getUserInfo().getBytes()));
//                connection.setRequestProperty("Authorization", basicAuth);
//            }
//            connection.setRequestMethod("POST");
//            connection.addRequestProperty(HttpHeaders.CONTENT_TYPE, APPLICATION_XML);
//            connection.addRequestProperty(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
//            connection.setDoOutput(true);
//
//            try (OutputStream output = connection.getOutputStream()) {
//                output.write(documentToXml(configDocument).getBytes());
//            }
//            final int responseCode = connection.getResponseCode();
//            if ((responseCode / 100) != 2) {
//                InputStream in = connection.getErrorStream();
//                if (in == null) {
//                    in = connection.getInputStream();
//                }
//                final String str;
//                try (Reader reader = new InputStreamReader(in)) {
//                    str = CharStreams.toString(reader);
//                }
//                LOG.error(str);
//            } else {
//                LOG.debug("factory link {} successfully added on description of Jenkins job ", factoryUrl, jobName);
//            }
//        } finally {
//            if (connection != null) {
//                connection.disconnect();
//            }
//        }
        String requestUrl = url + "/job/" + jobName + "/config.xml";
        doRequest("POST", requestUrl, APPLICATION_XML, documentToXml(configDocument));
        LOG.debug("factory link {} successfully added on description of Jenkins job ", factoryUrl, jobName);
    }

    private String doRequest(String requestMethod,
                             String requestUrl,
                             String contentType,
                             String data) throws IOException, ServerException {
        URL url = new URL(requestUrl + "/job/" + jobName + "/config.xml");
        HttpURLConnection httpConnection = (HttpURLConnection)url.openConnection();
        try {
            if (url.getUserInfo() != null) {
                String basicAuth = "Basic " + new String(Base64.getEncoder().encode(url.getUserInfo().getBytes()));
                httpConnection.setRequestProperty("Authorization", basicAuth);
            }
            httpConnection.setRequestMethod(requestMethod);
            httpConnection.addRequestProperty(HttpHeaders.CONTENT_TYPE, contentType);
            httpConnection.addRequestProperty(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
            httpConnection.setDoOutput(true);

            if (!isNullOrEmpty(data))
                try (OutputStream output = httpConnection.getOutputStream()) {
                    output.write(data.getBytes());
                }
            final int responseCode = httpConnection.getResponseCode();
            if ((responseCode / 100) != 2) {
                InputStream in = httpConnection.getErrorStream();
                if (in == null) {
                    in = httpConnection.getInputStream();
                }
                final String str;
                try (Reader reader = new InputStreamReader(in)) {
                    str = CharStreams.toString(reader);
                }
                throw new ServerException(str);
            }
            return readAndCloseQuietly(httpConnection.getInputStream());
        } finally {
            if (httpConnection != null) {
                httpConnection.disconnect();
            }
        }
    }

    private String documentToXml(Document configDocument) throws ServerException {
        try {
            StringWriter writer = new StringWriter();
            newInstance().newTransformer().transform(new DOMSource(configDocument), new StreamResult(writer));
            return writer.toString();
        } catch (TransformerException e) {
            throw new ServerException(e.getLocalizedMessage());
        }
    }

    private Document xmlToDocument(String jobConfigXml) throws ServerException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(new ByteArrayInputStream(jobConfigXml.getBytes("utf-8")));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new ServerException(e.getMessage());
        }
    }
}


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
package com.codenvy.plugin.webhooks.connectors;

import com.google.common.io.CharStreams;

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
import java.net.URL;
import java.rmi.ServerException;
import java.util.Base64;

import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.xml.transform.TransformerFactory.newInstance;
import static org.eclipse.che.commons.lang.IoUtil.readAndCloseQuietly;

/**
 * Jenkins implementation of {@link Connector}
 * One {@link JenkinsConnector} is configured for one Jenkins job
 *
 * @author Stephane Tournie
 */
public class JenkinsConnector implements Connector {

    private static final Logger LOG = LoggerFactory.getLogger(JenkinsConnector.class);

    // The name of the Jenkins job
    private final String jobName;
    // The URL of the XML configuration of the Jenkins job
    private final String jobConfigXmlUrl;

    /**
     * Constructor
     *
     * @param url
     *         the URL of the Jenkins instance to connect to
     * @param jobName
     *         the name of the Jenkins job
     */
    public JenkinsConnector(final String url, final String jobName) {
        this.jobName = jobName;
        this.jobConfigXmlUrl = url + "/job/" + jobName + "/config.xml";
    }

    /**
     * Add a factory link to configured Jenkins job
     *
     * @param factoryUrl
     *         the factory URL to add
     */
    @Override
    public void addFactoryLink(String factoryUrl) throws IOException {
        Document configDocument = xmlToDocument(getCurrentJenkinsJobConfiguration());
        Node descriptionNode = configDocument.getDocumentElement().getElementsByTagName("description").item(0);

        if (!descriptionNode.getTextContent().contains(factoryUrl)) {
            updateJenkinsJobDescription(factoryUrl, configDocument);
        } else {
            LOG.debug("factory link {} already displayed on description of Jenkins job {}", factoryUrl, jobName);
        }
    }

    @Override
    public void addBuildFailedFactoryLink(String factoryUrl) throws IOException {
        Document configDocument = xmlToDocument(getCurrentJenkinsJobConfiguration());
        Node descriptionNode = configDocument.getDocumentElement().getElementsByTagName("description").item(0);
        String content = descriptionNode.getTextContent();
        if (!content.contains(factoryUrl)) {
            content = content.substring(0, content.indexOf("\n") > 0 ? content.indexOf("\n") : content.length());
            descriptionNode.setTextContent(content + "\n" + "build brake factory: <a href=\"" + factoryUrl + "\">" + factoryUrl + "</a>");
            updateJenkinsJobDescription(factoryUrl, configDocument);
        } else {
            LOG.debug("factory link {} already displayed on description of Jenkins job {}", factoryUrl, jobName);
        }
    }

    private String getCurrentJenkinsJobConfiguration() throws IOException {
        URL url = new URL(jobConfigXmlUrl);
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        if (url.getUserInfo() != null) {
            String basicAuth = "Basic " + new String(Base64.getEncoder().encode(url.getUserInfo().getBytes()));
            connection.setRequestProperty("Authorization", basicAuth);
        }
        connection.setRequestMethod("GET");
        connection.addRequestProperty(HttpHeaders.CONTENT_TYPE, APPLICATION_XML);
        final int responseCode = connection.getResponseCode();
        if ((responseCode / 100) != 2) {
            InputStream in = connection.getErrorStream();
            if (in == null) {
                in = connection.getInputStream();
            }
            final String str;
            try (Reader reader = new InputStreamReader(in)) {
                str = CharStreams.toString(reader);
            }
            throw new IOException(str);
        }
        return readAndCloseQuietly(connection.getInputStream());
    }

    private void updateJenkinsJobDescription(String factoryUrl, Document configDocument) throws IOException {
        URL url = new URL(jobConfigXmlUrl);
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        try {
            if (url.getUserInfo() != null) {
                String basicAuth = "Basic " + new String(Base64.getEncoder().encode(url.getUserInfo().getBytes()));
                connection.setRequestProperty("Authorization", basicAuth);
            }
            connection.setRequestMethod("POST");
            connection.addRequestProperty(HttpHeaders.CONTENT_TYPE, APPLICATION_XML);
            connection.addRequestProperty(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
            connection.setDoOutput(true);

            try (OutputStream output = connection.getOutputStream()) {
                output.write(documentToXml(configDocument).getBytes());
            }
            final int responseCode = connection.getResponseCode();
            if ((responseCode / 100) != 2) {
                InputStream in = connection.getErrorStream();
                if (in == null) {
                    in = connection.getInputStream();
                }
                final String str;
                try (Reader reader = new InputStreamReader(in)) {
                    str = CharStreams.toString(reader);
                }
                LOG.error(str);
            } else {
                LOG.debug("factory link {} successfully added on description of Jenkins job ", factoryUrl, jobName);
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
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
            throw new ServerException (e.getMessage());
        }
    }
}


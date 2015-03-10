/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2015] Codenvy, S.A.
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
package com.codenvy.im.config;

import com.codenvy.im.install.InstallOptions;
import com.codenvy.im.node.NodeConfig;
import com.codenvy.im.utils.HttpTransport;
import com.codenvy.im.utils.Version;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import javax.annotation.Nullable;
import javax.inject.Named;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static com.codenvy.im.utils.Commons.combinePaths;
import static java.lang.String.format;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.newInputStream;

/** @author Dmytro Nochevnov */
@Singleton
public class ConfigUtil {
    private final HttpTransport transport;
    private final String        updateEndpoint;

    @Inject
    public ConfigUtil(@Named("installation-manager.update_server_endpoint") String updateEndpoint,
                      HttpTransport transport) {
        this.transport = transport;
        this.updateEndpoint = updateEndpoint;
    }

    /** Loads properties from the given file. */
    public Map<String, String> loadConfigProperties(Path confFile) throws IOException {
        if (!exists(confFile)) {
            throw new FileNotFoundException(format("Configuration file '%s' not found", confFile.toString()));
        }

        try (InputStream in = newInputStream(confFile)) {
            return doLoad(in);
        } catch (IOException e) {
            throw new ConfigException(format("Can't load properties: %s", e.getMessage()), e);
        }
    }

    /** Loads properties from the given file. */
    public Map<String, String> loadConfigProperties(String confFile) throws IOException {
        return loadConfigProperties(Paths.get(confFile));
    }

    /** Loads default properties. */
    public Map<String, String> loadCodenvyDefaultProperties(String version, InstallOptions.InstallType installType) throws IOException {
        Path tmpDir = Paths.get(System.getProperty("java.io.tmpdir"));

        String requestUrl = combinePaths(updateEndpoint, "/repository/public/download/codenvy-" +
                                                         (installType == InstallOptions.InstallType.CODENVY_MULTI_SERVER ? "multi" : "single")
                                                         + "-server-properties/" + version);
        Path properties;
        try {
            properties = transport.download(requestUrl, tmpDir);
        } catch (IOException e) {
            throw new IOException("Can't download installation properties. " + e.getMessage(), e);
        }

        try (InputStream in = newInputStream(properties)) {
            return doLoad(in);
        } catch (IOException e) {
            throw new ConfigException(format("Can't load properties: %s", e.getMessage()), e);
        }
    }

    /**
     * Merges two bunches of the properties from old and new configurations. As a rule method keeps the values of old configuration
     * except the {@link com.codenvy.im.config.Config#VERSION} property
     */
    public Map<String, String> merge(Map<String, String> oldProps, Map<String, String> newProps) {
        Map<String, String> m = new HashMap<>(oldProps);

        for (Map.Entry<String, String> e : newProps.entrySet()) {
            String aioOldKey = "aio_" + e.getKey();

            if (m.containsKey(aioOldKey)) {
                m.put(e.getKey(), m.get(aioOldKey));
                m.remove(aioOldKey);
            } else if (!m.containsKey(e.getKey())) {
                m.put(e.getKey(), e.getValue());
            }
        }

        m.remove(Config.VERSION);
        if (newProps.containsKey(Config.VERSION)) {
            m.put(Config.VERSION, newProps.get(Config.VERSION));
        }

        return m;
    }

    /**
     * Loads properties of the installed cdec artifact.
     * <p/>
     * The properties file has the follow format:
     * $property="value"
     * ...
     * <p/>
     * Finally method removes leading '$' for key name and quota characters for its value.
     */
    public Map<String, String> loadInstalledCodenvyProperties(InstallOptions.InstallType installType) throws IOException {
        Map<String, String> properties = new HashMap<>();

        Iterator<Path> files = getCodenvyPropertiesFiles(installType);
        while (files.hasNext()) {
            Path propertiesFile = files.next();

            try (InputStream in = newInputStream(propertiesFile)) {
                Map<String, String> m = doLoad(in);

                for (Map.Entry<String, String> e : m.entrySet()) {
                    String key = e.getKey().trim();
                    if (key.startsWith("$")) {
                        key = key.substring(1); // remotes '$'
                        String value = e.getValue().substring(1, e.getValue().length() - 1); // removes "

                        properties.put(key, value);
                    }
                }

            } catch (IOException e) {
                throw new ConfigException(format("Can't load Codenvy properties: %s", e.getMessage()), e);
            }
        }

        return properties;
    }

    protected Iterator<Path> getCodenvyPropertiesFiles(InstallOptions.InstallType installType) {
        switch (installType) {
            case CODENVY_MULTI_SERVER:
                return ImmutableList.of(Paths.get("/etc/puppet/" + Config.MULTI_SERVER_PROPERTIES),
                                        Paths.get("/etc/puppet/" + Config.MULTI_SERVER_BASE_PROPERTIES)).iterator();

            case CODENVY_SINGLE_SERVER:
            default:
                return ImmutableList.of(Paths.get("/etc/puppet/" + Config.SINGLE_SERVER_PROPERTIES),
                                        Paths.get("/etc/puppet/" + Config.SINGLE_SERVER_BASE_PROPERTIES)).iterator();
        }
    }

    protected Map<String, String> doLoad(InputStream in) throws IOException {
        Properties properties = new Properties();
        properties.load(in);

        Map<String, String> m = new HashMap<>();
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            String key = entry.getKey().toString().toLowerCase();
            String value = entry.getValue().toString();

            m.put(key, value);
        }

        return m;
    }

    /** @return list of replacements for multi-node master puppet config file Config.MULTI_SERVER_NODES_PROPERTIES based on the node configs. */
    public static Map<String, String> getPuppetNodesConfigReplacement(List<NodeConfig> nodeConfigs) {
        Map<String, String> replacements = new HashMap<>(nodeConfigs.size());

        for (NodeConfig node : nodeConfigs) {
            NodeConfig.NodeType type = node.getType();
            switch (type) {
                case DATA:
                case API:
                case SITE:
                case DATASOURCE:
                case ANALYTICS: {
                    String replacingToken = format("%s.example.com", type.toString().toLowerCase());
                    String replacement = node.getHost();
                    replacements.put(replacingToken, replacement);
                    break;
                }

                case BUILDER: {
                    String replacingToken = "builder.*example.com";
                    String replacement = format("builder\\\\d+\\\\%s", getBaseNodeDomain(node));
                    replacements.put(replacingToken, replacement);
                    break;
                }

                case RUNNER: {
                    String replacingToken = "runner.*example.com";
                    String replacement = format("runner\\\\d+\\\\%s", getBaseNodeDomain(node));
                    replacements.put(replacingToken, replacement);
                    break;
                }

                default:
                    break;
            }
        }

        return replacements;
    }

    public static String getBaseNodeDomain(NodeConfig node) {
        String regex = format("^%s\\d+", node.getType().toString().toLowerCase());
        return node.getHost().toLowerCase().replaceAll(regex, "");
    }

}
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

import com.codenvy.plugin.jenkins.webhooks.shared.JenkinsEvent;
import com.codenvy.plugin.webhooks.connectors.JenkinsConnector;

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.model.factory.Factory;
import org.eclipse.che.api.core.rest.Service;
import org.eclipse.che.api.factory.server.FactoryManager;
import org.eclipse.che.api.factory.server.model.impl.FactoryImpl;
import org.eclipse.che.api.user.server.UserManager;
import org.eclipse.che.commons.lang.Pair;
import org.eclipse.che.dto.server.DtoFactory;
import org.eclipse.che.inject.ConfigurationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toSet;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/jenkins-webhook")
public class JenkinsWebhookService extends Service{

    private static final Logger LOG = LoggerFactory.getLogger(JenkinsWebhookService.class);

    private static final String JENKINS_EVENT                     = "Jenkins-Event";
    private static final String WEBHOOK_PROPERTY_PATTERN          = "env.CODENVY_.+_WEBHOOK_.+";
    private static final String WEBHOOK_REPOSITORY_URL_SUFFIX     = "_REPOSITORY_URL";
    private static final String WEBHOOK_FACTORY_ID_SUFFIX_PATTERN = "_FACTORY.+_ID";
    private static final String JENKINS_CONNECTOR_PREFIX_PATTERN  = "env.CODENVY_JENKINS_CONNECTOR_.+";
    private static final String JENKINS_CONNECTOR_URL_SUFFIX      = "_URL";
    private static final String JENKINS_CONNECTOR_JOB_NAME_SUFFIX = "_JOB_NAME";

    private final FactoryManager          factoryManager;
    private final UserManager             userManager;
    private final ConfigurationProperties configurationProperties;
    private final String                  username;

    @Inject
    public JenkinsWebhookService(FactoryManager factoryManager,
                                 UserManager userManager,
                                 ConfigurationProperties configurationProperties,
                                 @Named("che.api") String baseUrl,
                                 @Named("integration.factory.owner.username") String username) {
        this.factoryManager = factoryManager;
        this.userManager = userManager;
        this.configurationProperties = configurationProperties;
        this.username = username;
    }

    @POST
    @Consumes(APPLICATION_JSON)
    public Response handleWebhookEvent(@Context HttpServletRequest request) throws ServerException {
        Response response = Response.ok().build();
        try (ServletInputStream inputStream = request.getInputStream()) {
            String jenkinsHeader = request.getHeader(JENKINS_EVENT);
            if (inputStream != null && !isNullOrEmpty(jenkinsHeader)) {
                JenkinsEvent jenkinsEvent = DtoFactory.getInstance().createDtoFromJson(inputStream, JenkinsEvent.class);
                updateFailedFactoriesForRepositoryAndBranch(jenkinsEvent);
            }
        } catch (IOException | ConflictException | NotFoundException e) {
            LOG.error(e.getLocalizedMessage());
            throw new ServerException(e.getLocalizedMessage());
        }
        return response;
    }

    /**
     * Get factories configured in a webhook for given base repository
     * and contain a project for given head repository and head branch
     *
     * @param repositoryUrl
     *         the URL of the repository for which a webhook is configured
     * @return the factories configured in a webhook and that contain a project that matches given repo and branch
     */
    private Set<String> getWebhookConfiguredFactoriesIDs(final String repositoryUrl) {
        Map<String, String> properties = configurationProperties.getProperties(WEBHOOK_PROPERTY_PATTERN);

        Set<String> webhooks = properties.entrySet()
                                         .stream()
                                         .filter(entry -> repositoryUrl.equals(entry.getValue()))
                                         .map(entry -> entry.getKey()
                                                            .substring(0, entry.getKey().lastIndexOf(WEBHOOK_REPOSITORY_URL_SUFFIX)))
                                         .collect(toSet());

        if (webhooks.isEmpty()) {
            LOG.warn("No webhooks were registered for repository {}", repositoryUrl);
            return emptySet();
        }

        return properties.entrySet()
                         .stream()
                         .filter(entry -> webhooks.stream()
                                                  .anyMatch(webhook -> entry.getKey().matches(webhook + WEBHOOK_FACTORY_ID_SUFFIX_PATTERN)))
                         .map(Map.Entry::getValue)
                         .collect(toSet());
    }

    private List<Factory> getUserFactories() throws NotFoundException, ServerException {
        List<Factory> factories = new ArrayList<>();
        List<Factory> request;
        int skipCount = 0;
        do {
            request = factoryManager.getByAttribute(30,
                                                    skipCount,
                                                    singletonList(Pair.of("creator.userId", userManager.getByName(username).getId())));
            factories.addAll(request);
            skipCount += request.size();
        } while (request.size() == 30);
        return factories;
    }

    private List<Factory> createFailedFactories(String repositoryUrl,
                                                String commitId) throws ConflictException, ServerException, NotFoundException {
        List<Factory> failedFactories = new ArrayList<>();
        for (String factoryId : getWebhookConfiguredFactoriesIDs(repositoryUrl)) {
            FactoryImpl factory = (FactoryImpl)factoryManager.getById(factoryId);
            factory.setName(factory.getName() + "_failed");
            factory.getWorkspace()
                   .getProjects()
                   .stream()
                   .filter(project -> project.getSource().getLocation().equals(repositoryUrl))
                   .forEach(project -> project.getSource().getParameters().put("commitId", commitId));
            failedFactories.add(factoryManager.saveFactory(factory));
        }
        return failedFactories;
    }

    private void updateFailedFactoriesForRepositoryAndBranch(JenkinsEvent jenkinsEvent) throws ServerException,
                                                                                               ConflictException,
                                                                                               NotFoundException,
                                                                                               IOException {
        LOG.debug("{}", jenkinsEvent);

        String jenkinsUrl = jenkinsEvent.getJenkinsUrl();

        String repositoryUrl;
        String commitId;


        Map<String, String> properties = configurationProperties.getProperties(JENKINS_CONNECTOR_PREFIX_PATTERN);

        Optional<JenkinsConnector> optional =
                properties.entrySet()
                          .stream()
                          .filter(e -> e.getValue().contains(jenkinsUrl.substring(jenkinsUrl.indexOf("://") + 3, jenkinsUrl.length() - 1)))
                          .map(entry -> {
                              String connector = entry.getKey().substring(0, entry.getKey().lastIndexOf(JENKINS_CONNECTOR_URL_SUFFIX));
                              return new JenkinsConnector(properties.get(connector + JENKINS_CONNECTOR_URL_SUFFIX),
                                                          properties.get(connector + JENKINS_CONNECTOR_JOB_NAME_SUFFIX));
                          })
                          .findAny();
        if (optional.isPresent()) {
            JenkinsConnector jenkinsConnector = optional.get();
            String buildInfo = jenkinsConnector.getBuildInfo(jenkinsEvent.getBuildId());
            commitId = buildInfo.substring(buildInfo.indexOf("SHA1"), )
        }

//        List<Factory> failedFactories =
//                getUserFactories().stream()
//                                  .filter(f -> f.getWorkspace()
//                                                .getProjects()
//                                                .stream()
//                                                .anyMatch(workspace -> repositoryUrl.equals(workspace.getSource().getLocation()) &&
//                                                                       commitId.equals(workspace.getSource()
//                                                                                                .getParameters()
//                                                                                                .get("commitId"))))
//                                  .collect(toList());
//
//        for (Factory factory : failedFactories.isEmpty() ? createFailedFactories(repositoryUrl, commitId) : failedFactories) {
//            List<Connector> connectors = getConnectorsByUrl(jenkinsEvent.getJenkinsUrl());
//            for (Connector connector : connectors) {
//                connector.addBuildFailedFactoryLink(baseUrl.substring(0, baseUrl.indexOf("/api")) + "/f?id=" + factory.getId());
//            }
//        }
    }

}

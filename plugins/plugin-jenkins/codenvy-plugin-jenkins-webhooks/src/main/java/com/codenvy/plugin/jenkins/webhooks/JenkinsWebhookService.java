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
import com.codenvy.plugin.webhooks.AuthConnection;
import com.codenvy.plugin.webhooks.FactoryConnection;
import com.codenvy.plugin.webhooks.BaseWebhookService;
import com.codenvy.plugin.webhooks.connectors.Connector;

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.model.factory.Factory;
import org.eclipse.che.api.factory.server.FactoryManager;
import org.eclipse.che.api.factory.shared.dto.FactoryDto;
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
import java.util.Set;

import static com.codenvy.plugin.webhooks.CloneUrlMatcher.DEFAULT_CLONE_URL_MATCHER;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/jenkins-webhook")
public class JenkinsWebhookService extends BaseWebhookService {

    private static final Logger LOG = LoggerFactory.getLogger(JenkinsWebhookService.class);

    private static final String JENKINS_EVENT                     = "Jenkins-Event";
    private static final String WEBHOOK_PROPERTY_PATTERN          = "env.CODENVY_.+_WEBHOOK_.+";
    private static final String WEBHOOK_REPOSITORY_URL_SUFFIX     = "_REPOSITORY_URL";
    private static final String WEBHOOK_FACTORY_ID_SUFFIX_PATTERN = "_FACTORY.+_ID";

    private final FactoryManager factoryManager;
    private final UserManager    userManager;
    private final ConfigurationProperties configurationProperties;
    private final String baseUrl;
    private final String username;

    @Inject
    public JenkinsWebhookService(AuthConnection authConnection,
                                 FactoryConnection factoryConnection,
                                 FactoryManager factoryManager,
                                 UserManager userManager,
                                 ConfigurationProperties configurationProperties,
                                 @Named("che.api") String baseUrl,
                                 @Named("integration.factory.owner.username") String username,
                                 @Named("integration.factory.owner.password") String password) {
        super(authConnection, factoryConnection, configurationProperties, username, password);
        this.factoryManager = factoryManager;
        this.userManager = userManager;
        this.configurationProperties = configurationProperties;
        this.baseUrl = baseUrl;
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
                updateFailedFactoriesForRepositoryAndBranch(getWebhookConfiguredFactoriesIDs(jenkinsEvent.getRepositoryUrl()),
                                                            jenkinsEvent);
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

        Set<String> webhooks =
                properties.entrySet()
                          .stream()
                          .filter(entry -> repositoryUrl.equals(entry.getValue()))
                          .map(entry -> entry.getKey().substring(0, entry.getKey().lastIndexOf(WEBHOOK_REPOSITORY_URL_SUFFIX)))
                          .collect(toSet());

        if (webhooks.isEmpty()) {
            LOG.warn("No webhooks were registered for repository {}", repositoryUrl);
        }

        return properties.entrySet()
                         .stream()
                         .filter(entry -> webhooks.stream()
                                                  .anyMatch(webhook -> entry.getKey().matches(webhook + WEBHOOK_FACTORY_ID_SUFFIX_PATTERN)))
                         .map(Map.Entry::getValue)
                         .collect(toSet());
    }

    private void updateFailedFactoriesForRepositoryAndBranch(Set<String> factoryIDs, JenkinsEvent jenkinsEvent) throws ServerException,
                                                                                                                       ConflictException,
                                                                                                                       NotFoundException,
                                                                                                                       IOException {
        LOG.debug("{}", jenkinsEvent);

        String repositoryUrl = jenkinsEvent.getRepositoryUrl();
        String branch = jenkinsEvent.getBranch();
        String commitId = jenkinsEvent.getCommitId();

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

        List<Factory> failedFactories =
                factories.stream()
                         .filter(f -> f.getWorkspace()
                                       .getProjects()
                                       .stream()
                                       .anyMatch(workspace -> repositoryUrl.equals(workspace.getSource().getLocation()) &&
                                                              commitId.equals(workspace.getSource().getParameters().get("commitId"))))
                         .collect(toList());
        if (failedFactories.isEmpty()) {
            List<FactoryDto> baseFactories = getFactoriesForRepositoryAndBranch(factoryIDs,
                                                                                repositoryUrl,
                                                                                branch,
                                                                                DEFAULT_CLONE_URL_MATCHER);
            for (FactoryDto factoryDto : baseFactories) {
                FactoryDto updatedFactory = updateProjectInFactory(factoryDto,
                                                                   repositoryUrl,
                                                                   branch,
                                                                   repositoryUrl,
                                                                   commitId,
                                                                   DEFAULT_CLONE_URL_MATCHER);
                updatedFactory.setName("failed" + updatedFactory.getName());
                failedFactories.add(factoryManager.saveFactory(updatedFactory));
            }
        }

        for (Factory factory : failedFactories) {
            List<Connector> connectors = getConnectorsByUrl(jenkinsEvent.getJenkinsUrl());
            for (Connector connector : connectors) {
                connector.addBuildFailedFactoryLink(baseUrl.substring(0, baseUrl.indexOf("/api")) + "/f?id=" + factory.getId());
            }
        }
    }

}

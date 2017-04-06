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
package com.codenvy.plugin.webhooks.dto;

import org.eclipse.che.dto.shared.DTO;

@DTO
public interface JenkinsEvent {
    String getRepositoryUrl();

    void setRepositoryUrl(String repositoryUrl);

    String getBranch();

    void setBranch(String branch);

    String getCommitId();

    void setCommitId(String commitId);

    String getJenkinsUrl();

    void setJenkinsUrl(String jenkinsUrl);

    String getJenkinsJobName();

    void setJenkinsJobName(String jenkinsJobName);
}

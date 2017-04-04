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
package com.codenvy.user.email.template;

import com.codenvy.template.processor.html.thymeleaf.ThymeleafTemplate;

/**
 * Thymeleaf template for user that created by admins.
 *
 * @author Anton Korneta
 */
public class CreateUserWithPasswordTemplate extends ThymeleafTemplate {

    public CreateUserWithPasswordTemplate(String masterEndpoint,
                                          String resetPasswordLink,
                                          String userName) {
        context.setVariable("masterEndpoint", masterEndpoint);
        context.setVariable("resetPasswordLink", resetPasswordLink);
        context.setVariable("userName", userName);
    }

    @Override
    public String getPath() {
        return "/email-templates/user_created_with_password";
    }

}

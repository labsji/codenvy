/*
 *
 * CODENVY CONFIDENTIAL
 * ________________
 *
 * [2012] - [2013] Codenvy, S.A.
 * All Rights Reserved.
 * NOTICE: All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any. The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.analytics.metrics.users;

import com.codenvy.analytics.metrics.*;

import javax.annotation.security.RolesAllowed;

/** @author <a href="mailto:abazko@codenvy.com">Anatoliy Bazko</a> */
@RolesAllowed({"system/admin", "system/manager"})
@OmitFilters({MetricFilter.WS})
public class CreatedUsersSet extends AbstractSetValueResulted {

    public CreatedUsersSet() {
        super(MetricType.CREATED_USERS_SET, USER);
    }

    @Override
    public String getStorageCollectionName() {
        return getStorageCollectionName(MetricType.CREATED_USERS);
    }

    @Override
    public Context applySpecificFilter(Context clauses) {
        if (!clauses.exists(MetricFilter.USER)) {
            Context.Builder builder = new Context.Builder(clauses);
            builder.put(MetricFilter.USER, Parameters.WS_TYPES.PERSISTENT.name());
            return builder.build();
        }

        return clauses;
    }

    @Override
    public String getDescription() {
        return "Created registered users";
    }
}

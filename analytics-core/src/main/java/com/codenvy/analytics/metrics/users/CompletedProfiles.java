/*
 *
 * CODENVY CONFIDENTIAL
 * ________________
 *
 * [2012] - [2014] Codenvy, S.A.
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

import com.codenvy.analytics.datamodel.LongValueData;
import com.codenvy.analytics.datamodel.ValueData;
import com.codenvy.analytics.metrics.Context;
import com.codenvy.analytics.metrics.MetricFilter;
import com.codenvy.analytics.metrics.MetricType;
import com.codenvy.analytics.metrics.OmitFilters;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

import javax.annotation.security.RolesAllowed;

/** @author Anatoliy Bazko */
@RolesAllowed({"system/admin", "system/manager"})
@OmitFilters({MetricFilter.WS})
public class CompletedProfiles extends AbstractUsersProfile {

    private final String VALUE = "value";

    public CompletedProfiles() {
        super(MetricType.COMPLETED_PROFILES);
    }

    @Override
    public String[] getTrackedFields() {
        return new String[]{VALUE};
    }

    @Override
    public String getStorageCollectionName() {
        return getStorageCollectionName(MetricType.USERS_PROFILES_LIST);
    }

    @Override
    public DBObject[] getSpecificDBOperations(Context clauses) {
        DBObject match = new BasicDBObject();
        match.put(ID, new BasicDBObject("$ne", ""));
        match.put(USER_COMPANY, new BasicDBObject("$ne", ""));
        match.put(USER_FIRST_NAME, new BasicDBObject("$ne", ""));
        match.put(USER_LAST_NAME, new BasicDBObject("$ne", ""));
        match.put(USER_JOB, new BasicDBObject("$ne", ""));
        match.put(USER_PHONE, new BasicDBObject("$ne", ""));

        DBObject count = new BasicDBObject();
        count.put(ID, null);
        count.put(VALUE, new BasicDBObject("$sum", 1));

        return new DBObject[]{new BasicDBObject("$match", match),
                              new BasicDBObject("$group", count)};
    }

    @Override
    public Class<? extends ValueData> getValueDataClass() {
        return LongValueData.class;
    }

    @Override
    public String getDescription() {
        return "The number of uses with completed profiles";
    }
}

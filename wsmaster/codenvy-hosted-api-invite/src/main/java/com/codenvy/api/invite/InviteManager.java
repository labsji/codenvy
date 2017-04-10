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
package com.codenvy.api.invite;

import com.codenvy.api.invite.event.InviteCreatedEvent;
import com.codenvy.api.permission.server.PermissionsManager;
import com.codenvy.api.workspace.server.WorkspaceDomain;
import com.codenvy.organization.api.permissions.OrganizationDomain;
import com.codenvy.shared.invite.model.Invite;
import com.codenvy.spi.invite.InviteDao;
import com.google.inject.persist.Transactional;

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.Page;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.user.server.UserManager;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.subject.Subject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * Facade for invite related operations.
 *
 * @author Sergii Leschenko
 */
@Singleton
public class InviteManager {
    private final PermissionsManager permissionsManager;
    private final UserManager        userManager;
    private final InviteDao          inviteDao;
    private final EventService       eventService;

    @Inject
    public InviteManager(PermissionsManager permissionsManager,
                         UserManager userManager,
                         InviteDao inviteDao,
                         EventService eventService) {
        this.permissionsManager = permissionsManager;
        this.userManager = userManager;
        this.inviteDao = inviteDao;
        this.eventService = eventService;
    }

    /**
     * Stores (create or updates) invite.
     *
     * <p>It also send email invite on initial invite creation.
     *
     * @param invite
     *         invite to store
     * @throws ConflictException
     *         when user is specified email is already registered
     * @throws ServerException
     *         when any other error occurs during invite storing
     */
    @Transactional(rollbackOn = {RuntimeException.class, ServerException.class})
    public void store(Invite invite) throws NotFoundException, ConflictException, ServerException {
        requireNonNull(invite, "Required non-null invite");
        String domainId = invite.getDomainId();
        if (!OrganizationDomain.DOMAIN_ID.equals(domainId) && !WorkspaceDomain.DOMAIN_ID.equals(domainId)) {
            throw new ConflictException("Invitations for specified domain are not supported");
        }
        permissionsManager.checkActionsSupporting(domainId, invite.getActions());

        try {
            userManager.getByEmail(invite.getEmail());
            throw new ConflictException("User with specified id is already registered");
        } catch (NotFoundException ignored) {
        }

        Optional<InviteImpl> existingInvite = inviteDao.store(new InviteImpl(invite));
        if (!existingInvite.isPresent()) {
            Subject currentSubject = EnvironmentContext.getCurrent().getSubject();
            eventService.publish(new InviteCreatedEvent(currentSubject.isAnonymous() ? null : currentSubject.getUserId(),
                                                        invite));
        }
    }

    public Invite getInvite(String domainId, String instanceId, String email) throws NotFoundException, ServerException {
        requireNonNull(domainId, "Required non-null domain id");
        requireNonNull(instanceId, "Required non-null instance id");
        requireNonNull(email, "Required non-null email");
        return inviteDao.getInvite(domainId, instanceId, email);
    }

    /**
     * Returns invites for specified email.
     *
     * @param email
     *         email to retrieve invites
     * @param maxItems
     *         the maximum number of invites to return
     * @param skipCount
     *         the number of invites to skip
     * @return invites for specified email
     * @throws ServerException
     *         when any other error occurs during invites fetching
     */
    public Page<? extends Invite> getInvites(String email, long skipCount, int maxItems) throws ServerException {
        requireNonNull(email, "Required non-null email");
        return inviteDao.getInvites(email, skipCount, maxItems);
    }

    /**
     * Returns invites for specified instance.
     *
     * @param domainId
     *         domain id to which specified instance belong to
     * @param instanceId
     *         instance id
     * @param maxItems
     *         the maximum number of invites to return
     * @param skipCount
     *         the number of invites to skip
     * @return invites for specified instance
     * @throws ServerException
     *         when any other error occurs during invites fetching
     */
    public Page<? extends Invite> getInvites(String domainId, String instanceId, long skipCount, int maxItems) throws ServerException {
        requireNonNull(domainId, "Required non-null domain id");
        requireNonNull(instanceId, "Required non-null instance id");
        return inviteDao.getInvites(domainId, instanceId, skipCount, maxItems);
    }

    /**
     * Removes invite of email related to the particular instance.
     *
     * @param domainId
     *         domainId id
     * @param instanceId
     *         instanceId id
     * @param email
     *         email
     * @throws ServerException
     *         when any other error occurs during permissions removing
     */
    public void remove(String domainId, String instanceId, String email) throws ServerException {
        requireNonNull(domainId, "Required non-null domain id");
        requireNonNull(instanceId, "Required non-null instance id");
        requireNonNull(email, "Required non-null email");

        inviteDao.remove(domainId, instanceId, email);
    }
}

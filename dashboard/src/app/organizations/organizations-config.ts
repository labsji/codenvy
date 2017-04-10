/*
 *  [2015] - [2017] Codenvy, S.A.
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
'use strict';
import {OrganizationsItemController} from './list-organizations/organizations-item/organizations-item.controller';
import {OrganizationsItem} from './list-organizations/organizations-item/organizations-item.directive';
import {ListOrganizationsController} from './list-organizations/list-organizations.controller';
import {ListOrganizations} from './list-organizations/list-organizations.directive';
import {OrganizationsController} from './organizations.controller';
import {CreateOrganizationController} from './create-organizations/create-organizations.controller';
import {OrganizationDetailsController} from './organization-details/organization-details.controller';
import {ListOrganizationMembersController} from './organization-details/organization-members/list-organization-members.controller';
import {ListOrganizationInviteMembersController} from './organization-details/organization-invite-members/list-organization-invite-members.controller';
import {ListOrganizationInviteMembers} from './organization-details/organization-invite-members/list-organization-invite-members.directive';
import {ListOrganizationMembers} from './organization-details/organization-members/list-organization-members.directive';
import {OrganizationMemberDialogController} from './organization-details/organization-member-dialog/organization-member-dialog.controller';
import {OrganizationsPermissionService} from './organizations-permission.service';
import {OrganizationsConfigService} from './organizations-config.service';
import {OrganizationNotFound} from './organization-details/organization-not-found/organization-not-found.directive';
import {OrganizationSelectMembersDialogController} from './organization-details/organization-select-members-dialog/organization-select-members-dialog.controller';
import {OrganizationMemberItem} from './organization-details/organization-select-members-dialog/organization-member-item/organization-member-item.directive';

/**
 * The configuration of teams, defines controllers, directives and routing.
 *
 * @author Oleksii Orel
 */
export class OrganizationsConfig {

  constructor(register: any) {
    register.controller('OrganizationsController', OrganizationsController);

    register.controller('OrganizationDetailsController', OrganizationDetailsController);

    register.controller('OrganizationsItemController', OrganizationsItemController);
    register.directive('organizationsItem', OrganizationsItem);

    register.controller('ListOrganizationMembersController', ListOrganizationMembersController);
    register.directive('listOrganizationMembers', ListOrganizationMembers);

    register.directive('listOrganizationInviteMembers', ListOrganizationInviteMembers);
    register.controller('ListOrganizationInviteMembersController', ListOrganizationInviteMembersController);

    register.controller('OrganizationMemberDialogController', OrganizationMemberDialogController);

    register.controller('CreateOrganizationController', CreateOrganizationController);

    register.controller('ListOrganizationsController', ListOrganizationsController);
    register.directive('listOrganizations', ListOrganizations);

    register.service('organizationsPermissionService', OrganizationsPermissionService);
    register.service('organizationsConfigService', OrganizationsConfigService);

    register.directive('organizationNotFound', OrganizationNotFound);

    register.controller('OrganizationSelectMembersDialogController', OrganizationSelectMembersDialogController);
    register.directive('organizationMemberItem', OrganizationMemberItem);

    const organizationDetailsLocationProvider = {
      title: (params: any) => {
        return params.organizationName;
      },
      reloadOnSearch: false,
      templateUrl: 'app/organizations/organization-details/organization-details.html',
      controller: 'OrganizationDetailsController',
      controllerAs: 'organizationDetailsController',
      resolve: {
        initData: ['organizationsConfigService', (organizationConfigService: OrganizationsConfigService) => {
          return organizationConfigService.resolveOrganizationDetailsRoute();
        }],
      }
    };

    const createOrganizationLocationProvider = {
      title: 'New Organization',
      templateUrl: 'app/organizations/create-organizations/create-organizations.html',
      controller: 'CreateOrganizationController',
      controllerAs: 'createOrganizationController',
      resolve: {
        initData: ['organizationsConfigService', (organizationsConfigService: OrganizationsConfigService) => {
          return organizationsConfigService.resolveCreateOrganizationRoute();
        }]
      }
    };

    // config routes
    register.app.config(($routeProvider: any) => {
      $routeProvider.accessWhen('/organizations', {
        title: 'organizations',
        templateUrl: 'app/organizations/organizations.html',
        controller: 'OrganizationsController',
        controllerAs: 'organizationsController'
      })
        .accessWhen('/admin/create-organization', createOrganizationLocationProvider)
        .accessWhen('/admin/create-organization/:parentQualifiedName*', createOrganizationLocationProvider)
        .accessWhen('/organization/:organizationName*', organizationDetailsLocationProvider);
    });
  }
}

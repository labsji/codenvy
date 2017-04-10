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
import {CodenvyAPIBuilder} from '../builder/codenvy-api-builder.factory';


/**
 * This class is providing helper methods for simulating a fake HTTP backend simulating
 * @author Florent Benoit
 * @author Oleksii Orel
 */
export class CodenvyHttpBackend {
  private httpBackend: ng.IHttpBackendService;
  private defaultBranding: any;
  private teamsMap: Map<string, any>;
  private organizationsMap: Map<string, codenvy.IOrganization>;
  private permissionsMap: Map<string, Array<codenvy.IPermissions>>;
  private resourcesMap: Map<string, Map<string, any>>;

  /**
   * Constructor to use
   */
  constructor($httpBackend: ng.IHttpBackendService, codenvyAPIBuilder: CodenvyAPIBuilder) {
    this.httpBackend = $httpBackend;

    this.defaultBranding = {};

    this.teamsMap = new Map();

    this.organizationsMap = new Map();

    this.permissionsMap = new Map();

    this.resourcesMap = new Map();

    this.httpBackend.when('OPTIONS', '/api/').respond({});

    // change password
    this.httpBackend.when('POST', '/api/user/password').respond(() => {
      return [200, {success: true, errors: []}];
    });

    // create new user
    this.httpBackend.when('POST', '/api/user').respond(() => {
      return [200, {success: true, errors: []}];
    });
    //license legality - true
    this.httpBackend.when('GET', '/api/license/system/legality').respond({isLegal: true});

    // admin role - false
    this.httpBackend.when('GET', '/api/user/inrole?role=admin&scope=system&scopeId=').respond(false);
    // user role - true
    this.httpBackend.when('GET', '/api/user/inrole?role=user&scope=system&scopeId=').respond(true);
    // branding
    this.httpBackend.when('GET', 'assets/branding/product.json').respond(this.defaultBranding);
  }

  /**
   * Gets the internal http backend used
   * @returns {CheHttpBackend.httpBackend|*}
   */
  getHttpBackend() {
    return this.httpBackend;
  }

  /**
   * Setup Backend for teams
   */
  teamsBackendSetup() {
    let allTeams = [];

    let teamsKeys = this.teamsMap.keys();
    for (let key of teamsKeys) {
      let team = this.teamsMap.get(key);
      this.httpBackend.when('GET', '/api/organization/' + team.id).respond(team);
      this.httpBackend.when('DELETE', '/api/organization/' + team.id).respond(() => {
        return [200, {success: true, errors: []}];
      });
      allTeams.push(team);
    }

    this.httpBackend.when('GET', '/api/organization').respond(allTeams);
  }

  /**
   * Add the given team to teamsMap
   * @param team
   */
  addTeamById(team) {
    this.teamsMap.set(team.id, team);
  }

  /**
   * Setup Backend for organizations
   */
  organizationsBackendSetup(): void {
    const allOrganizations = [];

    const organizationKeys = this.organizationsMap.keys();
    for (let key of organizationKeys) {
      const organization = this.organizationsMap.get(key);
      this.httpBackend.when('GET', '/api/organization/' + organization.id).respond(organization);
      this.httpBackend.when('DELETE', '/api/organization/' + organization.id).respond(() => {
        return [200, {success: true, errors: []}];
      });
      allOrganizations.push(organization);
    }

    this.httpBackend.when('GET', '/api/organization').respond(allOrganizations);
  }

  /**
   * Add the given organization to organizationsMap
   *
   * @param {codenvy.IOrganization} organization the organization
   */
  addOrganizationById(organization: codenvy.IOrganization): void {
    this.organizationsMap.set(organization.id, organization);
  }

  /**
   * Setup Backend for permissions.
   */
  permissionsBackendSetup(): void {
    const keys = this.permissionsMap.keys();
    for (let domainInstanceKey of keys) {
      const permissionsList = this.permissionsMap.get(domainInstanceKey);
      const {domainId, instanceId} = permissionsList[0];

      this.httpBackend.when('GET', `/api/permissions/${domainId}/all?instance=${instanceId}`).respond(permissionsList);
    }
  }

  /**
   * Add permission to a permissions map
   *
   * @param {codenvy.IPermissions} permissions
   */
  addPermissions(permissions: codenvy.IPermissions): void {
    let domainInstanceKey = permissions.domainId + '|' + permissions.instanceId;

    if (this.permissionsMap.has(domainInstanceKey)) {
      this.permissionsMap.get(domainInstanceKey).push(permissions);
    } else {
      this.permissionsMap.set(domainInstanceKey, [permissions]);
    }
  }

  /**
   * Setup Backend for resources.
   */
  resourcesBackendSetup(): void {
    const keys = this.resourcesMap.keys();
    for (let organizationId of keys) {
      const organizationResourcesMap = this.resourcesMap.get(organizationId);

      // total
      if (organizationResourcesMap.has('total')) {
        const resources = organizationResourcesMap.get('total');
        this.httpBackend.when('GET', `/api/resource/${organizationId}`).respond(resources);
      }
    }
  }

  /**
   * Add resource to a resources map
   *
   * @param {string} organizationId organization ID
   * @param {string} scope total, used or available
   * @param {any} resource
   */
  addResource(organizationId: string, scope: string, resource: any): void {
    if (!this.resourcesMap.has(organizationId)) {
      this.resourcesMap.set(organizationId, new Map());
    }

    const organizationResourcesMap = this.resourcesMap.get(organizationId);
    if (organizationResourcesMap.has(scope)) {
      organizationResourcesMap.get(scope).push(resource);
    } else {
      organizationResourcesMap.set(scope, [resource]);
    }
  }

}


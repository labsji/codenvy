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
import {CodenvyOrganization} from '../../components/api/codenvy-organizations.factory';
import {CodenvyPermissions} from '../../components/api/codenvy-permissions.factory';
import {CodenvyResourcesDistribution} from '../../components/api/codenvy-resources-distribution.factory';

export class OrganizationsConfigService {
  /**
   * Log service.
   */
  private $log: ng.ILogService;
  /**
   * Promises service.
   */
  private $q: ng.IQService;
  /**
   * Route service.
   */
  private $route: ng.route.IRouteService;
  /**
   * Organization API interaction.
   */
  private codenvyOrganization: CodenvyOrganization;
  /**
   * Permissions API interaction.
   */
  private codenvyPermissions: CodenvyPermissions;
  /**
   * Organization resources API interaction.
   */
  private codenvyResourcesDistribution: CodenvyResourcesDistribution;
  /**
   * User profile API interaction.
   */
  private cheUser: any;

  /** Default constructor that is using resource injection
   * @ngInject for Dependency injection
   */
  constructor($log: ng.ILogService,
              $q: ng.IQService,
              $route: ng.route.IRouteService,
              codenvyOrganization: CodenvyOrganization,
              codenvyPermissions: CodenvyPermissions,
              codenvyResourcesDistribution: CodenvyResourcesDistribution,
              cheUser: any) {

    this.$log = $log;
    this.$q = $q;
    this.$route = $route;
    this.codenvyOrganization = codenvyOrganization;
    this.codenvyPermissions = codenvyPermissions;
    this.codenvyResourcesDistribution = codenvyResourcesDistribution;
    this.cheUser = cheUser;
  }

  /**
   * Prints error message.
   *
   * @param {any} error error object or string
   */
  logError(error: any): void {
    if (!error) {
      return;
    }
    const message = error.data && error.data.message ? error.data.message : error;
    this.$log.error(message);
  }

  waitAll(promises: Array<ng.IPromise<any>>): ng.IPromise<any> {
    return this.$q.all(promises).then((results: any) => {
      return results;
    }, (error: any) => {
      this.logError(error);
    });
  }

  /**
   * Fetches all organizations.
   *
   * @return {IPromise<any>}
   */
  fetchOrganizations(): ng.IPromise<any> {
    const defer = this.$q.defer();

    // we should resolve this promise in any case to show 'not found page' in case with error
    this.codenvyOrganization.fetchOrganizations().finally(() => {
      const organizations = this.codenvyOrganization.getOrganizations();
      defer.resolve(organizations);
    });

    return defer.promise;
  }

  /**
   * Fetches organization by its name.
   *
   * @param {string} name organization name
   * @return {IPromise<any>}
   */
  getOrFetchOrganizationByName(name: string): ng.IPromise<any> {
    const defer = this.$q.defer();

    const organization = this.codenvyOrganization.getOrganizationByName(name);
    if (organization) {
      defer.resolve(organization);
    } else {
      this.codenvyOrganization.fetchOrganizations().then(() => {
        const organization = this.codenvyOrganization.getOrganizationByName(name);
        if (!organization) {
          this.logError(`Organization "${name}" is not found.`);
        }
        defer.resolve(organization);
      }, (error: any) => {
        this.logError(error);
        defer.resolve(null);
      });
    }

    return defer.promise;
  }

  /**
   * Fetches permissions of organization.
   *
   * @param {string} id organization ID
   * @return {IPromise<any>}
   */
  getOrFetchOrganizationPermissions(id: string): ng.IPromise<any> {
    const defer = this.$q.defer();

    const permissions = this.codenvyPermissions.getOrganizationPermissions(id);
    if (permissions) {
      defer.resolve(permissions);
    } else {
      this.codenvyPermissions.fetchOrganizationPermissions(id).then(() => {
        const permissions = this.codenvyPermissions.getOrganizationPermissions(id);
        defer.resolve(permissions);
      }, (error: any) => {
        this.logError(error);
        defer.resolve(null);
      })
    }

    return defer.promise;
  }

  /**
   * Fetches resources of organization.
   *
   * @param {string} id organization ID
   * @return {IPromise<any>}
   */
  getOrFetchOrganizationResources(id: string): ng.IPromise<any> {
    const defer = this.$q.defer();

    const resources = this.codenvyResourcesDistribution.getOrganizationResources(id);
    if (resources) {
      defer.resolve(resources);
    } else {
      this.codenvyResourcesDistribution.fetchOrganizationResources(id).then(() => {
        const resources = this.codenvyResourcesDistribution.getOrganizationResources(id);
        defer.resolve(resources);
      }, (error: any) => {
        this.logError(error);
        defer.resolve(null);
      })
    }

    return defer.promise;
  }

  /**
   * Fetches resources of root organization.
   *
   * @param {string} id organization ID
   * @return {IPromise<any>}
   */
  getOrFetchTotalOrganizationResources(id: string): ng.IPromise<any> {
    const defer = this.$q.defer();

    const resources = this.codenvyResourcesDistribution.getTotalOrganizationResources(id);
    if (resources) {
      defer.resolve(resources);
    } else {
      this.codenvyResourcesDistribution.fetchTotalOrganizationResources(id).then(() => {
        const resources = this.codenvyResourcesDistribution.getTotalOrganizationResources(id);
        defer.resolve(resources);
      }, (error: any) => {
        this.logError(error);
        defer.resolve();
      })
    }

    return defer.promise;
  }

  /**
   * Fetches users of organization.
   * todo: get Profiles instead of Users
   *
   * @param {Array<codenvy.IPermission>} permissions
   * @return {IPromise<any>}
   */
  getOrFetchOrganizationUsers(permissions: Array<codenvy.IPermissions>): ng.IPromise<any> {
    const userPromises = [];

    if (permissions && permissions.length) {
      permissions.forEach((permission: any) => {
        const userId = permission.userId;
        const user = this.cheUser.getUserFromId(userId);

        if (user) {
          userPromises.push(this.$q.when(user));
        } else {
          const userPromise = this.cheUser.fetchUserId(userId).then(() => {
            return this.cheUser.getUserFromId(userId);
          });
          userPromises.push(userPromise);
        }
      });
    }

    return this.$q.all(userPromises);
  }

  /**
   * Returns promise to resolve route for organization details page.
   *
   * @returns {ng.IPromise<any>}
   */
  resolveOrganizationDetailsRoute(): ng.IPromise<any> {
    const name = this.$route.current.params.organizationName;

    const organizationPromise = this.getOrFetchOrganizationByName(name);

    // get current organization permissions
    const permissionsPromise = organizationPromise.then((organization: codenvy.IOrganization) => {
      if (organization && organization.id) {
        return this.getOrFetchOrganizationPermissions(organization.id);
      }
      return this.$q.when();
    });

    // get current organization resources
    const resourcesPromise = organizationPromise.then((organization: codenvy.IOrganization) => {
      if (!organization) {
        return this.$q.when();
      }
      if (organization.parent) {
        return this.getOrFetchOrganizationResources(organization.id);
      } else {
        return this.getOrFetchTotalOrganizationResources(organization.id);
      }
    });

    // fetch parent organization members
    const parentMembersDefer = this.$q.defer();
    const parentOrgPermissionsPromise = organizationPromise.then((organization: codenvy.IOrganization) => {
      if (organization && organization.parent) {
        return this.getOrFetchOrganizationPermissions(organization.parent);
      }
      return this.$q.reject();
    });
    parentOrgPermissionsPromise.then(
      /* fetch parent organization members */
      (permissions: Array<condenvy.IPermissions>) => {
        return this.getOrFetchOrganizationUsers(permissions);
      }, (error: any) => {
        this.logError(error);
        parentMembersDefer.resolve([]);
        return this.$q.reject();
      }
    ).then(
      /* resolve parent organization members */
      (userResults: any[]) => {
        parentMembersDefer.resolve(userResults);
      }, (error: any) => {
        this.logError(error);
        parentMembersDefer.resolve([]);
      }
    );

    return this.$q.all({
      organization: organizationPromise,
      organizationPermissions: permissionsPromise,
      organizationResources: resourcesPromise,
      parentOrganizationMembers: parentMembersDefer.promise,
    }).then((results: any) => {
      return results;
    });
  }

  /**
   * Returns promise to resolve route for create organization page.
   *
   * @returns {ng.IPromise<any>}
   */
  resolveCreateOrganizationRoute(): ng.IPromise<any> {
    const parentQualifiedNameDefer = this.$q.defer();
    const parentIdDefer = this.$q.defer();
    const parentMembersDefer = this.$q.defer();

    parentQualifiedNameDefer.resolve(this.$route.current.params.parentQualifiedName || '');
    parentQualifiedNameDefer.promise.then(
      (name: string) => {
        return this.getOrFetchOrganizationByName(name);
      }
    ).then(
      /* resolve parent organization ID */
      (organization: codenvy.IOrganization) => {
        const id = organization ? organization.id : '';
        parentIdDefer.resolve(id);

        if (!organization) {
          return this.$q.reject();
        } else {
          return this.getOrFetchOrganizationPermissions(id);
        }
      }, (error: any) => {
        this.logError(error);
        parentIdDefer.resolve('');
        parentMembersDefer.resolve([]);
        return this.$q.reject(error);
      }
    ).then(
      /* fetch parent organization members */
      (permissions: Array<condenvy.IPermissions>) => {
        return this.getOrFetchOrganizationUsers(permissions);
      }, (error: any) => {
        this.logError(error);
        parentMembersDefer.resolve([]);
        return this.$q.reject();
      }
    ).then(
      /* resolve parent organization members */
      (userResults: any[]) => {
        parentMembersDefer.resolve(userResults);
      }, (error: any) => {
        this.logError(error);
        parentMembersDefer.resolve([]);
      }
    );

    return this.$q.all({
      parentQualifiedName: parentQualifiedNameDefer.promise,
      parentOrganizationId: parentIdDefer.promise,
      parentOrganizationMembers: parentMembersDefer.promise
    }).then((results: any) => {
      return results;
    });
  }

}

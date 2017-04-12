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
import {CodenvyAPI} from '../../components/api/codenvy-api.factory';
import {CodenvyPermissions} from '../../components/api/codenvy-permissions.factory';

export class CodenvyNavBarController {
  menuItemUrl = {
    login: '/site/login',
    dashboard: '#/',
    workspaces: '#/workspaces',
    stacks: '#/stacks',
    factories: '#/factories',
    administration: '#/onprem/administration',
    organizations: '#/organizations',
    usermanagement: '#/admin/usermanagement'
  };
  // account dropdown items
  accountItems = [
    {
      name: 'Profile & Account',
      url: '#/account'
    },
    {
      name: 'Administration',
      url: '#/administration'
    }, {
      name: 'Logout',
      onclick: () => {
        this.logout();
      }
    }
  ];
  links = [{
    href: '#/create-workspace',
    name: 'New Workspace'
  }];
  displayLoginItem: boolean;
  isFactoryServiceAvailable: boolean;

  private $scope: ng.IScope;
  private $window: ng.IWindowService;
  private $location: ng.ILocationService;
  private $route: ng.route.IRouteService;
  private $rootScope: ng.IRootScopeService;
  private $cookies: ng.cookies.ICookiesService;
  private $resource: ng.resource.IResourceService;
  private $mdSidenav: ng.material.ISidenavService;
  private userServices: codenvy.IUserServices;
  private codenvyAPI: CodenvyAPI;
  private cheFactory: any;
  private codenvyPermissions: CodenvyPermissions;
  private cheAPI: any;
  private profile: che.IProfile;
  private logoutAPI: any;
  private hasPersonalAccount: boolean;
  private organizations: Array<codenvy.IOrganization>;

  /**
   * Default constructor
   * @ngInject for Dependency injection
   */
  constructor($mdSidenav: ng.material.ISidenavService, $scope: ng.IScope, $location: ng.ILocationService,
              $route: ng.route.IRouteService, userDashboardConfig: any, cheAPI: any, codenvyAPI: CodenvyAPI,
              $rootScope: ng.IRootScopeService, $http: ng.IHttpService, $window: ng.IWindowService,
              $cookies: ng.cookies.ICookiesService, $resource: ng.resource.IResourceService) {
    this.$mdSidenav = $mdSidenav;
    this.$scope = $scope;
    this.$location = $location;
    this.$route = $route;
    this.cheAPI = cheAPI;
    this.codenvyAPI = codenvyAPI;
    this.cheFactory = cheAPI.getFactory();
    this.codenvyPermissions = codenvyAPI.getPermissions();
    this.$rootScope = $rootScope;
    this.$window = $window;
    this.$resource = $resource;
    this.$cookies = $cookies;
    this.logoutAPI = this.$resource('/api/auth/logout', {});

    this.displayLoginItem = userDashboardConfig.developmentMode;
    let promiseService = this.cheAPI.getService().fetchServices();
    promiseService.then(() => {
      this.isFactoryServiceAvailable = cheAPI.getService().isServiceAvailable(this.cheFactory.getFactoryServicePath());
      let isBillingServiceAvailable = cheAPI.getService().isServiceAvailable(codenvyAPI.getPayment().getPaymentServicePath());
      if (isBillingServiceAvailable) {
        this.accountItems.splice(1, 0, {
          name: 'Billing',
          url: '#/billing'
        });
      }
    });

    this.profile = cheAPI.getProfile().getProfile();

    // highlight navbar menu item
    $scope.$on('$locationChangeStart', () => {
      let path = '#' + $location.path();
      $scope.$broadcast('navbar-selected:set', path);
    });

    // update branding
    let assetPrefix = 'assets/branding/';
    $http.get(assetPrefix + 'product.json').then((data: any) => {
      if (data.data.navbarButton) {
        (this.$rootScope as any).branding.navbarButton = {
          title: data.data.navbarButton.title,
          tooltip: data.data.navbarButton.tooltip,
          link: data.data.navbarButton.link
        };
        this.accountItems.splice(2, 0, {
          name: data.data.navbarButton.title,
          url: data.data.navbarButton.link
        });
      }
    });

    cheAPI.cheWorkspace.fetchWorkspaces();
    this.userServices = this.codenvyPermissions.getUserServices();
    this.organizations = this.codenvyAPI.getOrganization().getOrganizations();
    if (this.codenvyPermissions.getSystemPermissions()) {
      this.updateData();
    } else {
      this.codenvyPermissions.fetchSystemPermissions().finally(() => {
        this.updateData();
      });
    }
  }

  /**
   * Update data.
   */
  updateData(): void {
    this.codenvyAPI.getOrganization().fetchOrganizations().then(() => {
      if (this.organizations) {
        let user = this.cheAPI.getUser().getUser();
        let personalAccount = this.organizations.find((organization: any) => {
          return organization.qualifiedName === user.name;
        });
        this.hasPersonalAccount = !angular.isUndefined(personalAccount);
      }
    });

  }

  reload(): void {
    this.$route.reload();
  }

  /**
   * Returns number of workspaces.
   *
   * @return {number}
   */
  getWorkspacesNumber(): number {
    return this.cheAPI.cheWorkspace.getWorkspaces().length;
  }

  /**
   * Returns number of factories.
   *
   * @return {number}
   */
  getFactoriesNumber(): number {
    let pagesInfo = this.cheFactory.getPagesInfo();
    return pagesInfo && pagesInfo.count ? pagesInfo.count : this.cheFactory.factoriesById.size;
  }

  /**
   * Returns number of all organizations.
   *
   * @return {number}
   */
  getOrganizationsNumber(): number {
    if (!this.organizations) {
      return 0;
    }

    return this.organizations.length;
  }

  /**
   * Returns number of root organizations.
   *
   * @return {number}
   */
  getRootOrganizationsNumber(): number {
    if (!this.organizations) {
      return 0;
    }
    let rootOrganizations = this.organizations.filter((organization: any) => {
      return !organization.parent;
    });

    return rootOrganizations.length;
  }

  /**
   * Logout current user
   */
  logout(): void {
    let data = {token: this.$cookies['session-access-key']};
    let promise = this.logoutAPI.save(data).$promise;
    promise.then(() => {
      (this.$rootScope as any).showIDE = false;
      this.$window.location.href = this.menuItemUrl.login;
      this.$cookies.remove('LICENSE_EXPIRED');
    });
  }
}

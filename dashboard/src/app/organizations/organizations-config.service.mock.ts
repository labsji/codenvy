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
import {CodenvyAPIBuilder} from '../../components/api/builder/codenvy-api-builder.factory';
import {CodenvyHttpBackend} from '../../components/api/test/codenvy-http-backend';

type User = {
  id: string,
  email: string,
  firstName: string,
  lastName: string
};

/**
 * This class creates mock data and sets up backend.
 *
 * @author Oleksii Kurinnyi
 */
export class OrganizationsConfigServiceMock {
  private codenvyAPIBuilder;
  private codenvyHttpBackend;
  private cheAPIBuilder;
  private cheHttpBackend;

  private users: any[] = [];
  private orgs: any[] = [];
  private permissions: any[] = [];
  private usersByOrgs: Map<string, any[]> = new Map();

  /**
   * Default constructor
   * @ngInject for Dependency injection
   */
  constructor(codenvyAPIBuilder: CodenvyAPIBuilder, codenvyHttpBackend: CodenvyHttpBackend, cheAPIBuilder: any, cheHttpBackend: any) {
    this.codenvyAPIBuilder = codenvyAPIBuilder;
    this.codenvyHttpBackend = codenvyHttpBackend;
    this.cheAPIBuilder = cheAPIBuilder;
    this.cheHttpBackend = cheHttpBackend;
  }

  private buildUser(suffix: number|string): User {
    return {
      id: `testUser_${suffix}`,
      email: `testUser_${suffix}@email.org`,
      firstName: `FirstName_${suffix}`,
      lastName: `LastName_${suffix}`
    }
  }

  private addProfile(suffix: number|string, isDefault?: boolean): void {
    const user = this.buildUser(suffix);
    const profile = this.cheAPIBuilder.getProfileBuilder().withId(user.id).withEmail(user.email).withFirstName(user.firstName).withLastName(user.lastName).build();

    this.cheHttpBackend.addProfileId(profile);
  }

  private addAndGetUser(suffix: number|string, isDefault?: boolean): any {
    const user = this.buildUser(suffix);
    const testUser = this.cheAPIBuilder.getUserBuilder().withId(user.id).withEmail(user.email).build();

    if (isDefault) {
      this.cheHttpBackend.setDefaultUser(testUser);
    } else {
      this.cheHttpBackend.addUserById(testUser);
    }

    this.users.push(testUser);
    return testUser;
  }

  private addAndGetOrganization(suffix: number|string, parent?: any): any {
    const id = `testOrgId_${suffix}`;
    const name = `testOrgName_${suffix}`;
    const qualifiedName = (parent ? parent.qualifiedName + '/' : '') + name;

    const testOrganization = this.codenvyAPIBuilder.getOrganizationsBuilder().withId(id).withName(name).withQualifiedName(qualifiedName).build();

    this.codenvyHttpBackend.addOrganizationById(testOrganization);

    this.orgs.push(testOrganization);
    this.usersByOrgs.set(id, []);
    return testOrganization;
  }

  private addPermission(organization: any, user: any): void {
    const domainId = 'organization';

    const testPermission = this.codenvyAPIBuilder.getPermissionsBuilder().withDomainId(domainId).withInstanceId(organization.id).withUserId(user.id).build();

    this.codenvyHttpBackend.addPermissions(testPermission);

    this.usersByOrgs.get(organization.id).push(user);
    this.permissions.push(testPermission);
  }

  private addResource(organization: any, scope: string, resource: any): void {
    const testResource = this.codenvyAPIBuilder.getResourceBuilder().withAmount(resource.amount).withType(resource.type).withUnit(resource.unit).build();

    this.codenvyHttpBackend.addResource(organization.id, scope, resource);
  }

  mockData(): void {
    // add default user
    const user1 = this.addAndGetUser(1, true);
    // add users
    const user2 = this.addAndGetUser(2);
    const user3 = this.addAndGetUser(3);

    // add default profile
    this.addProfile(1, true);
    // add profiles
    this.addProfile(2);
    this.addProfile(3);

    // add root organization
    const org1 = this.addAndGetOrganization(1);
    // add children organizations
    const org2 = this.addAndGetOrganization(2, org1);
    const org3 = this.addAndGetOrganization(3, org1);

    // for root organization

    // add permissions
    [user1, user2, user3].forEach((user: any) => {
      this.addPermission(org1, user);
    });
    // add resources
    const totalResources = [
      {
        'type': 'workspace',
        'amount': 30,
        'unit': 'item'
      },
      {
        'type': 'runtime',
        'amount': 10,
        'unit': 'item'
      },
      {
        'type': 'timeout',
        'amount': 240,
        'unit': 'minute'
      },
      {
        'type': 'RAM',
        'amount': 102400,
        'unit': 'mb'
      }
    ];
    totalResources.forEach((resource: any) => {
      this.addResource(org1, 'total', resource);
    });

    // for sub-organization

    // add permissions
    [user1, user2].forEach((user: any) => {
      this.addPermission(org2, user);
    });
    // add resources
    totalResources.forEach((resource: any) => {
      this.addResource(org2, 'total', resource);
    });

    // build all backends at once
    this.cheHttpBackend.setup();
    this.cheHttpBackend.usersBackendSetup();
    this.codenvyHttpBackend.organizationsBackendSetup();
    this.codenvyHttpBackend.permissionsBackendSetup();
    this.codenvyHttpBackend.resourcesBackendSetup();
  }

  getUsers(): any[] {
    return this.users;
  }

  getUsersByOrganizationId(id: string): any[] {
    return this.usersByOrgs.get(id) || [];
  }

  getOrganizations(): any[] {
    return this.orgs;
  }
}

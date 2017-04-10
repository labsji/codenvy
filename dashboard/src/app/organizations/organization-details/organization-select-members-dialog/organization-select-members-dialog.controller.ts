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
import {ListOrganizationInviteMembersController} from '../organization-invite-members/list-organization-invite-members.controller';
import {CodenvyOrganizationRoles} from "../../../../components/api/codenvy-organization-roles";

interface IOrganizationMember extends codenvy.IMember {
  fullName?: string;
}

/**
 * This class is handling the controller for the add organization's members dialog.
 *
 * @author Oleksii Kurinnyi
 */
export class OrganizationSelectMembersDialogController {
  /**
   * User profile API interaction.
   */
  private cheProfile: any;
  /**
   * User API interaction.
   */
  private cheUser: any;
  /**
   * Service for displaying dialogs.
   */
  private $mdDialog: angular.material.IDialogService;
  /**
   * Promises service.
   */
  private $q: ng.IQService;
  /**
   * Lodash library.
   */
  private lodash: any;
  /**
   * Callback handler (is set from outside).
   */
  private callbackController: ListOrganizationInviteMembersController;
  /**
   * The list of users, that already are members of this organization (is set from outside).
   */
  private members: Array<codenvy.IMember>;
  /**
   * The list of users, that are members of parent organization (is set from outside).
   */
  private parentOrganizationMembers: Array<che.IUser>;
  /**
   * The list of users, that are available to be added
   */
  private availableUsers: Array<IOrganizationMember>;
  /**
   * The list of users, that are going to be added
   */
  private usersToAdd: Array<IOrganizationMember>;
  /**
   * Current user.
   */
  private user: che.IUser;
  /**
   * Selected status of members in list.
   */
  private userSelectedStatus: any;
  /**
   * Bulk operation state.
   */
  private isBulkChecked: boolean;
  /**
   * No selected members state.
   */
  private isNoSelected: boolean;
  /**
   * All selected members state.
   */
  private isAllSelected: boolean;
  /**
   * True when loading resources.
   */
  private isLoading: boolean;

  /**
   * Default constructor.
   * @ngInject for Dependency injection
   */
  constructor($q: ng.IQService, $mdDialog: angular.material.IDialogService, lodash: any, cheProfile: any, cheUser: any) {
    this.$q = $q;
    this.$mdDialog = $mdDialog;
    this.lodash = lodash;
    this.cheProfile = cheProfile;
    this.cheUser = cheUser;

    this.isLoading = false;

    this.userSelectedStatus = {};
    this.isBulkChecked = false;
    this.isNoSelected = true;
    this.isAllSelected = false;

    this.user = this.cheUser.getUser();

    this.formUsersAvailableList();
  }

  /**
   * Builds list of users that are available to be added.
   */
  formUsersAvailableList(): void {
    const existingMembers = this.members.reduce((map: {[id: string]: codenvy.IMember}, member: codenvy.IMember) => {
      map[member.id] = member;
      return map;
    }, {});
    this.availableUsers = this.parentOrganizationMembers.filter((parentOrganizationMember: che.IUser) => {
      return !existingMembers[parentOrganizationMember.id] && parentOrganizationMember.id !== this.user.id;
    });

    if (!this.availableUsers.length) {
      return ;
    }

    const userProfilesPromises = [];

    this.isLoading = true;

    this.availableUsers.forEach((user: IOrganizationMember) => {
      const profile = this.cheProfile.getProfileFromId(user.id);
      if (profile) {
        user.fullName = this.cheProfile.getFullName(profile.attributes);
      } else {
        const promise = this.cheProfile.fetchProfileId(user.id).then(() => {
          const profile = this.cheProfile.getProfileFromId(user.id);
          user.fullName = this.cheProfile.getFullName(profile.attributes);
        });
        userProfilesPromises.push(promise);
      }
    });

    this.$q.all(userProfilesPromises).finally(() => {
      this.isLoading = false;
    });
  }

  /**
   * Callback of the cancel button of the dialog.
   */
  hide() {
    this.$mdDialog.hide();
  }

  /**
   * Callback of the "Add" button of the dialog.
   */
  addMembers() {
    const checkedUsers = this.availableUsers.reduce((usersToAdd: Array<codenvy.IMember>, member: IOrganizationMember) => {
      if (this.userSelectedStatus[member.id]) {
        usersToAdd.push(member);
      }
      return usersToAdd;
    }, []);

    this.callbackController.addMembers(checkedUsers, CodenvyOrganizationRoles.MEMBER.name);
    this.$mdDialog.hide();
  }

  /**
   * Return <code>true</code> if all users in list are checked.
   * @returns {boolean}
   */
  isAllUsersSelected(): boolean {
    return this.isAllSelected;
  }

  /**
   * Returns <code>true</code> if all users in list are not checked.
   * @returns {boolean}
   */
  isNoUsersSelected(): boolean {
    return this.isNoSelected;
  }

  /**
   * Make all users in list selected.
   */
  selectAllUsers(): void {
    this.availableUsers.forEach((user: IOrganizationMember) => {
      this.userSelectedStatus[user.id] = true;
    });
  }

  /**
   * Make all users in list deselected.
   */
  deselectAllUsers(): void {
    this.availableUsers.forEach((user: IOrganizationMember) => {
      this.userSelectedStatus[user.id] = false;
    });
  }

  /**
   * Change bulk selection value.
   */
  changeBulkSelection(): void {
    if (this.isBulkChecked) {
      this.deselectAllUsers();
      this.isBulkChecked = false;
    } else {
      this.selectAllUsers();
      this.isBulkChecked = true;
    }
    this.updateSelectedStatus();
  }

  /**
   * Set selected status for user.
   *
   * @param {string} id user ID
   * @param {boolean} isSelected
   */
  setSelectedStatus(id: string, isSelected: boolean) {
    this.userSelectedStatus[id] = isSelected;
    this.updateSelectedStatus();
  }

  /**
   * Update members selected status.
   */
  updateSelectedStatus(): void {
    this.isNoSelected = true;
    this.isAllSelected = true;

    Object.keys(this.userSelectedStatus).forEach((key: string) => {
      if (this.userSelectedStatus[key]) {
        this.isNoSelected = false;
      } else {
        this.isAllSelected = false;
      }
    });

    if (this.isNoSelected) {
      this.isBulkChecked = false;
      return;
    }

    this.isBulkChecked = (this.isAllSelected && Object.keys(this.userSelectedStatus).length === this.availableUsers.length);
  }
}


/**
 * Copyright (C) 2012 BonitaSoft S.A.
 * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2.0 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.bonitasoft.console.server.api.organization;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpSession;

import org.bonitasoft.console.server.AbstractConsoleTest;
import org.bonitasoft.engine.api.IdentityAPI;
import org.bonitasoft.engine.api.TenantAPIAccessor;
import org.bonitasoft.engine.identity.User;
import org.bonitasoft.engine.identity.UserCreator;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.test.toolkit.organization.TestUser;
import org.bonitasoft.test.toolkit.organization.TestUserFactory;
import org.bonitasoft.test.toolkit.server.MockHttpServletRequest;
import org.bonitasoft.test.toolkit.server.MockHttpServletResponse;
import org.bonitasoft.web.rest.api.model.identity.UserItem;
import org.bonitasoft.web.toolkit.client.common.exception.api.APIItemNotFoundException;
import org.bonitasoft.web.toolkit.client.data.APIID;
import org.bonitasoft.web.toolkit.client.data.item.template.ItemHasCreator;
import org.bonitasoft.web.toolkit.client.data.item.template.ItemHasLastUpdateDate;
import org.bonitasoft.web.toolkit.server.APIServletCall;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Vincent Elcrin
 * 
 * 
 *         FIXME to be refactored using test-toolkit
 */
public class APIUserAnotherIntegrationTest extends AbstractConsoleTest {

    private final static String PATH_INFO = "API/identity/user";

    /**
     * the request param for the username
     */
    public static final String USERNAME_SESSION_PARAM = "username";

    public static final String API_SESSION_PARAM_KEY = "apiSession";

    /**
     * Tested API
     */
    private APIUser apiUser;

    // magic number. Look at testSearchUser for details.
    private static final int NUM_EXPECTED_USERS = 12;

    private static final int STRESS_ADD_DELETE_MAX = 100;

    private HashMap<Integer, UserItem> expectedUsers;

    private static final List<String> userAttributesList = Arrays.asList(
            UserItem.ATTRIBUTE_FIRSTNAME,
            UserItem.ATTRIBUTE_LASTNAME,
            UserItem.ATTRIBUTE_PASSWORD,
            UserItem.ATTRIBUTE_USERNAME,
            UserItem.ATTRIBUTE_TITLE,
            UserItem.ATTRIBUTE_JOB_TITLE
            );

    private static final List<String> searchableAttributesList = Arrays.asList(
            UserItem.ATTRIBUTE_FIRSTNAME,
            UserItem.ATTRIBUTE_LASTNAME,
            UserItem.ATTRIBUTE_USERNAME,
            UserItem.ATTRIBUTE_JOB_TITLE
            );

    @Override
    public void consoleTestSetUp() throws Exception {
        this.apiUser = createAPIUser(getInitiator().getSession());
        createUsersViaEngineAPI(getInitiator().getSession(), NUM_EXPECTED_USERS);
    }

    @Override
    protected TestUser getInitiator() {
        return TestUserFactory.getJohnCarpenter();
    }

    @After
    public void deleteUsers() throws Exception {
        final IdentityAPI identityAPI = TenantAPIAccessor.getIdentityAPI(getInitiator().getSession());
        final Iterator<UserItem> it = this.expectedUsers.values().iterator();
        while (it.hasNext()) {
            identityAPI.deleteUser(it.next().getId().toLong());
        }
    }

    /**
     * Create api user.
     * Used to migrate other test to the new user API.
     * 
     * @param apiSession
     * @return
     */
    public static APIUser createAPIUser(final APISession apiSession) {

        // Get the httpSession and set attributes
        final MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        mockHttpServletRequest.setPathInfo(PATH_INFO);
        final MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();
        final HttpSession httpSession = mockHttpServletRequest.getSession();
        httpSession.setAttribute(USERNAME_SESSION_PARAM, "admin");
        httpSession.setAttribute(API_SESSION_PARAM_KEY, apiSession);

        // Initialize APIUser for HTTP requests of the API
        final APIUser apiUser = new APIUser();
        final APIServletCall caller = new APIServletCall(mockHttpServletRequest,
                mockHttpServletResponse);
        apiUser.setCaller(caller);
        return apiUser;
    }

    // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // CONVENIENCE METHODS
    // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Create user containing all attributes listed in userAttributeList with attributeKey_userNumber for value
     * 
     * @param userNumber
     * @return
     */
    private UserItem createCompleteUser(final List<String> userAttributesList, final int userNumber) {
        final HashMap<String, String> attributes = new HashMap<String, String>();
        for (final String attribute : userAttributesList) {
            if (UserItem.ATTRIBUTE_LAST_CONNECTION_DATE.equals(attribute)
                    || ItemHasCreator.ATTRIBUTE_CREATION_DATE.equals(attribute)
                    || ItemHasLastUpdateDate.ATTRIBUTE_LAST_UPDATE_DATE.equals(attribute)) {
                final Date date = new Date();
                attributes.put(attribute, date.toString());
            } else {
                attributes.put(attribute, attribute + "_" + userNumber);
            }
        }
        final UserItem user = new UserItem();
        user.setAttributes(attributes);
        return user;
    }

    /**
     * Create numberExpectedUser of users via the engine api
     * 
     * @param numberExpectedUser
     * @throws Exception
     */
    private void createUsersViaEngineAPI(final APISession apiSession, final int numberExpectedUser) throws Exception {
        final IdentityAPI identityAPI = TenantAPIAccessor.getIdentityAPI(apiSession);
        this.expectedUsers = new HashMap<Integer, UserItem>();
        for (int i = 0; i < numberExpectedUser; i++) {
            final UserItem newUser = createCompleteUser(userAttributesList, i);
            final APIID newUserId = APIID.makeAPIID(identityAPI.createUser(buildEngineUser(newUser))
                    .getId());
            newUser.setId(newUserId);
            this.expectedUsers.put(i, newUser);
        }
    }

    /**
     * Assert that listed attributes of expected user are equals to the actual ones.
     * 
     * @param userAttributesList
     *            : list of attribute to test
     * @param expected
     *            : Expected user
     * @param actual
     *            : User tested against
     */
    private void assertUserEquals(final List<String> userAttributesList, final UserItem expected, final UserItem actual) {
        final ArrayList<String> localAttributeList = new ArrayList<String>();
        localAttributeList.addAll(userAttributesList);
        // password do not come back from the api.
        localAttributeList.remove(UserItem.ATTRIBUTE_PASSWORD);
        // test
        for (final String attribute : localAttributeList) {
            Assert.assertEquals(attribute + " isnt equals to the attribute setted previously", expected.getAttributeValue(attribute),
                    actual.getAttributeValue(attribute));
        }
    }

    /**
     * Copied from UserDatastore
     * 
     * @param user
     * @return
     */
    protected UserItem convertEngineToConsoleItem(final User user) {
        if (user == null) {
            return null;
        }

        final UserItem result = new UserItem();
        result.setId(APIID.makeAPIID(user.getId()));
        result.setFirstName(user.getFirstName());
        result.setLastName(user.getLastName());
        result.setPassword(null);
        result.setUserName(user.getUserName());
        result.setManagerId(user.getManagerUserId());
        result.setIcon(user.getIconPath());
        result.setCreationDate(user.getCreationDate());
        result.setCreatedByUserId(user.getCreatedBy());
        result.setLastUpdateDate(user.getLastUpdate());
        result.setLastConnectionDate(user.getLastConnection());
        result.setTitle(user.getTitle());
        result.setJobTitle(user.getJobTitle());

        return result;
    }

    @Test
    public void testGetUser() {
        final Iterator<UserItem> it = this.expectedUsers.values().iterator();
        while (it.hasNext()) {
            final UserItem expectedUser = it.next();
            final UserItem anUser = this.apiUser.runGet(expectedUser.getId(), new ArrayList<String>(), new ArrayList<String>());

            // tests
            Assert.assertNotNull(anUser);
            assertUserEquals(userAttributesList, expectedUser, anUser);
        }
    }

    @Test
    public void testSearchUser() {
        final Iterator<UserItem> it = this.expectedUsers.values().iterator();
        while (it.hasNext()) {
            final UserItem expectedUser = it.next();
            for (final String attribute : searchableAttributesList) {
                final List<UserItem> results = this.apiUser.runSearch(0, 10, expectedUser.getAttributeValue(attribute), null, new HashMap<String, String>(),
                        new ArrayList<String>(), new ArrayList<String>()).getResults();

                // tests
                Assert.assertNotNull(results);
                if (expectedUser.getAttributeValue(attribute).endsWith("_1")) {

                    // we created 12 users so the search of attribute_1 should return _1, _10 and _11
                    Assert.assertEquals("Not the right number of result for <" + attribute + "> - value: " + expectedUser.getAttributeValue(attribute), 3,
                            results.size());
                } else {
                    Assert.assertEquals("Not the right number of result for <" + attribute + "> - value: " + expectedUser.getAttributeValue(attribute), 1,
                            results.size());
                    assertUserEquals(userAttributesList, expectedUser, results.get(0));
                }
            }
        }
    }

    @Test
    public void testAddUser() throws Exception {

        // add user
        final UserItem expectedUser = createCompleteUser(userAttributesList, 100);
        final UserItem addedUser = this.apiUser.add(expectedUser);
        Assert.assertNotNull(addedUser);
        assertUserEquals(userAttributesList, expectedUser, addedUser);

        // retrieve user added and check if equal
        final IdentityAPI identityAPI = TenantAPIAccessor.getIdentityAPI(getInitiator().getSession());
        final User retrievedUser = identityAPI.getUser(addedUser.getId().toLong());
        assertUserEquals(userAttributesList, expectedUser, convertEngineToConsoleItem(retrievedUser));

        // then clean
        identityAPI.deleteUser(addedUser.getId().toLong());
    }

    @Test
    public void testStressAddDeleteUser() throws Exception {
        for (int i = 0; i < STRESS_ADD_DELETE_MAX; i++) {

            // dont create user on top of the ones created during the setup
            final UserItem user = createCompleteUser(userAttributesList, 100);
            final UserItem addedUser = this.apiUser.add(user);
            assertUserEquals(userAttributesList, user, addedUser);

            // retrieve user
            assertUserEquals(userAttributesList, user, this.apiUser.runGet(addedUser.getId(), new ArrayList<String>(), new ArrayList<String>()));

            // delete user
            this.apiUser.delete(Arrays.asList(addedUser.getId()));
            try {
                Assert.assertNull("User still exist", this.apiUser.runGet(addedUser.getId(), new ArrayList<String>(), new ArrayList<String>()));
            } catch (final APIItemNotFoundException e) {
            }
        }
    }

    @Test
    public void testUpdateUser() throws Exception {
        final UserItem expectedUser = this.expectedUsers.get(0);
        for (final String attribute : userAttributesList) {

            // update an attribute
            final HashMap<String, String> changes = new HashMap<String, String>();
            changes.put(attribute, expectedUser.getAttributeValue(attribute) + "_");
            expectedUser.setAttribute(attribute, changes.get(attribute));
            final UserItem updatedUser = this.apiUser.update(expectedUser.getId(), changes);
            assertUserEquals(userAttributesList, expectedUser, updatedUser);

            // retrieve user and check it again
            final IdentityAPI identityAPI = TenantAPIAccessor.getIdentityAPI(getInitiator().getSession());
            final User retrievedUser = identityAPI.getUser(updatedUser.getId().toLong());
            assertUserEquals(userAttributesList, expectedUser, convertEngineToConsoleItem(retrievedUser));
        }
    }

    public UserCreator buildEngineUser(final UserItem user) throws NumberFormatException {
        if (user == null) {
            throw new IllegalArgumentException("The user must be not null!");
        }

        final UserCreator userCreator = new UserCreator(user.getAttributeValue(UserItem.ATTRIBUTE_USERNAME), user.getAttributeValue(UserItem.ATTRIBUTE_PASSWORD))
                .setFirstName(user.getAttributeValue(UserItem.ATTRIBUTE_FIRSTNAME))
                .setLastName(user.getAttributeValue(UserItem.ATTRIBUTE_LASTNAME))
                .setTitle(user.getAttributeValue(UserItem.ATTRIBUTE_TITLE))
                .setIconPath(user.getAttributeValue(UserItem.ATTRIBUTE_ICON))
                .setJobTitle(user.getAttributeValue(UserItem.ATTRIBUTE_JOB_TITLE));
        // .setPersonalData(personalInfo.done())
        // .setProfessionalData(professionalInfo.done());

        final String managerId = user.getAttributeValue(UserItem.ATTRIBUTE_MANAGER_ID);
        if (managerId != null && !managerId.isEmpty()) {
            userCreator.setManagerUserId(Long.valueOf(managerId));
        }
        return userCreator;
    }
}

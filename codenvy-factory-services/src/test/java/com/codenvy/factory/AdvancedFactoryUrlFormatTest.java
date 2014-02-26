/*
 * CODENVY CONFIDENTIAL
 * __________________
 * 
 *  [2012] - [2013] Codenvy, S.A. 
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
package com.codenvy.factory;

import com.codenvy.api.factory.FactoryUrlException;
import com.codenvy.api.factory.dto.AdvancedFactoryUrl;
import com.codenvy.dto.server.DtoFactory;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.net.URL;

import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

@Listeners(value = {MockitoTestNGListener.class})
public class AdvancedFactoryUrlFormatTest {
    private static String VALID_REPOSITORY_URL = "http://github.com/codenvy/cloudide";

    @Mock
    private FactoryClient factoryClient;

    @InjectMocks
    private AdvancedFactoryUrlFormat factoryUrlFormat;

    @Test
    public void shouldBeAbleToParseValidUrl() throws Exception {
        //given
        AdvancedFactoryUrl expectedFactoryUrl =
                DtoFactory.getInstance().createDto(AdvancedFactoryUrl.class);
        expectedFactoryUrl.setV("1.1");
        expectedFactoryUrl.setVcs("git");
        expectedFactoryUrl.setVcsurl(VALID_REPOSITORY_URL);
        expectedFactoryUrl.setCommitid("commit123456789");
        expectedFactoryUrl.setVcsinfo(false);
        expectedFactoryUrl.setVcsbranch("newBranch");
        expectedFactoryUrl.setId("123456789");

        URL factoryUrl = new URL("http://codenvy.com/factory?id=123456789");
        when(factoryClient.getFactory("123456789")).thenReturn(expectedFactoryUrl);

        //when
        AdvancedFactoryUrl actualFactoryUrl = factoryUrlFormat.parse(factoryUrl);

        //then
        assertEquals(actualFactoryUrl, expectedFactoryUrl);
    }

    @Test(expectedExceptions = FactoryUrlException.class)
    public void shouldNotValidateUrlIllegalFormatExceptionIfIdIsMissing() throws Exception {
        factoryUrlFormat.parse(new URL("http://codenvy.com/factory"));
    }

    @Test(expectedExceptions = FactoryUrlException.class)
    public void shouldNotValidateUrlExistenceExceptionIfFactoryWithSuchIdIsNotFound()
            throws Exception {
        //given
        URL factoryUrl = new URL("http://codenvy.com/factory?id=123456789");
        when(factoryClient.getFactory("123456789")).thenReturn(null);
        //when
        factoryUrlFormat.parse(factoryUrl);
    }
}

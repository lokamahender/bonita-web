package org.bonitasoft.console.common.server.page;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

import groovy.lang.GroovyClassLoader;
import org.apache.commons.io.IOUtils;
import org.bonitasoft.console.common.server.preferences.properties.CompoundPermissionsMapping;
import org.bonitasoft.console.common.server.preferences.properties.ResourcesPermissionsMapping;
import org.bonitasoft.engine.api.PageAPI;
import org.bonitasoft.engine.page.Page;
import org.bonitasoft.engine.session.APISession;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CustomPageServiceTest {

    public static final String PAGE_NO_API_EXTENSION_PROPERTIES = "pageNoApiExtension.properties";
    public static final String PAGE_PROPERTIES = "page.properties";

    @Spy
    private final CustomPageService customPageService = spy(new CustomPageService());

    @Mock
    CompoundPermissionsMapping compoundPermissionsMapping;

    @Mock
    ResourcesPermissionsMapping resourcesPermissionsMapping;

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Mock
    APISession apiSession;

    @Mock
    PageResourceProvider pageResourceProvider;

    @Mock
    PageAPI pageAPI;

    @Mock
    private HttpServletRequest request;

    @Mock
    private PageContext pageContext;

    @Mock
    private Page mockedPage;

    @Before
    public void before() {
        when(apiSession.getTenantId()).thenReturn(1L);
    }

    @Test
    public void should_load_page_return_page_impl() throws Exception {
        // Given
        final File pageFile = new File(getClass().getResource("/Index.groovy").toURI());
        final File pageDir = pageFile.getParentFile();
        assertThat(pageFile).as("no file " + pageFile.getAbsolutePath()).exists().canRead();
        when(pageResourceProvider.getPageDirectory()).thenReturn(pageDir);
        doReturn(pageFile).when(customPageService).getGroovyPageFile(any(File.class));
        final File pageLibDir = new File(pageFile.getParentFile(), File.separator + "lib");
        doReturn(pageLibDir).when(customPageService).getCustomPageLibDirectory(any(File.class));
        doReturn(Thread.currentThread().getContextClassLoader()).when(customPageService).getParentClassloader(anyString(), any(File.class), anyString());

        when(mockedPage.getLastModificationDate()).thenReturn(new Date(0L));
        doReturn(pageAPI).when(customPageService).getPageAPI(apiSession);
        when(pageAPI.getPageByName("")).thenReturn(mockedPage);

        // When
        final GroovyClassLoader classloader = customPageService.getPageClassloader(apiSession, pageResourceProvider);
        final Class<PageController> pageClass = customPageService.registerPage(classloader, pageResourceProvider);
        final PageController pageController = customPageService.loadPage(pageClass);

        // Then
        assertNotNull(pageController);
    }


    @Test
    public void should_load_rest_api_page_return_api_impl() throws Exception {
        // Given
        when(apiSession.getTenantId()).thenReturn(0L);
        final File pageFile = new File(getClass().getResource("/IndexRestApi.groovy").toURI());
        final File pageDir = pageFile.getParentFile();
        assertThat(pageFile).as("no file " + pageFile.getAbsolutePath()).exists().canRead();
        when(pageResourceProvider.getPageDirectory()).thenReturn(pageDir);
        doReturn(pageFile).when(customPageService).getPageFile(any(File.class), anyString());
        final File pageLibDir = new File(pageFile.getParentFile(), File.separator + "lib");
        doReturn(pageLibDir).when(customPageService).getCustomPageLibDirectory(any(File.class));
        doReturn(Thread.currentThread().getContextClassLoader()).when(customPageService).getParentClassloader(anyString(), any(File.class), anyString());
        final Page mockedPage = mock(Page.class);
        when(mockedPage.getLastModificationDate()).thenReturn(new Date(0L));
        doReturn(pageAPI).when(customPageService).getPageAPI(apiSession);
        when(pageAPI.getPageByName("")).thenReturn(mockedPage);

        // When
        final GroovyClassLoader classloader = customPageService.getPageClassloader(apiSession, pageResourceProvider);
        final Class<RestApiController> restApiControllerClass = customPageService.registerRestApiPage(classloader, pageResourceProvider, CustomPageService.PAGE_CONTROLLER_FILENAME);
        final RestApiController restApiController = customPageService.loadRestApiPage(restApiControllerClass);

        // Then
        assertNotNull(restApiController);
    }

    @Test
    public void should_retrievePageZipContent_save_it_in_bonita_home() throws Exception {
        // Given
        final Page mockedPage = mock(Page.class);
        when(mockedPage.getId()).thenReturn(1l);
        when(mockedPage.getName()).thenReturn("page1");
        when(apiSession.getTenantId()).thenReturn(0L);
        doReturn(pageAPI).when(customPageService).getPageAPI(apiSession);
        final byte[] zipFile = IOUtils.toByteArray(getClass().getResourceAsStream("page.zip"));
        when(pageAPI.getPageContent(1l)).thenReturn(zipFile);
        when(pageResourceProvider.getPage(pageAPI)).thenReturn(mockedPage);
        when(pageResourceProvider.getTempPageFile()).thenReturn(new File("target/bonita/home/client/tenant/1/temp"));
        when(pageResourceProvider.getPageDirectory()).thenReturn(new File("target/bonita/home/client/tenants/1/pages/page1"));

        // When
        customPageService.retrievePageZipContent(apiSession, pageResourceProvider);

        // Validate
        assertNotNull(pageResourceProvider.getPageDirectory().listFiles());
    }

    @Test
    public void should_get_Custom_Page_permissions_from_PageProperties() throws Exception {
        // Given
        final String fileContent = "name=customPage1\n" +
                "resources=[GET|identity/user, PUT|identity/user]";

        final File pagePropertiesFile = File.createTempFile(PAGE_PROPERTIES, ".tmp");
        IOUtils.write(fileContent.getBytes(), new FileOutputStream(pagePropertiesFile));
        doReturn(new HashSet<>(Arrays.asList("Organization Visualization"))).when(resourcesPermissionsMapping).getPropertyAsSet("GET|identity/user");
        doReturn(new HashSet<>(Arrays.asList("Organization Visualization", "Organization Managment")))
                .when(resourcesPermissionsMapping).getPropertyAsSet("PUT|identity/user");

        // When
        final Set<String> customPagePermissions = customPageService.getCustomPagePermissions(pagePropertiesFile, resourcesPermissionsMapping, true);

        // Then
        assertThat(customPagePermissions).contains("Organization Visualization", "Organization Managment");
    }

    @Test
    public void should_not_return_it_when_unkown_resource_is_declared_in_PageProperties_and_flag_false() throws Exception {
        // Given
        final String fileContent = "name=customPage1\n" +
                "resources=[GET|unkown/resource, PUT|identity/user]";

        final File pagePropertiesFile = File.createTempFile(PAGE_PROPERTIES, ".tmp");
        IOUtils.write(fileContent.getBytes(), new FileOutputStream(pagePropertiesFile));
        doReturn(Collections.emptySet()).when(resourcesPermissionsMapping).getPropertyAsSet("GET|unkown/resource");
        doReturn(new HashSet<>(Arrays.asList("Organization Visualization", "Organization Managment")))
                .when(resourcesPermissionsMapping).getPropertyAsSet("PUT|identity/user");

        // When
        final Set<String> customPagePermissions = customPageService.getCustomPagePermissions(pagePropertiesFile, resourcesPermissionsMapping, false);

        // Then
        assertThat(customPagePermissions).containsOnly("Organization Visualization", "Organization Managment");
    }

    @Test
    public void should_return_it_when_unkown_resource_is_declared_in_PageProperties_and_flag_true() throws Exception {
        // Given
        final String fileContent = "name=customPage1\n" +
                "resources=[GET|unkown/resource, GET|identity/user, PUT|identity/user]";

        final File pagePropertiesFile = File.createTempFile(PAGE_PROPERTIES, ".tmp");
        IOUtils.write(fileContent.getBytes(), new FileOutputStream(pagePropertiesFile));
        doReturn(new HashSet<>(Arrays.asList("Organization Visualization"))).when(resourcesPermissionsMapping).getPropertyAsSet("GET|identity/user");
        doReturn(new HashSet<>(Arrays.asList("Organization Visualization", "Organization Managment")))
                .when(resourcesPermissionsMapping).getPropertyAsSet("PUT|identity/user");

        // When
        final Set<String> customPagePermissions = customPageService.getCustomPagePermissions(pagePropertiesFile, resourcesPermissionsMapping, true);

        // Then
        assertThat(customPagePermissions).contains("<GET|unkown/resource>", "Organization Visualization", "Organization Managment");
    }

    @Test
    public void should_get_Custom_Page_permissions_to_CompoundPermissions_with_empty_list() throws Exception {
        // Given
        final String fileContent = "name=customPage1\n" +
                "resources=[]";

        final File pagePropertiesFile = File.createTempFile(PAGE_PROPERTIES, ".tmp");
        IOUtils.write(fileContent.getBytes(), new FileOutputStream(pagePropertiesFile));
        doReturn(Collections.emptySet()).when(resourcesPermissionsMapping).getPropertyAsSet("GET|unkown/resource");

        // When
        final Set<String> customPagePermissions = customPageService.getCustomPagePermissions(pagePropertiesFile, resourcesPermissionsMapping, false);

        // Then
        assertTrue(customPagePermissions.equals(new HashSet<String>()));
    }

    @Test
    public void should_add_Custom_Page_permissions_to_CompoundPermissions() throws Exception {
        // Given
        final HashSet<String> customPagePermissions = new HashSet<>(Arrays.asList("Organization Visualization", "Organization Managment"));

        // When
        customPageService.addPermissionsToCompoundPermissions("customPage1", customPagePermissions, compoundPermissionsMapping, resourcesPermissionsMapping);

        // Then
        verify(compoundPermissionsMapping).setPropertyAsSet("customPage1", customPagePermissions);
    }

    @Test
    public void should_GetPageProperties_does_not_throws_already_exist_exception_if_checkIfItAlreadyExists_is_false() throws Exception {
        //given
        final boolean checkIfItAlreadyExists = false;
        doReturn(pageAPI).when(customPageService).getPageAPI(apiSession);
        doReturn(new Properties()).when(pageAPI).getPageProperties(any(byte[].class), eq(checkIfItAlreadyExists));

        //when
        final byte[] zipContent = new byte[0];
        customPageService.getPageProperties(apiSession, zipContent, checkIfItAlreadyExists, 123123L);

        //then
        verify(pageAPI, times(0)).getPageByNameAndProcessDefinitionId(anyString(), anyLong());
    }

    @Test
    public void should_retrieve_class_file() throws Exception {
        // Given
        final Page mockedPage = mock(Page.class);
        when(mockedPage.getId()).thenReturn(1l);
        when(mockedPage.getName()).thenReturn("page1");
        when(apiSession.getTenantId()).thenReturn(0L);
        doReturn(pageAPI).when(customPageService).getPageAPI(apiSession);
        final byte[] zipFile = IOUtils.toByteArray(getClass().getResourceAsStream("page.zip"));
        when(pageAPI.getPageContent(1l)).thenReturn(zipFile);
        when(pageResourceProvider.getPage(pageAPI)).thenReturn(mockedPage);
        when(pageResourceProvider.getTempPageFile()).thenReturn(new File("target/bonita/home/client/tenant/1/temp"));
        when(pageResourceProvider.getPageDirectory()).thenReturn(new File("target/bonita/home/client/tenants/1/pages/page1"));

        // When
        customPageService.retrievePageZipContent(apiSession, pageResourceProvider);
        final File file = customPageService.getPageFile(pageResourceProvider.getPageDirectory(), "readme.txt");

        // Validate
        assertThat(file).as("should return file").exists();
    }

    @Test
    public void should_register_restApiPage() throws Exception {
        // Given
        final Page mockedPage = mock(Page.class);
        when(mockedPage.getId()).thenReturn(1l);
        when(mockedPage.getName()).thenReturn("page2");
        when(apiSession.getTenantId()).thenReturn(0L);
        doReturn(pageAPI).when(customPageService).getPageAPI(apiSession);
        final byte[] zipFile = IOUtils.toByteArray(getClass().getResourceAsStream("pageApiExtension.zip"));
        when(pageAPI.getPageContent(1l)).thenReturn(zipFile);
        when(pageResourceProvider.getPage(pageAPI)).thenReturn(mockedPage);
        when(pageResourceProvider.getTempPageFile()).thenReturn(new File("target/bonita/home/client/tenant/1/temp"));
        when(pageResourceProvider.getPageDirectory()).thenReturn(new File("target/bonita/home/client/tenants/1/pages/page2"));

        final GroovyClassLoader pageClassloader = customPageService.getPageClassloader(apiSession, pageResourceProvider);

        // When
        customPageService.retrievePageZipContent(apiSession, pageResourceProvider);
        final Class<RestApiController> restApiControllerClass = customPageService.registerRestApiPage(pageClassloader, pageResourceProvider, "RestResource.groovy");

        // then
        final RestApiController restApiController = restApiControllerClass.newInstance();
        final RestApiResponse restApiResponse = restApiController.doHandle(request, pageResourceProvider, pageContext, new RestApiResponseBuilder(), new RestApiUtilImpl());
        RestApiResponseAssert.assertThat(restApiResponse).as("should return result").hasResponse("RestResource.groovy!")
                .hasNoAdditionalCookies().hasHttpStatus(200);
    }

    @Test
    public void should_add_api_extension_permission() throws Exception {
        //given
        doReturn("custompage_test").when(mockedPage).getName();
        doReturn(null).when(mockedPage).getProcessDefinitionId();
        File propertyFile = new File(getClass().getResource(PAGE_PROPERTIES).toURI());
        doReturn(pageResourceProvider).when(customPageService).getPageResourceProvider(eq(mockedPage), anyLong());
        doReturn(propertyFile).when(pageResourceProvider).getResourceAsFile(anyString());
        doNothing().when(customPageService).ensurePageFolderIsUpToDate(apiSession,pageResourceProvider);

        //when
        customPageService.addRestApiExtensionPermissions(resourcesPermissionsMapping, pageResourceProvider, apiSession);

        //then
        verify(customPageService).ensurePageFolderIsUpToDate(apiSession,pageResourceProvider);
        verify(resourcesPermissionsMapping).setProperty("GET|extension/restApiGet", "[permission1]");
        verify(resourcesPermissionsMapping).setProperty("POST|extension/restApiPost", "[permission2,permission3]");
    }

    @Test
    public void should_add_no_api_extension_permission() throws Exception {
        //given
        doReturn("custompage_test").when(mockedPage).getName();
        doReturn(null).when(mockedPage).getProcessDefinitionId();
        File propertyFile = new File(getClass().getResource(PAGE_NO_API_EXTENSION_PROPERTIES).toURI());
        doReturn(pageResourceProvider).when(customPageService).getPageResourceProvider(eq(mockedPage), anyLong());
        doReturn(propertyFile).when(pageResourceProvider).getResourceAsFile(anyString());
        doNothing().when(customPageService).ensurePageFolderIsUpToDate(apiSession, pageResourceProvider);

        //when
        customPageService.addRestApiExtensionPermissions(resourcesPermissionsMapping, pageResourceProvider, apiSession);

        //then
        verify(customPageService).ensurePageFolderIsUpToDate(apiSession,pageResourceProvider);
        verifyZeroInteractions(resourcesPermissionsMapping);
    }


    @Test
    public void should_remove_api_extension_permissions() throws Exception {
        //given
        File propertyFile = new File(getClass().getResource(PAGE_PROPERTIES).toURI());
        doReturn(pageResourceProvider).when(customPageService).getPageResourceProvider(eq(mockedPage), anyLong());
        doReturn(propertyFile).when(pageResourceProvider).getResourceAsFile(anyString());
        doNothing().when(customPageService).ensurePageFolderIsUpToDate(apiSession, pageResourceProvider);

        //when
        customPageService.removeRestApiExtensionPermissions(resourcesPermissionsMapping, pageResourceProvider, apiSession);

        //then
        verify(customPageService).ensurePageFolderIsUpToDate(apiSession,pageResourceProvider);
        verify(resourcesPermissionsMapping).removeProperty("GET|extension/restApiGet");
        verify(resourcesPermissionsMapping).removeProperty("POST|extension/restApiPost");
    }

    @Test
    public void should_remove_no_permissions() throws Exception {
        //given
        File propertyFile = new File(getClass().getResource(PAGE_NO_API_EXTENSION_PROPERTIES).toURI());
        doReturn(pageResourceProvider).when(customPageService).getPageResourceProvider(eq(mockedPage), anyLong());
        doReturn(propertyFile).when(pageResourceProvider).getResourceAsFile(anyString());
        doNothing().when(customPageService).ensurePageFolderIsUpToDate(apiSession, pageResourceProvider);

        //when
        customPageService.removeRestApiExtensionPermissions(resourcesPermissionsMapping, pageResourceProvider, apiSession);

        //then
        verify(customPageService).ensurePageFolderIsUpToDate(apiSession,pageResourceProvider);
        verifyZeroInteractions(resourcesPermissionsMapping);
    }


}

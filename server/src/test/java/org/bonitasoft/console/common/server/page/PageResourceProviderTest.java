package org.bonitasoft.console.common.server.page;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.bonitasoft.engine.api.PageAPI;
import org.bonitasoft.engine.page.Page;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * @author Laurent Leseigneur
 */
@RunWith(MockitoJUnitRunner.class)
public class PageResourceProviderTest {

    public static final String PAGE_NAME = "pageName";
    public static final long PROCESS_DEFINITION_ID = 123L;
    public static final long PAGE_ID = 123L;

    @Mock
    private Page page;

    @Mock
    private PageAPI pageApi;

    PageResourceProvider pageResourceProvider;

    PageResourceProvider pageResourceProviderWithProcessDefinition;

    @Before
    public void before() throws Exception {
        doReturn(PROCESS_DEFINITION_ID).when(page).getProcessDefinitionId();
        doReturn(PAGE_NAME).when(page).getName();
        doReturn(PAGE_ID).when(page).getId();

        pageResourceProvider = new PageResourceProvider(PAGE_NAME, 1);
        pageResourceProviderWithProcessDefinition = new PageResourceProvider(page, 1);
    }

    @Test
    public void should_temp_file_be_distinct() throws Exception {
        assertThat(pageResourceProvider.getTempPageDirectory()).as("should be distinct").isNotEqualTo(pageResourceProviderWithProcessDefinition.getTempPageDirectory());
    }

    @Test
    public void should_pagedirectory_be_distinct() throws Exception {
        assertThat(pageResourceProvider.getPageDirectory()).as("should be distinct").isNotEqualTo(pageResourceProviderWithProcessDefinition.getPageDirectory());
    }

    @Test
    public void should_temp_page_file_be_distinct() throws Exception {
        assertThat(pageResourceProvider.getTempPageFile()).as("should be page name").isNotEqualTo(pageResourceProviderWithProcessDefinition.getTempPageFile());
    }

    @Test
    public void should_testGetFullPageName_return_unique_key() throws Exception {
        assertThat(pageResourceProvider.getFullPageName()).as("should be page name").isEqualTo(PAGE_NAME);
        assertThat(pageResourceProvider.getFullPageName()).as("should be page name").isNotEqualTo(pageResourceProviderWithProcessDefinition.getFullPageName());
    }

    @Test
    public void should_getPage_by_name() throws Exception {
        // when
        pageResourceProvider.getPage(pageApi);

        //then
        verify(pageApi).getPageByName(PAGE_NAME);
    }

    @Test
    public void should_getPage_by_id() throws Exception {
        // when
        pageResourceProviderWithProcessDefinition.getPage(pageApi);

        //then
        verify(pageApi,never()).getPageByName(anyString());
        verify(pageApi).getPage(PAGE_ID);

    }

}
package com.blibli.oss.common.paging;

import com.blibli.oss.common.TestApplication;
import com.blibli.oss.common.TestHelper;
import com.blibli.oss.common.paging.webmvc.PagingInterceptor;
import com.blibli.oss.common.properties.PagingProperties;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Eko Kurniawan Khannedy
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestApplication.class)
public class PagingInterceptorTest {

  @Autowired
  private PagingProperties properties;

  @Test
  public void toSortByList() throws Exception {
    PagingInterceptor interceptor = new PagingInterceptor(properties);

    assertTrue(PagingHelper.toSortByList("", properties).isEmpty());
    assertTrue(PagingHelper.toSortByList(":", properties).isEmpty());
    assertTrue(PagingHelper.toSortByList(",", properties).isEmpty());

    List<SortBy> sorts = PagingHelper.toSortByList("property", properties);
    assertEquals("property", sorts.get(0).getPropertyName());
    assertEquals("asc", sorts.get(0).getDirection());

    assertTrue(PagingHelper.toSortByList(":desc", properties).isEmpty());

    sorts = PagingHelper.toSortByList("property:", properties);
    assertEquals("property", sorts.get(0).getPropertyName());
    assertEquals("asc", sorts.get(0).getDirection());

    sorts = PagingHelper.toSortByList("property:desc", properties);
    assertEquals("property", sorts.get(0).getPropertyName());
    assertEquals("desc", sorts.get(0).getDirection());
  }

  @Test
  public void toInt() throws Exception {
    PagingInterceptor interceptor = new PagingInterceptor(properties);

    assertEquals(Integer.valueOf(1), PagingHelper.toInt("1", 100));
    assertEquals(Integer.valueOf(100), PagingHelper.toInt("salah", 100));
  }

  @Test
  public void fromServletNoQueryParameter() throws Exception {
    PagingInterceptor interceptor = new PagingInterceptor(properties);

    HttpServletRequest request = mock(HttpServletRequest.class);

    PagingRequest pagingRequest = PagingHelper.fromServlet(request, properties);

    assertEquals(properties.getDefaultPage(), pagingRequest.getPage());
    assertEquals(properties.getDefaultItemPerPage(), pagingRequest.getItemPerPage());
    assertTrue(pagingRequest.getSortBy().isEmpty());
  }

  @Test
  public void fromServletWithQueryParameter() throws Exception {
    PagingInterceptor interceptor = new PagingInterceptor(properties);

    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getParameter(properties.getPageQueryParam())).thenReturn("10");
    when(request.getParameter(properties.getItemPerPageQueryParam())).thenReturn("100");
    when(request.getParameter(properties.getSortByQueryParam())).thenReturn("propertyName:asc");

    PagingRequest pagingRequest = PagingHelper.fromServlet(request, properties);

    assertEquals(Integer.valueOf(10), pagingRequest.getPage());
    assertEquals(Integer.valueOf(100), pagingRequest.getItemPerPage());
    assertEquals("propertyName", pagingRequest.getSortBy().get(0).getPropertyName());
    assertEquals("asc", pagingRequest.getSortBy().get(0).getDirection());
  }

  @Test
  public void fromServletWithBadQueryParameter() throws Exception {
    PagingInterceptor interceptor = new PagingInterceptor(properties);

    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getParameter(properties.getPageQueryParam())).thenReturn("salah");
    when(request.getParameter(properties.getItemPerPageQueryParam())).thenReturn("salah");
    when(request.getParameter(properties.getSortByQueryParam())).thenReturn(":");

    PagingRequest pagingRequest = PagingHelper.fromServlet(request, properties);

    assertEquals(properties.getDefaultPage(), pagingRequest.getPage());
    assertEquals(properties.getDefaultItemPerPage(), pagingRequest.getItemPerPage());
    assertTrue(pagingRequest.getSortBy().isEmpty());
  }

  @Test
  public void fromServletWithQueryParameterThatHitMaxItemPerPage() throws Exception {
    PagingProperties pagingProperties = new PagingProperties();
    BeanUtils.copyProperties(properties, pagingProperties);
    pagingProperties.setMaxItemPerPage(10);

    PagingInterceptor interceptor = new PagingInterceptor(pagingProperties);

    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getParameter(pagingProperties.getPageQueryParam())).thenReturn("10");
    when(request.getParameter(pagingProperties.getItemPerPageQueryParam())).thenReturn("100");

    PagingRequest pagingRequest = PagingHelper.fromServlet(request, pagingProperties);

    assertEquals(Integer.valueOf(10), pagingRequest.getPage());
    assertEquals(Integer.valueOf(10), pagingRequest.getItemPerPage());
  }

  @Test
  public void preHandle() throws Exception {
    PagingInterceptor interceptor = new PagingInterceptor(properties);
    boolean result = interceptor.preHandle(mock(HttpServletRequest.class), mock(HttpServletResponse.class), null);
    assertTrue(result);
  }

  @Test
  public void preHandleNoPaging() throws Exception {
    PagingInterceptor interceptor = new PagingInterceptor(properties);
    HttpServletRequest servletRequest = mock(HttpServletRequest.class);

    boolean result = interceptor.preHandle(servletRequest, mock(HttpServletResponse.class), TestHelper.handlerMethodNoPaging());
    assertTrue(result);

    ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Object> objectArgumentCaptor = ArgumentCaptor.forClass(Object.class);
    verify(servletRequest, times(0)).setAttribute(stringArgumentCaptor.capture(), objectArgumentCaptor.capture());
  }

  @Test
  public void preHandleWithPaging() throws Exception {
    PagingInterceptor interceptor = new PagingInterceptor(properties);
    HttpServletRequest servletRequest = mock(HttpServletRequest.class);
    when(servletRequest.getParameter(properties.getPageQueryParam())).thenReturn("10");
    when(servletRequest.getParameter(properties.getItemPerPageQueryParam())).thenReturn("100");

    boolean result = interceptor.preHandle(servletRequest, mock(HttpServletResponse.class), TestHelper.handlerMethod());
    assertTrue(result);

    ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Object> objectArgumentCaptor = ArgumentCaptor.forClass(Object.class);
    verify(servletRequest, times(1)).setAttribute(stringArgumentCaptor.capture(), objectArgumentCaptor.capture());

    PagingRequest pagingRequest = (PagingRequest) objectArgumentCaptor.getValue();
    assertEquals(Integer.valueOf(10), pagingRequest.getPage());
    assertEquals(Integer.valueOf(100), pagingRequest.getItemPerPage());
  }
}

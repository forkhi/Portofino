<%@ page import="net.sourceforge.stripes.util.UrlBuilder" %>
<%@ page contentType="text/html;charset=ISO-8859-1" language="java" pageEncoding="ISO-8859-1"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes-dynattr.tld"%>
<%@taglib prefix="mde" uri="/manydesigns-elements"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<stripes:layout-render name="/skins/${skin}/portlet.jsp">
    <jsp:useBean id="actionBean" scope="request" type="com.manydesigns.portofino.pageactions.crud.CrudAction"/>
    <stripes:layout-component name="portletTitle">
        <c:out value="${actionBean.crudConfiguration.searchTitle}"/>
    </stripes:layout-component>
    <stripes:layout-component name="portletBody">
        <div class="embedded-content">
            <div class="search_results">
                <%
                    UrlBuilder urlBuilder =
                            new UrlBuilder(request.getLocale(), actionBean.getDispatch().getOriginalPath(), true);
                    urlBuilder.addParameter("searchVisible", true);
                %>
                <stripes:link href="<%= urlBuilder.toString() %>">
                    <fmt:message key="commons.advanced_search"/> &gt;&gt;
                </stripes:link>
                <%@include file="datatable.jsp"%>
            </div>
        </div>
    </stripes:layout-component>
</stripes:layout-render>
package com.adobe.aem.guides.bond.core.servlets;


import com.adobe.aem.guides.bond.core.services.SearchRatingService;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import java.io.IOException;

import static com.adobe.aem.guides.bond.core.servlets.SearchRatingServlet.SERVLET_PATH;


@Component(service = Servlet.class,
        property = {"sling.servlet.paths=" + SERVLET_PATH,
                "sling.servlet.methods=" + HttpConstants.METHOD_GET})
public class SearchRatingServlet extends SlingAllMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(SearchRatingServlet.class);
    public static final String SERVLET_PATH = "/bin/searchrating";
    @Reference
    SearchRatingService searchRatingService;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        response.getWriter().println(searchRatingService.ratingResult());

    }

}

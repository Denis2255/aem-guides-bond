package com.adobe.aem.guides.bond.core.servlets;


import com.adobe.aem.guides.bond.core.services.ServiceResolver;
import com.day.cq.replication.*;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;


@Component(service = Servlet.class,
        property = {"sling.servlet.paths=" + "/bin/replicationagent",
                "sling.servlet.methods=" + HttpConstants.METHOD_GET})
public class CustomerReplicationStep extends SlingAllMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(CustomerReplicationStep.class);

    @Reference
    private Replicator replicator;

    @Reference
    private AgentManager agentManager;

    @Reference
    private ServiceResolver serviceResolver;

    String replicationAgentName = "Default Agent publish2";
    String payloadPath = "/etc/packages/my_packages/sample.zip";

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        try (ResourceResolver resourceResolver = serviceResolver.getServiceResourceResolver()) {
            Session session = resourceResolver.adaptTo(Session.class);
            for(final Agent replicationAgent : agentManager.getAgents().values()){
                ReplicationOptions replicationOptions = new ReplicationOptions();
                replicationOptions.setFilter(agent -> replicationAgentName.equals(agent.getConfiguration().getName()));
                replicationOptions.setSynchronous(false);
                replicator.replicate(session, ReplicationActionType.ACTIVATE, payloadPath, replicationOptions);
                LOG.info("Replicated via Agent : {} for Payload {}", replicationAgentName, payloadPath);
                response.getWriter().println("Replicated via Agent :  for Payload ");
            }
        } catch (Exception e) {
            LOG.info(e.getMessage(), e);
        }
    }
}

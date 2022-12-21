package com.adobe.aem.guides.bond.core.schedulers;

import com.adobe.aem.guides.bond.core.services.ServiceResolver;
import com.day.cq.replication.*;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;


@Component(service = Runnable.class, immediate = true)
@Designate(ocd = SchedulerRating.ServiceConfigurationRatingScheduler.class)
public class SchedulerRating implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(SchedulerRating.class);
    private int schedulerId;
    private ServiceConfigurationRatingScheduler configuration;

    @Reference
    private Scheduler scheduler;

    @Reference
    private Replicator replicator;

    @Reference
    private AgentManager agentManager;

    @Reference
    private ServiceResolver serviceResolver;

    @Activate
    @Modified
    protected void activate(ServiceConfigurationRatingScheduler config) {
        schedulerId = config.schedulerName().hashCode();
        addScheduler(config);
        this.configuration = config;
    }

    private void addScheduler(ServiceConfigurationRatingScheduler config) {
        ScheduleOptions scheduleOptions = scheduler.EXPR(config.cronExpression());
        scheduleOptions.name(String.valueOf(schedulerId));
        scheduleOptions.canRunConcurrently(false);
        scheduler.schedule(this, scheduleOptions);

    }

    @Deactivate
    protected void deactivate(ServiceConfigurationRatingScheduler config) {
        removeScheduler();
    }

    private void removeScheduler() {
        scheduler.unschedule(String.valueOf(schedulerId));
    }

    @Override
    public void run() {
        String replicationAgentName = configuration.replicationAgentName();
        String payloadPath = configuration.payloadPath();
        LOG.info("_____________________________RUN METHOD_______________________________________");
        try (ResourceResolver resourceResolver = serviceResolver.getServiceResourceResolver()) {
            Session session = resourceResolver.adaptTo(Session.class);
            for (final Agent replicationAgent : agentManager.getAgents().values()) {
                ReplicationOptions replicationOptions = new ReplicationOptions();
                replicationOptions.setFilter(agent -> replicationAgentName.equals(agent.getConfiguration().getName()));
                replicationOptions.setSynchronous(false);
                replicator.replicate(session, ReplicationActionType.ACTIVATE, payloadPath, replicationOptions);
                LOG.info("Replicated via Agent : {} for Payload {}", replicationAgentName, payloadPath);
                resourceResolver.commit();
            }
        } catch (Exception e) {
            LOG.info(e.getMessage(), e);
        }
    }

    @ObjectClassDefinition(name = "BOND_CUSTOM - Scheduler Configuration and Replication")
    public @interface ServiceConfigurationRatingScheduler {
        @AttributeDefinition(
                name = "Scheduler_name",
                description = "Name of scheduler",
                type = AttributeType.STRING)
        String schedulerName() default "Custom";

        @AttributeDefinition(
                name = "Cron_expression",
                type = AttributeType.STRING)
        String cronExpression() default "0 0 0/1 1/1 * ? *";

        @AttributeDefinition(
                name = "Replication_Agent_Name",
                type = AttributeType.STRING)
        String replicationAgentName() default "Default Agent publish2";

        @AttributeDefinition(
                name = "Pay_load_Path",
                type = AttributeType.STRING)
        String payloadPath() default "/etc/packages/my_packages/sample.zip";
    }
}

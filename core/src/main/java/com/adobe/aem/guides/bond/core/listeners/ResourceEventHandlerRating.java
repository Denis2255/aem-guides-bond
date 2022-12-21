package com.adobe.aem.guides.bond.core.listeners;

import com.adobe.aem.guides.bond.core.schedulers.SchedulerRating;
import com.adobe.aem.guides.bond.core.services.ServiceResolver;
import org.apache.jackrabbit.vault.fs.api.PathFilterSet;
import org.apache.jackrabbit.vault.fs.api.ProgressTrackerListener;
import org.apache.jackrabbit.vault.fs.config.DefaultWorkspaceFilter;
import org.apache.jackrabbit.vault.fs.io.ImportOptions;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageDefinition;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.PackagingService;
import org.apache.jackrabbit.vault.util.DefaultProgressListener;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChangeListener;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.Session;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component(service = ResourceChangeListener.class,
        immediate = true,
        property = {
                ResourceChangeListener.PATHS + "=/content/bond/us/en",
                ResourceChangeListener.CHANGES + "=ADDED",
                ResourceChangeListener.CHANGES + "=CHANGED",
                ResourceChangeListener.CHANGES + "=REMOVED"
        })
@Designate(ocd = ResourceEventHandlerRating.ServiceConfigurationPackage.class)
public class ResourceEventHandlerRating implements ResourceChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceEventHandlerRating.class);
    private ServiceConfigurationPackage configuration;

    @Reference
    private ServiceResolver serviceResolver;

    @Activate
    @Modified
    protected void activate(ServiceConfigurationPackage config) {
        this.configuration = config;
    }

    @Override
    public void onChange(List<ResourceChange> list) {
        List<String> filterPaths = new ArrayList<>();
        filterPaths.add(configuration.pathsFilter());
        try (ResourceResolver resourceResolver = serviceResolver.getServiceResourceResolver();) {
            Session session = resourceResolver.adaptTo(Session.class);
            JcrPackageManager jcrPackageManager = PackagingService.getPackageManager(session);
            Node node = Optional.ofNullable(resourceResolver.getResource("/etc/packages/my_packages/sample.zip"))
                    .map(n -> n.adaptTo(Node.class)).orElse(null);
            if (node == null) {
                LOG.info("Node null");
                return;
            }
            JcrPackage jcrPackageOld = jcrPackageManager.open(node);
            jcrPackageManager.remove(jcrPackageOld);
            try (JcrPackage jcrPackage = jcrPackageManager.create(configuration.nameGroup(), configuration.namePackage())) {
                JcrPackageDefinition definition = jcrPackage.getDefinition();
                DefaultWorkspaceFilter filter = new DefaultWorkspaceFilter();
                for (String filterPath : filterPaths) {
                    PathFilterSet pathFilterSet = new PathFilterSet();
                    pathFilterSet.setRoot(filterPath);
                    filter.add(pathFilterSet);
                }
                assert definition != null;
                definition.setFilter(filter, true);

                ProgressTrackerListener listener = new DefaultProgressListener();
                jcrPackageManager.assemble(jcrPackage, listener);

                ImportOptions importOption = new ImportOptions();
                jcrPackage.install(importOption);
                LOG.info("Package created successfully !!!");
            }
        } catch (Exception e) {
            LOG.info(e.getMessage(), e);
        }
    }

    @ObjectClassDefinition(name = "BOND_CUSTOM - Configuration package")
    public @interface ServiceConfigurationPackage {
        @AttributeDefinition(
                name = "NAME_PACKAGE",
                type = AttributeType.STRING)
        String namePackage() default "sample";

        @AttributeDefinition(
                name = "NAME_GROUP",
                type = AttributeType.STRING)
        String nameGroup() default "my_packages";

        @AttributeDefinition(
                name = "PATHS_FILTER",
                type = AttributeType.STRING)
        String pathsFilter() default "/content/bond/us/en";
    }
}


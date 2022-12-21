package com.adobe.aem.guides.bond.core.services.impl;


import com.adobe.aem.guides.bond.core.services.SearchRatingService;
import com.adobe.aem.guides.bond.core.services.ServiceResolver;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import com.day.crx.JcrConstants;
import org.apache.sling.api.resource.ResourceResolver;
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
import java.util.*;

@Component(service = SearchRatingService.class, immediate = true)
@Designate(ocd = SearchRatingServiceImpl.ServiceConfigurationBondRating.class)
public class SearchRatingServiceImpl implements SearchRatingService {

    private static final Logger LOG = LoggerFactory.getLogger(SearchRatingServiceImpl.class);
    private ServiceConfigurationBondRating configuration;

    @Reference
    ServiceResolver serviceResolver;

    @Reference
    private QueryBuilder queryBuilder;

    @Activate
    @Modified
    private void activate(ServiceConfigurationBondRating configuration) {
        this.configuration = configuration;
    }

    @Override
    public List<Double> ratingResult() {
        List<Double> ratingResult = new ArrayList<>();
//        JSONObject ratingResult = new JSONObject();
        try (ResourceResolver resourceResolver = serviceResolver.getServiceResourceResolver()) {
            final Session session = resourceResolver.adaptTo(Session.class);
            Query query = queryBuilder.createQuery(PredicateGroup.create(createTextSearchQuery()), session);
            SearchResult result = query.getResult();
            List<Hit> hits = result.getHits();
            List<Double> allRating = new ArrayList<>();
            for (Hit hit : hits) {
                Node node = hit.getResource().adaptTo(Node.class);
                Node jcrNode = node.getNode(JcrConstants.JCR_CONTENT);
                double valueProperty = jcrNode.getProperty("rating").getValue().getDouble();
                allRating.add(valueProperty);
            }
            OptionalDouble average = allRating.stream().mapToDouble(e -> e).average();
            ratingResult.add(average.getAsDouble());
        } catch (Exception e) {
            LOG.info(e.getMessage(), e);
        }
        return ratingResult;
    }

    public Map<String, String> createTextSearchQuery() {
        Map<String, String> queryMap = new HashMap<>();
        queryMap.put("type", configuration.typeProperty());
        queryMap.put("path", configuration.rootPath());
        queryMap.put("1_property", configuration.nameProperty());
        queryMap.put("p.limit", "-1");
        return queryMap;
    }

    @ObjectClassDefinition(name = "BOND_CUSTOM - Service Configuration Rating")
    public @interface ServiceConfigurationBondRating {

        @AttributeDefinition(
                name = "ROOT_PATH",
                type = AttributeType.STRING)
        String rootPath() default " ";

        @AttributeDefinition(
                name = "TYPE_PROPERTY",
                type = AttributeType.STRING)
        String typeProperty() default " ";

        @AttributeDefinition(
                name = "NAME_PROPERTY",
                type = AttributeType.STRING)
        String nameProperty() default " ";
    }
}

package com.gnipcentral.client;

import com.gnipcentral.client.resource.Publisher;
import com.gnipcentral.client.resource.PublisherScope;
import com.gnipcentral.client.resource.RuleType;
import com.gnipcentral.client.resource.Publishers;

import java.util.HashSet;

/**
 * 
 */
public class PublisherTest extends GnipTestCase {

    public void testPublisherConstructors() throws Exception {
        {
            HashSet<RuleType> oneRuleType = new HashSet<RuleType>();
            oneRuleType.add(RuleType.ACTOR);
            
            Publisher publisher = new Publisher(PublisherScope.GNIP, "foobar", oneRuleType);
            assertEquals(PublisherScope.GNIP, publisher.getScope());
            assertEquals("foobar", publisher.getName());
            assertEquals(oneRuleType.size(), publisher.getSupportedRuleTypes().size());
            assertTrue(publisher.hasSupportedRuleType(RuleType.ACTOR));
        }

        {
            Publisher publisher = new Publisher(PublisherScope.MY, "foobar", RuleType.ACTOR, RuleType.TAG);
            assertEquals(PublisherScope.MY, publisher.getScope());
            assertEquals("foobar", publisher.getName());
            assertEquals(2, publisher.getSupportedRuleTypes().size());
            assertTrue(publisher.hasSupportedRuleType(RuleType.ACTOR));
            assertTrue(publisher.hasSupportedRuleType(RuleType.TAG));
        }
    }

    public void testGetPublisher() throws Exception {
        Publisher publisher = gnipConnection.getPublisher(PublisherScope.MY, localPublisher.getName());
        assertNotNull(publisher);
        assertEquals(localPublisher.getName(), publisher.getName());
    }

    public void testGetPublisherIncludesCapabilities() throws Exception {
        Publisher publisher = gnipConnection.getPublisher(PublisherScope.MY, localPublisher.getName());
        assertNotNull(publisher);
        assertTrue(localPublisher.hasSupportedRuleType(RuleType.ACTOR));
    }

    public void testGetPublishers() throws Exception {
        Publishers publishers = gnipConnection.getPublishers(PublisherScope.MY);
        assertNotNull(publishers);
        assertContains(localPublisher, publishers.getPublishers());
    }
}

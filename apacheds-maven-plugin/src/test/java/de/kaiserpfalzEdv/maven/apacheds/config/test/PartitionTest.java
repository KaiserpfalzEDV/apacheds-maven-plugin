package de.kaiserpfalzEdv.maven.apacheds.config.test;

import de.kaiserpfalzEdv.maven.apacheds.config.Partition;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.name.Rdn;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;

/**
 * @author klenkes
 * @since 2013Q
 */
public class PartitionTest {
    private static final Logger LOG = LoggerFactory.getLogger(PartitionTest.class);

    private Partition service;

    @Test
    public void testDn() throws LdapInvalidDnException {
        List<Rdn> expected = new ArrayList<>(2);
        expected.add(new Rdn("dc=example"));
        expected.add(new Rdn("dc=com"));


        LOG.debug("Checking getting DN object from: {}", service);
        Dn result = service.getDnObject();

        assertArrayEquals(expected.toArray(), result.getRdns().toArray());
    }


    @Before
    public void createService() {
        service = new Partition();

        service.setId("TEST");
        service.setDn("dc=example,dc=com");
        service.setType("JDBM");

        LOG.trace("Created test service: {}", service);
    }

    @After
    public void destroyService() {
        service = null;
    }
}

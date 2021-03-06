package org.apereo.cas.authorization;

import org.apereo.cas.configuration.support.Beans;
import org.ldaptive.ConnectionFactory;
import org.ldaptive.LdapAttribute;
import org.ldaptive.LdapEntry;
import org.ldaptive.LdapException;
import org.ldaptive.Response;
import org.ldaptive.SearchExecutor;
import org.ldaptive.SearchResult;
import org.pac4j.core.authorization.generator.AuthorizationGenerator;
import org.pac4j.core.profile.CommonProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * Provides a simple {@link AuthorizationGenerator} implementation that obtains user roles from an LDAP search.
 * Two searches are performed by this component for every user details lookup:
 * <ol>
 * <li>Search for an entry to resolve the username. In most cases the search should return exactly one result,
 * but the {@link #allowMultipleResults} property may be toggled to change that behavior.</li>
 * <li>Search for groups of which the user is a member. This search commonly occurs on a separate directory
 * branch than that of the user search.</li>
 * </ol>
 *
 * @author Jerome Leleu
 * @author Marvin S. Addison
 * @author Misagh Moayyed
 * @since 5.1.0
 */
public class LdapUserGroupsToRolesAuthorizationGenerator extends LdapUserAttributesToRolesAuthorizationGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(LdapUserGroupsToRolesAuthorizationGenerator.class);

    private final String groupAttributeName;
    private final String groupPrefix;
    private final SearchExecutor groupSearchExecutor;

    /**
     * Instantiates a new Ldap user groups to roles authorization generator.
     *
     * @param factory              the factory
     * @param userSearchExecutor   the user search executor
     * @param allowMultipleResults the allow multiple results
     * @param roleAttribute        the role attribute
     * @param rolePrefix           the role prefix
     * @param groupAttributeName   the group attribute name
     * @param groupPrefix          the group prefix
     * @param groupSearchExecutor  the group search executor
     */
    public LdapUserGroupsToRolesAuthorizationGenerator(final ConnectionFactory factory,
                                                       final SearchExecutor userSearchExecutor,
                                                       final boolean allowMultipleResults,
                                                       final String roleAttribute,
                                                       final String rolePrefix, 
                                                       final String groupAttributeName, 
                                                       final String groupPrefix, 
                                                       final SearchExecutor groupSearchExecutor) {
        super(factory, userSearchExecutor, allowMultipleResults, roleAttribute, rolePrefix);
        this.groupAttributeName = groupAttributeName;
        this.groupPrefix = groupPrefix;
        this.groupSearchExecutor = groupSearchExecutor;
    }

    @Override
    protected void addProfileRoles(final LdapEntry userEntry,
                                   final CommonProfile profile,
                                   final LdapAttribute attribute) {
        try {
            LOGGER.debug("Attempting to get roles for user {}.", userEntry.getDn());
            final Response<SearchResult> response = this.groupSearchExecutor.search(
                    this.connectionFactory,
                    Beans.newSearchFilter(this.groupSearchExecutor.getSearchFilter().getFilter(),
                            Beans.LDAP_SEARCH_FILTER_DEFAULT_PARAM_NAME, Arrays.asList(userEntry.getDn())));
            LOGGER.debug("LDAP role search response: {}", response);
            final SearchResult groupResult = response.getResult();
            
            for (final LdapEntry entry : groupResult.getEntries()) {
                final LdapAttribute groupAttribute = entry.getAttribute(this.groupAttributeName);
                if (groupAttribute == null) {
                    LOGGER.warn("Role attribute not found on entry {}", entry);
                    continue;
                }
                addProfileRolesFromAttributes(profile, groupAttribute, this.groupPrefix);
            }
        } catch (final LdapException e) {
            throw new RuntimeException("LDAP error fetching roles for user.", e);
        }
    }
}

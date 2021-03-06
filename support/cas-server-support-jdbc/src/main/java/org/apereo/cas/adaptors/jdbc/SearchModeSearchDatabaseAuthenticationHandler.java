package org.apereo.cas.adaptors.jdbc;

import org.apache.commons.lang3.StringUtils;
import org.apereo.cas.authentication.HandlerResult;
import org.apereo.cas.authentication.PreventedException;
import org.apereo.cas.authentication.UsernamePasswordCredential;
import org.springframework.dao.DataAccessException;

import javax.security.auth.login.FailedLoginException;
import java.security.GeneralSecurityException;

/**
 * Class that given a table, username field and password field will query a
 * database table with the provided encryption technique to see if the user
 * exists. This class defaults to a PasswordTranslator of
 * PlainTextPasswordTranslator.
 *
 * @author Scott Battaglia
 * @author Dmitriy Kopylenko
 * @author Marvin S. Addison
 * @since 3.0.0
 */
public class SearchModeSearchDatabaseAuthenticationHandler extends AbstractJdbcUsernamePasswordAuthenticationHandler {
    
    private final String fieldUser;
    private final String fieldPassword;
    private final String tableUsers;

    public SearchModeSearchDatabaseAuthenticationHandler(final String fieldUser, final String fieldPassword, final String tableUsers) {
        this.fieldUser = fieldUser;
        this.fieldPassword = fieldPassword;
        this.tableUsers = tableUsers;
    }

    @Override
    protected HandlerResult authenticateUsernamePasswordInternal(final UsernamePasswordCredential credential)
            throws GeneralSecurityException, PreventedException {

        String sql = null;
        if (StringUtils.isNotBlank(tableUsers) || StringUtils.isNotBlank(fieldUser) || StringUtils.isNotBlank(fieldPassword)) {
            sql = "SELECT COUNT('x') FROM ".concat(this.tableUsers).concat(" WHERE ").concat(this.fieldUser)
                    .concat(" = ? AND ").concat(this.fieldPassword).concat("= ?");
        }

        if (StringUtils.isBlank(sql) || getJdbcTemplate() == null) {
            throw new GeneralSecurityException("Authentication handler is not configured correctly. "
                    + "No SQL statement or JDBC template found");
        }

        final String username = credential.getUsername();
        try {
            logger.debug("Executing SQL query {}", sql);

            final int count = getJdbcTemplate().queryForObject(sql, Integer.class, username, credential.getPassword());
            if (count == 0) {
                throw new FailedLoginException(username + " not found with SQL query.");
            }
            return createHandlerResult(credential, this.principalFactory.createPrincipal(username), null);
        } catch (final DataAccessException e) {
            throw new PreventedException("SQL exception while executing query for " + username, e);
        }
    }
}

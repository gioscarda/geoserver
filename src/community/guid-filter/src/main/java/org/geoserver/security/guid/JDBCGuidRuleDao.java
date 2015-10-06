/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.guid;

import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import javax.sql.DataSource;

import org.geoserver.platform.exception.GeoServerException;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.opengis.filter.Filter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.TransactionException;

/**
 * Spring JDBC implementation of {@link GuidRuleDao}
 * 
 * @author Andrea Aime - GeoSolutions
 *
 */
public class JDBCGuidRuleDao implements GuidRuleDao {

    static final RowMapper<GuidRule> GUID_ROW_MAPPER = new RowMapper<GuidRule>() {

        public GuidRule mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
            GuidRule rule = new GuidRule();
            rule.setGuid(rs.getString("guid"));
            rule.setLayerName(rs.getString("layer_name"));
            rule.setUserId(rs.getString("user_id"));
            String filter = rs.getString("filter");
            if (filter == null) {
                rule.setFilter(Filter.INCLUDE);
            } else {
                try {
                    rule.setFilter(ECQL.toFilter(filter));
                } catch (CQLException e) {
                    throw new SQLException("Invalid filter for guid " + rule.getGuid(), e);
                }
            }
            return rule;
        }
    };

    private JdbcTemplate jt;

    public JDBCGuidRuleDao(DataSource dataSource) {
        this.jt = new JdbcTemplate(dataSource);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.geoserver.security.guid.GuidRuleDao#getRules(java.lang.String)
     */
    @Override
    public List<GuidRule> getRules(final String guid) {
        return jt.query("select * from guids where guid = ?", new Object[] { guid },
                GUID_ROW_MAPPER);
    }

    /**
     * Adds a rule, added for testing purposes
     */
    @Override
    public void addRule(final GuidRule rule) {
        jt.update(
                "insert into guids(guid, layer_name, user_id, filter) values(?, ?, ?, ?)",
                new Object[] { rule.getGuid(), rule.getLayerName(), rule.getUserId(),
                        ECQL.toCQL(rule.getFilter()) }, new int[] { Types.VARCHAR, Types.VARCHAR,
                        Types.VARCHAR, Types.VARCHAR });

    }

    /**
     * Deletes all rules, added for testing purposes
     * 
     * @throws GeoServerException
     * @throws TransactionException
     */
    @Override
    public void clearRules(final String guid) {
        if (guid == null || guid.isEmpty()) {
            jt.update("delete from guids");
        } else {
            jt.update("delete from guids where guid = ?", new Object[] { guid });
        }
    }

}
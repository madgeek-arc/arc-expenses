package arc.expenses.service;

import arc.expenses.domain.Executive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.List;

@Service("poiService")
public class POIServiceImpl {

    @Autowired
    private DataSource dataSource;

    public List<Executive> getPois() {
        return new JdbcTemplate(dataSource)
                .query(" SELECT foo.unnest as email, user_view.first_name as firstname, user_view.last_name as lastname FROM (SELECT DISTINCT UNNEST(project_operator) FROM project_view  " +
                        "UNION  " +
                        "SELECT DISTINCT UNNEST(project_operator_delegate) FROM project_view  " +
                        "UNION  " +
                        "SELECT DISTINCT project_scientificcoordinator FROM project_view " +
                        "UNION " +
                        "SELECT DISTINCT accountingdirector FROM institute_view " +
                        "UNION " +
                        "SELECT DISTINCT accountingpayment FROM institute_view " +
                        "UNION " +
                        "SELECT DISTINCT accountingregistration FROM institute_view " +
                        "UNION " +
                        "SELECT DISTINCT diataktis FROM institute_view " +
                        "UNION " +
                        "SELECT DISTINCT diaugeia FROM institute_view " +
                        "UNION " +
                        "SELECT DISTINCT director FROM institute_view " +
                        "UNION " +
                        "SELECT DISTINCT suppliesoffice FROM institute_view " +
                        "UNION " +
                        "SELECT DISTINCT travelmanager FROM institute_view " +
                        "UNION " +
                        "SELECT DISTINCT UNNEST(accountingdirector_delegate) FROM institute_view " +
                        "UNION " +
                        "SELECT DISTINCT UNNEST(accountingpayment_delegate) FROM institute_view " +
                        "UNION " +
                        "SELECT DISTINCT UNNEST(accountingregistration_delegate) FROM institute_view " +
                        "UNION " +
                        "SELECT DISTINCT UNNEST(diataktis_delegate) FROM institute_view " +
                        "UNION " +
                        "SELECT DISTINCT UNNEST(diaugeia_delegate) FROM institute_view " +
                        "UNION " +
                        "SELECT DISTINCT UNNEST(director_delegate) FROM institute_view " +
                        "UNION " +
                        "SELECT DISTINCT UNNEST(suppliesoffice_delegate) FROM institute_view " +
                        "UNION " +
                        "SELECT DISTINCT UNNEST(travelmanager_delegate) FROM institute_view " +
                        "UNION " +
                        "SELECT DISTINCT dioikitikosumvoulio FROM organization_view " +
                        "UNION " +
                        "SELECT DISTINCT director FROM organization_view " +
                        "UNION " +
                        "SELECT DISTINCT organization_poy FROM organization_view " +
                        "UNION " +
                        "SELECT DISTINCT UNNEST(dioikitikosumvoulio_delegate) FROM organization_view " +
                        "UNION " +
                        "SELECT DISTINCT UNNEST(director_delegate) FROM organization_view " +
                        "UNION " +
                        "SELECT DISTINCT UNNEST(inspectionteam) FROM organization_view " +
                        "UNION " +
                        "SELECT DISTINCT UNNEST(inspectionteam_delegate) FROM organization_view " +
                        "UNION " +
                        "SELECT DISTINCT UNNEST(poy_delegate) FROM organization_view) as foo INNER JOIN user_view on user_view.user_id= foo.unnest;",poiRowMapper);

    }

    private RowMapper<Executive> poiRowMapper = (rs, i) ->
            new Executive(rs.getString("email")
                            .replace("\"","")
                            .replace("{","")
                            .replace("(",""),
                    rs.getString("firstname"),
                    rs.getString("lastname")
                            .replace("\"","")
                            .replace(")",""));


}

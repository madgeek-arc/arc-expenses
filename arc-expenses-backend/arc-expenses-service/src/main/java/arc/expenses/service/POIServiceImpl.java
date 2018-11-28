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
    DataSource dataSource;

    public List<Executive> getPois() {
        return new JdbcTemplate(dataSource)
                .query(" select distinct(email) , firstname,lastname \n" +
                        "from ( select split_part(poi::text,',',1) as email,\n" +
                        "              split_part(poi::text,',',2) as firstname,\n" +
                        "              regexp_replace(split_part(poi::text,',',3),'[^[:alpha:]]','') as lastname\n" +
                        "      from   (\n" +
                        "                select regexp_matches(payload,'(?:\"email\":\")(.*?)(?:\",\"firstname\":\"(.*?)(?:\",\"lastname\":\"(.*?)(?:\")))','g')\n" +
                        "                from resource\n" +
                        "                where fk_name = 'project'\n" +
                        "             )  as poi\n" +
                        "     ) as poi\n" +
                        "where poi.firstname!='\"\"\"\"' and poi.lastname != '\"\"\"\"'",poiRowMapper);

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

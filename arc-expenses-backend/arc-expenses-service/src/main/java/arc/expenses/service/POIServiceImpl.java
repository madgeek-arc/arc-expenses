package arc.expenses.service;

import arc.expenses.domain.Vocabulary;
import gr.athenarc.domain.PersonOfInterest;
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

    public List<PersonOfInterest> getPois() {
        return null;
//        return new JdbcTemplate(dataSource)
//                .query("select project_id ,project_acronym,project_institute from project_view",vocabularyRowMapper);

    }

//    private RowMapper<PersonOfInterest> vocabularyRowMapper = (rs, i) ->
//            new PersonOfInterest());


}

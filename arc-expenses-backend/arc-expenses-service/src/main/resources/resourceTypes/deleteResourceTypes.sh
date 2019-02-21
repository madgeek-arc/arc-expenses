#!/bin/bash

#for i in   user organization project request institute approval payment; do
#	curl -X DELETE -k  https://$1/arc-expenses-service/resourceType/$i
#done

for i in   user organization project request institute approval payment; do
	PGPASSWORD=paparia psql -h $1 -Uvrasidas -p 5433 registry <<endOfMessage

delete from stringindexedfield_values where stringindexedfield_id in (select id from stringindexedfield where resource_id in (select id from resource where fk_name='$i'));
delete from stringindexedfield where id in (select id from stringindexedfield where resource_id in (select id from resource where fk_name='$i'));

delete from booleanindexedfield_values where booleanindexedfield_id in (select id from booleanindexedfield where resource_id in (select id from resource where fk_name='$i'));
delete from booleanindexedfield where id in (select id from stringindexedfield where resource_id in (select id from resource where fk_name='$i'));

delete from floatindexedfield_values where floatindexedfield_id in (select id from floatindexedfield where resource_id in (select id from resource where fk_name='$i'));
delete from floatindexedfield where id in (select id from stringindexedfield where resource_id in (select id from resource where fk_name='$i'));

delete from longindexedfield_values where longindexedfield_id in (select id from longindexedfield where resource_id in (select id from resource where fk_name='$i'));
delete from longindexedfield where id in (select id from stringindexedfield where resource_id in (select id from resource where fk_name='$i'));

delete from dateindexedfield_values where dateindexedfield_id in (select id from dateindexedfield where resource_id in (select id from resource where fk_name='$i'));
delete from dateindexedfield where id in (select id from stringindexedfield where resource_id in (select id from resource where fk_name='$i'));

delete from resourcetype_indexfield where resourcetype_name ='$i';
delete from indexedfield_values where indexedfield_id in (select id from indexedfield where resource_id in (select id from resource where fk_name='$i'));
delete from indexfield where resourcetype_name ='$i';

delete from resourcetype_indexfield where resourcetype_name ='$i';
delete from indexedfield where resource_id in (select id from resource where fk_name='$i');
delete from resourceversion where parent_id in ( select id from resource where fk_name='$i');
delete from resource where fk_name='$i';
delete from resourcetype where name='$i';
delete from schemadatabase where originalurl = '$i';
DROP VIEW ${i}_view;
endOfMessage
	curl -X DELETE http://$2:9200/$i
	curl -X DELETE   http://$1:8080/arc-expenses-service/resourceType/$i
done

PGPASSWORD=paparia psql -h $1 -Uvrasidas -p 5433 registry <<endOfMessage
delete from schemadatabase where originalurl like '%.xsd';
endOfMessage





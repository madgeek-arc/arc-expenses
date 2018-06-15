#!/bin/bash

for i in  user organization project request institute; do
	psql -h $1 -Uvrasidas registry <<endOfMessage
delete from resourcetype_indexfield where resourcetype_name ='$i';
delete from indexfield where resourcetype_name ='$i';
delete from indexedfield_values where indexedfield_id in (select id from indexedfield where resource_id in (select id from resource where fk_name='$i'));
delete from indexedfield where resource_id in (select id from resource where fk_name='$i');
delete from resourceversion where parent_id in ( select id from resource where fk_name='$i');
delete from resource where fk_name='$i';
delete from resourcetype where name='$i';
DROP VIEW ${i}_view;
endOfMessage
	curl -X DELETE http://$1:9200/$i
done

psql -h $1 -Uvrasidas registry <<endOfMessage
delete from schemadatabase where originalurl like '%.xsd';
endOfMessage

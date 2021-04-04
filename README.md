Endpoint https://realitica-parser.herokuapp.com/api/stuns

Heroku has 10000 rows restroction for posgresql
```
delete from stun s where s.last_modified < TO_TIMESTAMP('2021-10-01 9:30:20', 'YYYY-MM-DD HH:MI:SS');
commit;
```

#/bin/bash

db=$1
if [ ! -n "$db" ]; then
  db = 'notify'
else
  echo "select db is $db"
fi

java -jar mybatis-generator-core-1.3.2.jar -configfile $db.xml -overwrite

docker exec -it mongo bash
mongo 
use kafka_bank

# Kafka Connect

List connectors:
```sh
curl localhost:8083/connectors
curl -X POST -H "Content-Type: application/json" -d @connect-mongodb-sink.json localhost:8083/connectors
curl -X POST -H "Content-Type: application/json" -d @connect-mongodb-rolling-sink.json localhost:8083/connectors
curl -X POST -H "Content-Type: application/json" -d @connect-mongodb-total-sink.json localhost:8083/connectors
```


org.apache.kafka.connect.storage.StringConverter
org.apache.kafka.connect.json.JsonConverter

## Mongo Replica Set config

Need to use mongo shell to initialize replica set
```sh
docker exec -it mongo1 mongo
```

Inside mongo shell run the following code
```sh
config={"_id":"rs0","members":[{"_id":0,"host":"mongo1:27017"},{"_id":1,"host":"mongo2:27017"},{"_id":2,"host":"mongo3:27017"}]}
rs.initiate(config)
```

### Some useful mongo shell command
- Create collection  
```
db.createCollection(<name>)
# Example
db.createCollection("category")
```
- List collections in current database  
```
db.getCollectionNames()
```
- Insert one document
```
db.<collection>.insertOne({<payload>})
# Example
db.category.insertOne({_id:1, code: "CG01", name: "Food"})
``` 
- Find and replace one document
```
db.<collection>.findOneAndReplace({<filter>}, {<new_payload>})
# Example
db.category.insertOne({_id:1, code: "CG01", name: "Drink"})
```

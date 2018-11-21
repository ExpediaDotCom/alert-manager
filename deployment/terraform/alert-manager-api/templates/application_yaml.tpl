es:
  index.name: subscription
  create.index.if.not.found: false
  doctype: _doc
  urls: "http://elasticsearch:9200"
  connection.timeout: 5000
  max.connection.idletime: 1000
  max.total.connection: 1000
  read.timeout: 1000

kafka:
  producer:
    bootstrap.servers: ${kafka_endpoint}
    client.id: am_producer
    key.serializer: org.apache.kafka.common.serialization.StringSerializer
    value.serializer: org.springframework.kafka.support.serializer.JsonSerializer
    request.timeout.ms: 40000
    topic: alerts

alert.store:
  pluginDirectory: "storage-backends/elasticsearch/target"
  plugins:
  - name: elasticsearch
    jarName: "elasticsearch-store-1.0.0-SNAPSHOT.jar"
    conf:
     hostname: ${es_urls}

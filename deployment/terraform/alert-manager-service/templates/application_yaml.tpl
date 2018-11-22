es:
  index.name: subscription
  create.index.if.not.found: false
  doctype: _doc
  urls: "${es_urls}"
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
  pluginDirectory: "/app/bin/storage-backends"
  plugins:
  - name: elasticsearch
    jarName: "elasticsearch-store.jar"
    conf:
     host: "${es_urls}"
     template: ""

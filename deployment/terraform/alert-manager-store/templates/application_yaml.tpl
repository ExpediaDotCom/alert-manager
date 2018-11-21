plugin.directory: "/app/bin/storage-backends"
plugin:
  name: "elasticsearch"
  jar.name: "elasticsearch-store.jar"
  conf:
   host: "${es_urls}"
kafka:
  topic: alerts
  stream.threads: 2
  consumer:
    bootstrap.servers: ${kafka_endpoint}
    auto.offset.reset: latest
    group.id: alert-manager-store
    enable.auto.commit: false
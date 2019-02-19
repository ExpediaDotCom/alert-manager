plugin.directory: "/app/bin/storage-backends"
plugin:
  name: "elasticsearch"
  jar.name: "elasticsearch-store.jar"
  conf:
   host: "${es_urls}"
   aws-iam-auth-required: ${es_aws_iam_auth_required}
   aws-region: ${es_aws_region}
kafka:
  topic: alerts
  stream.threads: 4
  consumer:
    bootstrap.servers: ${kafka_endpoint}
    auto.offset.reset: latest
    group.id: alert-manager-store
    enable.auto.commit: false
health.status.file: /app/health_status

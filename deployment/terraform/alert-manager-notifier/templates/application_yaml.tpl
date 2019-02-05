kafka:
  consumer:
    bootstrap.servers: ${kafka_endpoint}
    group.id: am_notifier
    auto.offset.reset: earliest
    session.timeout.ms: 30000
    heartbeat.interval.ms: 10000
    request.timeout.ms: 40000
  topic: alerts

subscription-search:
  url: ${subscription_search_url}

mail:
  from: ${mail_from}

slack:
  url: https://slack.com/api/chat.postMessage
  token: $${SLACK_TOKEN}

rate-limit:
  enabled: ${rate_limit_enabled}
  value: 40000

alert-store-es:
  url: ${es_urls}

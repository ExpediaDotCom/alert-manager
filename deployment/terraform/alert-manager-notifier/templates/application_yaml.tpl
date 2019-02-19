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
  type: aws-ses

slack:
  url: https://slack.com/api/chat.postMessage
  token: $${SLACK_TOKEN}

alert:
  rate-limit:
    enabled: ${alert_rate_limit_enabled}
    value: ${alert_rate_limit_value}
  expiry-time-in-sec: ${alert_expiry_time_in_sec}

alert-store-es:
  url: ${es_urls}
  aws-iam-auth-required: ${es_aws_iam_auth_required}
  aws-region: ${es_aws_region}

smtp:
  host: localhost
  port: 25
  username: $${SMTP_USERNAME}
  password: $${SMTP_PASSWORD}



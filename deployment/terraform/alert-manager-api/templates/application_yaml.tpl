es:
  index.name: subscription
  create.index.if.not.found: false
  doctype: _doc
  urls: "${es_urls}"
  connection.timeout: 5000
  max.connection.idletime: 1000
  max.total.connection: 1000
  read.timeout: 1000

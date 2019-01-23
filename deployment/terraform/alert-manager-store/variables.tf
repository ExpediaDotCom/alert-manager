variable "image" {}
variable "image_pull_policy" {}
variable "replicas" {}
variable "namespace" {}
variable "kafka_endpoint" {}
variable "es_urls" {}
variable "graphite_hostname" {}
variable "graphite_port" {}
variable "graphite_enabled" {}

variable "enabled" {}

variable "aa_cname" {}
variable "kubectl_executable_name" {}
variable "kubectl_context_name" {}
variable "node_selector_label" {}
variable "memory_limit" {}
variable "memory_request" {}
variable "jvm_memory_limit" {}
variable "cpu_limit" {}
variable "cpu_request" {}
variable "env_vars" {}
variable "termination_grace_period" {
  default = 30
}

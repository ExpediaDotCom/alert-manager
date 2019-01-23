locals {
  app_name = "alert-manager-store"
  deployment_yaml_file_path = "${path.module}/templates/deployment_yaml.tpl"
  application_yaml_file_path = "${path.module}/templates/application_yaml.tpl"
  count = "${var.enabled?1:0}"
  checksum = "${sha1("${data.template_file.config_data.rendered}")}"
  configmap_name = "alert-manager-store-${local.checksum}"

}

//using kubectl to craete deployment construct since its not natively support by the kubernetes provider
data "template_file" "deployment_yaml" {
  template = "${file("${local.deployment_yaml_file_path}")}"

  vars {
    app_name = "${local.app_name}"
    aa_cname   = "${var.aa_cname}"
    namespace = "${var.namespace}"
    graphite_port = "${var.graphite_port}"
    graphite_host = "${var.graphite_hostname}"
    graphite_enabled = "${var.graphite_enabled}"
    node_selector_label = "${var.node_selector_label}"
    image = "${var.image}"
    image_pull_policy = "${var.image_pull_policy}"
    replicas = "${var.replicas}"
    memory_limit = "${var.memory_limit}"
    memory_request = "${var.memory_request}"
    jvm_memory_limit = "${var.jvm_memory_limit}"
    cpu_limit = "${var.cpu_limit}"
    cpu_request = "${var.cpu_request}"
    configmap_name = "${local.configmap_name}"
    env_vars = "${indent(9,"${var.env_vars}")}"
  }
}


data "template_file" "config_data" {
  template = "${file("${local.application_yaml_file_path}")}"

  vars {
    kafka_endpoint = "${var.kafka_endpoint}"
    es_urls = "${var.es_urls}"
  }
}

resource "kubernetes_config_map" "haystack-config" {
  metadata {
    name = "${local.configmap_name}"
    namespace = "${var.namespace}"
  }

  data {
    "application.yml" = "${data.template_file.config_data.rendered}"
  }

  count = "${local.count}"
}


resource "null_resource" "kubeconfig_dependency" {
  triggers {
    kubeconfig = "${var.kubectl_context_name}"
  }
}
resource "null_resource" "kubectl_apply" {
  depends_on = [
    "null_resource.kubeconfig_dependency"]
  triggers {
    template = "${data.template_file.deployment_yaml.rendered}"
  }

  provisioner "local-exec" {
    command = "echo '${data.template_file.deployment_yaml.rendered}' | ${var.kubectl_executable_name} apply -f - --kubeconfig ${var.kubectl_context_name}"
  }

  count = "${local.count}"
}

resource "null_resource" "kubectl_destroy" {
  depends_on = ["null_resource.kubeconfig_dependency"]
  provisioner "local-exec" {
    command = "echo '${data.template_file.deployment_yaml.rendered}' | ${var.kubectl_executable_name} delete -f - --kubeconfig ${var.kubectl_context_name}"
    when = "destroy"
  }

  count = "${local.count}"
}

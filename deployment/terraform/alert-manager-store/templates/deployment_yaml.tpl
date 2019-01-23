# ------------------- Deployment ------------------- #

kind: Deployment
apiVersion: apps/v1beta2
metadata:
  labels:
    k8s-app: ${app_name}
  name: ${app_name}
  namespace: ${namespace}
spec:
  replicas: ${replicas}
  revisionHistoryLimit: 10
  selector:
    matchLabels:
      k8s-app: ${app_name}
  template:
    metadata:
      labels:
        k8s-app: ${app_name}
    spec:
      containers:
      - name: ${app_name}
        image: ${image}
        imagePullPolicy: ${image_pull_policy}
        volumeMounts:
          # Create on-disk volume to store exec logs
        - mountPath: /config
          name: config-volume
        resources:
          limits:
            cpu: ${cpu_limit}
            memory: ${memory_limit}Mi
          requests:
            cpu: ${cpu_request}
            memory: ${memory_request}Mi
        env:
        - name: "HAYSTACK_GRAPHITE_HOST"
          value: "${graphite_host}"
        - name: "HAYSTACK_GRAPHITE_PORT"
          value: "${graphite_port}"
        - name: "HAYSTACK_GRAPHITE_ENABLED"
          value: "${graphite_enabled}"
        - name: "JAVA_XMS"
          value: "${jvm_memory_limit}m"
        - name: "JAVA_XMX"
          value: "${jvm_memory_limit}m"
        ${env_vars}
        livenessProbe:
         exec:
          command:
          - grep
          - "healthy"
          - /app/health_status
         initialDelaySeconds: 30
         periodSeconds: 10
         failureThreshold: 2
      nodeSelector:
        ${node_selector_label}
      volumes:
      - name: config-volume
        configMap:
          name: ${configmap_name}
# ------------------- Ingress ------------------- #
apiVersion: extensions/v1beta1
kind: Ingress
metadata:
  name: traefik-alert-manager
  namespace: ${namespace}
  annotations:
    kubernetes.io/ingress.class: traefik
    traefik.frontend.rule.type: PathPrefixStrip
spec:
  rules:
   - host: ${aa_cname}
     http:
        paths:
         - path: /alert-manager
           backend:
             serviceName: alert-manager-service
             servicePort: 80

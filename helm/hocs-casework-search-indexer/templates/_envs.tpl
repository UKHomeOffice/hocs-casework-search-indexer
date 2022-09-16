{{- define "deployment.envs" }}
- name: JAVA_OPTS
  value: '-XX:MaxRAMPercentage=70 -Djava.security.egd=file:/dev/./urandom -Djavax.net.ssl.trustStore=/etc/keystore/truststore.jks -Dhttps.proxyHost=hocs-outbound-proxy.{{ .Values.namespace }}.svc.cluster.local -Dhttps.proxyPort=31290 -Dhttp.nonProxyHosts=*.{{ .Values.namespace }}.svc.cluster.local'
- name: SPRING_PROFILES_ACTIVE
  value: 'aws'
- name: DB_HOST
  valueFrom:
    secretKeyRef:
      name: {{ .Values.namespace }}-casework-rds
      key: host
- name: DB_PORT
  valueFrom:
    secretKeyRef:
      name: {{ .Values.namespace }}-casework-rds
      key: port
- name: DB_NAME
  valueFrom:
    secretKeyRef:
      name: {{ .Values.namespace }}-casework-rds
      key: name
- name: DB_SCHEMA_NAME
  valueFrom:
    secretKeyRef:
      name: {{ .Values.namespace }}-casework-rds
      key: schema_name
- name: DB_USERNAME
  valueFrom:
    secretKeyRef:
      name: {{ .Values.namespace }}-casework-rds
      key: read_only_user_name
- name: DB_PASSWORD
  valueFrom:
    secretKeyRef:
      name: {{ .Values.namespace }}-casework-rds
      key: read_only_password
{{- end -}}

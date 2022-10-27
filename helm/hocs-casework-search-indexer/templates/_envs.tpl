{{- define "deployment.envs" }}
- name: JAVA_OPTS
  value: '-XX:MaxRAMPercentage=70 -Djava.security.egd=file:/dev/./urandom -Djavax.net.ssl.trustStore=/etc/keystore/truststore.jks -Dhttps.proxyHost=hocs-outbound-proxy.{{ .Values.namespace }}.svc.cluster.local -Dhttps.proxyPort=31290 -Dhttp.nonProxyHosts=*.{{ .Values.namespace }}.svc.cluster.local'
- name: SPRING_PROFILES_ACTIVE
  value: 'aws'
- name: MODE
  value: {{ .Values.app.mode }}
- name: NEW_INDEX
  value: {{ .Values.app.newIndex }}
- name: BATCH_SIZE
  value: {{ .Values.app.batchSize }}
- name: BATCH_INTERVAL
  value: {{ .Values.app.batchInterval }}
- name: ELASTICSEARCH_INDEX_PREFIX
  value: '{{ tpl .Values.app.elasticPrefix . }}'
- name: DB_HOST
  valueFrom:
    secretKeyRef:
      name: {{ .Release.Namespace }}-casework-rds
      key: host
- name: DB_PORT
  valueFrom:
    secretKeyRef:
      name: {{ .Release.Namespace }}-casework-rds
      key: port
- name: DB_NAME
  valueFrom:
    secretKeyRef:
      name: {{ .Release.Namespace }}-casework-rds
      key: name
- name: DB_SCHEMA_NAME
  valueFrom:
    secretKeyRef:
      name: {{ .Release.Namespace }}-casework-rds
      key: schema_name
- name: DB_USERNAME
  valueFrom:
    secretKeyRef:
      name: {{ .Release.Namespace }}-casework-rds
      key: read_only_user_name
- name: DB_PASSWORD
  valueFrom:
    secretKeyRef:
      name: {{ .Release.Namespace }}-casework-rds
      key: read_only_password
- name: ELASTICSEARCH_HOST
  valueFrom:
    secretKeyRef:
      name: {{ .Release.Namespace }}-opensearch
      key: endpoint
- name: ELASTICSEARCH_ACCESS_KEY
  valueFrom:
    secretKeyRef:
      name: {{ .Release.Namespace }}-opensearch
      key: access_key_id
- name: ELASTICSEARCH_SECRET_KEY
  valueFrom:
    secretKeyRef:
      name: {{ .Release.Namespace }}-opensearch
      key: secret_access_key
{{- end -}}

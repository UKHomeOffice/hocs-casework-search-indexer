{{- define "deployment.envs" }}
- name: JAVA_OPTS
  value: '-XX:MaxRAMPercentage=70 -Djava.security.egd=file:/dev/./urandom -Dhttps.proxyHost=hocs-outbound-proxy.{{ .Release.Namespace }}.svc.cluster.local -Dhttps.proxyPort=31290 -Dhttp.nonProxyHosts=*.{{ .Release.Namespace }}.svc.cluster.local'
- name: SPRING_PROFILES_ACTIVE
  value: 'aws'
- name: APP_BATCH_SIZE
  value: {{ .Values.app.batch.size }}
- name: APP_BATCH_INTERVAL
  value: {{ .Values.app.batch.interval }}
- name: APP_INDEX_BASELINE
  value: {{ .Values.app.index.baseline }}
- name: APP_INDEX_PREFIX
  value: {{ .Values.app.index.prefix }}
- name: APP_MODE
  value: {{ .Values.app.mode }}
- name: APP_DATACREATEDBEFORE
  value: {{ .Values.app.dataCreatedBefore }}
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

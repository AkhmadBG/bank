# Bank

Микросервисное банковское приложение на Java и Spring Boot.

## Технологии

- Java 21, Spring Boot
- Apache Kafka
- PostgreSQL
- Docker и Kubernetes
- Helm
- Prometheus, Grafana
- ELK Stack (Elasticsearch, Logstash, Kibana)
- Zipkin

## Возможности

- Управление банковскими счетами
- Снятие и внесение наличных
- Денежные переводы между счетами
- Асинхронные уведомления через Kafka
- Мониторинг метрик, логов и распределённых трассировок

## Запуск

Для развёртывания в Kubernetes с помощью Helm:

```bash
helm dependency build ./helm/bank
helm lint ./helm/bank
helm install bank ./helm/bank -n bank --create-namespace
```

Для проверки состояния приложения:

```bash
kubectl get pods -n bank
```

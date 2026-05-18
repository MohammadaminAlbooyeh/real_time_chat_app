FROM grafana/grafana:latest

COPY provisioning/datasources.yml /etc/grafana/provisioning/datasources/
COPY provisioning/dashboards.yml /etc/grafana/provisioning/dashboards/
COPY dashboards /var/lib/grafana/dashboards/

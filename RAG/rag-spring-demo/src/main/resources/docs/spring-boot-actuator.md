# Spring Boot Actuator

O Spring Boot Actuator adiciona recursos prontos para monitorar e gerenciar a aplicacao em producao. Ele expoe endpoints HTTP que revelam informacoes sobre a saude, as metricas e o estado interno da aplicacao.

O endpoint de health (/actuator/health) informa se a aplicacao esta saudavel, agregando o estado de varios componentes como banco de dados, espaco em disco e servicos externos. E muito usado por orquestradores e balanceadores de carga para verificar se uma instancia esta apta a receber trafego.

O endpoint de metrics (/actuator/metrics) expoe metricas como uso de memoria, numero de threads, tempo de resposta e contadores customizados. Quando integrado ao Micrometer, essas metricas podem ser exportadas para sistemas como Prometheus.

Por seguranca, a maioria dos endpoints do Actuator nao fica exposta por padrao. E preciso habilita-los explicitamente na configuracao, definindo quais endpoints podem ser acessados via web, geralmente com a propriedade management.endpoints.web.exposure.include.

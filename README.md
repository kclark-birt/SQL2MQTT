SQL2MQTT
========

A simple CLI utility that publishes an entire MySQL table to an MQTT Broker

USAGE
========
java -jar SQL2MQTT.jar -brokerHostname localhost -brokerPort 1883 -mysqlDatabase torque -mysqlHostname localhost -mysqlPassword PASSWORD -mysqlPort 3306 -mysqlTable raw_logs -mysqlUser torque -wait 3000

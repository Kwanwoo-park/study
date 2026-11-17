package spring.study.admin.entity;

public enum IntegrationEventLogRoute {
    CHAT(IntegrationEventLogBroker.KAFKA, "topic"),
    NOTIFICATION(IntegrationEventLogBroker.KAFKA, "topic2"),
    REALTIME_NOTIFICATION(IntegrationEventLogBroker.REDIS, "notification-events"),
    REALTIME_NOTIFICATION_READ(IntegrationEventLogBroker.REDIS, "notification-read-events"),
    UNKNOWN_KAFKA(IntegrationEventLogBroker.KAFKA, "unknown");

    public final IntegrationEventLogBroker broker;
    public final String destination;

    IntegrationEventLogRoute(IntegrationEventLogBroker broker, String destination) {
        this.broker = broker;
        this.destination = destination;
    }

    public static IntegrationEventLogRoute kafka(String topic) {
        if ("topic".equals(topic)) return CHAT;
        if ("topic2".equals(topic)) return NOTIFICATION;
        return UNKNOWN_KAFKA;
    }
}

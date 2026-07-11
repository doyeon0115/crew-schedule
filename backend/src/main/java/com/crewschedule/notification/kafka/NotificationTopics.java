package com.crewschedule.notification.kafka;

/** Kafka 토픽 상수. Kafka broker의 auto-create가 없을 경우 애플리케이션 기동 시 NewTopic으로도 생성. */
public final class NotificationTopics {

    public static final String NOTIFICATIONS = "crew-schedule.notifications";

    private NotificationTopics() {}
}

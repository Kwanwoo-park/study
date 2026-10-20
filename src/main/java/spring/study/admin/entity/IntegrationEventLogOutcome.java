package spring.study.admin.entity;

public enum IntegrationEventLogOutcome {
    QUEUED, SUCCESS, FAILED, RETRY_SCHEDULED, DEAD_LETTER, DLT_PUBLISHED,
    DUPLICATE, SKIPPED, NO_SUBSCRIBERS, DISCARDED
}

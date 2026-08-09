package spring.study.admin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "system_incident")
public class SystemIncident {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "request_method", nullable = false, length = 10)
    private String requestMethod;

    @Column(name = "request_path", nullable = false, length = 500)
    private String requestPath;

    @Column(name = "http_status", nullable = false)
    private int httpStatus;

    @Column(name = "exception_type", nullable = false, length = 255)
    private String exceptionType;

    @Column(name = "error_message", nullable = false, length = 1000)
    private String errorMessage;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean acknowledged;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Builder
    public SystemIncident(LocalDateTime occurredAt,
                          String requestMethod,
                          String requestPath,
                          int httpStatus,
                          String exceptionType,
                          String errorMessage) {
        this.occurredAt = occurredAt;
        this.requestMethod = requestMethod;
        this.requestPath = requestPath;
        this.httpStatus = httpStatus;
        this.exceptionType = exceptionType;
        this.errorMessage = errorMessage;
    }

    public void acknowledge(LocalDateTime time) {
        acknowledged = true;
        acknowledgedAt = time;
    }
}

package co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "request_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString
public class RequestTypeJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 100, nullable = false)
    private String name;

    @Column(length = 500)
    private String description;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;
}

package com.teachnet.institution;

import com.teachnet.profile.Board;
import com.teachnet.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "institutions")
@Getter
@Setter
@NoArgsConstructor
public class Institution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false, unique = true)
    private User owner;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "institution_type", nullable = false)
    private InstitutionType institutionType;

    @Enumerated(EnumType.STRING)
    private Board board;

    private String city;

    private String state;

    @Column(columnDefinition = "TEXT")
    private String about;

    private String website;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(nullable = false)
    private boolean verified;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Institution(User owner, String name, InstitutionType institutionType) {
        this.owner = owner;
        this.name = name;
        this.institutionType = institutionType;
    }
}

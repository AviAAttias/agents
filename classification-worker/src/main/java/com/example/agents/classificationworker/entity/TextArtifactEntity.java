package com.example.agents.classificationworker.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "text_artifact")
@Getter
@Setter
public class TextArtifactEntity {

    @Id
    private Long id;

    @Column(name = "text_body", nullable = false, columnDefinition = "TEXT")
    private String textBody;
}

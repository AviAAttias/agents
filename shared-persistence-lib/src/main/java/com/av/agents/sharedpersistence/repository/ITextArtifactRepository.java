package com.av.agents.sharedpersistence.repository;

import com.av.agents.sharedpersistence.entity.TextArtifactEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ITextArtifactRepository extends JpaRepository<TextArtifactEntity, Long> {}

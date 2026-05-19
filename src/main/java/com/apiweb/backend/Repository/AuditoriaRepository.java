package com.apiweb.backend.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.apiweb.backend.Model.AuditoriaModel;

public interface AuditoriaRepository extends JpaRepository<AuditoriaModel, Long> {
}

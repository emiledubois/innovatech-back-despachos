package com.citt.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.citt.persistence.entity.Despacho;

public interface DespachoRepository extends JpaRepository<Despacho, Long> {
}

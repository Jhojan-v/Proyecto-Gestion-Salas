package com.apiweb.backend.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.apiweb.backend.Model.ProductoModel;

public interface IProductoRepository extends JpaRepository<ProductoModel, Integer> {
}

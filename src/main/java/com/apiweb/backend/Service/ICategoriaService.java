package com.apiweb.backend.Service;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.apiweb.backend.DTO.CategoriaDTO;

public interface ICategoriaService {
    CategoriaDTO guardar(CategoriaDTO categoria);
    Page<CategoriaDTO> listarCategorias(Pageable pageable);
    // otras operaciones
}

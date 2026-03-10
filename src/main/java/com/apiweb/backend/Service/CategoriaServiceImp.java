package com.apiweb.backend.Service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.apiweb.backend.DTO.CategoriaDTO;
import com.apiweb.backend.Model.CategoriaModel;
import com.apiweb.backend.Repository.ICategoriaRepository;

@Service
public class CategoriaServiceImp implements ICategoriaService {
    @Autowired
    ICategoriaRepository categoriaRepository;

    @Override
    public CategoriaDTO guardar(CategoriaDTO categoria) {
        CategoriaModel entidad = toEntity(categoria);
        CategoriaModel guardada = categoriaRepository.save(entidad);
        return toDTO(guardada);
    }

    @Override
    public Page<CategoriaDTO> listarCategorias(Pageable pageable) {
        return categoriaRepository.findAll(pageable)
            .map(this::toDTO); // Convertimos cada elemento del Page a DTO

    }

    private CategoriaModel toEntity(CategoriaDTO dto) {
        if (dto == null) {
            return null;
        }
        CategoriaModel model = new CategoriaModel();
        model.setIdCategoria(dto.getIdCategoria());
        model.setNombre(dto.getNombre());
        return model;
    }

    private CategoriaDTO toDTO(CategoriaModel model) {
        if (model == null) {
            return null;
        }
        CategoriaDTO dto = new CategoriaDTO();
        dto.setIdCategoria(model.getIdCategoria());
        dto.setNombre(model.getNombre());
        return dto;
    }
}

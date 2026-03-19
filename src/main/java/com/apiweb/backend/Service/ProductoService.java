package com.apiweb.backend.Service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.apiweb.backend.DTO.ProductoDTO;
import com.apiweb.backend.Exception.RecursoNoEncontradoException;
import com.apiweb.backend.Model.CategoriaModel;
import com.apiweb.backend.Model.ProductoModel;
import com.apiweb.backend.Repository.ICategoriaRepository;
import com.apiweb.backend.Repository.IProductoRepository;

@Service
public class ProductoService implements IProductoService {
   @Autowired
   IProductoRepository productoRepository;

   @Autowired
   ICategoriaRepository categoriaRepository;

   @Override
   public ProductoDTO crearProducto(ProductoDTO producto) {
    // 1. Verificar existencia de la categoría antes de intentar el mapeo
        CategoriaModel categoria = categoriaRepository.findById(producto.getIdCategoria())
            .orElseThrow(() -> new RecursoNoEncontradoException("Categoría no encontrada"));

    // 2. Mapear y asignar
        ProductoModel productoEntity = toEntity(producto);
        productoEntity.setCategoriaId(categoria);
    
    // 3. Guardar y retornar
    return toDTO(productoRepository.save(productoEntity));
    }

   @Override
   public Page<ProductoDTO> listarProductos(Pageable pageable) {
        return productoRepository.findAll(pageable)
                .map(this::toDTO);// Convertimos cada elemento del Page a DTO
   }

   private ProductoModel toEntity(ProductoDTO dto) {
        if (dto == null) {
            return null;
        }
        ProductoModel model = new ProductoModel();
        model.setIdProducto(dto.getIdProducto());
        model.setNombre(dto.getNombre());

        if (dto.getIdCategoria() != null) {
            CategoriaModel categoria = categoriaRepository.findById(dto.getIdCategoria()).orElse(null);
            model.setCategoriaId(categoria);
        } else {
            model.setCategoriaId(null);
        }
        return model;
   }

   private ProductoDTO toDTO(ProductoModel model) {
        if (model == null) {
            return null;
        }
        ProductoDTO dto = new ProductoDTO();
        dto.setIdProducto(model.getIdProducto());
        dto.setNombre(model.getNombre());
        if (model.getCategoriaId() != null) {
            dto.setIdCategoria(model.getCategoriaId().getIdCategoria());
        }
        return dto;
   }
}

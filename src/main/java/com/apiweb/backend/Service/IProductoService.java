package com.apiweb.backend.Service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.apiweb.backend.DTO.ProductoDTO;

public interface IProductoService {
    ProductoDTO crearProducto(ProductoDTO producto);
    Page<ProductoDTO> listarProductos(Pageable pageable);
}

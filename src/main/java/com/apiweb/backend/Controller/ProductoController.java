package com.apiweb.backend.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.apiweb.backend.DTO.ProductoDTO;
import com.apiweb.backend.Service.IProductoService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/UAO/tienda/productos")

public class ProductoController {
    @Autowired IProductoService productoService;
    @PostMapping("/insertar")
    public ResponseEntity<ProductoDTO> crearProducto(@Valid @RequestBody ProductoDTO producto){
        return new ResponseEntity<>(productoService.crearProducto(producto), HttpStatus.CREATED);
    }
    @GetMapping("/listar")
    public ResponseEntity<Page<ProductoDTO>> listarProductos(Pageable pageable){
        return new ResponseEntity<>(productoService.listarProductos(pageable), HttpStatus.OK);
    }
    
}
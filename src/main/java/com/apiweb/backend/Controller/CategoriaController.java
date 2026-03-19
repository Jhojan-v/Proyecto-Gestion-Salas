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

import com.apiweb.backend.DTO.CategoriaDTO;
import com.apiweb.backend.Service.ICategoriaService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/UAO/tienda/categorias")

public class CategoriaController {
    @Autowired ICategoriaService categoriaService;
    @PostMapping("/insertar")
    public ResponseEntity<CategoriaDTO> crearCategoria(@Valid @RequestBody CategoriaDTO categoria){
        return new ResponseEntity<>(categoriaService.guardar(categoria), HttpStatus.CREATED);
    }
    
    @GetMapping("/listar")
    public ResponseEntity<Page<CategoriaDTO>>listarCategorias(Pageable pageable){
        return new ResponseEntity<>(categoriaService.listarCategorias(pageable), HttpStatus.OK);
    }
    
}

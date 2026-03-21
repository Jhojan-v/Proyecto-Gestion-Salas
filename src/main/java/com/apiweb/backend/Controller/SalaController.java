package com.apiweb.backend.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.apiweb.backend.Model.SalaModel;
import com.apiweb.backend.Service.SalaService;

@RestController
@RequestMapping("/salas")
public class SalaController {

    @Autowired
    private SalaService service;

    @PostMapping
    public SalaModel crearSala(@RequestBody SalaModel sala) {
        return service.crearSala(sala);
    }
}

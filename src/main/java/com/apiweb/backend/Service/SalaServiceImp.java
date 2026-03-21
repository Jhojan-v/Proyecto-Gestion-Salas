package com.apiweb.backend.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.apiweb.backend.Model.SalaModel;
import com.apiweb.backend.Repository.SalaRepository;

@Service
public class SalaServiceImp implements SalaService {

    @Autowired
    private SalaRepository salaRepository;

    @Override
    public SalaModel crearSala(SalaModel sala) {

        if (sala.getCapacidad() < 2 || sala.getCapacidad() > 100) {
            throw new RuntimeException("La capacidad debe estar entre 2 y 100");
        }

        if (salaRepository.existsByNombre(sala.getNombre())) {
            throw new RuntimeException("Ya existe una sala con ese nombre");
        }

        return salaRepository.save(sala);
    }
}

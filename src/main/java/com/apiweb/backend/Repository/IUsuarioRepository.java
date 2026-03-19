
package com.apiweb.backend.Repository;

//interfaz que ya trae metodos para guardar, eleminar y buscar
import org.springframework.data.jpa.repository.JpaRepository;

import com.apiweb.backend.Model.UsuarioModel;

public interface IUsuarioRepository extends JpaRepository<UsuarioModel,Integer>{
    //metodo para buscar el usuario por el correo
    UsuarioModel findByEmail(String emailString);
    
}

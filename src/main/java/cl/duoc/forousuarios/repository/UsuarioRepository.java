package cl.duoc.forousuarios.repository;

import cl.duoc.forousuarios.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import cl.duoc.forousuarios.model.Rol;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    boolean existsByCorreo(String correo);
    Optional<Usuario> findByCorreo(String correo);
    List<Usuario> findByActivoTrueOrderByIdAsc();
    long countByRolAndActivoTrue(Rol rol);
    boolean existsByIdAndRolAndActivoTrue(Long id, Rol rol);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.activo = true and u.rol = :rol order by u.id")
    List<Usuario> bloquearActivosPorRol(@Param("rol") Rol rol);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.id = :id")
    Optional<Usuario> bloquearPorId(@Param("id") Long id);
}

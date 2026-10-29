package cl.duoc.forousuarios.model;

import jakarta.persistence.*;
import org.hibernate.type.NumericBooleanConverter;

@Entity
@Table(name = "USUARIOS", uniqueConstraints = @UniqueConstraint(name = "UK_USUARIOS_CORREO", columnNames = "CORREO"))
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", precision = 19, scale = 0)
    private Long id;

    @Column(name = "NOMBRE", nullable = false, length = 100)
    private String nombre;

    @Column(name = "CORREO", nullable = false, length = 254)
    private String correo;

    @Column(name = "PASSWORD_HASH", nullable = false, length = 60)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "ROL", nullable = false, length = 20)
    private Rol rol;

    @Convert(converter = NumericBooleanConverter.class)
    @Column(name = "ACTIVO", nullable = false, precision = 1, scale = 0)
    private Boolean activo;

    protected Usuario() {
        // Requerido por JPA.
    }

    public Usuario(String nombre, String correo, String passwordHash) {
        this.nombre = nombre;
        this.correo = correo;
        this.passwordHash = passwordHash;
        this.rol = Rol.USUARIO;
        this.activo = true;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getCorreo() { return correo; }
    public String getPasswordHash() { return passwordHash; }
    public Rol getRol() { return rol; }
    public Boolean getActivo() { return activo; }

    public void actualizar(String nombre, String correo, String nuevoHash) {
        this.nombre = nombre;
        this.correo = correo;
        if (nuevoHash != null) this.passwordHash = nuevoHash;
    }

    public void desactivar() { this.activo = false; }

    public void cambiarRol(Rol rol) { this.rol = rol; }
}

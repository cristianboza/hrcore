package com.example.hrcore.entity;

import com.example.hrcore.entity.enums.UserRole;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_email", columnList = "email"),
    @Index(name = "idx_users_manager_id", columnList = "manager_id"),
    @Index(name = "idx_users_role", columnList = "role"),
    @Index(name = "idx_users_department", columnList = "department")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(exclude = {"manager", "directReports"})
@EqualsAndHashCode(of = "id", callSuper = false)
public class User extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @NotBlank(message = "First name is required")
    @Column(nullable = false, length = 100)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Column(nullable = false, length = 100)
    private String lastName;

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String department;

    @NotNull(message = "Role is required")
    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private UserRole role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id", foreignKey = @ForeignKey(name = "fk_manager"))
    private User manager;

    @OneToMany(mappedBy = "manager", fetch = FetchType.LAZY)
    @Builder.Default
    private List<User> directReports = new ArrayList<>();

    public void setManager(User manager) {
        if (this.manager != null) {
            this.manager.getDirectReports().remove(this);
        }
        this.manager = manager;
        if (manager != null && !manager.getDirectReports().contains(this)) {
            manager.getDirectReports().add(this);
        }
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public boolean isManager() {
        return role.isManager();
    }

    public boolean canManage(User employee) {
        return role.canManageRole(employee.getRole()) || 
               (role.isManager() && employee.getManager() != null && employee.getManager().getId().equals(this.id));
    }
}

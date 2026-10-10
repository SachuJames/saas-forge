package com.saasforge.user;

import com.saasforge.audit.Audited;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * User management inside the current tenant. Everything here runs against
 * the tenant's schema thanks to the TenantFilter + tenant-aware DataSource.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final AppUserRepository users;

    public UserController(AppUserRepository users) {
        this.users = users;
    }

    public record CreateUserRequest(
            @NotBlank @Email String email,
            @NotBlank String name,
            String role) {
    }

    @GetMapping
    public List<AppUser> list() {
        return users.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(action = "USER_CREATED", entity = "app_user")
    public AppUser create(@Valid @RequestBody CreateUserRequest req) {
        AppUser user = new AppUser();
        user.setEmail(req.email());
        user.setName(req.name());
        user.setRole(req.role() == null ? "USER" : req.role());
        return users.save(user);
    }

    @GetMapping("/{id}")
    public AppUser get(@PathVariable UUID id) {
        return users.findById(id).orElseThrow(() -> new IllegalArgumentException("unknown user"));
    }
}

package com.turnero.web;

import com.turnero.domain.AppUser;
import com.turnero.domain.Tenant;
import com.turnero.repo.AppUserRepo;
import com.turnero.repo.TenantRepo;
import com.turnero.security.AuthPrincipal;
import com.turnero.security.JwtService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final TenantRepo tenants;
  private final AppUserRepo users;
  private final PasswordEncoder encoder;
  private final JwtService jwt;

  public AuthController(TenantRepo tenants, AppUserRepo users, PasswordEncoder encoder, JwtService jwt) {
    this.tenants = tenants; this.users = users; this.encoder = encoder; this.jwt = jwt;
  }

  public record RegisterReq(@NotBlank String tenantName, @NotBlank String name,
                            @Email @NotBlank String email, @NotBlank @Size(min = 8) String password) {}
  public record LoginReq(@Email @NotBlank String email, @NotBlank String password) {}

  @PostMapping("/register")
  public Map<String, Object> register(@RequestBody RegisterReq req) {
    if (users.findByEmail(req.email().toLowerCase()).isPresent())
      throw new IllegalArgumentException("Ya existe una cuenta con ese email");
    Tenant t = new Tenant();
    t.setName(req.tenantName());
    t.setPlan("FREE");
    t.setFreeExpiresAt(LocalDate.now().plusMonths(3));
    tenants.save(t);
    AppUser u = new AppUser();
    u.setTenantId(t.getId());
    u.setEmail(req.email().toLowerCase());
    u.setPasswordHash(encoder.encode(req.password()));
    u.setName(req.name());
    u.setRole("ADMIN");
    users.save(u);
    return session(u, t);
  }

  @PostMapping("/login")
  public Map<String, Object> login(@RequestBody LoginReq req) {
    AppUser u = users.findByEmail(req.email().toLowerCase())
        .orElseThrow(() -> new IllegalArgumentException("Credenciales inválidas"));
    if (!encoder.matches(req.password(), u.getPasswordHash()))
      throw new IllegalArgumentException("Credenciales inválidas");
    Tenant t = tenants.findById(u.getTenantId()).orElseThrow();
    return session(u, t);
  }

  @GetMapping("/me")
  public Map<String, Object> me(@AuthenticationPrincipal AuthPrincipal p) {
    AppUser u = users.findById(p.userId()).orElseThrow();
    Tenant t = tenants.findById(p.tenantId()).orElseThrow();
    return Map.of("user", Map.of("id", u.getId(), "name", u.getName(), "email", u.getEmail(), "role", u.getRole()),
        "tenant", tenantDto(t));
  }

  private Map<String, Object> session(AppUser u, Tenant t) {
    return Map.of(
        "token", jwt.issue(u.getId(), t.getId(), u.getRole()),
        "user", Map.of("id", u.getId(), "name", u.getName(), "email", u.getEmail(), "role", u.getRole()),
        "tenant", tenantDto(t));
  }

  private Map<String, Object> tenantDto(Tenant t) {
    return Map.of("id", t.getId(), "name", t.getName(), "plan", t.getPlan(),
        "freeExpiresAt", t.getFreeExpiresAt() != null ? t.getFreeExpiresAt().toString() : "");
  }
}

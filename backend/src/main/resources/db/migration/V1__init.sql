CREATE TABLE tenant (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(120) NOT NULL,
  plan VARCHAR(20) NOT NULL DEFAULT 'FREE',
  free_expires_at DATE NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE app_user (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  email VARCHAR(160) NOT NULL UNIQUE,
  password_hash VARCHAR(100) NOT NULL,
  name VARCHAR(120) NOT NULL,
  role VARCHAR(20) NOT NULL DEFAULT 'ADMIN',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_user_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id),
  INDEX ix_user_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE branch (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  name VARCHAR(120) NOT NULL,
  display_key VARCHAR(64) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_branch_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id),
  INDEX ix_branch_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE service_def (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  branch_id BIGINT NOT NULL,
  name VARCHAR(120) NOT NULL,
  prefix VARCHAR(5) NOT NULL,
  priority_allowed TINYINT(1) NOT NULL DEFAULT 0,
  active TINYINT(1) NOT NULL DEFAULT 1,
  CONSTRAINT fk_service_branch FOREIGN KEY (branch_id) REFERENCES branch(id),
  INDEX ix_service_tb (tenant_id, branch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE service_point (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  branch_id BIGINT NOT NULL,
  name VARCHAR(120) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
  CONSTRAINT fk_point_branch FOREIGN KEY (branch_id) REFERENCES branch(id),
  INDEX ix_point_tb (tenant_id, branch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE point_service (
  point_id BIGINT NOT NULL,
  service_id BIGINT NOT NULL,
  PRIMARY KEY (point_id, service_id),
  CONSTRAINT fk_ps_point FOREIGN KEY (point_id) REFERENCES service_point(id) ON DELETE CASCADE,
  CONSTRAINT fk_ps_service FOREIGN KEY (service_id) REFERENCES service_def(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE ticket (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  branch_id BIGINT NOT NULL,
  service_id BIGINT NOT NULL,
  point_id BIGINT NULL,
  code VARCHAR(20) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'WAITING',
  priority TINYINT NOT NULL DEFAULT 0,
  public_token VARCHAR(64) NOT NULL UNIQUE,
  visitor_name VARCHAR(120) NULL,
  call_count INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  called_at TIMESTAMP NULL,
  started_at TIMESTAMP NULL,
  finished_at TIMESTAMP NULL,
  CONSTRAINT fk_ticket_branch FOREIGN KEY (branch_id) REFERENCES branch(id),
  INDEX ix_ticket_queue (tenant_id, branch_id, status, priority, id),
  INDEX ix_ticket_day (tenant_id, branch_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE ticket_event (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  ticket_id BIGINT NOT NULL,
  type VARCHAR(30) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_te_ticket FOREIGN KEY (ticket_id) REFERENCES ticket(id),
  INDEX ix_te_ticket (ticket_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE daily_counter (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  branch_id BIGINT NOT NULL,
  service_id BIGINT NOT NULL,
  day DATE NOT NULL,
  value INT NOT NULL,
  UNIQUE KEY uq_counter (tenant_id, branch_id, service_id, day)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

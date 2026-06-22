package app.lexo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * Rate limiting persistido (janela deslizante) sem dependencia paga.
 * Cada linha e um "hit" associado a uma chave; janelas expiradas sao limpas a cada checagem.
 */
@Entity
@Table(name = "rate_hits", indexes = @Index(name = "idx_ratehit_key_created", columnList = "rate_key,createdAt"))
public class RateHit extends BaseEntity {

    @Column(name = "rate_key", nullable = false)
    private String key;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}

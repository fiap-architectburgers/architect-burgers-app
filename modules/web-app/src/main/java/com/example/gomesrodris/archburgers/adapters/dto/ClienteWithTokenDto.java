package com.example.gomesrodris.archburgers.adapters.dto;

import com.example.gomesrodris.archburgers.domain.entities.Cliente;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.Nullable;

/**
 * Representação simplificada "crua" da entidade Cliente para tráfego nos serviços
 */
public record ClienteWithTokenDto(
        Integer id,
        String nome,
        String cpf,
        String email,
        @JsonProperty("IdToken")
        String token
) {

    public static ClienteWithTokenDto fromEntity(Cliente cliente, String token) {
        return new ClienteWithTokenDto(
                cliente.id() != null ? cliente.id().id() : null,
                cliente.nome(),
                cliente.cpf().cpfNum(),
                cliente.email(),
                token
        );
    }
}

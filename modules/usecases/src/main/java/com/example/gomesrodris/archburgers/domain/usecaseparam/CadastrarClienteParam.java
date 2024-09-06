package com.example.gomesrodris.archburgers.domain.usecaseparam;

import com.example.gomesrodris.archburgers.domain.utils.StringUtils;
import com.example.gomesrodris.archburgers.domain.valueobjects.Cpf;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;

public final class CadastrarClienteParam {
    private final @Nullable String nome;
    private final @Nullable String cpf;
    private final @Nullable String email;
    private final @Nullable String senha;

    private Cpf cpfValidado;

    public CadastrarClienteParam(
            @Nullable String nome,
            @Nullable String cpf,
            @Nullable String email,
            @Nullable String senha) {
        this.nome = nome;
        this.cpf = cpf;
        this.email = email;
        this.senha = senha;
    }

    public static CadastrarClienteParam fromMap(Map<String, String> map) {
        return new CadastrarClienteParam(
                map.get("nome"),
                map.get("cpf"),
                map.get("email"),
                map.get("senha")
        );
    }

    public void validar() {
        if (StringUtils.isEmpty(nome)) {
            throw new IllegalArgumentException("nome não informado");
        }

        cpfValidado = new Cpf(cpf);

        if (StringUtils.isEmpty(email)) {
            throw new IllegalArgumentException("email não informado");
        }

        if (StringUtils.isEmpty(senha)) {
            throw new IllegalArgumentException("senha não informada");
        }
    }

    public Cpf getCpfValidado() {
        if (cpfValidado == null) {
            throw new IllegalStateException("Obrigatório passar pela validação de parâmetros");
        }
        return cpfValidado;
    }

    public @Nullable String nome() {
        return nome;
    }

    public @Nullable String cpf() {
        return cpf;
    }

    public @Nullable String email() {
        return email;
    }

    public @Nullable String senha() {
        return senha;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (CadastrarClienteParam) obj;
        return Objects.equals(this.nome, that.nome) &&
                Objects.equals(this.cpf, that.cpf) &&
                Objects.equals(this.email, that.email) &&
                Objects.equals(this.senha, that.senha);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nome, cpf, email, senha);
    }

    @Override
    public String toString() {
        return "CadastrarClienteParam[" +
                "nome=" + nome + ", " +
                "cpf=" + cpf + ", " +
                "email=" + email + ", " +
                "senha=" + senha + ']';
    }

}

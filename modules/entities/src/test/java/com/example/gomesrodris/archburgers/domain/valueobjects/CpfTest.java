package com.example.gomesrodris.archburgers.domain.valueobjects;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class CpfTest {

    @Test
    void validate() {
        // Brancos, nulos, tamanhos incorretos, digitos invalidos...
        assertThrows(IllegalArgumentException.class, () -> new Cpf(null));
        assertThrows(IllegalArgumentException.class, () -> new Cpf(""));
        assertThrows(IllegalArgumentException.class, () -> new Cpf("1233211234"));
        assertThrows(IllegalArgumentException.class, () -> new Cpf("123321123401"));
        assertThrows(IllegalArgumentException.class, () -> new Cpf("A1233211234"));
        assertThrows(IllegalArgumentException.class, () -> new Cpf("1233211234z"));

        // DV invalido
        assertThrows(IllegalArgumentException.class, () -> new Cpf("12332112339"));
        assertThrows(IllegalArgumentException.class, () -> new Cpf("12332112341"));
        assertThrows(IllegalArgumentException.class, () -> new Cpf("12332112330"));
        assertThrows(IllegalArgumentException.class, () -> new Cpf("12332112350"));
        assertThrows(IllegalArgumentException.class, () -> new Cpf("99988877713"));
        assertThrows(IllegalArgumentException.class, () -> new Cpf("99988877715"));
        assertThrows(IllegalArgumentException.class, () -> new Cpf("99988877704"));
        assertThrows(IllegalArgumentException.class, () -> new Cpf("99988877794"));

        // Valido
        assertThat(new Cpf("12332112340").cpfNum()).isEqualTo("12332112340");
        assertThat(new Cpf("99988877714").cpfNum()).isEqualTo("99988877714");
    }
}
package com.comandago.api.shared.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

class StrongPasswordValidatorTest {

    private StrongPasswordValidator validator;

    @BeforeEach
    void setUp() {
        validator = new StrongPasswordValidator();
    }

    @Test
    void passwordValida_retornaTrue() {
        assertThat(validator.isValid("Password1", Mockito.mock(ConstraintValidatorContext.class))).isTrue();
    }

    @Test
    void passwordSinMayuscula_retornaFalse() {
        assertThat(validator.isValid("password1", Mockito.mock(ConstraintValidatorContext.class))).isFalse();
    }

    @Test
    void passwordCorta_retornaFalse() {
        assertThat(validator.isValid("Pass1", Mockito.mock(ConstraintValidatorContext.class))).isFalse();
    }
}

package org.example.service;

import java.security.SecureRandom;

/**
 * Responsável só por UMA coisa: gerar senhas aleatórias.
 * Não sabe nada sobre a interface gráfica nem sobre onde a senha vai ser
 * salva — só recebe as regras (tamanho, quais tipos de caractere) e devolve
 * uma senha pronta. Isso facilita testar e reaproveitar essa lógica.
 */
public class PasswordGenerator {

    // "Pools" (conjuntos) de caracteres possíveis para cada categoria.
    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String NUMBERS = "0123456789";
    private static final String SYMBOLS = "!@#$%^&*()-_=+[]{}";

    // SecureRandom (em vez do Random comum) gera números aleatórios mais
    // difíceis de prever — importante quando o resultado é usado como senha.
    private final SecureRandom random = new SecureRandom();

    /**
     * Gera uma senha com o tamanho pedido, sorteando caracteres apenas dos
     * grupos marcados como "true". Lança IllegalArgumentException se nenhum
     * grupo foi selecionado (não tem como gerar senha sem nenhum caractere
     * disponível) ou se o tamanho pedido for inválido.
     */
    public String generate(int length, boolean useUpper, boolean useLower, boolean useNumbers, boolean useSymbols) {
        // Monta o "pool" final juntando só os grupos que o usuário marcou.
        StringBuilder pool = new StringBuilder();
        if (useUpper) pool.append(UPPER);
        if (useLower) pool.append(LOWER);
        if (useNumbers) pool.append(NUMBERS);
        if (useSymbols) pool.append(SYMBOLS);

        if (pool.isEmpty()) {
            throw new IllegalArgumentException("Selecione ao menos um tipo de caractere.");
        }
        if (length <= 0) {
            throw new IllegalArgumentException("O tamanho da senha deve ser maior que zero.");
        }

        // Sorteia, um por um, um caractere aleatório do pool, "length" vezes.
        StringBuilder password = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(pool.length());
            password.append(pool.charAt(index));
        }
        return password.toString();
    }
}

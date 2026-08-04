package org.example.service;

/**
 * Calcula, de forma simples (heurística, não é ciência exata), quão "forte"
 * uma senha parece ser, com base em dois fatores: o tamanho dela e a
 * variedade de tipos de caractere usados (maiúscula, minúscula, número,
 * símbolo). Quanto mais desses fatores a senha tiver, maior a pontuação.
 */
public class PasswordStrengthEvaluator {

    /**
     * "record" é um jeito curto do Java de criar uma classe só de dados
     * (sem lógica), com construtor, getters (label(), color(), fraction())
     * e equals/hashCode/toString gerados automaticamente.
     *
     * label:    texto mostrado ("Fraca", "Forte"...)
     * color:    cor (em hexadecimal) usada na barra e no texto
     * fraction: de 0.0 a 1.0, o quanto a barra de progresso deve preencher
     */
    public record Result(String label, String color, double fraction) {
    }

    public Result evaluate(String password) {
        if (password == null || password.isEmpty()) {
            // Campo vazio: barra zerada, sem texto, cor neutra (cinza-azulado).
            return new Result("", "#24304a", 0);
        }

        // Conta quantos TIPOS diferentes de caractere aparecem na senha
        // (no máximo 4: maiúscula, minúscula, número, símbolo).
        int variety = 0;
        if (password.chars().anyMatch(Character::isUpperCase)) variety++;
        if (password.chars().anyMatch(Character::isLowerCase)) variety++;
        if (password.chars().anyMatch(Character::isDigit)) variety++;
        if (password.chars().anyMatch(c -> !Character.isLetterOrDigit(c))) variety++;

        int length = password.length();

        // Pontuação: soma pontos por tamanho e por variedade de caracteres.
        int score = 0;
        if (length >= 6) score++;
        if (length >= 10) score++;
        if (length >= 14) score++;
        // "variety - 1" porque ter só 1 tipo de caractere não conta ponto
        // extra (é o mínimo esperado); a partir do 2º tipo, cada um soma 1.
        score += Math.max(0, variety - 1);
        score = Math.min(score, 5); // trava a pontuação em no máximo 5

        // switch de expressão (Java moderno): cada faixa de pontuação vira
        // um resultado diferente, já combinando texto + cor + preenchimento.
        return switch (score) {
            case 0, 1 -> new Result("Muito fraca", "#ff4d4f", 0.2);
            case 2 -> new Result("Fraca", "#ff8c42", 0.4);
            case 3 -> new Result("Média", "#ffd93d", 0.6);
            case 4 -> new Result("Forte", "#4ade80", 0.8);
            default -> new Result("Muito forte", "#22c55e", 1.0);
        };
    }
}

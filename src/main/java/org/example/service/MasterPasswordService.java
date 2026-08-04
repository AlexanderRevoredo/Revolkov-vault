package org.example.service;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

/**
 * Cuida do cadastro e da verificação da "senha mestre" — a senha que o
 * usuário usa pra revelar as credenciais salvas dentro do app.
 *
 * IMPORTANTE (limite de segurança deste projeto): essa senha mestre só
 * protege a TELA (o que aparece no app). O arquivo passwords.txt continua
 * sendo salvo em texto puro no disco — quem tiver acesso direto a esse
 * arquivo consegue ler as senhas sem passar por aqui. Criptografar o
 * arquivo em si seria um passo extra, não implementado ainda.
 *
 * Nunca guardamos a senha mestre em texto puro, nem mesmo criptografada de
 * um jeito reversível. Guardamos um HASH dela (ver método hash()), que é uma
 * função matemática de "mão única": dá pra transformar a senha num hash,
 * mas não dá pra voltar do hash pra senha original. Pra verificar login,
 * a gente recalcula o hash da senha digitada e compara com o hash salvo.
 */
public class MasterPasswordService {

    // Quantas vezes o algoritmo de hash é repetido internamente. Um número
    // alto (65536) é proposital: torna o cálculo mais lento (uma fração de
    // segundo pra nós, mas MUITO mais lento pra quem tentar "adivinhar"
    // senhas testando milhões de combinações por segundo).
    private static final int ITERATIONS = 65536;
    // Tamanho, em bits, do hash gerado.
    private static final int KEY_LENGTH = 256;

    private final Path file;
    private final SecureRandom random = new SecureRandom();

    public MasterPasswordService() {
        Path dir = Path.of(System.getProperty("user.home"), "password-manager");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        this.file = dir.resolve("master.key");
    }

    /** true se já existe uma senha mestre cadastrada nessa máquina. */
    public boolean isRegistered() {
        return Files.exists(file) && file.toFile().length() > 0;
    }

    /**
     * Cadastra (ou substitui) a senha mestre. Gera um "salt" (um pedacinho
     * aleatório de bytes) novo a cada cadastro — isso garante que, mesmo se
     * duas pessoas usarem a mesma senha mestre, os hashes salvos são
     * diferentes, dificultando ataques que usam tabelas de hashes prontos.
     */
    public void register(String password) {
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        byte[] hash = hash(password, salt);
        // Salva o salt e o hash juntos, separados por ":", em Base64 (formato
        // de texto seguro pra representar bytes "crus").
        String line = Base64.getEncoder().encodeToString(salt) + ":" + Base64.getEncoder().encodeToString(hash);
        try {
            Files.writeString(file, line);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Verifica se a senha digitada é a correta: recalcula o hash usando o
     * MESMO salt que foi salvo no cadastro, e compara os dois hashes.
     */
    public boolean verify(String password) {
        try {
            String content = Files.readString(file).trim();
            String[] parts = content.split(":", 2);
            byte[] salt = Base64.getDecoder().decode(parts[0]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[1]);
            byte[] actualHash = hash(password, salt);
            // MessageDigest.isEqual (em vez de Arrays.equals) compara os bytes
            // sempre no mesmo tempo, não importa onde eles diferem. Isso evita
            // um tipo de ataque ("timing attack") que tenta adivinhar a senha
            // medindo quanto tempo a comparação demora.
            return MessageDigest.isEqual(expectedHash, actualHash);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * PBKDF2 é um algoritmo pensado especificamente para hash de senhas
     * (ao contrário de algo como MD5/SHA256 "puro", que é rápido demais e
     * mais fácil de atacar por força bruta). Aqui, além da senha e do salt,
     * entram as ITERATIONS (repetições) e o KEY_LENGTH (tamanho do resultado).
     */
    private byte[] hash(String password, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return factory.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            // Esses erros só aconteceriam se o algoritmo não existisse na
            // JVM, o que não é esperado — por isso viram uma RuntimeException.
            throw new RuntimeException(e);
        }
    }
}

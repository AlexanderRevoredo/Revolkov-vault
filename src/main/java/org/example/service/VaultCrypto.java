package org.example.service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Responsável por CRIPTOGRAFAR e DESCRIPTOGRAFAR o conteúdo do cofre.
 *
 * Antes desta classe existir, o arquivo com as credenciais era salvo em texto
 * puro: qualquer pessoa que abrisse o arquivo no Bloco de Notas veria todas as
 * senhas. Agora o arquivo só contém bytes embaralhados, que só podem ser lidos
 * de volta por quem souber a senha mestre.
 *
 * Dois conceitos importantes aqui:
 *
 * 1) DERIVAÇÃO DE CHAVE (PBKDF2)
 *    Uma senha digitada por humano ("minhasenha123") não serve diretamente
 *    como chave de criptografia — é curta e previsível demais. O PBKDF2 pega
 *    essa senha + um "salt" aleatório e, repetindo o cálculo milhares de
 *    vezes, produz uma chave de 256 bits adequada para o AES. As repetições
 *    (ITERATIONS) tornam o processo lento de propósito, para dificultar
 *    ataques que testam milhões de senhas por segundo.
 *
 * 2) AES-GCM (criptografia AUTENTICADA)
 *    O GCM não só embaralha os dados: ele também gera uma "etiqueta de
 *    autenticação" (tag). Se alguém alterar um único byte do arquivo — ou se
 *    a senha usada para descriptografar estiver errada — a verificação da tag
 *    falha e a operação lança exceção, em vez de devolver lixo silenciosamente.
 *    Ou seja: o próprio GCM já detecta senha incorreta e arquivo corrompido.
 */
public class VaultCrypto {

    // Repetições do PBKDF2. Quanto maior, mais lento (para nós e para um atacante).
    private static final int ITERATIONS = 65536;
    // Tamanho da chave AES gerada, em bits.
    private static final int KEY_LENGTH_BITS = 256;

    // Tamanho, em bytes, do salt (o "tempero" aleatório da derivação de chave).
    public static final int SALT_LENGTH = 16;
    // Tamanho do IV (vetor de inicialização). 12 bytes é o valor recomendado
    // para o modo GCM. O IV precisa ser DIFERENTE a cada criptografia — por
    // isso geramos um novo toda vez que salvamos o cofre.
    public static final int IV_LENGTH = 12;
    // Tamanho da tag de autenticação, em bits.
    private static final int TAG_LENGTH_BITS = 128;

    private static final SecureRandom RANDOM = new SecureRandom();

    /** Gera uma sequência aleatória de bytes (usada para salt e IV). */
    public static byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        RANDOM.nextBytes(bytes);
        return bytes;
    }

    /**
     * Transforma a senha mestre digitada pelo usuário numa chave AES de 256 bits.
     * O mesmo par (senha, salt) sempre gera a MESMA chave — é isso que permite
     * abrir de novo, amanhã, um arquivo criptografado hoje.
     *
     * Recebe char[] em vez de String porque, em teoria, um array pode ser
     * apagado da memória depois do uso, enquanto Strings ficam soltas na
     * memória até o coletor de lixo decidir removê-las.
     */
    public static SecretKey deriveKey(char[] password, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH_BITS);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] keyBytes = factory.generateSecret(spec).getEncoded();
            // O PBKDF2 devolve bytes "genéricos"; aqui dizemos que esses bytes
            // devem ser interpretados especificamente como uma chave AES.
            return new SecretKeySpec(keyBytes, "AES");
        } catch (GeneralSecurityException e) {
            // Só aconteceria se o algoritmo não existisse nesta JVM.
            throw new IllegalStateException("Falha ao derivar a chave de criptografia.", e);
        }
    }

    /**
     * Criptografa um texto e devolve os bytes prontos para gravar no arquivo,
     * no formato: [IV (12 bytes)][dados criptografados + tag].
     *
     * O IV é guardado junto com os dados de propósito: ele não é secreto, mas
     * é obrigatório para conseguir descriptografar depois.
     */
    public static byte[] encrypt(String plainText, SecretKey key) {
        try {
            byte[] iv = randomBytes(IV_LENGTH);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] cipherText = cipher.doFinal(plainText.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            // Junta IV + texto criptografado num único array de bytes.
            byte[] result = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(cipherText, 0, result, iv.length, cipherText.length);
            return result;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Falha ao criptografar o cofre.", e);
        }
    }

    /**
     * Faz o caminho inverso de encrypt(): separa o IV do resto e descriptografa.
     *
     * Lança {@link WrongPasswordException} se a chave estiver errada ou se o
     * arquivo tiver sido alterado/corrompido — nos dois casos a verificação da
     * tag do GCM falha, e não há como distinguir um do outro (o que, do ponto
     * de vista de segurança, é justamente o desejado).
     */
    public static String decrypt(byte[] ivAndCipherText, SecretKey key) throws WrongPasswordException {
        try {
            byte[] iv = Arrays.copyOfRange(ivAndCipherText, 0, IV_LENGTH);
            byte[] cipherText = Arrays.copyOfRange(ivAndCipherText, IV_LENGTH, ivAndCipherText.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(cipherText), java.nio.charset.StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new WrongPasswordException();
        }
    }

    /** Exceção usada quando a senha mestre não abre o cofre. */
    public static class WrongPasswordException extends Exception {
        public WrongPasswordException() {
            super("Não foi possível abrir o cofre: senha mestre incorreta ou arquivo corrompido.");
        }
    }
}

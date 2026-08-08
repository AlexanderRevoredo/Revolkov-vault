package org.example.service;

import org.example.model.PasswordEntry;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Cuida de ler e gravar a lista de credenciais em disco, sempre de forma
 * CRIPTOGRAFADA. É a única classe que sabe onde e como os dados ficam salvos
 * — o resto do app só chama loadAll()/saveAll().
 *
 * O arquivo fica em: C:\Users\<usuário>\password-manager\vault.dat
 * (fora da pasta do projeto, para não se perder ao recompilar o código).
 *
 * Formato do arquivo (bytes crus, não é texto legível):
 *
 *     [ salt: 16 bytes ][ IV: 12 bytes ][ dados criptografados + tag ]
 *      \_____________/   \____________________________________________/
 *       usado para           produzido pelo VaultCrypto.encrypt()
 *       derivar a chave
 *
 * O salt fica no começo do arquivo porque precisamos dele para recriar a
 * mesma chave na próxima vez que o app abrir. Ele não é secreto: sua função
 * é apenas garantir que a mesma senha mestre gere chaves diferentes em
 * máquinas/cofres diferentes.
 *
 * Depois de descriptografado, o conteúdo é o mesmo texto de antes: uma
 * credencial por linha, campos separados por "|" (ver PasswordEntry.toLine()).
 */
public class PasswordStorage {

    private final Path vaultFile;
    private final Path legacyPlainTextFile;
    private final SecretKey key;
    private final byte[] salt;

    /**
     * Só é possível criar um PasswordStorage depois que o usuário digitou a
     * senha mestre — sem a chave derivada dela, não há como ler nem gravar
     * nada. Por isso a chave e o salt vêm prontos, via construtor.
     */
    private PasswordStorage(Path dir, SecretKey key, byte[] salt) {
        this.vaultFile = dir.resolve("vault.dat");
        this.legacyPlainTextFile = dir.resolve("passwords.txt");
        this.key = key;
        this.salt = salt;
    }

    /** Pasta onde ficam os arquivos do app, dentro da pasta do usuário. */
    public static Path defaultDirectory() {
        return Path.of(System.getProperty("user.home"), "password-manager");
    }

    /** true se já existe um cofre criptografado criado nesta máquina. */
    public static boolean vaultExists() {
        return Files.exists(defaultDirectory().resolve("vault.dat"));
    }

    /**
     * Abre um cofre JÁ EXISTENTE usando a senha mestre digitada.
     *
     * Lê o salt gravado no início do arquivo, deriva a chave a partir dele e
     * tenta descriptografar. Se a senha estiver errada, a verificação da tag
     * do AES-GCM falha e uma WrongPasswordException é lançada — ou seja, o
     * próprio arquivo criptografado funciona como verificação da senha.
     */
    public static PasswordStorage unlock(char[] masterPassword) throws VaultCrypto.WrongPasswordException {
        Path dir = defaultDirectory();
        Path vaultFile = dir.resolve("vault.dat");
        try {
            byte[] fileBytes = Files.readAllBytes(vaultFile);
            byte[] salt = Arrays.copyOfRange(fileBytes, 0, VaultCrypto.SALT_LENGTH);
            byte[] payload = Arrays.copyOfRange(fileBytes, VaultCrypto.SALT_LENGTH, fileBytes.length);

            SecretKey key = VaultCrypto.deriveKey(masterPassword, salt);
            // Descriptografa só para conferir que a senha está correta. Se
            // passar daqui sem exceção, a chave é a certa.
            VaultCrypto.decrypt(payload, key);

            return new PasswordStorage(dir, key, salt);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Cria um cofre NOVO (primeira execução do app nesta máquina), protegido
     * pela senha mestre escolhida. Gera um salt aleatório novo e grava um
     * cofre vazio já criptografado.
     *
     * Se existir um arquivo passwords.txt do formato antigo (texto puro),
     * as credenciais dele são importadas para dentro do cofre criptografado
     * e o arquivo antigo é apagado — deixá-lo no disco anularia todo o ganho
     * de segurança da criptografia.
     */
    public static PasswordStorage createNew(char[] masterPassword) {
        Path dir = defaultDirectory();
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        byte[] salt = VaultCrypto.randomBytes(VaultCrypto.SALT_LENGTH);
        SecretKey key = VaultCrypto.deriveKey(masterPassword, salt);
        PasswordStorage storage = new PasswordStorage(dir, key, salt);

        storage.saveAll(storage.importLegacyEntries());
        storage.deleteLegacyFile();
        return storage;
    }

    /** Lê o cofre, descriptografa e devolve a lista de credenciais. */
    public List<PasswordEntry> loadAll() {
        List<PasswordEntry> entries = new ArrayList<>();
        try {
            byte[] fileBytes = Files.readAllBytes(vaultFile);
            byte[] payload = Arrays.copyOfRange(fileBytes, VaultCrypto.SALT_LENGTH, fileBytes.length);
            String content = VaultCrypto.decrypt(payload, key);

            // Uma credencial por linha. Quebras de linha que façam parte do
            // conteúdo já foram "escapadas" pelo PasswordEntry.toLine(), então
            // aqui todo "\n" de verdade é mesmo o fim de uma credencial.
            for (String line : content.split("\n")) {
                if (!line.isBlank()) {
                    entries.add(PasswordEntry.fromLine(line));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (VaultCrypto.WrongPasswordException e) {
            // Não deveria acontecer: a chave já foi validada no unlock().
            // Se chegar aqui, o arquivo foi alterado enquanto o app rodava.
            throw new IllegalStateException(e.getMessage(), e);
        }
        return entries;
    }

    /**
     * Criptografa a lista inteira e sobrescreve o arquivo.
     *
     * Reescrever tudo (em vez de tentar atualizar uma linha específica) é bem
     * mais simples e, com dezenas ou centenas de credenciais, não faz
     * diferença de desempenho. Com criptografia isso é ainda mais verdade:
     * o conteúdo é embaralhado como um bloco único, então não existe a opção
     * de "editar só um pedaço" do arquivo.
     */
    public void saveAll(List<PasswordEntry> entries) {
        String content = String.join("\n", entries.stream().map(PasswordEntry::toLine).toList());
        byte[] payload = VaultCrypto.encrypt(content, key);

        // Remonta o arquivo: salt (mesmo de sempre) + IV/dados (novos a cada gravação).
        byte[] fileBytes = new byte[salt.length + payload.length];
        System.arraycopy(salt, 0, fileBytes, 0, salt.length);
        System.arraycopy(payload, 0, fileBytes, salt.length, payload.length);

        try {
            Files.write(vaultFile, fileBytes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Lê o antigo passwords.txt em texto puro, se existir, para não perder
     * dados de quem já usava a versão anterior do app.
     */
    private List<PasswordEntry> importLegacyEntries() {
        List<PasswordEntry> entries = new ArrayList<>();
        if (!Files.exists(legacyPlainTextFile)) {
            return entries;
        }
        try {
            for (String line : Files.readAllLines(legacyPlainTextFile)) {
                if (!line.isBlank()) {
                    entries.add(PasswordEntry.fromLine(line));
                }
            }
        } catch (IOException | RuntimeException e) {
            // Se o arquivo antigo estiver corrompido, seguimos com o cofre
            // vazio em vez de impedir o app de abrir.
            return new ArrayList<>();
        }
        return entries;
    }

    /** Apaga o arquivo de texto puro após a migração. */
    private void deleteLegacyFile() {
        try {
            Files.deleteIfExists(legacyPlainTextFile);
        } catch (IOException ignored) {
            // Se não der para apagar (arquivo aberto em outro programa, por
            // exemplo), não é motivo para impedir o app de funcionar.
        }
    }
}

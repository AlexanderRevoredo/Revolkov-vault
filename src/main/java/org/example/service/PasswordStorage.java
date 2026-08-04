package org.example.service;

import org.example.model.PasswordEntry;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Cuida de ler e escrever a lista de credenciais no arquivo passwords.txt.
 * É a única classe que sabe onde e como os dados ficam salvos em disco — o
 * resto do app só chama loadAll()/saveAll() e não precisa saber que por
 * trás disso existe um arquivo de texto (poderia virar um banco de dados no
 * futuro sem mudar quem usa essa classe).
 *
 * O arquivo fica em: C:\Users\<usuário>\password-manager\passwords.txt
 * (fora da pasta do projeto, pra não se perder se o código for reconstruído).
 */
public class PasswordStorage {

    private final Path file;

    /**
     * O construtor já garante que a pasta e o arquivo existem, criando-os se
     * for a primeira vez que o app roda nessa máquina.
     */
    public PasswordStorage() {
        Path dir = Path.of(System.getProperty("user.home"), "password-manager");
        this.file = dir.resolve("passwords.txt");
        try {
            Files.createDirectories(dir);
            if (!Files.exists(file)) {
                Files.createFile(file);
            }
        } catch (IOException e) {
            // UncheckedIOException "embrulha" a IOException (checked) numa
            // RuntimeException, pra não sermos obrigados a colocar "throws"
            // em todo método que usa essa classe. Se der erro aqui, é algo
            // sério (disco cheio, sem permissão) que não dá pra recuperar.
            throw new UncheckedIOException(e);
        }
    }

    /** Lê o arquivo inteiro e transforma cada linha não-vazia num PasswordEntry. */
    public List<PasswordEntry> loadAll() {
        List<PasswordEntry> entries = new ArrayList<>();
        try {
            for (String line : Files.readAllLines(file)) {
                if (!line.isBlank()) {
                    entries.add(PasswordEntry.fromLine(line));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return entries;
    }

    /**
     * Sobrescreve o arquivo inteiro com a lista atual. É mais simples do que
     * tentar "atualizar só uma linha" — como a lista costuma ser pequena
     * (dezenas/centenas de credenciais), reescrever tudo a cada salvamento
     * não é um problema de performance.
     */
    public void saveAll(List<PasswordEntry> entries) {
        List<String> lines = entries.stream().map(PasswordEntry::toLine).toList();
        try {
            Files.write(file, lines);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}

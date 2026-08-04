package org.example;

/**
 * Existe só por causa de um detalhe chato do empacotamento do JavaFX:
 * quando geramos o .exe com jpackage e a classe principal do .jar É uma
 * Application (como a MainApp), rodar o programa direto ("java -jar ...")
 * falha com o erro "JavaFX runtime components are missing".
 *
 * A solução padrão é ter uma classe SEPARADA, que não estende Application,
 * só pra dar o start — é ela que fica configurada como classe principal no
 * jar/instalador. Aqui ela não faz nada além de repassar a chamada pra
 * MainApp.main(), que é quem realmente inicia o JavaFX.
 */
public class Launcher {
    public static void main(String[] args) {
        MainApp.main(args);
    }
}

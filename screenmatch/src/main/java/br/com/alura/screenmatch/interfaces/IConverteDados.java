package br.com.alura.screenmatch.interfaces;

public interface IConverteDados {
    <T> T obterDados(String json, Class<T> classe) throws Exception;
}

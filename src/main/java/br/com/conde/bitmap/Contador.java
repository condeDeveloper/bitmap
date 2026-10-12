package br.com.conde.bitmap;

/**
 * O CONTADOR: palavras lidas e comparacoes. Nunca segundos.
 *
 * <p>A PALAVRA LIDA e a medida certa e nao so a conveniente. Toda representacao daqui
 * guarda o conjunto numa sequencia de palavras de 64 bits, e toda operacao entre dois
 * conjuntos e uma varredura dessas palavras. O que separa uma representacao da outra e
 * QUANTAS palavras ela precisa tocar para responder.
 *
 * <p>E essa medida tem uma propriedade que e o repositorio inteiro: para o mapa de bits ela
 * nao depende de quantos elementos o conjunto tem. Um conjunto de dez elementos e um de um
 * milhao ocupam o mesmo numero de palavras se o universo e o mesmo, e a intersecao dos dois
 * custa igual. O mapa de bits cobra o UNIVERSO, nao o dado.
 *
 * <p>Segundos nao serviriam: uma operacao entre mapas de bits e uma sequencia de instrucoes
 * que o processador faz varias por ciclo, e uma intersecao de vetores ordenados e cheia de
 * desvios que ele erra. Medir tempo compararia a sorte do preditor de desvio com a
 * quantidade de trabalho, e so a segunda e sobre a estrutura.
 */
public final class Contador {

    private long palavrasLidas;
    private long comparacoes;
    private long saltos;
    private long operacoes;

    /** Uma palavra de 64 bits foi lida ou escrita. */
    public void lePalavra() {
        palavrasLidas++;
    }

    public void lePalavra(long quantas) {
        palavrasLidas += quantas;
    }

    /** Dois valores foram comparados. */
    public void compara() {
        comparacoes++;
    }

    /** Um salto da busca em galope, que pula de potencia em potencia de dois. */
    public void salta() {
        saltos++;
    }

    /** Uma operacao entre conjuntos foi pedida. */
    public void operacao() {
        operacoes++;
    }

    public long palavrasLidas() {
        return palavrasLidas;
    }

    public long comparacoes() {
        return comparacoes;
    }

    public long saltos() {
        return saltos;
    }

    public long operacoes() {
        return operacoes;
    }

    /**
     * O trabalho total de uma operacao, somando as duas colunas.
     *
     * <p>Somar palavra lida com comparacao nao e exato: uma palavra de 64 bits carrega 64
     * elementos e uma comparacao decide sobre um. Mas as duas sao acesso a memoria mais uma
     * instrucao, e uma coluna so e o que permite ordenar os metodos. As colunas separadas
     * ficam do lado para quem quiser desfazer a soma.
     */
    public long trabalho() {
        return palavrasLidas + comparacoes;
    }

    public double trabalhoPorOperacao() {
        return operacoes == 0 ? 0 : (double) trabalho() / operacoes;
    }

    public void zerar() {
        palavrasLidas = 0;
        comparacoes = 0;
        saltos = 0;
        operacoes = 0;
    }
}

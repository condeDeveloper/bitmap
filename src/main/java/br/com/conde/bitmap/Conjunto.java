package br.com.conde.bitmap;

/**
 * UM CONJUNTO DE INTEIROS entre 0 e o universo, guardado de um jeito ou de outro.
 *
 * <p>Todas as representacoes daqui respondem exatamente a mesma coisa. Nenhuma aproxima,
 * nenhuma perde elemento. O que muda e quanto espaco cada uma ocupa e quanto trabalho cada
 * operacao custa, e e so por responderem igual que a comparacao entre elas vale.
 *
 * <p>O UNIVERSO e um parametro e nao um detalhe. Ele e o maior elemento possivel mais um, e
 * e ele, e nao a quantidade de elementos, que decide o tamanho de um mapa de bits. A
 * DENSIDADE, que e a quantidade de elementos dividida pelo universo, e o eixo de quase toda
 * tabela deste repositorio.
 */
public interface Conjunto {

    boolean tem(int valor, Contador contador);

    /** Os elementos em ordem crescente. */
    int[] emVetor();

    int cardinalidade();

    int universo();

    /**
     * Quantos BITS a representacao ocupa de verdade.
     *
     * <p>E a medida de espaco do repositorio, e ela e contada na representacao e nao no
     * objeto Java: um vetor de n inteiros ocupa 32n bits, um mapa de bits ocupa o universo
     * arredondado para cima em palavras de 64, e uma lista de corridas ocupa 64 por
     * corrida. O cabecalho do objeto e o ponteiro nao entram porque sao os mesmos para
     * todas e so acrescentariam uma constante igual em toda linha.
     */
    long bitsOcupados();

    /** Os bits por elemento guardado, que e a coluna que a tabela da memoria imprime. */
    default double bitsPorElemento() {
        return cardinalidade() == 0 ? 0 : (double) bitsOcupados() / cardinalidade();
    }

    /**
     * A intersecao com outro conjunto da MESMA representacao.
     *
     * <p>Da mesma de proposito. Misturar representacoes e um assunto de verdade e nao e
     * este: aqui a pergunta e quanto cada uma custa fazendo o trabalho dela, e uma operacao
     * entre tipos diferentes mediria o custo da conversao.
     */
    Conjunto interseccao(Conjunto outro, Contador contador);

    Conjunto uniao(Conjunto outro, Contador contador);

    String nome();
}

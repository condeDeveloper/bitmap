package br.com.conde.bitmap;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * OS CONJUNTOS DE ENTRADA e as fabricas de representacao.
 *
 * <p>A forma do conjunto decide qual representacao ganha, e por isso cada gerador daqui
 * existe para fazer uma delas brilhar e outra sofrer. Um conjunto sorteado e o pesadelo das
 * corridas; um conjunto em blocos e o paraiso delas.
 */
public final class Conjuntos {

    private Conjuntos() {
    }

    /**
     * O SORTEIO DA CASA: um gerador congruente linear escrito aqui, com constantes fixas.
     *
     * <p>Nao e {@link java.util.Random} de proposito. Uma medida que depende de uma
     * biblioteca nao e reproduzivel: a implementacao pode mudar entre versoes da maquina
     * virtual e as tabelas do README deixariam de bater sem que nada aqui tivesse mudado.
     */
    public static final class Sorteio {

        private long estado;

        public Sorteio(long semente) {
            this.estado = semente;
        }

        /**
         * O proximo inteiro.
         *
         * <p>O DESLOCAMENTO DE 32 CARREGA O METODO. Os bits BAIXOS de um gerador congruente
         * linear de modulo potencia de dois sao pessimos: o ultimo bit alterna zero e um
         * para sempre, o penultimo tem periodo quatro.
         *
         * <p>Num repositorio sobre mapas de bits isso seria especialmente ridiculo. O indice
         * da palavra e o valor deslocado de seis bits e o bit dentro dela sao os seis bits
         * BAIXOS: um gerador com bits baixos ruins encheria sempre as mesmas posicoes dentro
         * de cada palavra, e a tabela das corridas mediria o gerador em vez de medir o dado.
         * Ha um teste so para isto.
         */
        public int proximo() {
            estado = estado * 6364136223846793005L + 1442695040888963407L;
            return (int) (estado >>> 32);
        }

        public int ate(int limite) {
            return Math.floorMod(proximo(), limite);
        }
    }

    /**
     * Valores sorteados por igual no universo, sem repetir.
     *
     * <p>E o pior caso das corridas: com densidade baixa, quase toda corrida tem um
     * elemento so.
     */
    public static int[] sorteado(int universo, int quantos, long semente) {
        if (quantos > universo) throw new IllegalArgumentException("mais valores que o universo");

        Sorteio sorteio = new Sorteio(semente);
        boolean[] visto = new boolean[universo];
        int[] valores = new int[quantos];
        int escritos = 0;

        while (escritos < quantos) {
            int valor = sorteio.ate(universo);

            if (visto[valor]) continue;

            visto[valor] = true;
            valores[escritos++] = valor;
        }

        return valores;
    }

    /**
     * Valores em pedacos contiguos espalhados pelo universo.
     *
     * <p>E o melhor caso das corridas, e nao e uma situacao inventada: identificadores
     * criados em sequencia, linhas de uma tabela carregadas de uma vez, datas de um mes.
     */
    public static int[] emBlocos(int universo, int quantos, int tamanhoDoBloco, long semente) {
        Sorteio sorteio = new Sorteio(semente);
        boolean[] marcado = new boolean[universo];
        int escritos = 0;

        while (escritos < quantos) {
            int comeco = sorteio.ate(universo);

            for (int i = 0; i < tamanhoDoBloco && escritos < quantos; i++) {
                int valor = comeco + i;

                if (valor >= universo || marcado[valor]) continue;

                marcado[valor] = true;
                escritos++;
            }
        }

        int[] valores = new int[escritos];
        int quantas = 0;

        for (int valor = 0; valor < universo; valor++) if (marcado[valor]) valores[quantas++] = valor;

        return valores;
    }

    /**
     * Valores em poucos pedacos grandes e o resto vazio.
     *
     * <p>E o conjunto que o hibrido existe para representar: alguns blocos cheios, muitos
     * blocos vazios, e nenhuma representacao unica serve bem para os dois.
     */
    public static int[] desigual(int universo, int quantos, long semente) {
        Sorteio sorteio = new Sorteio(semente);
        int blocos = Math.max(1, universo / Hibrido.TAMANHO_DO_BLOCO);
        boolean[] marcado = new boolean[universo];
        int escritos = 0;
        int cheios = Math.max(1, blocos / 4);

        while (escritos < quantos) {
            int bloco = sorteio.ate(cheios);
            int valor = bloco * Hibrido.TAMANHO_DO_BLOCO
                    + sorteio.ate(Math.min(Hibrido.TAMANHO_DO_BLOCO, universo));

            if (valor >= universo || marcado[valor]) continue;

            marcado[valor] = true;
            escritos++;
        }

        int[] valores = new int[escritos];
        int quantas = 0;

        for (int valor = 0; valor < universo; valor++) if (marcado[valor]) valores[quantas++] = valor;

        return valores;
    }

    /** Um valor a cada passo, sem sorteio nenhum. */
    public static int[] regular(int universo, int passo) {
        int quantos = (universo + passo - 1) / passo;
        int[] valores = new int[quantos];

        for (int i = 0; i < quantos; i++) valores[i] = i * passo;

        return valores;
    }

    /** As representacoes que as tabelas percorrem, cada uma com o nome que a tabela imprime. */
    public static Map<String, BiFunction<Integer, int[], Conjunto>> asRepresentacoes() {
        Map<String, BiFunction<Integer, int[], Conjunto>> fabricas = new LinkedHashMap<>();

        fabricas.put("mapa de bits", MapaDeBits::new);
        fabricas.put("vetor ordenado", (universo, valores) ->
                new VetorOrdenado(universo, valores, false));
        fabricas.put("vetor em galope", (universo, valores) ->
                new VetorOrdenado(universo, valores, true));
        fabricas.put("por corridas", PorCorridas::new);
        fabricas.put("hibrido", Hibrido::new);

        return fabricas;
    }

    public static Map<String, int[]> todos(int universo, int quantos, long semente) {
        Map<String, int[]> entradas = new LinkedHashMap<>();

        entradas.put("sorteado", sorteado(universo, quantos, semente));
        entradas.put("em blocos", emBlocos(universo, quantos, 64, semente + 1));
        entradas.put("desigual", desigual(universo, quantos, semente + 2));
        entradas.put("regular", regular(universo, Math.max(1, universo / Math.max(1, quantos))));

        return entradas;
    }

    /** Os valores em ordem, sem repetir, para o juiz. */
    public static int[] limpar(int[] valores) {
        int[] copia = valores.clone();

        Arrays.sort(copia);

        int quantos = 0;

        for (int i = 0; i < copia.length; i++) {
            if (i > 0 && copia[i] == copia[i - 1]) continue;

            copia[quantos++] = copia[i];
        }

        return Arrays.copyOf(copia, quantos);
    }
}

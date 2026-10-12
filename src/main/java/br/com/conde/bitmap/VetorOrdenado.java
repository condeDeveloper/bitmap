package br.com.conde.bitmap;

import java.util.Arrays;

/**
 * O VETOR ORDENADO: os elementos, em ordem, num vetor de inteiros.
 *
 * <p>Ele e o oposto do mapa de bits em tudo. O tamanho dele depende so de QUANTOS elementos
 * existem e nao do universo, e o custo de uma operacao depende so dos dois tamanhos.
 *
 * <p>Sao 32 bits por elemento, contra 1 bit por valor POSSIVEL do mapa de bits, e dai sai o
 * numero que a tabela da memoria confere: os dois empatam quando o conjunto tem um elemento
 * a cada 32 do universo, ou seja, densidade 1/32.
 *
 * <h2>Dois jeitos de intersectar</h2>
 *
 * <p>A MESCLA anda nos dois vetores ao mesmo tempo e custa a soma dos tamanhos. Ela e a
 * escolha obvia e e a errada quando um conjunto e muito menor que o outro: intersectar dez
 * elementos com um milhao custa um milhao.
 *
 * <p>O GALOPE percorre so o menor e, para cada elemento dele, procura no maior pulando de
 * potencia em potencia de dois ate passar do alvo, e so entao faz uma busca binaria no
 * trecho. Custa o menor vezes o logaritmo da razao entre os dois, que com dez contra um
 * milhao e umas cento e setenta comparacoes em vez de um milhao.
 */
public final class VetorOrdenado implements Conjunto {

    private final int[] valores;
    private final int universo;
    private final boolean galope;

    public VetorOrdenado(int universo, int[] valores, boolean galope) {
        this.universo = universo;
        this.galope = galope;

        int[] copia = valores.clone();

        Arrays.sort(copia);

        int quantos = 0;

        for (int i = 0; i < copia.length; i++) {
            if (copia[i] < 0 || copia[i] >= universo)
                throw new IllegalArgumentException(
                        "valor " + copia[i] + " fora do universo " + universo);

            if (i > 0 && copia[i] == copia[i - 1]) continue;

            copia[quantos++] = copia[i];
        }

        this.valores = Arrays.copyOf(copia, quantos);
    }

    private VetorOrdenado(int universo, int[] jaLimpos, boolean galope, boolean confiar) {
        this.universo = universo;
        this.valores = jaLimpos;
        this.galope = galope;
    }

    @Override
    public String nome() {
        return galope ? "vetor em galope" : "vetor ordenado";
    }

    @Override
    public boolean tem(int valor, Contador contador) {
        return procurar(valores, 0, valores.length, valor, contador) >= 0;
    }

    private static int procurar(int[] vetor, int de, int ate, int alvo, Contador contador) {
        int baixo = de;
        int alto = ate - 1;

        while (baixo <= alto) {
            int meio = (baixo + alto) >>> 1;

            contador.compara();

            if (vetor[meio] == alvo) return meio;
            if (vetor[meio] < alvo) baixo = meio + 1;
            else alto = meio - 1;
        }

        return -(baixo + 1);
    }

    @Override
    public Conjunto interseccao(Conjunto outro, Contador contador) {
        VetorOrdenado dele = exigirVetor(outro);

        contador.operacao();

        if (galope && valores.length != dele.valores.length)
            return emGalope(dele, contador);

        int[] saida = new int[Math.min(valores.length, dele.valores.length)];
        int quantos = 0;
        int i = 0;
        int j = 0;

        while (i < valores.length && j < dele.valores.length) {
            contador.compara();

            if (valores[i] == dele.valores[j]) {
                saida[quantos++] = valores[i];
                i++;
                j++;
            } else if (valores[i] < dele.valores[j]) {
                i++;
            } else {
                j++;
            }
        }

        return new VetorOrdenado(universo, Arrays.copyOf(saida, quantos), galope, true);
    }

    /**
     * A intersecao em galope: percorre o menor e procura cada elemento no maior.
     *
     * <p>O galope existe porque uma busca binaria pura no vetor grande custaria o logaritmo
     * do TAMANHO dele a cada elemento, e isso joga fora a informacao de que os elementos do
     * menor vem em ordem: cada um comeca a procurar depois de onde o anterior parou.
     */
    private Conjunto emGalope(VetorOrdenado dele, Contador contador) {
        int[] menor = valores.length <= dele.valores.length ? valores : dele.valores;
        int[] maior = valores.length <= dele.valores.length ? dele.valores : valores;

        int[] saida = new int[menor.length];
        int quantos = 0;
        int base = 0;

        for (int alvo : menor) {
            if (base >= maior.length) break;

            int passo = 1;
            int alto = base;

            while (alto < maior.length) {
                contador.salta();
                contador.compara();

                if (maior[alto] >= alvo) break;

                base = alto + 1;
                passo *= 2;
                alto = base + passo - 1;
            }

            int fim = Math.min(alto + 1, maior.length);
            int onde = procurar(maior, base, fim, alvo, contador);

            if (onde >= 0) {
                saida[quantos++] = alvo;
                base = onde + 1;
            } else {
                base = -(onde + 1);
            }
        }

        return new VetorOrdenado(universo, Arrays.copyOf(saida, quantos), galope, true);
    }

    @Override
    public Conjunto uniao(Conjunto outro, Contador contador) {
        VetorOrdenado dele = exigirVetor(outro);

        contador.operacao();

        int[] saida = new int[valores.length + dele.valores.length];
        int quantos = 0;
        int i = 0;
        int j = 0;

        while (i < valores.length && j < dele.valores.length) {
            contador.compara();

            if (valores[i] == dele.valores[j]) {
                saida[quantos++] = valores[i++];
                j++;
            } else if (valores[i] < dele.valores[j]) {
                saida[quantos++] = valores[i++];
            } else {
                saida[quantos++] = dele.valores[j++];
            }
        }

        while (i < valores.length) saida[quantos++] = valores[i++];
        while (j < dele.valores.length) saida[quantos++] = dele.valores[j++];

        return new VetorOrdenado(universo, Arrays.copyOf(saida, quantos), galope, true);
    }

    private VetorOrdenado exigirVetor(Conjunto outro) {
        if (!(outro instanceof VetorOrdenado dele))
            throw new IllegalArgumentException("so opera com outro vetor ordenado");

        if (dele.universo != universo) throw new IllegalArgumentException("universos diferentes");

        return dele;
    }

    @Override
    public int[] emVetor() {
        return valores.clone();
    }

    @Override
    public int cardinalidade() {
        return valores.length;
    }

    @Override
    public int universo() {
        return universo;
    }

    @Override
    public long bitsOcupados() {
        return (long) valores.length * 32;
    }
}

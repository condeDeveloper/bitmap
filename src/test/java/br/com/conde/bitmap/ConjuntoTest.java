package br.com.conde.bitmap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ConjuntoTest {

    // ───────────────────── o juiz contra todas as representacoes ─────────────────────

    @Test
    @DisplayName("toda representacao bate com o vetor de booleanos, em toda entrada")
    void todasBatemComOJuiz() {
        List<String> falhas = new ArrayList<>();
        int conferidas = 0;

        for (int universo : new int[] {1 << 16, 2 << 16})
            for (int quantos : new int[] {1, 7, 500, 20_000})
                for (var entrada : Conjuntos.todos(universo, quantos, 13).entrySet()) {
                    int[] outro = Conjuntos.sorteado(universo, quantos, 29);

                    for (var representacao : Conjuntos.asRepresentacoes().entrySet()) {
                        Juiz.Veredito veredito = Juiz.julgar(representacao.getKey(), universo,
                                entrada.getValue(), outro, representacao.getValue());

                        conferidas++;

                        if (!veredito.passou())
                            falhas.add(veredito.falha() + " em " + entrada.getKey()
                                    + " com " + quantos + " de " + universo);
                    }
                }

        assertThat(conferidas).isGreaterThan(100);
        assertThat(falhas).isEmpty();
    }

    @Test
    @DisplayName("o conjunto vazio e o conjunto cheio funcionam em todas")
    void vazioECheio() {
        int universo = 1 << 16;

        for (var representacao : Conjuntos.asRepresentacoes().entrySet()) {
            Conjunto vazio = representacao.getValue().apply(universo, new int[0]);

            assertThat(vazio.cardinalidade()).as(representacao.getKey()).isZero();
            assertThat(vazio.emVetor()).as(representacao.getKey()).isEmpty();
            assertThat(vazio.tem(0, new Contador())).as(representacao.getKey()).isFalse();

            Conjunto cheio = representacao.getValue()
                    .apply(universo, Conjuntos.regular(universo, 1));

            assertThat(cheio.cardinalidade()).as(representacao.getKey()).isEqualTo(universo);
            assertThat(cheio.tem(universo - 1, new Contador())).as(representacao.getKey()).isTrue();

            assertThat(cheio.interseccao(vazio, new Contador()).cardinalidade())
                    .as(representacao.getKey()).isZero();

            assertThat(cheio.uniao(vazio, new Contador()).cardinalidade())
                    .as(representacao.getKey()).isEqualTo(universo);
        }
    }

    @Test
    @DisplayName("valores repetidos na entrada entram uma vez so")
    void valoresRepetidos() {
        int universo = 1 << 16;
        int[] comRepetidos = {5, 5, 5, 9, 9, 1};

        for (var representacao : Conjuntos.asRepresentacoes().entrySet()) {
            Conjunto conjunto = representacao.getValue().apply(universo, comRepetidos);

            assertThat(conjunto.cardinalidade()).as(representacao.getKey()).isEqualTo(3);
            assertThat(conjunto.emVetor()).as(representacao.getKey()).containsExactly(1, 5, 9);
        }
    }

    @Test
    @DisplayName("valor fora do universo e recusado em vez de corromper a estrutura")
    void valorForaDoUniverso() {
        for (var representacao : Conjuntos.asRepresentacoes().entrySet()) {
            assertThatThrownBy(() -> representacao.getValue()
                    .apply(1 << 16, new int[] {1 << 16}))
                    .as(representacao.getKey())
                    .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> representacao.getValue().apply(1 << 16, new int[] {-1}))
                    .as(representacao.getKey())
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    @DisplayName("operar com representacao diferente e recusado")
    void representacoesMisturadas() {
        int universo = 1 << 16;
        int[] valores = {1, 2, 3};

        Conjunto mapa = new MapaDeBits(universo, valores);
        Conjunto vetor = new VetorOrdenado(universo, valores, false);

        assertThatThrownBy(() -> mapa.interseccao(vetor, new Contador()))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> vetor.uniao(mapa, new Contador()))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> mapa.interseccao(new MapaDeBits(2 << 16, valores), new Contador()))
                .as("universos diferentes tambem")
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ───────────────────── o que cada uma promete sobre si ─────────────────────

    /**
     * A PROPRIEDADE QUE DECIDE ONDE O MAPA DE BITS SERVE.
     *
     * <p>O custo dele nao depende do dado. Nem o espaco nem o trabalho.
     */
    @Test
    @DisplayName("o mapa de bits cobra o universo, e nao o dado")
    void oMapaCobraOUniverso() {
        int universo = 1 << 18;

        Conjunto grande = new MapaDeBits(universo, Conjuntos.sorteado(universo, universo / 2, 1));
        Conjunto minusculo = new MapaDeBits(universo, new int[] {7});
        Conjunto outroGrande = new MapaDeBits(universo, Conjuntos.sorteado(universo, universo / 2, 2));

        assertThat(minusculo.bitsOcupados())
                .as("um elemento ocupa o mesmo que meio milhao")
                .isEqualTo(grande.bitsOcupados());

        Contador comMinusculo = new Contador();
        Contador comGrande = new Contador();

        grande.interseccao(minusculo, comMinusculo);
        grande.interseccao(outroGrande, comGrande);

        assertThat(comMinusculo.trabalho())
                .as("e a interseccao com um elemento custa o mesmo que com meio milhao")
                .isEqualTo(comGrande.trabalho());

        assertThat(comMinusculo.trabalho()).isEqualTo(2L * universo / 64);
    }

    @Test
    @DisplayName("o vetor em galope custa muito menos que a mescla quando os tamanhos diferem")
    void oGalopeGanhaQuandoOsTamanhosDiferem() {
        int universo = 1 << 20;
        int[] grande = Conjuntos.sorteado(universo, universo / 2, 1);
        int[] pequeno = Conjuntos.sorteado(universo, 10, 2);

        Contador daMescla = new Contador();
        Contador doGalope = new Contador();

        new VetorOrdenado(universo, grande, false)
                .interseccao(new VetorOrdenado(universo, pequeno, false), daMescla);

        new VetorOrdenado(universo, grande, true)
                .interseccao(new VetorOrdenado(universo, pequeno, true), doGalope);

        assertThat(doGalope.trabalho()).isLessThan(daMescla.trabalho() / 1_000);

        Contador iguais = new Contador();

        new VetorOrdenado(universo, grande, true)
                .interseccao(new VetorOrdenado(universo,
                        Conjuntos.sorteado(universo, universo / 2, 3), true), iguais);

        assertThat(iguais.trabalho())
                .as("e com tamanhos iguais ele cai na mescla, que ali e o certo")
                .isGreaterThan(doGalope.trabalho() * 100);
    }

    /**
     * A COMPRESSAO QUE AUMENTA O TAMANHO.
     *
     * <p>Num conjunto sem nenhuma corrida de verdade, cada corrida tem um elemento e gasta
     * dois inteiros para guardar o que caberia em um.
     */
    @Test
    @DisplayName("por corridas gasta o dobro do vetor quando nao ha corrida nenhuma")
    void asCorridasPodemExpandir() {
        int universo = 1 << 20;
        int[] regular = Conjuntos.regular(universo, 16);

        PorCorridas corridas = new PorCorridas(universo, regular);
        VetorOrdenado vetor = new VetorOrdenado(universo, regular, false);

        assertThat(corridas.elementosPorCorrida())
                .as("um valor a cada 16: nenhuma corrida tem mais de um elemento")
                .isEqualTo(1.0);

        assertThat(corridas.bitsPorElemento()).isEqualTo(64.0);
        assertThat(corridas.bitsPorElemento()).isEqualTo(2 * vetor.bitsPorElemento());

        PorCorridas tudo = new PorCorridas(universo, Conjuntos.regular(universo, 1));

        assertThat(tudo.corridas())
                .as("e no outro extremo, o universo inteiro e uma corrida so")
                .isEqualTo(1);

        assertThat(tudo.bitsOcupados()).isEqualTo(64);
    }

    @Test
    @DisplayName("o hibrido escolhe mapa acima do limiar e vetor abaixo")
    void oHibridoEscolhe() {
        int universo = 4 * Hibrido.TAMANHO_DO_BLOCO;

        int[] ralo = new int[Hibrido.LIMIAR];

        for (int i = 0; i < ralo.length; i++) ralo[i] = i * 2;

        Hibrido comVetor = new Hibrido(universo, ralo);

        assertThat(comVetor.blocosEmVetor()).isEqualTo(1);
        assertThat(comVetor.blocosEmMapa()).isZero();
        assertThat(comVetor.bitsPorElemento()).isEqualTo(16.0);

        int[] denso = new int[Hibrido.LIMIAR + 1];

        for (int i = 0; i < denso.length; i++) denso[i] = i;

        Hibrido comMapa = new Hibrido(universo, denso);

        assertThat(comMapa.blocosEmMapa()).isEqualTo(1);
        assertThat(comMapa.blocosEmVetor()).isZero();
        assertThat(comMapa.bitsOcupados()).isEqualTo(Hibrido.TAMANHO_DO_BLOCO);
    }

    @Test
    @DisplayName("o universo do hibrido precisa ser multiplo do bloco")
    void oHibridoExigeUniversoRedondo() {
        assertThatThrownBy(() -> new Hibrido(1_000, new int[] {1}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("multiplo");
    }

    // ───────────────────── o sorteio da casa ─────────────────────

    /**
     * O TESTE DO DESLOCAMENTO DE 32.
     *
     * <p>Num repositorio sobre mapas de bits isso seria especialmente ridiculo: o bit dentro
     * da palavra sao os seis bits BAIXOS do valor, e um gerador com bits baixos ruins
     * encheria sempre as mesmas posicoes dentro de cada palavra.
     */
    @Test
    @DisplayName("o deslocamento de 32 e o que faz os bits baixos do valor serem sorteados")
    void oDeslocamentoDeTrintaEDois() {
        Conjuntos.Sorteio sorteio = new Conjuntos.Sorteio(1);
        int[] porPosicao = new int[64];

        for (int i = 0; i < 64_000; i++) porPosicao[sorteio.ate(1 << 20) & 63]++;

        for (int posicao = 0; posicao < 64; posicao++)
            assertThat(porPosicao[posicao])
                    .as("posicao " + posicao + " dentro da palavra")
                    .isBetween(850, 1_150);

        long estado = 1;
        int alternancias = 0;
        int anterior = -1;

        for (int i = 0; i < 1_000; i++) {
            estado = estado * 6364136223846793005L + 1442695040888963407L;

            int baixo = (int) (estado & 1);

            if (anterior >= 0 && baixo != anterior) alternancias++;

            anterior = baixo;
        }

        assertThat(alternancias)
                .as("o ultimo bit do estado cru alterna em TODOS os passos")
                .isEqualTo(999);
    }

    @Test
    @DisplayName("as entradas sao o que dizem ser")
    void asEntradasSaoOQueDizem() {
        int universo = 1 << 20;

        assertThat(new PorCorridas(universo, Conjuntos.sorteado(universo, 1_000, 1))
                .elementosPorCorrida())
                .as("sorteado nao tem corrida")
                .isLessThan(1.1);

        assertThat(new PorCorridas(universo, Conjuntos.emBlocos(universo, 10_000, 1_024, 1))
                .elementosPorCorrida())
                .as("em blocos de mil tem corridas de mil")
                .isGreaterThan(500.0);

        Hibrido desigual = new Hibrido(universo, Conjuntos.desigual(universo, 60_000, 1));

        assertThat(desigual.blocosEmMapa() + desigual.blocosEmVetor())
                .as("desigual concentra tudo em poucos blocos")
                .isLessThanOrEqualTo(4);

        assertThat(Conjuntos.limpar(new int[] {3, 1, 3, 2})).containsExactly(1, 2, 3);
    }
}

package br.com.alura.screenmatch.main;

import br.com.alura.screenmatch.model.DadosEpisodio;
import br.com.alura.screenmatch.model.DadosSerie;
import br.com.alura.screenmatch.model.DadosTemporada;
import br.com.alura.screenmatch.model.Episodio;
import br.com.alura.screenmatch.service.ConsumoApi;
import br.com.alura.screenmatch.service.ConverteDados;

import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class Main {

    private Scanner leitura = new Scanner(System.in);
    private static final String ENDERECO = "https://www.omdbapi.com/?t=";
    private static final String API_KEY = "&apikey=938df9ca";
    private ConsumoApi consumo = new ConsumoApi();
    private ConverteDados conversor = new ConverteDados();

    public void exibeMenu() {
        System.out.println("Digite o nome da série para busca: ");
        var nomeSerie = leitura.nextLine();
        try {
            String nomeSerieCodificado = URLEncoder.encode(nomeSerie, StandardCharsets.UTF_8.toString());
            String url = ENDERECO + nomeSerieCodificado + API_KEY;
            var json = consumo.obterDados(url);
            DadosSerie dados = conversor.obterDados(json, DadosSerie.class);
            System.out.println(dados);

            List<DadosTemporada> temporadas = new ArrayList<>();

            for (int i = 1; i <= dados.totalTemporadas(); i++) {
                var urlTemporadas = ENDERECO + nomeSerieCodificado + "&season=" + i + API_KEY;
                var jsonTemporadas = consumo.obterDados(urlTemporadas);
                DadosTemporada dadosTemporada = conversor.obterDados(jsonTemporadas, DadosTemporada.class);
                temporadas.add(dadosTemporada);
            }
            temporadas.forEach(System.out::println);

            // Imprimindo os episódios de cada temporada
            temporadas.forEach(t -> t.episodios().forEach(e -> System.out.println(e.titulo())));


            // Exibindo os 10 episódios com maior avaliação
            System.out.println("Top 10 episódios com maior avaliação:");
            List<DadosEpisodio> dadosEpisodios = temporadas.stream()
                    .flatMap(t -> t.episodios().stream())
                    .collect(Collectors.toList());
            dadosEpisodios.stream()
                    .filter(e -> !e.avaliacao().equalsIgnoreCase("N/A"))
                    .sorted(Comparator.comparing(DadosEpisodio::avaliacao).reversed())
                    .limit(10)
                    .map(e -> e.titulo().toUpperCase())
                    .forEach(System.out::println);

            // Exibindo os episódios com data de lançamento
            List<Episodio> episodios = temporadas.stream()
                    .flatMap(t -> t.episodios().stream().
                            map(d -> new Episodio(t.numero(), d))
                    ).collect(Collectors.toList());
            System.out.println("Lista de todos episódios por temporada:");
            episodios.forEach(System.out::println);
            System.out.println("A partir de que ano você deseja ver os episódios?");
            var ano = leitura.nextInt();
            leitura.nextLine();

            LocalDate dataBusca = LocalDate.of(ano, 1, 1);
            DateTimeFormatter formatador = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            System.out.println("Episódios lançados após " + dataBusca + ":");

            episodios.stream()
                    .filter(e -> e.getDataLancamento() != null &&
                            e.getDataLancamento().isAfter(dataBusca))
                    .sorted(Comparator.comparing(Episodio::getDataLancamento))
                    .forEach(e -> System.out.println(
                            "Temporada: " + e.getTemporada() +
                                    ", Episódio: " + e.getTitulo() +
                                    ", Data de lançamento: " + e.getDataLancamento().format(formatador)
                    ));

            // Filtrando qual a temporada é determinado episódio
            System.out.println("Qual o nome do episódio que você deseja buscar?");
            var nomeEpisodio = leitura.nextLine();
            Optional<Episodio> episodioBuscado = episodios.stream()
                    .filter(e -> e.getTitulo() != null &&
                            e.getTitulo().toUpperCase().contains(nomeEpisodio.toUpperCase()))
                    .findFirst();
            if (episodioBuscado.isPresent()) {
                System.out.println("Episódio encontrado!");
                System.out.println("Temporada: " + episodioBuscado.get().getTemporada());
            } else {
                System.out.println("Episódio não encontrado.");
            }

            // Média de avaliação por temporada
            System.out.println("Média de avaliação por temporada:");
            System.out.println("Qual temporada deseja calcular a média?");
            var temporadaEscolhida = leitura.nextInt();
            leitura.nextLine();
            Map<Integer, Double> avaliacoesPorTemporada = episodios.stream()
                    .filter(e -> e.getTemporada() == temporadaEscolhida &&
                            e.getAvaliacao() > 0.0)
                    .collect(Collectors.groupingBy(
                            Episodio::getTemporada,
                            Collectors.averagingDouble(Episodio::getAvaliacao)
                    ));
            DecimalFormat formatMedia = new DecimalFormat("#.##");
            System.out.println("Temporada " + temporadaEscolhida +
                    " - Média de avaliação: " + formatMedia.format(avaliacoesPorTemporada.get(temporadaEscolhida)));

            // Calculando estatisticas com a classe DoubleSUmmaryStatistics
            DoubleSummaryStatistics estatistica = episodios.stream()
                    .filter(e -> e.getAvaliacao() > 0.0)
                    .collect(Collectors.summarizingDouble(Episodio::getAvaliacao));

            System.out.println("Estatísticas de avaliação:");
            System.out.println("Total de episódios: " + estatistica.getCount());
            System.out.println("Média: " + formatMedia.format(estatistica.getAverage()));
            System.out.println("Pior Episódio: " + formatMedia.format(estatistica.getMin()));
            System.out.println("Melhor episódio: " + formatMedia.format(estatistica.getMax()));

        } catch (Exception e) {
            System.out.println("Erro ao buscar a série: " + e.getMessage());
        }
    }
}

import java.net.URI; //identificador de recurso que nesse caso está representando o endereço da API
import java.net.URLEncoder; //classe que ajuda na codificação de texto para ser utilizado em parametros de URL
import java.net.http.HttpClient; //classe usada para fazer comunicacao HTTP
import java.net.http.HttpRequest; //representa uma requisicao HTTP, que ainda estou preparando
import java.net.http.HttpResponse; //traz a resposta HTTP depois da requisicao feita
import java.nio.charset.StandardCharsets; //disponibiliza padroes de codificacao de caracteres
import java.util.Scanner; //classe usada para receber informações dos usuários

//os imports da biblioteca gson, que exportei manualmente por nao ter usado o maven
import com.google.gson.JsonArray; //me permite utilizar array
import com.google.gson.JsonObject; //me permite trabalhar com objetos json
import com.google.gson.JsonParser; //o parser vai interpretar o texto json recebido

public class Main {

    public static void main(String[] args) throws Exception { //*pensar em possiveis erros e tratar sem o exception

        //aqui criei o objeto cliente do tipo httpcliente é por meio desse objeto que farei comunicacao http com as APIs e também as requisições
        HttpClient cliente = HttpClient.newHttpClient(); //aqui criei o objeto cliente do tipo httpcliente e  por meio desse objeto farei as requisições

        //criei o leitor que recebe o que o usário digita no terminal
        Scanner leitor = new Scanner(System.in);

        System.out.println("~~~~~~~~~~ClimaBR~~~~~~~~~~");
        System.out.print("Digite uma cidade do Brasil: "); //usei print para o java nao pular automaticamnete a linha 

        String nomeDigitado = leitor.nextLine().trim(); //nextLine para ler a linha toda, trim remove os espaços em branco do começo e do final e a Sting guarda o nome digitado

        //relembrando que: o if executa um bloco quando a condição for verdadeira
        if (nomeDigitado.isEmpty()) {
            System.out.println("Você precisa digitar uma cidade.");
            return;
        }

        //utilizei aqui a biblioteca URLEnconder para codificar o nome no padrao UTF com segurança e nao dar erro
        String nomeCodificado = URLEncoder.encode(nomeDigitado, StandardCharsets.UTF_8);

        //aqui montei a URL da API geocoding que pesquisa a cidade
        String enderecoGeoCoding = "https://geocoding-api.open-meteo.com/v1/search" //GeoCoding guarda o endereco da API
            + "?name=" + nomeCodificado //o + indica o que vem depois da String, o ? inicio do parametro de consulta
                + "&count=10" // o & faz uma separacao dos parametros anteriores
                + "&language=pt"
                + "&format=json" // solicita a resposta em formato json
                + "&countryCode=BR"; 

        //aqui uma preparaçãp para a primeira requisição GET de buscar a cidade
        HttpRequest requisicaoLocalizacao = HttpRequest.newBuilder() // inicia a construçao de requisiçao que vai ficar guardada em requisicaoLocalizacao
                .uri(URI.create(enderecoGeoCoding)) //define o endereço de destino
                .GET() //define o método HTTP como GET
                .build(); //constrói e devolve o objeto HttpRequest

        // Envia a primeira requisição e recebe a resposta
        HttpResponse<String> respostaLocalizacao = cliente.send(requisicaoLocalizacao, HttpResponse.BodyHandlers.ofString());

        // Interpreta o JSON retornado pela Geocoding API
        JsonObject jsonLocalizacao = JsonParser
                .parseString(respostaLocalizacao.body())
                .getAsJsonObject();

        // Pega o array que contém as localidades encontradas
        JsonArray resultados = jsonLocalizacao.getAsJsonArray("results");

        // Verifica se a pesquisa encontrou alguma localidade
        if (resultados == null || resultados.size() == 0) {
            System.out.println("Nenhuma cidade encontrada.");
            return;
        }

        // Por enquanto, utiliza o primeiro resultado da pesquisa
        JsonObject cidade = resultados.get(0).getAsJsonObject();

        // Extrai as informações da cidade encontrada
        String nomeCidade = cidade.get("name").getAsString();
        String estado = cidade.has("admin1")
                ? cidade.get("admin1").getAsString()
                : "";
        String pais = cidade.get("country").getAsString();

        double latitude = cidade.get("latitude").getAsDouble();
        double longitude = cidade.get("longitude").getAsDouble();

        String fusoHorario = cidade.get("timezone").getAsString();

        System.out.println();
        System.out.println("Cidade encontrada: " + nomeCidade);
        System.out.println("Estado: " + estado);
        System.out.println("País: " + pais);

        // Codifica o fuso horário para utilizá-lo na URL
        String fusoCodificado = URLEncoder.encode(
                fusoHorario,
                StandardCharsets.UTF_8
        );

        // Monta o endereço da Weather Forecast API com as
        // coordenadas encontradas pela primeira requisição
        String enderecoClima =
                "https://api.open-meteo.com/v1/forecast"
                + "?latitude=" + latitude
                + "&longitude=" + longitude
                + "&current=temperature_2m"
                + "&timezone=" + fusoCodificado;

        // Prepara a segunda requisição GET: buscar o clima
        HttpRequest requisicaoClima = HttpRequest.newBuilder()
                .uri(URI.create(enderecoClima))
                .GET()
                .build();

        // Envia a segunda requisição e recebe a resposta
        HttpResponse<String> respostaClima = cliente.send(
                requisicaoClima,
                HttpResponse.BodyHandlers.ofString()
        );

        // Interpreta o JSON retornado pela Weather Forecast API
        JsonObject jsonClima = JsonParser
                .parseString(respostaClima.body())
                .getAsJsonObject();

        // Acessa o objeto que contém os dados meteorológicos atuais
        JsonObject current = jsonClima.getAsJsonObject("current");

        // Extrai a temperatura e o horário
        double temperatura = current.get("temperature_2m")
                .getAsDouble();

        String horario = current.get("time")
                .getAsString();

        // Mostra os resultados no terminal
        System.out.println();
        System.out.println("===== RESULTADO DO CLIMA =====");
        System.out.println("Cidade: " + nomeCidade + " - " + estado);
        System.out.println("Horário da medição: " + horario);
        System.out.println("Temperatura atual: " + temperatura + " °C");
    }
}

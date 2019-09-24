package ch.erard22.booklookup;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    private static HttpClient httpClient = HttpClient.newBuilder().build();
    private static Pattern pattern = Pattern.compile("<average_rating>(\\S+)</average_rating>");
    private static Path outputPath = Paths.get("ratings.txt");

    public static void main(String[] args) throws IOException, InterruptedException {
        String apiKey = System.getProperty("api.key", "notSet");
        Scanner scanner = new Scanner(new File("short.txt"));

        createOutputFile();

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.trim().length() == 0) {
                writeEntry(outputPath, "", "");
            } else {
                HttpResponse<String> response = callGoodreads(apiKey, line);

                if (response.statusCode() != 200) {
                    writeEntry(outputPath, line, "0.00");
                } else {
                    Optional<String> rating = extractRating(response);
                    rating.ifPresent(s -> writeEntry(outputPath, line, s));
                }

                Thread.sleep(1000);
            }
        }
    }

    private static Optional<String> extractRating(HttpResponse<String> response) {
        Matcher m = pattern.matcher(response.body());
        String rating = null;
        if (m.find()) {
            rating = m.group(1);
        }
        return Optional.ofNullable(rating);
    }

    private static HttpResponse<String> callGoodreads(String apiKey, String line) throws IOException, InterruptedException {
        String escaped = line.replaceAll(" ", "+");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://www.goodreads.com/book/title.xml?key=" + apiKey + "&title=" + escaped.trim()))
                .header("Content-Type", "application/json")
                .GET()
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static void createOutputFile() throws IOException {
        try {
            Files.createFile(outputPath);
        } catch (FileAlreadyExistsException e) {}
    }

    private static void writeEntry(Path path, String name, String rating) {
        try {
            String s = name + ";" + rating + System.lineSeparator();
            System.out.print(s);
            Files.writeString(path, s, StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

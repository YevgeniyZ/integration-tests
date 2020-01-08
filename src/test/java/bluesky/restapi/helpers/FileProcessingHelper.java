package bluesky.restapi.helpers;

import com.opencsv.CSVWriter;
import io.qameta.allure.Step;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

public class FileProcessingHelper {

    public static final String SEPARATOR = System.getProperty("file.separator");

    public static String getProperty(String propertyName) throws IOException {

        Path path = Paths.get(String.format("src%stest%sresources%sBlueSky.IntegrationTests.properties", SEPARATOR, SEPARATOR, SEPARATOR));
        String part2 = null;

        try (Scanner scanner = new Scanner(path)) {
            while (scanner.hasNextLine()) {
                //process each line
                String line = scanner.nextLine();
                //split properties by '='
                if (line.startsWith(propertyName)) {
                    String[] parts = line.split("=");
                    part2 = parts[1];
                   // break;
                }
            }
        }
            return part2;
    }

    @Step("Create new csv file {0} and add data {1}")
    public static void createCsvFileWithData(String fileName, List<String[]> data){

        // first create file object for file placed at location
        // specified by filepath
        File file = new File(fileName);

        try {
            // create FileWriter object with file as parameter
            FileWriter outputFile = new FileWriter(file);

            // create CSVWriter object filewriter object as parameter

            CSVWriter writer = new CSVWriter(outputFile, ',',
                    CSVWriter.NO_QUOTE_CHARACTER,
                    CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                    CSVWriter.DEFAULT_LINE_END);

            // write data to .csv file
            writer.writeAll(data);

            // closing writer connection
            writer.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
      }

      @Step("Delete file {0}")
      public static void deleteFile(String fileName){
          try {
              Files.deleteIfExists(Paths.get(fileName));
          } catch (IOException e) {
              e.printStackTrace();
          }
      }
}


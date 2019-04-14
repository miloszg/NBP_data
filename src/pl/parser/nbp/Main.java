package pl.parser.nbp;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Array;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import static java.lang.System.exit;

class MainClass {

    public static void main(String[] args) throws ParserConfigurationException {
        float buy_value=0;
        float sale_value=0;
        ArrayList<Float> saleValuesArray=new ArrayList<>();
        String[] CURRENCIES={"USD","EUR","CHF","GBP"};
        List<String> listCurrency=Arrays.asList(CURRENCIES);
        DateTimeFormatter formatter=DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // Starting using params
        String currencyCode=args[0];
        if(!listCurrency.contains(currencyCode.toUpperCase())) {
            System.out.println("Wrong currency code! Program will shutdown!");
            exit(1);
        }
        String date_beg=args[1];
        if(!date_beg.matches("\\d{4}-\\d{2}-\\d{2}")) {
            System.out.println("Wrong date format! Program will shutdown!");
            exit(1);
        }
        LocalDate startingDate=LocalDate.parse(args[1],formatter);

        String date_end=args[2];
        if(!date_end.matches("\\d{4}-\\d{2}-\\d{2}")) {
            System.out.println("Wrong date format! Program will shutdown");
            exit(1);
        }

        LocalDate endingDate=LocalDate.parse(args[2],formatter);

        //Days between Starting and Ending Date
        float daysBetween = ChronoUnit.DAYS.between(startingDate, endingDate)+1;

        for(int i=0;i<daysBetween;i++) {
            //Getting dir.txt with xml table names
            String line = "";
            try {
                String url_text = "http://www.nbp.pl/kursy/xml/dir" + startingDate.getYear() + ".txt";
                URL url = new URL(url_text);
                BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
                int year=startingDate.getYear() - 2000;
                String regex_text = "c...z" + String.format("%02d",year) + String.format("%02d", startingDate.getMonthValue()) + String.format("%02d", startingDate.getDayOfMonth());
                while ((line = in.readLine()) != null) {
                    if (Pattern.matches(regex_text, line)) {
                        break;
                    }
                }
                in.close();
            } catch (MalformedURLException e) {
                System.out.println("Malformed URL: " + e.getMessage());
            } catch (IOException e) {
                System.out.println("I/O Error: " + e.getMessage());
            }

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = dbf.newDocumentBuilder();

            //Reading xml data and parsing
            try {
                URL url = new URL("http://www.nbp.pl/kursy/xml/" + line + ".xml");
                InputStream stream = url.openStream();
                Document doc = builder.parse(stream);
                doc.normalize();

                NodeList rootNodes = doc.getElementsByTagName("tabela_kursow");
                Node rootNode = rootNodes.item(0);
                Element rootElement = (Element) rootNode;
                NodeList rootList = rootElement.getElementsByTagName("pozycja");
                for (int j = 0; j < rootList.getLength(); j++) {
                    Node currency = rootList.item(j);
                    Element currencyElement = (Element) currency;
                    String xmlCurrencyCode = ((Element) currency).getElementsByTagName("kod_waluty").item(0).getTextContent();

                    if (xmlCurrencyCode.equals(currencyCode.toUpperCase())) {
                        String stringBuyRate = currencyElement.getElementsByTagName("kurs_kupna").item(0).getTextContent().replace(",", ".");
                        buy_value += Float.parseFloat(stringBuyRate);
                        String stringSaleRate = currencyElement.getElementsByTagName("kurs_sprzedazy").item(0).getTextContent().replace(",", ".");
                        saleValuesArray.add(Float.parseFloat(stringSaleRate));
                        sale_value += Float.parseFloat(stringSaleRate);
                    }
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                daysBetween--;
                e.printStackTrace();
            } catch (SAXException e) {
                e.printStackTrace();
            }
            startingDate=startingDate.plusDays(1);
        }
        float sum=0;
        for(int k=0;k<saleValuesArray.size();k++){
            sum+=Math.pow((saleValuesArray.get(k)-(sale_value/daysBetween)),2);
        }
        System.out.println("Average currency buy rate:          "+String.format("%.4f",buy_value/daysBetween));
        System.out.println("Standard deviation of sales rates:  "+String.format("%.4f",Math.sqrt(sum/daysBetween)));
    }
}
